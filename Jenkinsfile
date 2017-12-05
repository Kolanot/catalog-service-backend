#!/usr/bin/env groovy

node('nimble-jenkins-slave') {

    stage('Clone and Update') {
        // slackSend 'Started build no. ${env.BUILD_ID} of ${env.JOB_NAME}'
        git(url: 'https://github.com/nimble-platform/catalog-service-backend.git', branch: env.BRANCH_NAME)
    }

    stage('Build Dependencies') {
        sh 'rm -rf common   '
        sh 'git clone https://github.com/nimble-platform/common'
        dir ('common') {
            sh 'mvn clean install'
        }
    }

    stage('Build Java') {
        sh 'mvn clean install -DskipTests'
    }

    stage('Build Docker') {
        sh 'mvn -f identity-service/pom.xml docker:build'
    }

    if (env.BRANCH_NAME == 'master') {
        stage('Deploy') {
            sh 'ssh nimble "cd /data/deployment_setup/prod/infra/marmotta/ && sudo ./run.sh deploy"'
        }
    }
}
