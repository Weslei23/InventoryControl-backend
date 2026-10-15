pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 20, unit: 'MINUTES')
    }

    environment {
        IMAGE_NAME = 'inventory-backend'
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
        COMPOSE_PROJECT_NAME = 'inventory'
        HOST_PORT  = '8083'
        // Origens permitidas no CORS (frontend do inventario)
        CORS_ALLOWED_ORIGINS = 'http://192.168.3.183:8084,http://localhost:5173'
        ADMIN_USERNAME = 'weslei'
        ADMIN_EMAIL = 'sschmittweslei@gmail.com'
        DOCKER_BUILDKIT = '1'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // O Dockerfile compila com Maven (JDK 25) e roda os testes dentro do build
        stage('Build & Test') {
            steps {
                sh 'docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest .'
            }
        }

        stage('Deploy') {
            when { expression { env.BRANCH_NAME == 'main' || env.GIT_BRANCH in ['origin/main', 'main'] } }
            steps {
                withCredentials([
                    string(credentialsId: 'inventory-db-password',    variable: 'DB_PASSWORD'),
                    string(credentialsId: 'inventory-jwt-secret',     variable: 'JWT_SECRET'),
                    string(credentialsId: 'inventory-admin-password', variable: 'ADMIN_PASSWORD')
                ]) {
                    sh 'docker compose -f compose.prod.yaml up -d --remove-orphans'
                }
            }
        }

        stage('Smoke test') {
            when { expression { env.BRANCH_NAME == 'main' || env.GIT_BRANCH in ['origin/main', 'main'] } }
            steps {
                sh '''
                    for i in $(seq 1 40); do
                        if curl -fsS http://127.0.0.1:${HOST_PORT}/v3/api-docs > /dev/null; then
                            echo "API respondendo"; exit 0
                        fi
                        sleep 3
                    done
                    echo "API nao respondeu a tempo"
                    docker compose -f compose.prod.yaml logs --tail=80 backend || true
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            sh 'docker image prune -f --filter "until=168h" || true'
        }
        success { echo "Deploy ${env.IMAGE_NAME}:${env.IMAGE_TAG} concluido" }
        failure { echo 'Pipeline falhou - veja os logs acima' }
    }
}
