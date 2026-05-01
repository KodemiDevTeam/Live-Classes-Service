pipeline {

    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
    }

    environment {
        JAVA_HOME = '/opt/java/openjdk'
        MAVEN_HOME = '/usr/share/maven'
        PATH = "/opt/java/openjdk/bin:/usr/share/maven/bin:/usr/bin:/bin:/usr/local/bin"

        SONAR_PROJECT_KEY  = 'Live-Classes-Service'
        SONAR_PROJECT_NAME = 'Live-Classes-Service'
    }

    stages {

        /* ================= CLEAN ================= */

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        /* ================= CHECKOUT ================= */

        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        /* ================= BUILD + TEST ================= */

        stage('Build & Test (with Coverage)') {
            steps {
                dir('live-classes-service') {
                    sh '''
                        echo "===== BUILD + TEST ====="

                        mvn clean verify \
                        -Deureka.client.enabled=false \
                        -Dspring.cloud.discovery.enabled=false
                    '''
                }
            }
        }

        /* ================= VERIFY JACOCO ================= */

        stage('Verify Coverage Report') {
            steps {
                dir('live-classes-service') {
                    sh '''
                        echo "===== VERIFYING JACOCO ====="
                        test -f target/site/jacoco/jacoco.xml && echo "JaCoCo report found" || (echo "JaCoCo report missing" && exit 1)
                    '''
                }
            }
        }

        /* ================= SONAR ANALYSIS ================= */

        stage('SonarQube Analysis') {
            steps {
                dir('live-classes-service') {
                    withSonarQubeEnv('SonarQube2') {
                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                            sh '''
                                echo "===== SONAR ANALYSIS ====="

                                mvn sonar:sonar \
                                -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                                -Dsonar.projectName=$SONAR_PROJECT_NAME \
                                -Dsonar.login=$SONAR_TOKEN \
                                -Dsonar.java.binaries=target/classes \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                                -Dsonar.qualitygate.wait=true
                            '''
                        }
                    }
                }
            }
        }

        /* ================= QUALITY GATE ================= */

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "❌ Pipeline failed due to Quality Gate: ${qg.status}"
                        }
                    }
                }
            }
        }

        /* ================= SECURITY ================= */

        stage('OWASP Dependency Check') {
            steps {
                dir('live-classes-service') {
                    withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_KEY')]) {

                        sh 'echo "===== RUNNING OWASP DEPENDENCY CHECK ====="'

                        dependencyCheck(
                            additionalArguments: "--nvdApiKey ${NVD_KEY} --format XML --out . --disableOssIndex",
                            odcInstallation: 'Default'
                        )
                    }

                    dependencyCheckPublisher pattern: 'dependency-check-report.xml'
                }
            }
        }

        /* ================= ARCHIVE ================= */

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: 'live-classes-service/dependency-check-report.xml',
                                 fingerprint: true

                junit allowEmptyResults: true,
                      testResults: 'live-classes-service/target/surefire-reports/*.xml'
            }
        }
    }

    post {
        success {
            echo '✅ SUCCESS: Build, Test, Sonar & Security checks passed'
        }
        unstable {
            echo '⚠️ UNSTABLE: Check Quality Gate or test results'
        }
        failure {
            echo '❌ FAILED: Pipeline execution failed'
        }
        always {
            echo '📌 Pipeline execution completed'
        }
    }
}
