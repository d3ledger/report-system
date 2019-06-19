pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
    }
    agent {
        docker {
            label 'd3-build-agent'
            image 'openjdk:8-jdk-alpine'
            args '-v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp'
        }
    }
    stages {
        stage('Build') {
            steps {
                script {
                    sh "#!/bin/sh\n./gradlew build --info"
                }
            }
        }
        stage('Test') {
            steps {
                script {
                    sh "#!/bin/sh\n./gradlew test --info"
                }
            }
        }
        stage('Build artifacts') {
            steps {
                script {
                    if (env.BRANCH_NAME ==~ /(master|develop)/) {
                        DOCKER_TAGS = ['master': 'latest', 'develop': 'develop']
                        TAG = DOCKER_TAGS[env.BRANCH_NAME]
                        withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
                          TAG = env.TAG_NAME ? env.TAG_NAME : env.BRANCH_NAME
                          env.DOCKER_REGISTRY_URL="https://nexus.iroha.tech:19002"
                          env.DOCKER_REGISTRY_USERNAME="${login}"
                          env.DOCKER_REGISTRY_PASSWORD="${password}"
                          env.TAG="${TAG}"
                          sh "#!/bin/sh\n./gradlew dockerPush"
                        }
                    }
                }
            }
        }
    }
    post {
        cleanup {
            cleanWs()
        }
    }
}
