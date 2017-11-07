FROM tomcat:7-jre8
MAINTAINER Salzburg Research <nimble-srfg@salzburgresearch.at>

ARG TOMCAT_USER
ARG TOMCAT_PASSWORD

# copy application
COPY ./docker/marmotta.xml /usr/local/tomcat/conf/Catalina/localhost/
COPY ./webapp/target/catalog-service.war /usr/share/marmotta/marmotta.war

# Tomcat configuration
ENV TOMCAT_USER $TOMCAT_USER
ENV TOMCAT_PASSWORD $TOMCAT_PASSWORD

COPY ./entrypoint.sh /entrypoint.sh
ENTRYPOINT ["bash", "/entrypoint.sh"]