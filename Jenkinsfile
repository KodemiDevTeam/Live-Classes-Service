pipeline {

    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
        skipDefaultCheckout(true)
    }

    environment {
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
                    bat '''
                    echo ===== BUILD + TEST =====

                    call mvnw.cmd clean verify ^
                    -Deureka.client.enabled=false ^
                    -Dspring.cloud.discovery.enabled=false

                    if %ERRORLEVEL% NEQ 0 (
                        echo ERROR: Maven build/test failed
                        exit /b %ERRORLEVEL%
                    )

                    echo ===== BUILD + TEST COMPLETED =====
                    '''
                }
            }
        }

        /* ================= VERIFY JACOCO ================= */

        stage('Verify Coverage Report') {
            steps {
                dir('live-classes-service') {
                    bat '''
                    echo ===== VERIFYING JACOCO =====

                    if exist target\\site\\jacoco\\jacoco.xml (
                        echo JaCoCo report found
                        dir target\\site\\jacoco
                    ) else (
                        echo ERROR: JaCoCo report missing
                        exit /b 1
                    )
                    '''
                }
            }
        }

        /* ================= SONAR ANALYSIS ================= */

        stage('SonarQube Analysis') {
            steps {
                dir('live-classes-service') {

                    withSonarQubeEnv('SonarQube2') {

                        withCredentials([
                            string(
                                credentialsId: 'sonar-token',
                                variable: 'SONAR_TOKEN'
                            )
                        ]) {

                            bat '''
                            echo ===== SONAR ANALYSIS =====

                            call mvnw.cmd -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar ^
                            -Dsonar.projectKey=%SONAR_PROJECT_KEY% ^
                            -Dsonar.projectName=%SONAR_PROJECT_NAME% ^
                            -Dsonar.token=%SONAR_TOKEN% ^
                            -Dsonar.java.binaries=target/classes ^
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

                            if %ERRORLEVEL% NEQ 0 (
                                echo ERROR: SonarQube analysis failed
                                exit /b %ERRORLEVEL%
                            )

                            echo ===== SONAR ANALYSIS COMPLETED =====
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
                            error "Pipeline failed due to Quality Gate: ${qg.status}"
                        }
                    }
                }
            }
        }

        /* ================= SECURITY ================= */

        stage('OWASP Dependency Check') {
            steps {

                dir('live-classes-service') {

                    withCredentials([
                        string(
                            credentialsId: 'nvd-api-key',
                            variable: 'NVD_KEY'
                        )
                    ]) {

                        bat '''
                        echo ===== RUNNING OWASP DEPENDENCY CHECK =====
                        '''

                        dependencyCheck(
                            additionalArguments: "--nvdApiKey ${NVD_KEY} --format XML --out . --disableOssIndex",
                            odcInstallation: 'Default'
                        )
                    }

                    dependencyCheckPublisher(
                        pattern: 'dependency-check-report.xml'
                    )
                }
            }
        }

        /* ================= ARCHIVE ================= */

        stage('Archive Reports') {
            steps {

                archiveArtifacts(
                    artifacts: 'live-classes-service/dependency-check-report.xml',
                    fingerprint: true
                )

                junit(
                    allowEmptyResults: true,
                    testResults: 'live-classes-service/target/surefire-reports/*.xml'
                )
            }
        }
    }

    post {

        success {
            echo 'SUCCESS: Build, Test, Sonar & Security checks passed'
        }

        unstable {
            echo 'UNSTABLE: Coverage threshold was not met, but SonarQube analysis was completed'
        }

        failure {
            echo 'FAILED: Pipeline execution failed'
        }

        always {
            echo 'Pipeline execution completed'
        }
    }
}
