FROM tomcat:7-jre8
MAINTAINER Salzburg Research <nimble-srfg@salzburgresearch.at>

ARG DB_NAME
ARG DB_HOST
ARG DB_PORT
ARG DB_USER
ARG DB_PASSWORD
ARG TOMCAT_USER
ARG TOMCAT_PASSWORD

# copy application data
COPY ./docker/marmotta.xml /usr/local/tomcat/conf/Catalina/localhost/
COPY ./webapp/target/catalog-service.war /usr/share/marmotta/marmotta.war

# Marmotta configuration
ENV CONF_PATH /var/lib/marmotta/system-config.properties
ENV DB_NAME $DB_NAME
ENV DB_HOST $DB_HOST
ENV DB_PORT $DB_PORT
ENV DB_USER $DB_USER
ENV DB_PASSWORD $DB_PASSWORD

# Tomcat configuration
ENV TOMCAT_USER $TOMCAT_USER
ENV TOMCAT_PASSWORD $TOMCAT_PASSWORD

COPY ./entrypoint.sh /entrypoint.sh
ENTRYPOINT ["bash", "/entrypoint.sh"]