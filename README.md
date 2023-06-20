# Java Lucene Search API

### Pre-requisites
- Java (JDK 11+)
- Maven
- Lucene

### Setup
- Enter the project directory `cd search-api`
- Build the project using `mvn clean install`
- Run the project using `java -jar target/search-api-1.0-SNAPSHOT.jar`

The server will be running on `port:8000` of your machine. 

### Queries

- Open a web browser or use tools like cURL or Postman.
- Send a GET request to http://localhost:8000/search?q=<your_query>, where <your_query> is the search query you want to perform.
- The API will return a response containing the query and a list of search results.

