## Prerequisites

* [Docker](https://docs.docker.com/engine/install/)
* kubectl
* Azure CLI
* Helm3
* for Java based metric provider API
  * Java JDK11
  * Maven
* for Python based metric provider API
  * python3

## Set up Cluster

In this sample we'll be using Azure Kubernetes Service, but you can install KEDA on any Kubernetes cluster.
Run [this script](deploy/deploy_aks.sh) to deploy an AKS cluster 

References:

* [Deploy AKS using Portal](https://docs.microsoft.com/en-us/azure/aks/kubernetes-walkthrough-portal)
* [Deploy AKS using CLI](https://docs.dapr.io/operations/hosting/kubernetes/cluster/setup-aks/)

## Install KEDA

* Run `kubectl create namespace keda`
* Run `helm install keda kedacore/keda --namespace keda`

## Create Blob Storage

1. Run [this script](deploy/deploy_storage.sh).

2. create the Kubernetes secret (replace *** with storage account Connection String):
     `kubectl create secret generic queue-connect-string --from-literal=CONNECTION_STRING=*********`

3. Create one Storage Account Queue, named "keda-queue" in the same Storage Account.

4. Using Azure Portal, create a Event Grid event for the Storage Account:

   * On the seleted Storage Account Overview page, select `Events`, and then select `+ Event Subscription` as follows: ![Create Event Subscription](images/create-event-subscription.png)

   * On the `Create Event Subscription` page, enter the values as follow: ![Create Event Subscription](images/new-event-subscription.png)

References:

* [Use Azure Event Grid to route Blob storage events to web endpoint (Azure portal) - Portal](https://docs.microsoft.com/en-us/azure/event-grid/blob-event-quickstart-portal)
* [Manage Azure Storage resources - CLI](https://docs.microsoft.com/en-us/cli/azure/storage?view=azure-cli-latest)

## Create ACR and attach to AKS

1. Create an Azure Container Registry (ACR) (Lowercase registry name is recommended to avoid warnings):

    ```powershell
    az acr create --resource-group <resource-group-name> --name <acr-name> --sku Basic
    ```

    Take note of loginServer in the output.

2. Integrate an existing ACR with existing AKS clusters:

    ```powershell
    az aks update -n <cluster-name> -g <resource-group-name> --attach-acr <acr-name>
    ```

3. Change ACR loginServer and name in `deploy/filesize-metric-provider.sh`

References:
[Create a private container registry using the Azure CLI](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli)

## Create File Processor pod

run `kubectl create deployment hello-pod --image=k8s.gcr.io/echoserver:1.4`

## Option 1: Build and Deploy Python Filesize Metric Provider pod and service

run `deploy/python/filesize-metric-provider.sh`

## Option 2: Build and Deploy Java Filesize Metric Provider pod and service

run `deploy/java/filesize-metric-provider.sh`

## Deploy Keda ScaleObject

run `kubectl apply -f  deploy/fileprocessor-scaledobject.yaml`

## Test the application

a. Upload a file of size > 1MB to 'order' container storage.
b. watch for file-processor pod to scale to 5 replicas: `watch kubectl get deployments file-processor`
