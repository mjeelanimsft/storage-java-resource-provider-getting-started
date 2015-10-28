/*----------------------------------------------------------------------------------
* Microsoft Developer & Platform Evangelism
*
* Copyright (c) Microsoft Corporation. All rights reserved.
*
* THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND,
* EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE.
* ----------------------------------------------------------------------------------
* The example companies, organizations, products, domain names,
* e-mail addresses, logos, people, places, and events depicted
* herein are fictitious.  No association with any real company,
* organization, product, domain name, email address, logo, person,
* places, or events is intended or should be inferred.
* ----------------------------------------------------------------------------------
*/

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.resources.ResourceManagementService;
import com.microsoft.azure.management.resources.models.ResourceGroup;
import com.microsoft.azure.management.storage.StorageManagementClient;
import com.microsoft.azure.management.storage.StorageManagementService;
import com.microsoft.azure.management.storage.models.AccountType;
import com.microsoft.azure.management.storage.models.KeyName;
import com.microsoft.azure.management.storage.models.StorageAccount;
import com.microsoft.azure.management.storage.models.StorageAccountCreateParameters;
import com.microsoft.azure.management.storage.models.StorageAccountKeys;
import com.microsoft.azure.management.storage.models.StorageAccountUpdateParameters;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;


/*
 * Azure Storage Resource Provider Sample - Demonstrate how to create and manage storage accounts using Storage Resource Provider. 
 * Azure Storage Resource Provider enables customers to create and manage storage accounts 
 *
 * Documentation References:
 *  - What is a Storage Account - http://azure.microsoft.com/en-us/documentation/articles/storage-whatis-account
 *  - Storage Resource Provider REST API documentation - https://msdn.microsoft.com/en-us/library/azure/mt163683.aspx 
 *
 * Instructions:
 *      This sample can only be run using your Azure Storage account by updating the config.properties file with your "subscriptionId", "ApplicationId", "TenantId" and "Password".
 *      
 *      Please note: Follow the steps in the ReadMe.md file to add an application to Azure AD, set permissions and get your ApplicationID, TenantID and Password 
 */

public class StorageResourceProviderSample {
    protected static String subscriptionId;

    //Resource Group Name. Replace this with a Resource Group of your choice.
    protected static String rgName = "TestResourceGroup";
    
    //Storage Account Name. Replace this with a storage account of your choice.
    protected static String accountName = "stractemp";
    
    protected static String applicationId;
    protected static String password;
    protected static String tenantId;

    // These are used to create default accounts. You can choose any location and any storage account type.
    protected final static String defaultLocation = "westus";
    protected final static AccountType defaultAccountType = AccountType.STANDARDGRS;
    protected static ExecutorService service = null;

