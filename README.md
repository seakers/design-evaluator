# Design Evaluator


## AWS Deployment

- Registry `923405430231.dkr.ecr.us-east-2.amazonaws.com/brain`


### Commands


##### AWS Docker Login

`aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin 923405430231.dkr.ecr.us-east-2.amazonaws.com`


##### Local

`docker-compose build`

`docker-compose up -d`

`dshell evaluator`

`./gradlew run`


##### Dev

`docker build -f DockerfileDev -t apazagab/design-evaluator:latest .`

`docker run -it -d --entrypoint=/bin/bash --network=daphne-network --name=evaluator apazagab/design-evaluator:latest`


##### Prod

`docker build -f DockerfileProd -t 923405430231.dkr.ecr.us-east-2.amazonaws.com/design-evaluator:latest .`

`docker push 923405430231.dkr.ecr.us-east-2.amazonaws.com/brain`



### Graphql Schema

To generate the schema file necessary for graphql to compile, run the following command.

`gradle downloadApolloSchema`

If you need to change the graphql endpoint, this can be done in the `build.gradle` file