# Design Evaluator



### Graphql Schema

To generate the schema file necessary for graphql to compile, run the following command.

`gradle downloadApolloSchema`

If you need to change the graphql endpoint, this can be done in the `build.gradle` file




### Testing the evaluator

0. Make sure AWS services are running from daphne dir: docker compose --file docker-compose-aws.yaml --project-name daphne-cluster up -d
1. docker-compose up -d
2. dshell design_evaluator_llm
3. 