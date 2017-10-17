#!/usr/bin/env bash

# create Marmotta configuration
if [ ! -f "$CONF_PATH" ]; then
    mkdir -p "$(dirname $CONF_PATH)"
    echo "security.enabled = false" > $CONF_PATH
    echo "database.type = postgres" >> $CONF_PATH
    echo "database.url = jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME?prepareThreshold=3" >> $CONF_PATH
    echo "database.user = $DB_USER" >> $CONF_PATH
    echo "database.password = $DB_PASSWORD" >> $CONF_PATH
	echo "kiwi.setup.database = true" >> $CONF_PATH
    echo "kiwi.path = /marmotta" >> $CONF_PATH
    echo "kiwi.context = http://nimble.uk-south.containers.mybluemix.net/marmotta/" >> $CONF_PATH
    echo "kiwi.host = http://nimble.uk-south.containers.mybluemix.net/marmotta" >> $CONF_PATH
	echo "kiwi.setup.host = true" >> $CONF_PATH

    echo "created new config file $CONF_PATH"
else
    echo "$CONF_PATH already exists"
fi

# create Tomcat configuration
TOMCAT_USERSFILE=/usr/local/tomcat/conf/tomcat-users.xml
echo "<?xml version='1.0' encoding='utf-8'?>" > $TOMCAT_USERSFILE
echo "<tomcat-users>" >> $TOMCAT_USERSFILE
echo "    <role rolename='manager-gui'/>" >> $TOMCAT_USERSFILE
echo "    <role rolename='manager-gui'/>" >> $TOMCAT_USERSFILE
echo "    <role rolename='manager-script'/>" >> $TOMCAT_USERSFILE
echo "    <user username='${TOMCAT_USER:-admin}' password='${TOMCAT_PASSWORD:-changeme}' roles='manager,manager-gui,manager-script'/>" >> $TOMCAT_USERSFILE
echo "</tomcat-users>" >> $TOMCAT_USERSFILE

echo "created new tomcat user file $TOMCAT_USERSFILE"


# start tomcat
catalina.sh run