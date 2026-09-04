pipeline {
  agent any

  // When the job is "Pipeline script from SCM" + GitHub hook enabled,
  // a push to GitHub triggers this build.
  triggers {
    githubPush()
  }

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    skipDefaultCheckout(true)
  }

  environment {
    SONAR_HOST_URL = 'http://sonarqube:9000'
  }

  stages {
    stage('Checkout') {
      steps {
        script {
          // GitHub / SCM job
          if (env.GIT_URL || env.CHANGE_URL || fileExists('.git')) {
            checkout scm
          } else if (fileExists('/workspace/backend/pom.xml')) {
            // Local Docker mount fallback (no GitHub yet)
            echo 'Using mounted /workspace (local Docker mode)'
            sh 'cp -a /workspace/. "$WORKSPACE/"'
          } else {
            checkout scm
          }
        }
      }
    }

    stage('Prepare') {
      steps {
        sh '''
          set -e
          test -f backend/pom.xml || (echo "backend/pom.xml missing after checkout" && exit 1)
          java -version
          mvn -version
          echo "Branch=${GIT_BRANCH:-local} Commit=${GIT_COMMIT:-n/a}"
        '''
      }
    }

    stage('Unit tests') {
      steps {
        dir('backend') {
          sh 'mvn -B clean test'
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
        }
      }
    }

    stage('SonarQube') {
      steps {
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
          script {
            def runSonar = { String extraArgs ->
              sh """
                set -e
                echo "Waiting for SonarQube..."
                for i in \$(seq 1 90); do
                  STATUS=\$(curl -sf "\$SONAR_HOST_URL/api/system/status" 2>/dev/null | sed -n 's/.*"status":"\\([^"]*\\)".*/\\1/p' || true)
                  if [ "\$STATUS" = "UP" ]; then
                    echo "SonarQube is UP"
                    break
                  fi
                  echo "Sonar status=\$STATUS (\$i/90)"
                  sleep 5
                done

                cd backend
                mvn -B -DskipTests org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar \\
                  -Dsonar.projectKey=training-platform \\
                  -Dsonar.projectName='Training Platform' \\
                  -Dsonar.host.url="\$SONAR_HOST_URL" \\
                  -Dsonar.java.binaries=target/classes \\
                  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \\
                  ${extraArgs}
              """
            }

            try {
              withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                runSonar("-Dsonar.token=\${SONAR_TOKEN}")
              }
            } catch (err) {
              echo "Sonar token missing or binding failed — ${err}"
              runSonar('')
            }
          }
        }
      }
    }
  }

  post {
    success { echo 'CI OK — unit tests passed' }
    unstable { echo 'Tests OK but Sonar unstable — add credential sonar-token' }
    failure { echo 'CI FAILED' }
    cleanup { cleanWs(deleteDirs: true, notFailBuild: true) }
  }
}
