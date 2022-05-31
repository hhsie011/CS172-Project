import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.*;
import java.net.MalformedURLException;
import java.net.URL;

public class Crawler implements Runnable {


    private static ConcurrentLinkedQueue<String> frontier = new ConcurrentLinkedQueue<String>();
    private static ConcurrentHashMap<String, String> crawledPages = new ConcurrentHashMap<String, String>(10000);
    private static ConcurrentHashMap<String, Vector<String>> crawledHosts = new ConcurrentHashMap<String, Vector<String>>(10000);

    public void run() {
        while (!frontier.isEmpty()) { 
            // Retrieve link from frontier
            String link = frontier.peek();
            frontier.remove(link);

            // Breakdown URLs
            String protocol = "";
            String host = "";
            String path = "";
            String file = "";

            if (link == null) {
                continue;
            }
            System.out.println(link);

            try {
                URL url = new URL(link);
                protocol = url.getProtocol();
                host = url.getHost();
                path = url.getPath();
                file = url.getFile();

                // Check protocol, discard if empty or not https or http
                if (!(protocol.equals("https") || protocol.equals("http")) || (protocol.length() == 0)) {
                    continue;
                }

                // Check if it's a .edu site/page
                if (host.endsWith(".edu") == false) {
                    continue;
                }

                // Check file type, discard if it's pdf or image
                if (file.endsWith(".pdf") || file.endsWith(".jpg") || file.endsWith(".jpeg") || file.endsWith(".png") || 
                    file.endsWith(".svg") || file.endsWith(".gif")) {
                    continue;
                }

                // Check host to see if it is a new site
                if (crawledHosts.containsKey(host) == false) {
                    // Unvisited site: read from robots.txt
                    try {
                        URL ethics = new URL(protocol + "://" + host + "/robots.txt");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(ethics.openStream()));
                        String line;
                        while (((line = reader.readLine()) != null) && !(line.startsWith("User-agent: *")));
                        Vector<String> disallowedPaths = new Vector<String>();
                        while (((line = reader.readLine()) != null) && !(line.startsWith("User-agent:"))) {
                            if (line.startsWith("Disallow:")) {
                                // Store disallowed paths into vector
                                line = line.replace("Disallow: ", ""); // Remove the "Disallow: " part
                                if (line.contains("*") == true) {
                                    // trim out * and onward for simplicity
                                    if (line.indexOf("*") >= 1) {
                                        line = line.substring(0, line.indexOf("*") - 1);
                                    }
                                }
                                if (line.length() > 0) {
                                    disallowedPaths.add(line);
                                }
                            }
                        }
                        reader.close();
                        // Mark host as having been crawled
                        crawledHosts.put(host, disallowedPaths);
                    }
                    catch (FileNotFoundException e) {
                        e.printStackTrace();
                        // Mark host as having been crawled
                        Vector<String> disallowedPaths = new Vector<String>();
                        crawledHosts.put(host, disallowedPaths);
                    }
                }
                else {
                    // Visited site: check if path is disallowed
                    Boolean allowed = true;
                    Vector<String> disallowedPaths = crawledHosts.get(host);
                    for (String dp : disallowedPaths) {
                        // Check for identical start
                        if (path.startsWith(dp) == true) {
                            allowed = false;
                            break;
                        }
                    }
                    // Discard and move on if disallowed
                    if (allowed == false) {
                        continue;
                    }
                }
                
                // Check for embedded links
                Document doc = Jsoup.connect(link).get();
                Elements links = doc.select("a[href]");
                for (Element l : links) {
                    String parsedLink = l.attr("abs:href");
                    // Add unparsed links to frontier
                    if ((crawledPages.containsKey(parsedLink) == false) && (frontier.contains(parsedLink) == false) && 
                        (parsedLink.equals(link) == false)) {
                        frontier.add(parsedLink);
                    }
                }

                // Download text content
                String source = doc.body().text();
                file = file.replace("/", "_");
                BufferedWriter writer = new BufferedWriter(new FileWriter("temp//" + host + file + ".txt"));
                writer.write(source);
                writer.close();

                crawledPages.put(link, "crawled");
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    

    public static void main(String[] args) {
        ArrayList<String> seeds = new ArrayList<String>();
        seeds.add("https://go.okstate.edu/");
        seeds.add("https://www.baylor.edu/");
        seeds.add("https://www.utexas.edu/");
        seeds.add("https://www.tamu.edu/");
        seeds.add("https://www.ttu.edu/");

        // Put seeds into frontier
        for (String s : seeds) {
            frontier.add(s);
        }
        
        // Set up multithreading
        Runnable crawler1 = new Crawler();
        Runnable crawler2 = new Crawler();

        Thread thread1 = new Thread(crawler1);
        Thread thread2 = new Thread(crawler2);

        thread1.start();
        thread2.start();
    }
}