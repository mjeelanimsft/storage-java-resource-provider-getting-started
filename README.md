---
services: storage
platforms: java
author: mjeelanimsft
---

# Azure Storage : Storage Resource Provider
Demonstrates Basic Operations with Azure Storage Resource Provider

## About the code
This sample demonstrates how to manage your storage accounts using the Storage Resource Provider Java APIs. 

Storage Resource Provider enables you to manage your storage account and keys programmatically while inheriting the benefits of Azure Resource Manager

Note: If you don't have a Microsoft Azure subscription you can get a FREE trial account [here](http://go.microsoft.com/fwlink/?LinkId=330212)

## Running this sample

This sample can be run by updating the parameters indicated in config.properties 

To run the sample -

1. Setup an application and permissions by following the steps in the "Add an application to Azure AD and set permissions" section of this readme file
2. Update the config.properties file with values of subscriptionid, applicationId, password, tenantId which you obtain from the previous step
3. Set breakpoints and run the project.


## Add an application to Azure AD and set permissions
To use Azure AD to authenticate requests to Azure Resource Manager, an application must be added to the Default Directory. Do the following to add an application:

1. Open an Azure PowerShell prompt, and then run this command, and enter the credentials for your subscription when prompted:

    **Login-AzureRmAccount**

2. Replace {password} in the following command with the one that you want to use and then run it to create the application:

    **New-AzureRmADApplication -DisplayName "My AD Application 1" -HomePage "https://myapp1.com" -IdentifierUris "https://myapp1.com"  -Password "{password}"**
    
**NOTE:**
*Take note of the application identifer that is returned after the application is created because you'll need it for the next step. You can also find the application identifier in the client id field of the application in the Active Directory section of the portal.*

3. Replace {application-id} with the identifier that you just recorded and then create the service principal for the application:

    **New-AzureRmADServicePrincipal -ApplicationId {application-id}**

4. Set the permission to use the application:

    **New-AzureRmRoleAssignment -RoleDefinitionName Owner -ServicePrincipalName "https://myapp1.com"**


## More information
- [What is a Storage Account](http://azure.microsoft.com/en-us/documentation/articles/storage-whatis-account/)
- [Azure Storage Resource Provider REST API reference](https://msdn.microsoft.com/en-us/library/azure/Mt163683.aspx)