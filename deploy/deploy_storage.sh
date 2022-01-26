storageAccountName="kedafilesizestorage"
resourceGroupName="ca-rg"
location="eastus"

echo "Creating storage account..."
az storage account create \
    --name $storageAccountName \
    --resource-group $resourceGroupName \
    --location $location \
    --sku Standard_RAGRS \
    --kind StorageV2
    
echo "Creating blob container..."
az storage container create \
    --name orders \
    --account-name $storageAccountName \
    --auth-mode login
    
echo "Getting storage account connection string..."
az storage account show-connection-string --name $storageAccountName --resource-group $resourceGroupName