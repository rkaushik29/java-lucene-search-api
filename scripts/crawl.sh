#!/bin/bash

link="$1"
result=$(python3 link_crawler.py "$link")
echo "$result"