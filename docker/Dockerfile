FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build shadowJar

FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get install -y curl
RUN mkdir /app

COPY --from=build /home/gradle/src/docker/build/libs/ /app/

ENTRYPOINT ["java","-jar","/app/src-all.jar"]