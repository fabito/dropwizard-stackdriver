FROM openjdk:8-jre-alpine

MAINTAINER Fábio Uechi

ARG ARTIFACT_ID
ARG VERSION
ARG JAVA_OPTS="-Xms256M -Xmx256M"

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY config.yml /usr/src/app/config.yml
COPY build/installShadow/${ARTIFACT_ID}/lib/${ARTIFACT_ID}-${VERSION}-all.jar  /usr/src/app/app.jar

EXPOSE 8080 8081

CMD java ${JAVA_OPTS} -jar app.jar server config.yml
