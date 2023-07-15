#!/bin/bash

index="$1"
codec="${2:-[CODEC]}"
api_port="${3:-8000}"
cors_endpoint="${4:-http://localhost:3000/}"

source convert_index.sh $index $2
source ../scripts/start_api.sh $index $api_port $cors_endpoint
