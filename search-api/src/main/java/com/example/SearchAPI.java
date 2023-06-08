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
import org.apache.lucene.search.Query;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SearchAPI {
    private static final String INDEX_PATH = "/Users/rohitkaushik/dev/tugraz/java-lucene-search-api/search-api/src/main/resources/cranfield";

    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create().start(8000);
        System.out.print("Running app\n");

        app.get("/search", SearchAPI::handleSearchRequest);
    }

    private static Query createQuery(String queryString) throws ParseException {
        QueryParser queryParser = new QueryParser("content", new StandardAnalyzer());
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

        // Index Test - evaluates to True
        boolean indexExists = DirectoryReader.indexExists(indexDir);
        System.out.println("Index Exists: " + indexExists + "\n");

        IndexReader reader = DirectoryReader.open(indexDir);
        IndexSearcher searcher = createSearcher();

        searcher.setSimilarity(new BM25Similarity());

        // Perform the search
        TopDocs topDocs = searcher.search(query, 10);

        ScoreDoc[] hits = topDocs.scoreDocs;
        System.out.println("Total Results: " + topDocs.totalHits);

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
        context.json(result);

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
}
