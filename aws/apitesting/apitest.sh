#!/bin/bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
QUEUE_URL="http://localhost:9324/000000000000/$1"
MSGPATH="file://${SCRIPTPATH}/$2"

sudo aws sqs send-message --endpoint http://localhost:9324 --queue-url "$QUEUE_URL" --cli-input-json "$MSGPATH"


# sudo ./apitest.sh "vassar_request" "connRequest.json"
# sudo ./apitest.sh "user-queue-request-19" "evalDesign2.json"
# sudo ./apitest.sh "user-queue-request-19" "d1.json"
