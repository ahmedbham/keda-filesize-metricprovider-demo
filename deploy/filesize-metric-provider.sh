cd keda-filesize-metricprovider-demo

echo "mvn install filesize-metric-provider"
./mvnw clean install


echo "build docker image" 
./mvnw spring-boot:build-image

echo "Pushing filesize-metric-provider image to ACR"

acrLoginServer="dapr1batch.azurecr.io"
acrName="dapr1batch"

# Log in to the registry
az acr login --name $acrName


# Tag the image
docker tag keda-filesize-metricprovider-demo:0.0.1-SNAPSHOT $acrLoginServer/filesize-metric-provider:v1

# Push the image to the Azure Container Registry instance
docker push $acrLoginServer/filesize-metric-provider:v1

cd ..

# Deploy to AKS cluster
kubectl delete -f deploy/filesize-metric-provider.yaml
sleep 10
kubectl apply -f deploy/filesize-metric-provider.yaml