FROM tomcat:7-jre8
MAINTAINER Salzburg Research <nimble-srfg@salzburgresearch.at>

# copy application
COPY ./docker/marmotta.xml /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml
COPY ./webapp/target/catalog-service.war /usr/share/marmotta/marmotta.war

COPY ./entrypoint.sh /entrypoint.sh
ENTRYPOINT ["bash", "/entrypoint.sh"]