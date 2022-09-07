########################
### Build Containers ###
########################

# Download and install dependencies not available on maven
FROM maven:3-jdk-11-slim AS BUILD_TOOL
WORKDIR /repos
RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get install git -y
RUN git clone https://github.com/seakers/SystemArchitectureProblems.git
RUN git clone https://github.com/seakers/orekit.git

## 1. SystemArchitectureProblems ##
WORKDIR /repos/SystemArchitectureProblems
RUN git fetch && git checkout jdk-11
RUN mvn install

## 2. Orekit ##
WORKDIR /repos/orekit
RUN git checkout fov_changes
WORKDIR /repos/orekit/orekit
RUN mvn install

## 3. JESS ##
WORKDIR /repos/jars
COPY /jars/jess.jar /repos/jars/jess.jar
RUN mvn install:install-file -Dfile=./jess.jar -DgroupId=gov.sandia -DartifactId=jess -Dversion=7.1p2 -Dpackaging=jar

# Compile and package the app
WORKDIR /app
COPY /gradle /app/gradle
COPY /src /app/src
COPY /build.gradle /app/build.gradle
COPY /settings.gradle /app/settings.gradle
COPY /gradlew /app/gradlew

RUN ./gradlew installDist



#######################
### Final Container ###
#######################

# - Environment variables are passed in the AWS Fargate task definition
FROM amazoncorretto:11
COPY --from=BUILD_TOOL /app/build/install/evaluator /app/evaluator

# Debug stuff
RUN yum install -y procps htop top free

# -- Set default directory for running --
WORKDIR /app/evaluator
CMD ./bin/evaluator