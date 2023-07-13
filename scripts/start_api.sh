#!/bin/bash

index="$1"
api_endpoint="${2:-8000}"
cors_endpoint="${3:-http://localhost:3000/}"

parquet-tools csv ../resources/$index.parquet.gz > ../scripts/search_pq.csv
cd ../search-api/
mvn clean install
mvn compile exec:java -Dexec.mainClass="com.example.SearchAPI" -Dexec.args="$index $api_endpoint $cors_endpoint"
