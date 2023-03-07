#!/bin/bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
QUEUE_URL="http://localhost:9324/000000000000/$1"
MSGPATH="file://${SCRIPTPATH}/$2"


sudo aws sqs purge-queue --queue-url "$QUEUE_URL"