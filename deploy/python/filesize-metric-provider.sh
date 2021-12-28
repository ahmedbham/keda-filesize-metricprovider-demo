cd keda-filesize-metricprovider-python

pip install -r requirements.txt

echo "Pushing filesize-metric-provider image to ACR"
acrLoginServer="dapr1batch.azurecr.io"
acrName="dapr1batch"

docker build -t $acrLoginServer/filesize-metric-provider:v1 .

# Log in to the registry
az acr login --name $acrName

# Push the image to the Azure Container Registry instance
docker push $acrLoginServer/filesize-metric-provider:v1

cd ..

# Deploy to AKS cluster
kubectl delete -f deploy/python/filesize-metric-provider.yaml
sleep 10
kubectl apply -f deploy/python/filesize-metric-provider.yaml