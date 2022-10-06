#!/bin/bash
INSTANCE_ID=$(wget -q -O - http://169.254.169.254/latest/meta-data/instance-id)
RESOURCE_TYPE_TAGS=$(aws ec2 describe-tags --filters "Name=resource-id, Values=${INSTANCE_ID}" "Name=key, Values=RESOURCE_TYPE" --region=us-east-2)
RESOURCE_TYPE=$(echo ${RESOURCE_TYPE_TAGS} | jq -r '.Tags[].Value')
echo $RESOURCE_TYPE