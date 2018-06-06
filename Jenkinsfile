#!/usr/bin/env groovy

node('nimble-jenkins-slave') {

    stage('Clone and Update') {
        // slackSend 'Started build no. ${env.BUILD_ID} of ${env.JOB_NAME}'
        git(url: 'https://github.com/nimble-platform/catalog-service-backend.git', branch: env.BRANCH_NAME)
        sh 'git submodule init'
        sh 'git submodule update'
    }

    stage('Build Java') {
        sh 'mvn clean package -DskipTests'
    }

    stage('Build Docker') {
        sh 'docker build -t nimbleplatform/marmotta .'
    }

    if (env.BRANCH_NAME == 'staging') {
        stage('Build Docker') {
            sh '/bin/bash -xe util.sh docker-build-staging'
        }

        stage('Push Docker') {
            sh 'docker push nimbleplatform/marmotta:staging'
        }

        stage('Deploy') {
            sh 'ssh staging "cd /srv/nimble-staging/ && ./run-staging.sh marmotta"'
        }
    } else {
        stage('Deploy') {
            sh 'ssh nimble "cd /data/deployment_setup/prod/infra/marmotta/ && sudo ./run.sh deploy"'
        }
    }
}
