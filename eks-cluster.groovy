pipeline {
    agent any

    environment {
        AWS_REGION   = 'ap-south-1'
        CLUSTER_NAME = 'demo-eks-cluster'
    }

    stages {

        stage('Delete Existing EKS Cluster') {
            steps {
                sh '''
                echo "Deleting old EKS cluster if exists..."

                eksctl delete cluster \
                  --name $CLUSTER_NAME \
                  --region $AWS_REGION || true
                '''
            }
        }

        stage('Delete Remaining CloudFormation Stack') {
            steps {
                sh '''
                echo "Deleting leftover CloudFormation stack..."

                aws cloudformation delete-stack \
                  --stack-name eksctl-demo-eks-cluster-cluster \
                  --region $AWS_REGION || true
                '''
            }
        }

        stage('Wait for Stack Deletion') {
            steps {
                sh '''
                echo "Waiting for stack deletion..."

                aws cloudformation wait stack-delete-complete \
                  --stack-name eksctl-demo-eks-cluster-cluster \
                  --region $AWS_REGION || true
                '''
            }
        }

        stage('Verify Cleanup') {
            steps {
                sh '''
                echo "Checking remaining stacks..."

                aws cloudformation list-stacks \
                  --stack-status-filter CREATE_COMPLETE DELETE_FAILED CREATE_FAILED ROLLBACK_COMPLETE \
                  --region $AWS_REGION
                '''
            }
        }
    }

    post {
        success {
            echo 'Old EKS resources cleaned successfully'
        }

        failure {
            echo 'Cleanup Pipeline Failed'
        }

        always {
            cleanWs()
        }
    }
}
