def call(Map configMap){

pipeline {
    agent { label 'AGENT-1' }

    environment {

        greeter = configMap.get('greeter')
        PROJECT = "expense"
        COMPONENT = "backend"
        ACC_Id = "894650614410"
        APP_VERSION = " "
    }
    options {
       disableConcurrentBuilds()
    }

    parameters {
        string(name: 'deploy', description: 'Enter the application version')
        
    }

    stages {


        stage('print shared library variable') {
            steps {
                script {
                    echo "${greeter}"
                }
            }
        }   

        stage('Read Version') {
            steps {
                script {
                    def packageJson = readJSON file: 'package.json'
                    APP_VERSION = packageJson.version  
                    echo "Version is: ${APP_VERSION}"
                }
            }
        }   

        stage('Install dependencies') {
            steps {

                script {
                    sh """
                    npm install
                    
                    """
                }
               
            }
        }

        stage('Docker Build') {
            steps {
            withAWS(region: 'us-east-1', credentials: 'aws-creds') {
            sh """
             aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ACC_Id}.dkr.ecr.us-east-1.amazonaws.com
            docker build \
          --platform linux/amd64 \
          --provenance=false \
          --sbom=false \
          -t ${ACC_Id}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${APP_VERSION} .
                docker push ${ACC_Id}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${APP_VERSION}
            """
                }
            }
        }

        stage('Trigger Deploy') {
           when{

                expression { params.deploy }
            }
            steps {
                script {
                    build job: 'backend-cd', parameters: [string(name: 'version', value: "${APP_VERSION}")],wait: true
                }
            }
           }
        }

        



        }




    
}