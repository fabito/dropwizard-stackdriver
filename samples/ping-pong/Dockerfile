FROM openjdk:8-jre-alpine

MAINTAINER Fábio Uechi

ARG environment=prd
ARG BUILD_DATE
ARG VCS_REF
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.license="BSD" \
      org.label-schema.name="Ping pong sample" \
      org.label-schema.url="" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="" \
      org.label-schema.vcs-type="Git"


RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
ONBUILD ADD . /usr/src/app
ONBUILD RUN mvn install


COPY config/config.yml /opt/deskdrop/people-api/config.yml
COPY build/installShadow/people-rest /opt/deskdrop/people-api/

EXPOSE 8080 8081

WORKDIR /opt/deskdrop/people-api

ENV JAVA_OPTS="-Xms256M -Xmx256M"

CMD java ${JAVA_OPTS} -jar lib/people-rest-all.jar server config.yml
