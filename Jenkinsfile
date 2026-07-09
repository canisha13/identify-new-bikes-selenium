pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(name: 'FORCE_RECREATE', defaultValue: true,
            description: 'Force recreate the tests container (recommended - guarantees latest code runs)')
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

        stage('Build Test Image') {
            steps {
                sh 'docker compose build tests'
            }
        }

        stage('Start Grid') {
            steps {
                sh 'docker compose up -d selenium-hub chrome firefox edge'
            }
        }

        stage('Wait for Grid Readiness') {
            steps {
                sh '''
                    for attempt in $(seq 1 30); do
                        if curl --fail --silent "http://localhost:4444/status" | grep -q '"ready": *true'; then
                            echo "Selenium Grid is ready."
                            exit 0
                        fi
                        echo "Waiting for Selenium Grid: ${attempt}/30"
                        sleep 2
                    done
                    echo "Selenium Grid did not become ready."
                    docker compose logs selenium-hub chrome firefox edge || true
                    exit 1
                '''
            }
        }

        stage('Run Selenium Tests') {
            steps {
                script {
                    def recreateFlag = params.FORCE_RECREATE ? '--force-recreate' : ''
                    // Tests container exits non-zero when TestNG reports failures
                    // (e.g. the intentional accessibility failure). We don't want
                    // that to abort the pipeline before reports are collected, so
                    // we capture the result and surface it after archiving.
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

            junit allowEmptyResults: true,
                testResults: 'target/surefire-reports/TEST-*.xml,target/surefire-reports/*.xml'

            archiveArtifacts allowEmptyArchive: true,
                artifacts: 'target/extent/**, target/allure-results/**, target/surefire-reports/**, target/screenshots/**, docker-tests.log, docker-grid.log'

            // If the Allure Jenkins plugin is installed, this renders a full
            // Allure report as a build tab. Comment out if the plugin isn't installed.
            script {
                if (fileExists('target/allure-results')) {
                    try {
                        allure includeProperties: false, jdk: '', results: [[path: 'target/allure-results']]
                    } catch (e) {
                        echo "Allure plugin not installed or not configured - skipping Allure report rendering. Results are still archived above."
                    }
                }
            }

            sh 'docker compose down -v || true'

            script {
                if (env.TEST_EXIT_CODE != '0') {
                    unstable("TestNG reported test failures (see JUnit/Extent/Allure reports). Exit code: ${env.TEST_EXIT_CODE}")
                }
            }
        }
    }
}