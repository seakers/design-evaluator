#!/bin/bash
sudo service docker start

# --> 1. Check if container already running
CONTAINER_RUNNING="$(docker ps -q -f name=resource)"
if [ "$CONTAINER_RUNNING" ]; then
    echo 'CONTAINER ALREADY RUNNING'
    return
fi

# --> 2. Get env string
ENV_STRING=""
INSTANCE_ID=$(wget -q -O - http://169.254.169.254/latest/meta-data/instance-id)
RESOURCE_TYPE_TAGS=$(aws ec2 describe-tags --filters "Name=resource-id, Values=${INSTANCE_ID}" "Name=key, Values=RESOURCE_TYPE" --region=us-east-2)
RESOURCE_TYPE=$(echo ${RESOURCE_TYPE_TAGS} | jq -r '.Tags[].Value')
JSON_TAGS=$(aws ec2 describe-tags --filters "Name=resource-id,Values=${INSTANCE_ID}" --region=us-east-2)
for row in $(echo ${JSON_TAGS} | jq -c '.Tags[]'); do
        var_key=$(echo ${row} | jq -r '.Key')
        var_value=$(echo ${row} | jq -r '.Value')
        ENV_STRING+="--env ${var_key}=${var_value} "
done

# --> 3. Run container
CONTAINER_EXISTS="$(docker ps -a -q -f name=resource)"
if [ "$CONTAINER_EXISTS" ]; then
    sudo docker start resource
else
    sudo docker run -d --name=resource ${ENV_STRING} 923405430231.dkr.ecr.us-east-2.amazonaws.com/"${RESOURCE_TYPE}":latest
fi