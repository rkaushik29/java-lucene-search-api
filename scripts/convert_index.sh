#!/bin/bash

ciff_name="$1"
codec="${2:-[CODEC]}"

cd ../ciff-lucene-importer/
mvn compile exec:java -Dexec.mainClass="nl.ru.ciffimporter.CiffImporter" -Dexec.args="../resources/$1.ciff ../resources/$1/ $2"
