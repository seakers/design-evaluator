########################
### Build Containers ###
########################

## 1. SystemArchitectureProblems ##
FROM maven:3-jdk-11-slim AS BUILD_TOOL
WORKDIR /repos
RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get install git -y
RUN git clone https://github.com/seakers/SystemArchitectureProblems.git
RUN git clone https://github.com/seakers/orekit.git

WORKDIR /repos/SystemArchitectureProblems
RUN git fetch && git checkout jdk-11
RUN mvn install

WORKDIR /repos/orekit
RUN git checkout fov_changes
WORKDIR /repos/orekit/orekit
RUN mvn install

WORKDIR /repos/jars
COPY /jars/jess.jar /repos/jars/jess.jar
RUN mvn install:install-file -Dfile=./jess.jar -DgroupId=gov.sandia -DartifactId=jess -Dversion=7.1p2 -Dpackaging=jar


## 2. Graphql Schema: node ##
# FROM node:14-alpine AS Schema
# WORKDIR /schema
# RUN npm install -g apollo
# RUN apollo schema:download --endpoint https://172.18.0.7:8080/v1/graphql --header 'X-Hasura-Admin-Secret: daphne'



#######################
### Final Container ###
#######################


FROM amazoncorretto:11
# COPY --from=Schema /schema/schema.json /app/src/main/graphql/com/evaluator/schema.json
COPY --from=BUILD_TOOL /root/.m2 /root/.m2


# -- DEPS --
WORKDIR /installs

RUN yum update -y && \
    yum upgrade -y && \
    yum install git wget unzip tar -y

# -- GRADLE --
RUN wget https://services.gradle.org/distributions/gradle-6.0-bin.zip && \
    unzip gradle-6.0-bin.zip && \
    rm gradle-6.0-bin.zip
ENV PATH="/installs/gradle-6.0/bin:${PATH}"

# -- GRAPHQL SCHEMA --
WORKDIR /genetic-algorithm
# RUN gradle generateApolloSources
# CMD gradle run


















