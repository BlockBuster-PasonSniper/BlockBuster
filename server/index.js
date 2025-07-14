import express from "express";
import cors from "cors";
import fs from "fs";

const app = express();

app.use(cors());

app.use(express.static("public"));

app.get("/api/data", (req, res) => {
  fs.readFile("data.json", "utf8", (err, data) => {
    if (err) {
      console.error(err);
      res.status(500).send("Error reading data file");
      return;
    }
    res.json(JSON.parse(data));
  });
});

app.listen(3000);
