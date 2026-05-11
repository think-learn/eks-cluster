pipeline {
    agent any

    environment {
        AWS_REGION = 'ap-south-1'
        CLUSTER_NAME = 'demo-eks-cluster'
        NODEGROUP_NAME = 'demo-nodegroup'
    }

    stages {

        stage('Check Tools') {
            steps {
                sh '''
                eksctl version
                kubectl version --client
                aws --version
                '''
            }
        }

        stage('Create EKS Cluster') {
            steps {
                sh '''
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
                  --ssh-access \
                  --alb-ingress-access \
                  --external-dns-access \
                  --full-ecr-access \
                  --asg-access \
                  --node-private-networking=false
                '''
            }
        }

        stage('Verify Cluster') {
            steps {
                sh '''
                aws eks update-kubeconfig \
                  --region $AWS_REGION \
                  --name $CLUSTER_NAME

                kubectl get nodes
                '''
            }
        }

        stage('Deploy Pod') {
            steps {
                sh '''
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
                kubectl get pods
                '''
            }
        }
    }

    post {
        success {
            echo 'EKS Cluster and Pod Created Successfully'
        }

        failure {
            echo 'Pipeline Failed'
        }
    }
}
