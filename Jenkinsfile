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

        // Java 21 for SonarQube Scanner
        SONAR_JAVA_HOME = 'C:\\Program Files\\Java\\jdk-21.0.12.1'
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

                        echo Java version used for application build:
                        java -version

                        call mvnw.cmd clean test ^
                        -Deureka.client.enabled=false ^
                        -Dspring.cloud.discovery.enabled=false

                        if %ERRORLEVEL% NEQ 0 (
                            echo.
                            echo ERROR: Maven tests failed.
                            exit /b %ERRORLEVEL%
                        )

                        echo.
                        echo TESTS COMPLETED SUCCESSFULLY
                        echo ========================================
                    '''
                }
            }
        }

        stage('Generate JaCoCo Coverage Report') {
            steps {
                dir('live-classes-service') {
                    bat '''
                        echo ========================================
                        echo     GENERATING JACOCO REPORT
                        echo ========================================

                        call mvnw.cmd jacoco:report

                        if %ERRORLEVEL% NEQ 0 (
                            echo.
                            echo ERROR: JaCoCo report generation failed.
                            exit /b %ERRORLEVEL%
                        )

                        echo.
                        echo JACOCO REPORT GENERATED SUCCESSFULLY
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
                        echo     VERIFYING JACOCO XML REPORT
                        echo ========================================

                        if exist target\\site\\jacoco\\jacoco.xml (
                            echo.
                            echo JaCoCo XML report found successfully.
                            echo.
                            dir target\\site\\jacoco\\jacoco.xml
                        ) else (
                            echo.
                            echo ERROR: JaCoCo XML report is missing.
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
                                echo       SONARQUBE ANALYSIS
                                echo ========================================

                                echo Switching to Java 21 for SonarQube...

                                set "JAVA_HOME=%SONAR_JAVA_HOME%"
                                set "PATH=%JAVA_HOME%\\bin;%PATH%"

                                echo.
                                echo Java version for SonarQube:
                                java -version

                                echo.
                                echo JAVA_HOME:
                                echo %JAVA_HOME%

                                echo.
                                echo Running SonarQube analysis...
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
