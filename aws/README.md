# AWS Fargate Implementation

 - The proper aws fargate implementation is on branch: `deployment`

### Build Task Image

- Note, these command should be ran from the root of the project

1. `docker build -f ./aws/Dockerfile -t apazagab/design-evaluator .`
2. `docker push apazagab/design-evaluator`

