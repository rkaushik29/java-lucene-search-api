# OpenWebSearch API

The OpenWebSearch API is an application created to access the Open Web Index and send queries to it through a REST API. It returns search results from the Parquet files that contain metadata associated with the URLs that are indexed. 

Another component of this package is the Index Converter [Created by @gijshendriksen] which converts a CIFF index to an Apache Lucene index, using Anserini tools. Find out more about the working of the standalone application [here](https://github.com/informagi/lucene-ciff). This component allows users to insert a CIFF index in the `resources/` folder and convert it into a Lucene index.

### Pre-requisites
- Install Java (JDK 11+)
- Install Python (3.8+)
- Install Maven
- Ensure that the latest version of `pip` is installed: 
```
python3 -m ensurepip --upgrade
python3 -m pip install --upgrade pip
```
- Install Lucene (from the Lucene official website)
- Install the required Python3.10+ dependencies: `pip install parquet-tools`.
- Add the following jar files to the Classpath: `lucene-core-{version}.jar` and `lucene-demos-{version}.jar` by using the following command `export CLASSPATH=/full/path/to/{lucene1}.jar:/full/path/to/{lucene2}.jar:$CLASSPATH`.
- Include the Lucene index in the directory `resources/`.
- Include the Parquet file with the same name as the index in `resources/`. The fileame should be : `<your-index-name>.parquet.gz`. If it differs, modify `scripts/start_api.sh`.

- Install [Anserini](https://github.com/castorini/anserini) [only for index converter]
- Install [ciff tools](https://github.com/osirrc/ciff.git) [only for index converter]

### Running the API
- Enter the scripts directory `cd scripts`.
- Run the API using the following command: `./start_api.sh <YOUR_INDEX_NAME> <YOUR_API_PORT> <YOUR_CORS_ENDPOINT>`, where the index name corresponds to the name of your lucene index directory and parquet file in the resources folder of the API.
- The `API_PORT` and `CORS_ENDPOINT` parameters are optional, and are set to `8000` and `http://localhost:3000/` respectively by default, for the purposes of this application.
- Example script call: `./start_api.sh websites-graz 8000 https://localhost:3000`

The server will be running on `port:8000` of your machine.

### Running the Index Converter
- Enter the scripts directory `cd scripts`.
- Run the converter using the following command: `./convert_index.sh <YOUR_CIFF_FILE_NAME> <CODEC_NAME>`. Ensure that you DO NOT include `.ciff` extention in `<YOUR_CIFF_FILE_NAME>`.
- `<CODEC_NAME>` is optional, and can be included if needed. If not specified, the default Lucene codec (i.e. the latest version) will be used. Find out more about the `SimpleText` CODEC [here](https://blog.mikemccandless.com/2010/10/lucenes-simpletext-codec.html).
- The converted index will be stores in `resources/` as a directory with the same name as above.

### Queries

- Open a web browser or use tools like cURL or Postman.
- Send a GET request to `http://localhost:8000/search?q=<your-query>`, where `<your-query>` is the query term of the search you want to perform.
- The API will return a response containing the query and a list of search results, containing the URL along with the text metadata.


### Notes about the API
- This API was designed to be a backend for the OWS SearchApp. To make it more generic, the manner in which index results are interpreted need to be analyzed. For example, the search results here give URLs which the App directly uses. Other indexes searched using this may return an ID which will need further querying in parquet files or databases.
- This can be used as an API to create search applications over the internet, and can be modified to access metadata in alternate Parquet formats.
- This API uses a Lucene `StandardAnalyzer` to read the index. If the index is not being read, it could be due to a different analyzer being used while generating the index.
- The Java API retrieves metadata from the Parquet files by querying the file. Therefore, if the schema of the Parquet file is different from that of the websites-graz.parquet.gz file, then the API will need to be modified (Until a standard Parquet format is agreed upon).
- In case the parquet metadata is not needed, simply comment out the code after `PARQUET METADATA INCLUSION` and the API will work for any Lucene index.
- If the Parquet metadata needs to be stored in the response, then modify the `PARQUET METADATA INCLUSION` section to match the schema of your Parquet file for queries.
- The Parquet file is converted to a CSV internally before the API launches, and CSV querying libraries are used within the above section.

### Improvements to the API
- It is also useful to perhaps change the API to search over more than one index, if multiple indexes are provided to the API. This can be done by looping over all index names that the user has provided, creating index readers for each, searching and storing the result in a data structure in each iteration. Finally, return the top results from the whole search. 
- The API needs to be hosted on a server eventually, and allow users to upload CIFF and Parquet files to it before performing search via the API. 
