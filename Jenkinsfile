pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
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
