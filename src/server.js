const express = require("express");
const app = express();
const path = require("path");

app.set('view engine', 'ejs');

app.use(express.static(path.join(__dirname, "public")));

app.get("/", function (req, res) {
    res.render("server");
  });

app.listen(3000, function () {
    console.log("Server is running on localhost3000");
});