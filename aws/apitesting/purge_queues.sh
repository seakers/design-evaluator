#!/bin/bash

prefix="\"\""
queues=$(sudo aws sqs list-queues --queue-name-prefix "")
for q in ${queues[@]}
do
if [ "${q:0:1}" == "\"" ]; then
    x=${q:1:${#q}-3}
    if [ $x != "QueueUrls" ]; then
            echo $x
            sudo aws sqs purge-queue --queue-url $x
    fi
fi
done
}