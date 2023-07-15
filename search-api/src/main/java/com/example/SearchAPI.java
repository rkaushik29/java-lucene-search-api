package com.example;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import com.fasterxml.jackson.databind.ObjectMapper; 
import com.fasterxml.jackson.databind.ObjectWriter; 
import org.apache.lucene.search.Query;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SearchAPI {
    private static final String CSV_PATH = "../scripts/search_pq.csv";

    public static void main(String[] args) {
        String indexPath = "../resources/" + args[0];
        Javalin app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.allowHost(args[2], "javalin.io");
                });
            });
        });

        app.start(Integer.parseInt(args[1]));
        System.out.print("Running app on port:" + args[1] + "\n");

        app.get("/search", ctx -> handleSearchRequest(ctx, indexPath));
        app.exception(Exception.class, (e, ctx) -> {
        });
    }

    // Function to create a query object from the query string
    // Contains field in the index you are searching while making a Query
    private static Query createQuery(String queryString) throws ParseException {
        QueryParser queryParser = new QueryParser("contents", new StandardAnalyzer());
        return queryParser.parse(queryString);
    }

    // Function to create index searcher from the file path of the index
    private static IndexSearcher createSearcher(String INDEX_PATH) throws IOException 
    {
        FSDirectory dir = FSDirectory.open(Paths.get(INDEX_PATH));
        IndexReader reader = DirectoryReader.open(dir);
        return new IndexSearcher(reader);
    }

    // Function to handle a search request sent to this API
    private static void handleSearchRequest(Context context, String INDEX_PATH) throws Exception {
        Query query = createQuery(context.queryParam("q"));

        // Create an IndexSearcher
        FSDirectory indexDir = FSDirectory.open(Paths.get(INDEX_PATH));

        // Index Test
        boolean indexExists = DirectoryReader.indexExists(indexDir);
        System.out.println("Index Exists: " + indexExists);

        IndexReader reader = DirectoryReader.open(indexDir);
        IndexSearcher searcher = createSearcher(INDEX_PATH);
        searcher.setSimilarity(new BM25Similarity());

        // Perform the search
        TopDocs topDocs = searcher.search(query, 10);
        ScoreDoc[] hits = topDocs.scoreDocs;
        System.out.println("Total Results: " + topDocs.totalHits  + "\n");

        // Collect the search results
        List<Document> documents = new ArrayList<>();
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Set<String> fieldsToLoad = new HashSet<>();
            fieldsToLoad.add("content");
            Document document = reader.document(docId);
            documents.add(document);
        }

        // Prepare the result object
        SearchResult result = new SearchResult(documents);

        // Convert result to JSON object
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String resp = ow.writeValueAsString(result);
        JsonObject jsonObject = new Gson().fromJson(resp, JsonObject.class);
        JsonArray resultsArray = jsonObject.getAsJsonArray("results");

        // --------------------------------- PARQUET METADATA INCLUSION ---------------------------------------------
        //         Comment this section out if Parquet metadata is not needed, or the file schema is unknown

        // Iterate through JSON obj, get links and extract path, use path to get text from parquet file and put it
        // into the result
        for (JsonElement element : resultsArray) {
            JsonObject resultObject = element.getAsJsonObject();
            JsonArray fieldsArray = resultObject.getAsJsonArray("fields");
            JsonObject fieldObject = fieldsArray.get(0).getAsJsonObject();

            // Get the links, to be queried on csv from parquet
            String charSequenceValue = fieldObject.get("charSequenceValue").getAsString();

            String urlPath = extractPath(charSequenceValue);
            String linkText = "";

            try (Reader csvReader = new FileReader(CSV_PATH);
                CSVParser csvParser = new CSVParser(csvReader, CSVFormat.DEFAULT)) {

                // Iterate over CSV records and perform queries
                for (CSVRecord csvRecord : csvParser) {
                    String csvPath = csvRecord.get(6); // Access urlPath column
                    
                    if (csvPath.equals(urlPath)) {
                        linkText = csvRecord.get(2);
                        fieldObject.addProperty("text", longestSequence(linkText).trim());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // --------------------------------------------------------------------------------------------------------

        // Add array with cralwed text to Json object
        jsonObject.add("results", resultsArray);

        // Return Data
        context.contentType("application/json");
        context.result(jsonObject.toString());

        // Close the IndexReader
        reader.close();
    }

    // Class definition for SearchResult object, where query results are initally stored.
    static class SearchResult {
        private List<Document> results;

        public SearchResult(List<Document> results) {
            this.results = results;
        }

        public List<Document> getResults() {
            return results;
        }
    }

    // Function to extract the path from the input, which must be a URL.
    // Eg: URL: https://www.wikipedia.org/Graz ; Res: /Graz
    public static String extractPath(String input) {
        int count = 0;
        int index = -1;
        
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '/') {
                count++;
                if (count == 3) {
                    index = i;
                    break;
                }
            }
        }
        
        if (index != -1 && index + 1 < input.length()) {
            return input.substring(index);
        } else {
            return ""; 
        }
    }

    // Function to convert a JSON string to a JSON object
    public static JsonObject convertToJSON(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, JsonObject.class);
    }

    // Function to return the longest sequence of characters in a string without a linebreak.
    public static String longestSequence(String input) {
        String[] sequences = input.split("\n");
        String longestSequence = "";

        for (String sequence : sequences) {
            if (sequence.length() > longestSequence.length()) {
                longestSequence = sequence;
            }
        }

        return longestSequence;
    }
}
