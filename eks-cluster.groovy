pipeline {
    agent any

    environment {
        AWS_REGION     = 'ap-south-1'
        CLUSTER_NAME   = 'demo-eks-cluster'
        NODEGROUP_NAME = 'demo-nodegroup'
    }

    options {
        timeout(time: 90, unit: 'MINUTES')
    }

    stages {

        stage('Check Tools') {
            steps {
                sh '''
                echo "Checking required tools..."

                eksctl version
                kubectl version --client
                aws --version
                '''
            }
        }

        stage('Create EKS Cluster') {
            steps {
                sh '''
                echo "Creating EKS Cluster..."

                eksctl create cluster \
                  --name $CLUSTER_NAME \
                  --region $AWS_REGION \
                  --nodegroup-name $NODEGROUP_NAME \
                  --node-type t3.medium \
                  --nodes 1 \
                  --nodes-min 1 \
                  --nodes-max 1 \
                  --managed \
                  --with-oidc \
                  --ssh-access=false \
                  --alb-ingress-access \
                  --external-dns-access \
                  --full-ecr-access \
                  --asg-access \
                  --node-private-networking=false
                '''
            }
        }

        stage('Update kubeconfig') {
            steps {
                sh '''
                echo "Updating kubeconfig..."

                aws eks update-kubeconfig \
                  --region $AWS_REGION \
                  --name $CLUSTER_NAME
                '''
            }
        }

        stage('Verify Cluster') {
            steps {
                sh '''
                echo "Checking cluster nodes..."

                kubectl get nodes -o wide
                '''
            }
        }

        stage('Deploy Nginx Pod') {
            steps {
                sh '''
                echo "Deploying Nginx Pod..."

                cat <<EOF > pod.yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
spec:
  containers:
  - name: nginx
    image: nginx
    ports:
    - containerPort: 80
EOF

                kubectl apply -f pod.yaml

                echo "Waiting for pod to become Ready..."

                kubectl wait --for=condition=Ready pod/nginx-pod --timeout=180s

                kubectl get pods -o wide
                '''
            }
        }
    }

    post {

        success {
            echo 'EKS Cluster and Nginx Pod Created Successfully'
        }

        failure {
            echo 'Pipeline Failed'
        }

        always {
            cleanWs()
        }
    }
}
