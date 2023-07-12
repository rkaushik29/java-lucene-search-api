# OpenWebSearch API

The OpenWebSearch API is an interface created for easy searching of Lucene indices, specifically those that are imported from CIFF indices. 

### Pre-requisites
- Install Java (JDK 11+)
- Install Python (3.8+)
- Install Maven
- Install Lucene (from the Lucene official website)
- Install the required Python3.10+ dependencies: `pip install bs4`.
- Add the following jar files to the Classpath: `lucene-core-{version}.jar` and `lucene-demos-{version}.jar` by using the following command `export CLASSPATH=/full/path/to/{lucene1}.jar:/full/path/to/{lucene2}.jar:$CLASSPATH`.
- Include the Lucene index in `search-api/src/main/resources`.
- Include the Parquet file with the same name as the index in `search-api/src/main/resources`. The fileame should be : `<your-index-name>.parquet.gz`. If it differs, modify `run_api.sh`.

### Running the API
- Enter the scripts directory `cd scripts`.
- Run the API using the following command : `./run_api.sh <YOUR_INDEX_NAME>`, where the index name corresponds to the name of your lucene index directory and parquet file in the resources folder of the API.

The server will be running on `port:8000` of your machine.

### Queries

- Open a web browser or use tools like cURL or Postman.
- Send a GET request to http://localhost:8000/search?q=<your-query>, where <your-query> is the search query you want to perform.
- The API will return a response containing the query and a list of search results, containing the URL along with the text metadata.


### Notes about the API
- This API was designed to be a backend for the OWS SearchApp. To make it more generic, the manner in which index results are interpreted need to be analyzed. For example, the search results here give URLs which the App directly uses. Other indices searched using this may return an ID which will need further querying in parquet files or databases.
- This API uses a Lucene `StandardAnalyzer` to read the index. If the index is not being read, it could be due to a different analyzer being used while generating the index.
- The Java API retrieved metadata from the Parquet files by querying the file. Therefore, if the schema of the Parquet file is different from that of the websites-graz.parquet.gz file, then the API will need to be modified.
- In case the parquet metadata is not needed, simply comment out the code after `PARQUET METADATA INCLUSION` and the API will work for any Lucene index.
- If the Parquet metadata needs to be stored in the response, then modify the `PARQUET METADATA INCLUSION` section to match the schema of your Parquet file for queries.
- The Parquet file is converted to a CSV internally before the API launches, and CSV querying libraries are used within the above section.
- CORS has been enabled for `port:3000` only - this must be kept in mind while developing further applications with this API. More CORS can be added if required.