FROM amazoncorretto:21

RUN yum update -y && yum install -y curl
RUN mkdir /app

COPY docker/build/libs/ /app/

ENTRYPOINT ["java","-jar","/app/docker-all.jar"]