#!/usr/bin/env bash

if [ "$1" == "run" ]; then
    # build maven project
    mvn clean install -DskipTests

    # run docker compose
    docker-compose up --build

elif [ "$1" == "bx-deploy" ]; then

    # build maven project
    mvn clean install -DskipTests

    # create docker image on Bluemix
    bx ic build \
        -t registry.eu-gb.bluemix.net/semantic_mediator_container/marmotta-backend:latest \
        --build-arg DB_HOST=marmotta-db \
        --build-arg DB_PORT=5432 \
        --build-arg DB_USER=root \
        --build-arg DB_PASS=changeme .

    # stop and delete current container (necessary for update)
    echo "********* Stopping and removing old container *********"
    bx ic stop marmotta-backend
    bx ic wait marmotta-backend
    bx ic rm --force marmotta-backend

    # run marmotta
    echo "********* Starting new container *********"
    bx ic run --name marmotta-backend \
        -v /var/lib/marmotta \
        --link marmotta-db:marmotta-db \
        -p 8080:8080 \
        -v marmotta-settings:/var/lib/marmotta \
        --restart=unless-stopped \
        -m 2048 registry.eu-gb.bluemix.net/semantic_mediator_container/marmotta-backend

    # bind IP address to new container
    bx ic ip-bind 134.168.33.237 marmotta-backend

elif [ "$1" == "bx-init" ]; then

    # create volume for settings
    bx ic volume-create marmotta-settings

    # run postgres container
    bx ic run --name marmotta-db \
        -e "POSTGRES_PASSWORD=changeme" \
        -e "POSTGRES_USER=root" \
        -e "POSTGRES_DB=marmotta" \
        -p 5432:5432 \
        -m 256 registry.eu-gb.bluemix.net/semantic_mediator_container/postgre

fi