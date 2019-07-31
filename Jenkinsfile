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
                    sh "./gradlew build --info"
                }
            }
        }
        stage('Test') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
                      sh """
                        apk update && apk add docker
                        docker login --username ${login} --password '${password}' https://nexus.iroha.tech:19002
                        ./gradlew test --info
                       """
                    }
                }
            }
        }
        stage('Build artifacts') {
            steps {
                script {
                    if (env.BRANCH_NAME ==~ /(master|develop)/ || env.TAG_NAME) {
                        DOCKER_TAGS = ['master': 'latest', 'develop': 'develop']
                        withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
                          env.DOCKER_REGISTRY_URL = "https://nexus.iroha.tech:19002"
                          env.DOCKER_REGISTRY_USERNAME = "${login}"
                          env.DOCKER_REGISTRY_PASSWORD = "${password}"
                          env.TAG = env.TAG_NAME ? env.TAG_NAME : DOCKER_TAGS[env.BRANCH_NAME]
                          sh "./gradlew dockerPush"
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
