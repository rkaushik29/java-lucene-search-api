# Java Lucene Search API

### Pre-requisites
- Install Java (JDK 11+)
- Install Maven
- Install Lucene (from the Lucene official website)
- Install the required Python dependencies: `pip install bs4`
- Add the following jar files to the Classpath: `lucene-core-{version}.jar` and `lucene-demos-{version}.jar` by using the following command `export CLASSPATH=/full/path/to/{lucene1}.jar:/full/path/to/{lucene2}.jar:$CLASSPATH`

### Setup
- Include the lucene index in `search-api/src/main/resources`
- Check if the index directory in the code corresponds to the location where the index is stored in your computer.
- Enter the project directory `cd search-api`
- Build the project using `mvn clean install`
- Run the project using `mvn compile exec:java -Dexec.mainClass="com.example.SearchAPI"`

The server will be running on `port:8000` of your machine. 

### Queries

- Open a web browser or use tools like cURL or Postman.
- Send a GET request to http://localhost:8000/search?q=<your_query>, where <your_query> is the search query you want to perform.
- The API will return a response containing the query and a list of search results.

