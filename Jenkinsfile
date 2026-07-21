pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(
            name: 'FORCE_RECREATE',
            defaultValue: true,
            description: 'Force recreate the tests container'
        )
    }

    environment {
        COMPOSE_PROJECT_NAME = "identify-new-bikes-selenium-${env.BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Clean Old Reports') {
            steps {
                sh '''
                    docker run --rm -v "$PWD":/workspace alpine sh -c "rm -rf /workspace/target /workspace/allure-report"
                    mkdir -p target/extent target/allure-results target/surefire-reports target/screenshots
                '''
            }
        }

        stage('Build Test Image') {
            steps {
                sh 'docker compose build --no-cache tests'
            }
        }

        stage('Start Selenium Grid') {
            steps {
                sh 'docker compose up -d selenium-hub chrome firefox edge'
            }
        }

        stage('Wait for Grid') {
            steps {
                sh '''
                    echo "Waiting for Selenium Grid to start..."
                    sleep 15
                    docker compose ps
                '''
            }
        }

        stage('Run Selenium Tests') {
            steps {
                script {
                    def recreateFlag = params.FORCE_RECREATE ? '--force-recreate' : ''

                    env.TEST_EXIT_CODE = sh(
                        script: "docker compose up ${recreateFlag} --exit-code-from tests tests",
                        returnStatus: true
                    ).toString()
                }
            }
        }
    }

    post {
        always {

            sh '''
                docker compose logs tests > docker-tests.log 2>&1 || true
                docker compose logs selenium-hub chrome firefox edge > docker-grid.log 2>&1 || true
            '''

            sh '''
                CONTAINER_ID=$(docker compose ps -a -q tests)
                if [ -n "$CONTAINER_ID" ]; then
                    docker cp "$CONTAINER_ID":/app/target/. target/ || true
                fi
            '''

            junit allowEmptyResults: true,
                testResults: 'target/surefire-reports/TEST-*.xml,target/surefire-reports/*.xml'

            archiveArtifacts allowEmptyArchive: true,
                artifacts: '''
                    target/extent/**,
                    target/allure-results/**,
                    target/surefire-reports/**,
                    target/screenshots/**,
                    docker-tests.log,
                    docker-grid.log
                '''

            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target/extent',
                reportFiles: '*.html',
                reportName: 'Extent HTML Report'
            ])

            script {
                if (fileExists('target/allure-results')) {
                    try {
                        allure includeProperties: false,
                               jdk: '',
                               results: [[path: 'target/allure-results']]
                    } catch (e) {
                        echo "Allure plugin not installed or not configured."
                    }
                }
            }

            sh 'docker compose down -v || true'

            script {
                if (env.TEST_EXIT_CODE != null && env.TEST_EXIT_CODE != '0') {
                    unstable("TestNG reported test failures. Exit code: ${env.TEST_EXIT_CODE}")
                }
            }
        }
    }
}