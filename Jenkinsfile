pipeline {
  agent any

  triggers {
    githubPush()
  }

  options {
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
          if (env.GIT_URL || env.CHANGE_URL || fileExists('.git')) {
            checkout scm
          } else if (fileExists('/workspace/backend/pom.xml')) {
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
          withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
            sh '''
              set -euo pipefail
              echo "Waiting for SonarQube at $SONAR_HOST_URL ..."
              UP=0
              for i in $(seq 1 90); do
                STATUS=$(curl -sf "$SONAR_HOST_URL/api/system/status" 2>/dev/null | sed -n 's/.*"status":"\\([^"]*\\)".*/\\1/p' || true)
                if [ "$STATUS" = "UP" ]; then
                  echo "SonarQube is UP"
                  UP=1
                  break
                fi
                echo "Sonar status=${STATUS:-down} ($i/90)"
                sleep 5
              done
              if [ "$UP" != "1" ]; then
                echo "SonarQube did not become UP in time"
                exit 1
              fi

              test -n "$SONAR_TOKEN" || (echo "SONAR_TOKEN env is empty — check Jenkins credential id=sonar-token" && exit 1)
              echo "Running Sonar analysis with token (length=${#SONAR_TOKEN})"

              cd backend
              # SonarQube 9.9 uses sonar.login (token as login); sonar.token is for newer servers
              mvn -B -DskipTests org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar \
                -Dsonar.projectKey=training-platform \
                -Dsonar.projectName='Training Platform' \
                -Dsonar.host.url="$SONAR_HOST_URL" \
                -Dsonar.java.binaries=target/classes \
                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                -Dsonar.login="$SONAR_TOKEN"
            '''
          }
        }
      }
    }
  }

  post {
    success { echo 'CI OK — unit tests + Sonar passed' }
    unstable { echo 'UNSTABLE — usually Sonar. Check credential id=sonar-token (Secret text) and Sonar token validity.' }
    failure { echo 'CI FAILED' }
    cleanup { cleanWs(deleteDirs: true, notFailBuild: true) }
  }
}
