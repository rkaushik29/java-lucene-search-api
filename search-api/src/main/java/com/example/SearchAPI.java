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
// import org.apache.parquet.hadoop.ParquetReader;
// import org.apache.parquet.hadoop.example.GroupReadSupport;
// import org.apache.parquet.hadoop.ParquetFileReader;
// import org.apache.parquet.hadoop.metadata.ParquetMetadata;
// import org.apache.parquet.schema.MessageType;
// import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.lucene.search.Query;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
// import java.lang.module.Configuration;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import java.util.HashSet;
import java.util.Set;
// import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SearchAPI {
    private static final String INDEX_PATH = "/Users/rohitkaushik/dev/tugraz/java-lucene-search-api/search-api/src/main/resources/graz";
    private static final String PRQ_PATH = "/Users/rohitkaushik/dev/tugraz/java-lucene-search-api/search-api/src/main/resources/websites-graz.parquet";

    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.allowHost("http://localhost:3000", "javalin.io");
                });
            });
        });

        app.start(8000);
        System.out.print("Running app\n");

        app.get("/search", SearchAPI::handleSearchRequest);
        app.exception(Exception.class, (e, ctx) -> {
        });
    }

    private static Query createQuery(String queryString) throws ParseException {
        QueryParser queryParser = new QueryParser("contents", new StandardAnalyzer());
        Query query = queryParser.parse(queryString);

        return query;
    }

    private static IndexSearcher createSearcher() throws IOException 
    {
        FSDirectory dir = FSDirectory.open(Paths.get(INDEX_PATH));
        IndexReader reader = DirectoryReader.open(dir);
        return new IndexSearcher(reader);
    }

    private static void handleSearchRequest(Context context) throws Exception {
        Query query = createQuery(context.queryParam("q"));

        // Create an IndexSearcher
        FSDirectory indexDir = FSDirectory.open(Paths.get(INDEX_PATH));

        // Index Test
        boolean indexExists = DirectoryReader.indexExists(indexDir);
        System.out.println("Index Exists: " + indexExists);

        IndexReader reader = DirectoryReader.open(indexDir);
        IndexSearcher searcher = createSearcher();
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

        // Prepare the response
        SearchResult result = new SearchResult(documents);

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String resp = ow.writeValueAsString(result);
        JsonObject jsonObject = new Gson().fromJson(resp, JsonObject.class);

        // Iterate through JSON obj, get links and extract path, use path to get text from parquet file and put it
        // into the result
        JsonArray resultsArray = jsonObject.getAsJsonArray("results");
        for (JsonElement element : resultsArray) {
            JsonObject resultObject = element.getAsJsonObject();
            JsonArray fieldsArray = resultObject.getAsJsonArray("fields");
            JsonObject fieldObject = fieldsArray.get(0).getAsJsonObject();

            // Get the link for path extraction, which is needed to get text from parquet file
            String charSequenceValue = fieldObject.get("charSequenceValue").getAsString();

            // String path = extractPath(charSequenceValue);

            // try (ParquetReader<GenericRecord> pq_reader = AvroParquetReader.<GenericRecord>builder(PRQ_PATH).build()) {
            //     GenericRecord record;
            //     while ((record = pq_reader.read()) != null) {
            //         // Perform your search operations or process the record as needed
            //         System.out.println(record);
            //     }
            // } catch (IOException e) {
            //     e.printStackTrace();
            // }

            // // Add the "text" field next to the "charSequenceValue" field
            // fieldObject.addProperty("text", "Some text");

            // Link Crawling
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("python", "/Users/rohitkaushik/dev/tugraz/java-lucene-search-api/scripts/link_crawler.py", charSequenceValue);
                Process process = processBuilder.start();
                
                // Read the output stream
                InputStream inputStream = process.getInputStream();
                BufferedReader io_reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = io_reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                // Wait for the process to complete
                int exitCode = process.waitFor();
                
                // Print the output or store it in a variable
                String io_result = output.toString();
                
                
                // Add text to fieldObject
                fieldObject.addProperty("text", io_result);

                io_reader.close();
                process.destroy();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        jsonObject.add("results", resultsArray);

        System.out.println(jsonObject);

        context.json(jsonObject);
        // String res = context.result();

        // Close the IndexReader
        reader.close();
    }

    static class SearchResult {
        private List<Document> results;

        public SearchResult(List<Document> results) {
            this.results = results;
        }

        public List<Document> getResults() {
            return results;
        }
    }

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

    public static JsonObject convertToJSON(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, JsonObject.class);
    }
}
