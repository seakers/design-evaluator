#!/bin/bash

declare -a QueueNames=("vassar_request" "vassar_response" "user_request" "user_response")
for val in ${QueueNames[@]}; do
    sudo aws sqs create-queue --endpoint http://localhost:9324 --queue-name "$val"
done