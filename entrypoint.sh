#!/usr/bin/env bash

# handle configuration
if [ ! -f "$CONF_PATH" ]; then
    mkdir -p "$(dirname $CONF_PATH)"
    echo "security.enabled = false" > $CONF_PATH
    echo "database.type = postgres" >> $CONF_PATH
    echo "database.url = jdbc:postgresql://$DB_HOST:$DB_PORT/marmotta?prepareThreshold=3" >> $CONF_PATH
    echo "database.user = $DB_USER" >> $CONF_PATH
    echo "database.password = $DB_PASS" >> $CONF_PATH

    echo "created new config file $CONF_PATH"
else
    echo "$CONF_PATH already exists"
fi

# start tomcat
catalina.sh run