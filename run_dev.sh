#!/bin/sh
echo 'RUNNING PROGRAM'
cd /app/evaluator
env $(cat /env/.env | tr -d '\r') ./bin/evaluator
