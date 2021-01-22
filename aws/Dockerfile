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


#######################
### Final Container ###
#######################

# - Environment variables are passed in the AWS Fargate task definition


FROM amazoncorretto:11
COPY --from=BUILD_TOOL /root/.m2 /root/.m2

WORKDIR /app
COPY /. /app/.


# -- DEPS --
WORKDIR /installs

RUN yum install git wget unzip tar -y

# -- GRADLE --
RUN wget https://services.gradle.org/distributions/gradle-6.0-bin.zip && \
    unzip gradle-6.0-bin.zip && \
    rm gradle-6.0-bin.zip && \
    yum remove git wget unzip tar -y
    

ENV PATH="/installs/gradle-6.0/bin:${PATH}"

# -- GRAPHQL SCHEMA --
WORKDIR /app
CMD gradle run
