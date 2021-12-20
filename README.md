## Prerequisites

* [Docker](https://docs.docker.com/engine/install/)
* kubectl
* Azure CLI
* Helm3
* Java JDK11
* Maven


## Set up Cluster

In this sample we'll be using Azure Kubernetes Service, but you can install Dapr on any Kubernetes cluster.
Run [this script](deploy/deploy_aks.sh) to deploy an AKS cluster 

References:

* [Deploy AKS using Portal](https://docs.microsoft.com/en-us/azure/aks/kubernetes-walkthrough-portal)
* [Deploy AKS using CLI](https://docs.dapr.io/operations/hosting/kubernetes/cluster/setup-aks/)
* [Dapr Environment - Setup Cluster](https://docs.dapr.io/getting-started/install-dapr/#setup-cluster)

## Install Dapr

Run [this script](deploy/deploy_dapr_aks.sh) to install Dapr on the Kubernetes cluster using Helm, or follow the steps below.

```bash
wget -q https://raw.githubusercontent.com/dapr/cli/master/install/install.sh -O - | DAPR_INSTALL_DIR="$HOME/dapr" /bin/bash
dapr init -k
```

[Dapr extension for Azure Kubernetes Service (AKS) (preview)](https://docs.microsoft.com/en-us/azure/aks/dapr) is yet another way to install Dapr on your AKS cluster.

References:

* [Install the Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/)
* [Install Dapr on a Kubernetes cluster](https://docs.dapr.io/operations/hosting/kubernetes/kubernetes-deploy)
* [Dapr extension for Azure Kubernetes Service (AKS) (preview)](https://docs.microsoft.com/en-us/azure/aks/dapr)

## Create Blob Storage

1. Run [this script](deploy/deploy_storage.sh).

2. create the Kubernetes secret (replace *** with storage account key):
   ```bash
     kubectl create secret generic output-queue-secret --from-literal=connectionString=*********
     kubectl create secret generic output-queue-secret --from-literal=connectionString=cri0BZd4CXMBBLqLmFcGXuGd9GpGOPnkJr2CfDhqNzPLDeOrcfXjOa/HbDfafLqXWIrlISIJL7WcSY6w9LfptA==
    ```
3. Replace <storage_account_name> in [deploy/blob-storage.yaml](deploy/blob-storage.yaml) with your storage account name.

4. Create two Storage Account Queues, named "dapr-batch-queue" and "dapr-output-queue" in the same Storage Account.

5. Using Azure Portal, create a Event Grid event for the Storage Account:

   * On the seleted Storage Account Overview page, select `Events`, and then select `+ Event Subscription` as follows: ![Create Event Subscription](images/create-event-subscription.png)

   * On the `Create Event Subscription` page, enter the values as follow: ![Create Event Subscription](images/new-event-subscription.png)

References:

* [Use Azure Event Grid to route Blob storage events to web endpoint (Azure portal) - Portal](https://docs.microsoft.com/en-us/azure/event-grid/blob-event-quickstart-portal)
* [Manage Azure Storage resources - CLI](https://docs.microsoft.com/en-us/cli/azure/storage?view=azure-cli-latest)

## Build and push images to AKS

1. Create an Azure Container Registry (ACR) (Lowercase registry name is recommended to avoid warnings):

    ```powershell
    az acr create --resource-group <resource-group-name> --name <acr-name> --sku Basic
    ```

    Take note of loginServer in the output.

2. Integrate an existing ACR with existing AKS clusters:

    ```powershell
    az aks update -n <cluster-name> -g <resource-group-name> --attach-acr <acr-name>
    ```

3. Change ACR loginServer and name in batch-file-processing/javabatchreceiver/batch-receiver/java-batch-receiver.sh

References:
[Create a private container registry using the Azure CLI](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli)

## Install Kafka on AKS

helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
kubectl create ns kafka
helm install dapr-kafka bitnami/kafka --wait --namespace kafka -f batch-file-processing/deploy/kafka-non-persistence.yaml

## Deploy microservices

1. Deploy Dapr components:

    ```bash
    kubectl apply -f batch-file-processing/deploy/blob-storage.yaml   
    kubectl apply -f batch-file-processing/deploy/queue-storage.yaml 
    kubectl apply -f batch-file-processing/deploy/output-queue.yaml
    kubectl apply -f batch-file-processing/deploy/messagebus.yaml
    kubectl apply -f batch-file-processing/deploy/subscriptions.yaml
    kubectl apply -f batch-file-processing/deploy/kafka-pubsub.yaml
    ```

2. Deploy Java Batch Receiver microservice:

    ```bash
    cd batch-file-processing/javabatchreceiver/batch-receiver
    ./java-batch-receiver.sh
    cd ../../../
    ```

    Check the logs for java-batch-receiver.
    ```bash 
    javabatchreceivername=$(kubectl get po --selector=app=java-batch-receiver -o jsonpath='{.items[*].metadata.name}')
    kubectl logs $javabatchreceivername -c java-batch-receiver -f
    ```
3. Test the application.

    a. Upload a file to 'order' container storage.
    b. view the log output in java-batch-receiver
    c. view entry in 'dapr-output-queue'

    k port-forward svc/java-batch-receiver-service 8084:80
    k port-forward deploy/java-batch-receiver 8084:8084
kubectl delete scaledobject http-scaledobject -n custom-metric
kubectl describe scaledobject http-scaledobject -n custom-metric

kubectl apply -f  batch-file-processing/deploy/keda-batch-receiver-scaleobject.yaml

k delete -f batch-file-processing/php-apache/php-apache.yaml
k apply -f batch-file-processing/php-apache/php-apache.yaml
