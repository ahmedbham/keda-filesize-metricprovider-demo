cd keda-filesize-metricprovider-python

pip install -r requirements.txt

echo "Pushing filesize-metric-provider image to ACR"
acrLoginServer="$acrName.azurecr.io"

docker build -t $acrLoginServer/filesize-metric-provider:v1 .

# Log in to the registry
az acr login --name $acrName

# Push the image to the Azure Container Registry instance
docker push $acrLoginServer/filesize-metric-provider:v1

cd ..

# Deploy to AKS cluster
kubectl apply -f deploy/filesize-metric-provider.yaml