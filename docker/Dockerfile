FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get install -y curl
RUN mkdir /app

COPY docker/build/libs/ /app/

ENTRYPOINT ["java","-jar","/app/docker-all.jar"]