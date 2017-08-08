# Catalog Service Backend
This repository represent the Apache Marmotta backend of the Catalog Microservice.


## Obtain Source

1. Either download the source code from [here](https://github.com/nimble-platform/catalog-service-backend) or
1. Clone the project with the command: `git clone --recursive https://github.com/nimble-platform/catalog-service-backend`

## How to build

1. Run `mvn clean install` from the command line

## How to run

1. Switch to the webapp directory
1. Run `mvn tomcat7:run`
1. Point your browser to `http://localhost:8084/catalog-service`

# Bluemix Deployment

General Info: https://console.ng.bluemix.net/docs/containers/container_cli_reference_cfic.html#container_cli_reference_cfic

1. Install Docker and login
2. Install Bluemix CLI and log in (http://clis.ng.bluemix.net/ui/home.html)
    * Example:`bx login -a https://api.eu-gb.bluemix.net -u <usermail> -o hrlbmix@il.ibm.com -s Nimble`    
3. Install the container plugin (https://console.ng.bluemix.net/docs/containers/container_cli_cfic_install.html#container_cli_cfic_install)
    * Example: `bx plugin install IBM-Containers -r Bluemix`
4. Run `./run.sh bx-deploy`