    public static void main(String[] args) 
    {
        try
        {
            //Load SubscriptionID, TenantID, Application ID and Password from the config file
            LoadConfiguration();
            
            //Fetch the Auth Headers for our requests
            String token = GetAuthorizationHeader();
        
            //Create Configuration Object
            Configuration config = ManagementConfiguration.configure(
                    null,
                    new URI("https://management.azure.com/"),
                    subscriptionId,
                    token);
            
            //Create the Resource Management Client, we will use this for creating a Resource Group later
            ResourceManagementClient resourcesClient = ResourceManagementService.create(config); 
            
            //Create the Resource Management Client, we will use this for managing storage accounts
            StorageManagementClient storageMgmtClient = StorageManagementService.create(config);
        
            //Create a new resource group
            CreateResourceGroup(rgName, resourcesClient);

            //Register our subscription with the Storage Resource Provider
            resourcesClient.getProvidersOperations().register("Microsoft.Storage");
            
            //Create a new account in a specific resource group with the specified account name
            CreateStorageAccount(rgName, accountName, storageMgmtClient);

            //Get all the account properties for a given resource group and account name
            StorageAccount storAcct = storageMgmtClient.getStorageAccountsOperations().getProperties(rgName, accountName).getStorageAccount();

            //Get a list of storage accounts within a specific resource group
            ArrayList<StorageAccount> storAccts = storageMgmtClient.getStorageAccountsOperations().listByResourceGroup(rgName).getStorageAccounts();

            //Get all the storage accounts for a given subscription
            ArrayList<StorageAccount> storAcctsSub = storageMgmtClient.getStorageAccountsOperations().list().getStorageAccounts();

            //Get the storage account keys for a given account and resource group
            StorageAccountKeys acctKeys = storageMgmtClient.getStorageAccountsOperations().listKeys(rgName, accountName).getStorageAccountKeys();

            //Regenerate the account key for a given account in a specific resource group
            StorageAccountKeys regenAcctKeys = storageMgmtClient.getStorageAccountsOperations().regenerateKey(rgName, accountName, KeyName.KEY1).getStorageAccountKeys();

            //Update the storage account for a given account name and resource group
            UpdateStorageAccount(rgName, accountName, storageMgmtClient);

            //Check if the account name is available
            boolean nameAvailable = storageMgmtClient.getStorageAccountsOperations().checkNameAvailability(accountName).isNameAvailable();
            
            //Delete a storage account with the given account name and a resource group
            DeleteStorageAccount(rgName, accountName, storageMgmtClient);
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    
    /*
     * The following method will load the SubscriptionID, TenantID, ApplicationID and Password from the config.properties file
     *
     * @throws RuntimeException, IOException
     */
    private static void LoadConfiguration() throws RuntimeException, IOException
    {
        Properties prop = new Properties();
        try {
            InputStream propertyStream = StorageResourceProviderSample.class.getClassLoader().getResourceAsStream("config.properties");
            if (propertyStream != null) {
                prop.load(propertyStream);
            }
            else {
                throw new RuntimeException();
            }
        } catch (RuntimeException|IOException e) {
            System.out.println("\nFailed to load config.properties file.");
            throw e;
        }
        
        subscriptionId = prop.getProperty("SubscriptionId");
        applicationId = prop.getProperty("ApplicationId");
        password = prop.getProperty("Password");
        tenantId = prop.getProperty("TenantId");
    }
    
    /*
     * The following method will enable you to use the token to create credentials
     *
     * @throws MalformedURLException, Exception
     */
    private static String GetAuthorizationHeader() throws MalformedURLException, Exception
    {           
        ClientCredential cc = new ClientCredential(applicationId, password);
        String token = null;
        
        service = Executors.newFixedThreadPool(1);
        AuthenticationContext context;
        try {
            context = new AuthenticationContext("https://login.windows.net/" + tenantId, false, service);
        
        Future<AuthenticationResult> result = context.acquireToken("https://management.azure.com/", cc, null);

        if (result == null)
        {
            throw new Exception("Failed to obtain the JWT token");
        }

        token = result.get().getAccessToken();
        }
        catch (MalformedURLException e) {
            System.out.println("URL is incorrect.");
        }
        finally
        {
            return token;
        }
    }

     /* Creates a new resource group with the specified name
      * If one already exists then it gets updated
      * 
      * @param String The {@link rgname} to create or update
      * @param ResourceManagementClient The {@link resourcesClient} to use
      * 
      * @throws URISyntaxException, IOException
      */
    public static void CreateResourceGroup(String rgname, ResourceManagementClient resourcesClient) throws URISyntaxException, IOException, ServiceException, URISyntaxException
    {
        ResourceGroup rgroup = new ResourceGroup();
        rgroup.setLocation(defaultLocation);
        
        try
        {
            resourcesClient.getResourceGroupsOperations().createOrUpdate(rgname, rgroup);
        }
        catch(URISyntaxException | IOException e)
        {
            System.out.println(e.getMessage());
        }
    }

    /* Create a new Storage Account. If one already exists then the request still succeeds
     * 
     * @param String The {@link rgname} to use
     * @param String The {@link acctName} to create
     * @param StorageManagementClient The {@link storageMgmtClient} to use
     * 
     * @throws InterruptedException, ExecutionException, IOException
     *  
     */
    private static void CreateStorageAccount(String rgname, String acctName, StorageManagementClient storageMgmtClient)
    {                                                                       
        StorageAccountCreateParameters parameters = GetDefaultStorageAccountParameters();
        System.out.println("Creating a storage account...");
        
        try {
            storageMgmtClient.getStorageAccountsOperations().create(rgname, acctName, parameters);
        } catch (InterruptedException | ExecutionException | IOException e) {
            System.out.println("Error creating storage account - " + e.getMessage());
        }
        System.out.println("Storage account created with name " + acctName);                                                                     
    }

    /* Deletes a storage account for the specified account name
     * 
     * @param String The {@link rgname} to use
     * @param String The {@link acctName} to delete
     * @param StorageManagementClient The {@link storageMgmtClient} to use
     * 
     * @throws ServiceException, IOException
     *  
     */
    private static void DeleteStorageAccount(String rgname, String acctName, StorageManagementClient storageMgmtClient)
    {
        System.out.println("Deleting a storage account...");
        try {
            storageMgmtClient.getStorageAccountsOperations().delete(rgname, acctName);
        } catch (IOException | ServiceException e) {
            System.out.println("Error deleting storage account - " + e.getMessage());
        }
        System.out.println("Storage account " + acctName + " deleted");
    }

    /* Updates the storage account
     * 
     * @param String The {@link rgname} to use
     * @param String The {@link acctName} to update
     * @param StorageManagementClient The {@link storageMgmtClient} to use
     * 
     * @throws ServiceException, IOException, URISyntaxException
     *  
     */
    private static void UpdateStorageAccount(String rgname, String acctName, StorageManagementClient storageMgmtClient)
    {
        System.out.println("Updating storage account...");

        // Update storage account type
        StorageAccountUpdateParameters parameters = new StorageAccountUpdateParameters();
        parameters.setAccountType(AccountType.STANDARDLRS);
        
        try {
            storageMgmtClient.getStorageAccountsOperations().update(rgname, acctName, parameters);
        } catch (IOException | ServiceException | URISyntaxException e) {
            System.out.println("Error updating storage account - " + e.getMessage());
        }
        System.out.println("Account type on storage account updated.");
    }

    /* Returns default values to create a storage account
     */
    private static StorageAccountCreateParameters GetDefaultStorageAccountParameters()
    {
        HashMap<String, String> defaultTags;defaultTags = new HashMap<String, String>();
        defaultTags.put("key1", "value1");
        defaultTags.put("key2", "value2");
        
        StorageAccountCreateParameters params = new StorageAccountCreateParameters();
        params.setLocation(defaultLocation);
        params.setTags(defaultTags);
        params.setAccountType(defaultAccountType);
        
        return params;
    }
}