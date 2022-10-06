#!/bin/bash
sudo service docker start

# --> 1. Check if container running
CONTAINER_RUNNING="$(docker ps -q -f name=resource)"
if [ ! "$CONTAINER_RUNNING" ]; then
    echo "CONTAINER NOT RUNNING"
    return
fi

# --> 2. Stop container
sudo docker stop resource