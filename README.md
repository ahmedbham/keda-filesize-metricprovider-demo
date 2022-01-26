## Prerequisites

* [Docker](https://docs.docker.com/engine/install/)
* kubectl
* Azure CLI
* Helm3
* Python3

## Set up AKS Cluster

1. Setup name of key resources:
   * export aks_cluster_name=myAKSCluster
   * export resourceGroupName=filesize-rg
   * export location=eastus
   * export acr_name=filesizeacr

2. Create Resource Group
   * Run `az create group -n $resourceGroupName -l $location`
   * Run `az configure --defaults group=$resourceGroupName location=$location`

3. Create an Azure Container Registry (ACR) (Lowercase registry name is recommended to avoid warnings):
   * Run `az acr create --name $acr_name --sku Basic`

4. Create an AKS Cluster
   * Run: ```
          az aks create \
          --name $aks_cluster_name \
          --node-count 1 \
          --node-vm-size Standard_A2_v2 \
          --vm-set-type VirtualMachineScaleSets \
          --load-balancer-sku standard \
          --attach-acr $acr_name \
          --enable-addons monitoring \
          --generate-ssh-keys \
          --enable-cluster-autoscaler \
          --min-count 1 \
          --max-count 3
          ```

5. Run `az aks get-credentials -n $aks_cluster_name`

6. Change ACR name in `deploy/filesize-metric-provider.yaml`
   * Run `deploy_filesize_metric_provider_yaml="deploy/filesize-metric-provider.yaml"`
   * Run `cat $deploy_filesize_metric_provider_yaml | sed -e "s/ACR_NAME/$acr_name/g" > $deploy_filesize_metric_provider_yaml`

## Install KEDA

* Run `helm repo update`
* Run `helm repo add kedacore https://kedacore.github.io/charts`
* Run `kubectl create namespace keda`
* Run `helm install keda kedacore/keda --namespace keda`

## Create Blob Storage

1. Run `export storageAccountName="kedafilesizestorage"`
2. Run the following command:
   ```
   az storage account create \
    --name $storageAccountName \
    --resource-group $resourceGroupName \
    --location $location \
    --sku Standard_RAGRS \
    --kind StorageV2
   * Run `az storage account show -n <storageAccountName>` to verify if storage account was created successfully
   * Run `export connectionString=$(az storage account show-connection-string -n $storageAccountName -o tsv)`
   * Run `export storageContainerName=order`
   * Run `az storage container create -n $storageContainerName --auth-mode login --connection-string $connectionString`
   ```
3. Create the Kubernetes secret (replace *** with storage account Connection String):
    * Run `kubectl create secret generic queue-connect-string --from-literal=CONNECTION_STRING=$connectionString`

4. Create one Storage Account Queue, named `keda-queue` in the same Storage Account.
    * Run `storageQueueName=keda-queue`
    * Run `az storage queue create -n $storageQueueName --auth-mode login --connection-string $connectionString`

5. Create an Event Grid subscription for the Storage Account:
   * Run `storageAccountId=$(az storage account show -n $storageAccountName --query id -o tsv)`
   * Run `queueId=$storageAccountId/queueServices/default/queues/$storageQueueName`
   * Run `az eventgrid event-subscription create --name create-blob-event --endpoint $queueId --endpoint-type storagequeue --included-event-types Microsoft.Storage.BlobCreated --source-resource-id $storageAccountId`

References:

* [Use Azure Event Grid to route Blob storage events to web endpoint (Azure portal) - Portal](https://docs.microsoft.com/en-us/azure/event-grid/blob-event-quickstart-portal)
* [Manage Azure Storage resources - CLI](https://docs.microsoft.com/en-us/cli/azure/storage?view=azure-cli-latest)

## Create File Processor pod

* Run `kubectl apply -f deploy/file-processor.yaml`

## Build and Deploy Python Filesize Metric Provider pod and service

* Run `chmod +x deploy/filesize-metric-provider.sh`
* Run `./deploy/filesize-metric-provider.sh`
* Run `kubectl apply -f deploy/filesize-metric-provider.yaml`

## Deploy Keda ScaleObject

* Run `kubectl apply -f  deploy/fileprocessor-scaledobject.yaml`

## Test the application

1. Upload a file of size > 1MB to the container storage.
   * Run `az storage blob upload --container-name $storageContainerName --file sample-files/kitchen_renovation.pdf  --connection-string $connectionString`
2. watch for file-processor pod to scale to 5 replicas:
   * Run `watch kubectl get deployments file-processor`
