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
                    // TODO: Remove wrapper installation as soon as PR#21 is merged
                    sh "apk --no-cache add gradle"
                    sh "gradle wrapper"
                    sh "./gradlew build --info"
                }
            }
        }
        stage('Test') {
            steps {
                script {
                    sh "./gradlew test --info"
                }
            }
        }
        stage('Build artifacts') {
            steps {
                script {
                    if (env.BRANCH_NAME ==~ /(master|develop)/) {
                        DOCKER_TAGS = ['master': 'latest', 'develop': 'develop']
                        TAG = DOCKER_TAGS[env.BRANCH_NAME]
                        JARS = ['data-collector', 'report-service']
                        sh """
                            apk --no-cache add docker
                        """
                        withDockerRegistry(credentialsId: 'nexus-d3-docker', url: 'https://nexus.iroha.tech:19002') {
                            JARS.each {
                                image = docker.build("nexus.iroha.tech:19002/d3-deploy/${it}:$TAG", "-f ${it}/Dockerfile ${it}")
                                image.push()
                            }
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
