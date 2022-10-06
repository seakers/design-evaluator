#!/bin/bash
sudo service docker start

CONTAINER_RUNNING="$(docker ps -q -f name=resource)"
if [ "$CONTAINER_RUNNING" ]; then
    docker stop resource
fi

CONTAINER_EXISTS="$(docker ps -a -q -f name=resource)"
if [ "$CONTAINER_EXISTS" ]; then
    docker rm resource
fi

INSTANCE_ID=$(wget -q -O - http://169.254.169.254/latest/meta-data/instance-id)
RESOURCE_TYPE_TAGS=$(aws ec2 describe-tags --filters "Name=resource-id, Values=${INSTANCE_ID}" "Name=key, Values=RESOURCE_TYPE" --region=us-east-2)
RESOURCE_TYPE=$(echo ${RESOURCE_TYPE_TAGS} | jq -r '.Tags[].Value')

sudo $(sudo aws ecr get-login --region us-east-2 --no-include-email)
sudo docker pull 923405430231.dkr.ecr.us-east-2.amazonaws.com/"${RESOURCE_TYPE}":latest

. /home/ec2-user/run.sh