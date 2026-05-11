pipeline {
    agent any

    environment {
        AWS_REGION   = 'ap-south-1'
        CLUSTER_NAME = 'demo-eks-cluster'
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
    }

    stages {

        stage('Check Tools') {
            steps {
                sh '''
                echo "Checking required tools..."

                eksctl version
                aws --version
                kubectl version --client
                '''
            }
        }

        stage('Delete EKS Cluster') {
            steps {
                sh '''
                echo "Deleting EKS Cluster..."

                eksctl delete cluster \
                  --name $CLUSTER_NAME \
                  --region $AWS_REGION
                '''
            }
        }

        stage('Verify Deletion') {
            steps {
                sh '''
                echo "Checking remaining EKS clusters..."

                aws eks list-clusters --region $AWS_REGION
                '''
            }
        }
    }

    post {

        success {
            echo 'EKS Cluster Deleted Successfully'
        }

        failure {
            echo 'Pipeline Failed While Deleting Cluster'
        }

        always {
            cleanWs()
        }
    }
}
