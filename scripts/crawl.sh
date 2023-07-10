#!/bin/bash

link="$1"
result=$(python3 /Users/rohitkaushik/dev/tugraz/java-lucene-search-api/scripts/link_crawler.py "$link")
echo "$result"