pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Build & Test') {
            steps {
                // Requer JDK 25 disponível no agente Jenkins (java.version=25 no pom.xml)
                sh 'chmod +x mvnw && ./mvnw -B clean verify'
            }
            post {
                always { junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml' }
            }
        }
        stage('Package') {
            steps { sh './mvnw -B -DskipTests package' }
            post {
                success { archiveArtifacts artifacts: 'target/*.jar', fingerprint: true }
            }
        }
    }
}
