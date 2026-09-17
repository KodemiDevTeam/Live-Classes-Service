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

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                dir('live-classes-service') {
                    bat '''
                        echo ========================================
                        echo        BUILD AND TEST
                        echo ========================================

                        call mvnw.cmd clean verify ^
                        -Deureka.client.enabled=false ^
                        -Dspring.cloud.discovery.enabled=false

                        if %ERRORLEVEL% NEQ 0 (
                            echo.
                            echo ERROR: Maven build/test failed.
                            exit /b %ERRORLEVEL%
                        )

                        echo.
                        echo BUILD AND TEST COMPLETED SUCCESSFULLY
                        echo ========================================
                    '''
                }
            }
        }

        stage('Verify Coverage Report') {
            steps {
                dir('live-classes-service') {
                    bat '''
                        echo ========================================
                        echo        VERIFYING JACOCO REPORT
                        echo ========================================

                        if exist target\\site\\jacoco\\jacoco.xml (
                            echo JaCoCo report found successfully.
                            dir target\\site\\jacoco
                        ) else (
                            echo ERROR: JaCoCo report is missing.
                            exit /b 1
                        )

                        echo ========================================
                    '''
                }
            }
        }

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
                                echo ========================================
                                echo        SONARQUBE ANALYSIS
                                echo ========================================

                                call mvnw.cmd -B ^
                                org.sonarsource.scanner.maven:sonar-maven-plugin:sonar ^
                                -Dsonar.projectKey=%SONAR_PROJECT_KEY% ^
                                -Dsonar.projectName=%SONAR_PROJECT_NAME% ^
                                -Dsonar.token=%SONAR_TOKEN% ^
                                -Dsonar.java.binaries=target/classes ^
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

                                if %ERRORLEVEL% NEQ 0 (
                                    echo.
                                    echo ERROR: SonarQube analysis failed.
                                    exit /b %ERRORLEVEL%
                                )

                                echo.
                                echo SONARQUBE ANALYSIS COMPLETED SUCCESSFULLY
                                echo ========================================
                            '''
                        }
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    script {

                        def qg = waitForQualityGate()

                        if (qg.status != 'OK') {
                            error "Pipeline failed due to Quality Gate: ${qg.status}"
                        }

                        echo "SonarQube Quality Gate: PASSED"
                    }
                }
            }
        }

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
                            echo ========================================
                            echo     OWASP DEPENDENCY CHECK
                            echo ========================================
                        '''

                        dependencyCheck(
                            additionalArguments: "--nvdApiKey ${NVD_KEY} --format XML --out . --disableOssIndex",
                            odcInstallation: 'Default'
                        )

                        dependencyCheckPublisher(
                            pattern: 'dependency-check-report.xml'
                        )
                    }
                }
            }
        }

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
            echo '''
            ========================================
            PIPELINE COMPLETED SUCCESSFULLY
            ========================================
            '''
        }

        unstable {
            echo '''
            ========================================
            PIPELINE COMPLETED WITH WARNINGS
            ========================================
            '''
        }

        failure {
            echo '''
            ========================================
            PIPELINE FAILED
            ========================================
            '''
        }

        always {
            echo "Jenkins pipeline execution completed."
        }
    }
}
