pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install') {
            steps {
                sh -c '/home/apache-maven-3.9.6/bin/mvn install'
            }
        }

        stage('Deploy') {
            steps {
                sh -c 'docker-compose up -d'
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
