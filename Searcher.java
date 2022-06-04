import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

/** Simple command-line based search demo. */
public class Searcher {

  private Searcher() {}

  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.exit(0);
    }
    // input local file location of folder containing indexed files created from Indexer.java
    String index = "C:\\Users\\stude\\Desktop\\sample\\Index";
    String field = "contents";
    String queries = null;
    boolean raw = false;
    // regular search
//    String queryString = "computer";
    
    //wildcard query
//    String queryString = "te*t";
    
    //fuzzy query
//    String queryString = "roam~2";
    
   //phrase query 
//    String queryString = "\"apache lucene\"~5";

    //boolean search
//    String queryString = "\"networks\" AND \"protocol\"";


    //boosted search
    String queryString = "bernardino";
    
    int hitsPerPage = 10;
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new StandardAnalyzer();

    BufferedReader in = null;
    QueryParser parser = new QueryParser(field, analyzer);

      Query query = parser.parse(queryString);
      
      System.out.println("Searching for: " + query.toString(field));
      // (query, n results, Sort object)
      searcher.search(query, 100, Sort.RELEVANCE);
      doSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);    
      reader.close();
  }

  /**
   * This demonstrates a typical paging search scenario, where the search engine presents 
   * pages of size n to the user. The user can then go to the next page if interested in
   * the next hits.
   * 
   * When the query is executed for the first time, then only enough results are collected
   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
   * is executed another time and all hits are collected.
   * 
   */
  public static void doSearch(BufferedReader in, IndexSearcher searcher, Query query, 
                                     int hitsPerPage, boolean raw, boolean interactive) throws IOException {
 
    // Collect enough docs to show 5 pages
    TopDocs results = searcher.search(query, 5 * hitsPerPage);
    ScoreDoc[] hits = results.scoreDocs;
    
    int numTotalHits = (int) results.totalHits.value;
    System.out.println(numTotalHits + " total matching documents");

    int start = 0;
    int end = Math.min(numTotalHits, hitsPerPage);
      
      for (int i = start; i < end; i++) {
        Document doc = searcher.doc(hits[i].doc);
       String path = doc.get("path");
        if (path != null) {
          System.out.println((i+1) + ". " + path);
          String title = doc.get("title");
          if (title != null) {
            System.out.println("   Title: " + doc.get("title"));
          }
        } else {
          System.out.println((i+1) + ". " + "No path for this document");
        }
                  
      }
    }
  }