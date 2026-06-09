pipeline {

    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
    }

    environment {
        JAVA_HOME             = 'C:\\Program Files\\Java\\jdk-17'
        MAVEN_HOME            = 'C:\\Program Files\\Apache\\maven'
        PATH                  = "${env.JAVA_HOME}\\bin;${env.MAVEN_HOME}\\bin;${env.PATH}"

        SONAR_PROJECT_KEY     = 'Live-Classes-Service'
        SONAR_PROJECT_NAME    = 'Live-Classes-Service'

        COVERAGE_LINE_MIN     = '70'
        COVERAGE_BRANCH_MIN   = '60'
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

        stage('Build & Test (with Coverage)') {
            steps {
                dir('live-classes-service') {
                    bat '''
                        echo ===== BUILD + TEST =====
                        mvn clean verify ^
                            -Deureka.client.enabled=false ^
                            -Dspring.cloud.discovery.enabled=false
                    '''
                }
            }
            post {
                always {
                    junit testResults: 'live-classes-service/target/surefire-reports/*.xml',
                          allowEmptyResults: true

                    publishHTML(target: [
                        allowMissing         : true,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'live-classes-service/target/site/jacoco',
                        reportFiles          : 'index.html',
                        reportName           : 'JaCoCo Coverage Report'
                    ])
                }
            }
        }

        stage('Verify Coverage Report') {
            steps {
                dir('live-classes-service') {
                    bat '''
                        echo ===== VERIFYING JACOCO =====
                        if exist target\\site\\jacoco\\jacoco.xml (
                            echo JaCoCo report found
                        ) else (
                            echo JaCoCo report missing
                            exit /b 1
                        )
                    '''
                }
            }
        }

        stage('Coverage Gate') {
            steps {
                script {
                    def csvFile = 'live-classes-service/target/site/jacoco/jacoco.csv'

                    if (!fileExists(csvFile)) {
                        echo "WARNING: ${csvFile} not found — skipping coverage gate."
                        return
                    }

                    def lines           = readFile(csvFile).trim().split('\n')
                    def header          = lines[0].split(',')
                    def lineMissedIdx   = header.findIndexOf { it.trim() == 'LINE_MISSED'    }
                    def lineCoveredIdx  = header.findIndexOf { it.trim() == 'LINE_COVERED'   }
                    def branchMissedIdx = header.findIndexOf { it.trim() == 'BRANCH_MISSED'  }
                    def branchCoveredIdx= header.findIndexOf { it.trim() == 'BRANCH_COVERED' }

                    long totalLineMissed = 0, totalLineCovered = 0
                    long totalBranchMissed = 0, totalBranchCovered = 0

                    lines.drop(1).each { row ->
                        def cols = row.split(',')
                        if (cols.size() > lineCoveredIdx) {
                            totalLineMissed    += (cols[lineMissedIdx]   ?.trim()?.toLong() ?: 0)
                            totalLineCovered   += (cols[lineCoveredIdx]  ?.trim()?.toLong() ?: 0)
                            totalBranchMissed  += (cols[branchMissedIdx] ?.trim()?.toLong() ?: 0)
                            totalBranchCovered += (cols[branchCoveredIdx]?.trim()?.toLong() ?: 0)
                        }
                    }

                    long totalLines    = totalLineMissed   + totalLineCovered
                    long totalBranches = totalBranchMissed + totalBranchCovered

                    double linePct   = totalLines    > 0 ? (totalLineCovered   * 100.0 / totalLines)    : 0
                    double branchPct = totalBranches > 0 ? (totalBranchCovered * 100.0 / totalBranches) : 0

                    echo "=========================================="
                    echo "  Line   Coverage : ${String.format('%.2f', linePct)} %  (min: ${COVERAGE_LINE_MIN}%)"
                    echo "  Branch Coverage : ${String.format('%.2f', branchPct)} %  (min: ${COVERAGE_BRANCH_MIN}%)"
                    echo "=========================================="

                    boolean failed = false

                    if (linePct < COVERAGE_LINE_MIN.toDouble()) {
                        echo "FAIL: Line coverage ${String.format('%.2f', linePct)}% is below threshold ${COVERAGE_LINE_MIN}%"
                        failed = true
                    }
                    if (branchPct < COVERAGE_BRANCH_MIN.toDouble()) {
                        echo "FAIL: Branch coverage ${String.format('%.2f', branchPct)}% is below threshold ${COVERAGE_BRANCH_MIN}%"
                        failed = true
                    }

                    if (failed) {
                        currentBuild.result = 'UNSTABLE'
                        echo "Coverage gate not met — build marked UNSTABLE."
                    } else {
                        echo "Coverage gate passed."
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('live-classes-service') {
                    withSonarQubeEnv('SonarQube2') {
                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                            bat '''
                                echo ===== SONAR ANALYSIS =====
                                mvn sonar:sonar ^
                                    -Dsonar.projectKey=%SONAR_PROJECT_KEY% ^
                                    -Dsonar.projectName=%SONAR_PROJECT_NAME% ^
                                    -Dsonar.login=%SONAR_TOKEN% ^
                                    -Dsonar.java.binaries=target\\classes ^
                                    -Dsonar.coverage.jacoco.xmlReportPaths=target\\site\\jacoco\\jacoco.xml ^
                                    -Dsonar.qualitygate.wait=true
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
                    }
                }
            }
        }

        stage('OWASP Dependency Check') {
            steps {
                dir('live-classes-service') {
                    withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_KEY')]) {
                        bat 'echo ===== RUNNING OWASP DEPENDENCY CHECK ====='
                        dependencyCheck(
                            additionalArguments: "--nvdApiKey ${NVD_KEY} --format XML --out . --disableOssIndex",
                            odcInstallation: 'Default'
                        )
                    }
                    dependencyCheckPublisher pattern: 'dependency-check-report.xml'
                }
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: 'live-classes-service/dependency-check-report.xml, live-classes-service/target/site/jacoco/**',
                                 fingerprint: true,
                                 allowEmptyArchive: true
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Build, Test, Coverage, Sonar and Security checks passed'
        }
        unstable {
            echo 'UNSTABLE: Coverage gate not met or test failures — review Coverage Gate and test results'
        }
        failure {
            echo 'FAILED: Pipeline execution failed — check logs'
        }
        always {
            echo 'Pipeline execution completed'
        }
    }
}
