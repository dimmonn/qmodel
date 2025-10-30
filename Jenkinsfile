pipeline {
    agent any

    stages {

        stage('Stop') {
            steps {
                sh 'docker-compose down --rmi all'
            }
        }

        stage('Clean') {
            steps {
                sh '/home/apache-maven-3.9.6/bin/mvn clean'
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install') {
            steps {
                sh '/home/apache-maven-3.9.6/bin/mvn install'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker-compose up -d'
            }
        }
    }

    post {
        success {
            // Send notification on success
            echo 'Build successful! Deployed successfully!'
        }

        failure {
            // Send notification on failure
            echo 'Build or deploy failed!'
        }
    }
}
