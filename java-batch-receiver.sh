echo "mvn install java-batch-receiver"
./mvnw clean install

echo "Pushing java-batch-receiver image to ACR"

acrLoginServer="dapr1batch.azurecr.io"
acrName="dapr1batch"

# Log in to the registry
az acr login --name $acrName

# Build an image 
./mvnw spring-boot:build-image

# Tag the image
docker tag batch-receiver:0.0.1-SNAPSHOT $acrLoginServer/java-batch-receiver:v1

# Push the image to the Azure Container Registry instance
docker push $acrLoginServer/java-batch-receiver:v1