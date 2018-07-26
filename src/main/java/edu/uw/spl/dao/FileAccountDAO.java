package edu.uw.spl.dao;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.uw.ext.framework.account.Account;
import edu.uw.ext.framework.account.AccountException;
import edu.uw.ext.framework.account.Address;
import edu.uw.ext.framework.account.CreditCard;
import edu.uw.ext.framework.dao.AccountDao;
import edu.uw.ext.framework.dao.DaoFactoryException;
import edu.uw.spl.account.AccountImpl;
import edu.uw.spl.account.CreditCardImpl;
import edu.uw.ext.framework.dao.DaoFactory;

import edu.uw.spl.account.AddressImpl;

/**Implentation which defines the methods needed to get, store, and delete accounts
 * from a persistent storage mechanism.  
 * @author slajaunie
 */ 
public class FileAccountDAO extends AccountDAOs implements AccountDao, DaoFactory{
    
    /*Right now the data source is the files in the target/accounts directory-
     *  this DAO reads/writes to/from those files- 
     *  a client app will interact with this DAO to read/write
     * data from the data source
     */
    
    /*The parent directory where accounts will be stored*/
    private static final File PARENT_DIRECTORY= new File("target", "accounts");
    
    private static final Logger log = LoggerFactory.getLogger(FileAccountDAO.class);
    
    /**
     *Instantiates a new AccountDao 
     */
    public FileAccountDAO() {}
    
    /**Closes the DAO, releasing any resources used by this DAO. If the DAO is already
     * closed then invoking this method has no effect.
     * @throws AccountException if the operation fails
     * @see edu.uw.ext.framework.dao.AccountDao#close()
     */
    @Override
    public void close() throws AccountException {
        /*closes any open data streams- currently, any open data streams are 
        closed automatically in the try-with-resources statements within their 
        respective methods*/ 
    }

    /**Lookup and return an Account from the given account name. If no such account is
     * located, returns null
     * @param accountName the name of the Account to lookup
     * @see edu.uw.ext.framework.dao.AccountDao#getAccount(java.lang.String)
     * @return the given Account, or <code>null</code> if the account was not located
     */
    @Override
    public Account getAccount(final String accountName) {
        /*read an account from an input stream
        *construct an Account instance from the read files and return the Account*/
        Account account = null;
        CreditCard creditCard = null;
        Address address = null; 
        
        File accountDirectory = new File(PARENT_DIRECTORY.toString(),accountName);
        
        if (!accountDirectory.exists()) {
            log.info("Unable to locate directory for Account {}",accountName);
        } else {
            File accountFile = new File(accountDirectory.toString(), "account.bin");
            File creditCardFile = new File(accountDirectory.toString(), "creditCard.bin");
            File addressFile = new File(accountDirectory.toString(),"address.bin");
            
            /*Now take the account and read each object from their respective file*/
            try (
                    /*input Streams for the files*/
                    InputStream isAccountFile = Files.newInputStream(accountFile.toPath());
                    DataInputStream disAccountFile = new DataInputStream(isAccountFile);
                    
                    InputStream isCreditCardFile = Files.newInputStream(creditCardFile.toPath());
                    DataInputStream disCreditCardFile = new DataInputStream(isCreditCardFile);
                    ObjectInputStream oisCreditCardFile = new ObjectInputStream(disCreditCardFile);
                    
                    InputStream isAddressFile = Files.newInputStream(addressFile.toPath());
                    DataInputStream disAddressFile = new DataInputStream(isAddressFile);
                    ObjectInputStream oisAddressFile = new ObjectInputStream(disAddressFile);
                    
                ){
                
                /*Write each property of the object separate to the dos*/
                /*Set the password hash*/
                
                /*First value encoded is a short indicating the length of the pw array*/
                short pwLength = disAccountFile.readShort();  
                
                /*Use that value to create a new pw array*/
                byte[] pw = new byte[pwLength];
                
                disAccountFile.read(pw,0,pwLength);
                account = new AccountImpl();
                account.setPasswordHash(pw);
                account.setName(disAccountFile.readUTF());
                account.setBalance(disAccountFile.readInt());
                /*read the email address*/
                account.setEmail(disAccountFile.readUTF());
                /*read the fullname*/
                account.setFullName(disAccountFile.readUTF());
                /*read the phone number*/
                account.setPhone(disAccountFile.readUTF());
                
                /*read the returned objects*/
                creditCard = (CreditCardImpl) oisCreditCardFile.readObject();
                address = (AddressImpl) oisAddressFile.readObject();
                
                /*set specific attributes of the returned account*/
                account.setAddress(address);
                account.setCreditCard(creditCard);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (AccountException e) {
                e.printStackTrace();
            }
            
            /*Log if account is going to return null*/
            if (account != null) {
                log.info("Get for Account {} successful",accountName);
            } else {
                log.info("Get for Account {} unsuccessful; account will be null",accountName);
            }
        }
        return account;
    }

    /** Persists new Accounts to the file Directory, or updates an existing account
     * @param account the account to be persisted
     * @see edu.uw.ext.framework.dao.AccountDao#setAccount(edu.uw.ext.framework.account.Account)
     * @throws AccountException if the operation failed
     */
    @Override
    public void setAccount(final Account account) throws AccountException {
        /*Break up instances into separate files and write as binary files*/
        /*Creates a new directory for the account if it doesn't currently exist*/
        
        String accountName = account.getName();
        File accountDirectory = new File(PARENT_DIRECTORY.toString(),accountName);
        
        File accountFile = new File(accountDirectory.toString(), "account.bin");
        
        File creditCardFile = new File(accountDirectory.toString(), "creditCard.bin");
        File addressFile = new File(accountDirectory.toString(),"address.bin");

        /*If account name directory doesn't exist, create it*/
         if (!accountDirectory.exists()) {
            final boolean success = accountDirectory.mkdirs();
            if (!success) {
                throw new AccountException(String.format("Unable to create directory %s",
                                                        accountDirectory));
            }
            log.info("Directory not found, creating");
         } 
         
        /*Now take the account and write each object to the appropriate file*/
        try (
                /*output Streams for the files*/
                OutputStream osAccountFile = Files.newOutputStream(accountFile.toPath());
                DataOutputStream dosAccountFile = new DataOutputStream(osAccountFile);
                
                OutputStream osCreditCardFile = Files.newOutputStream(creditCardFile.toPath());
                DataOutputStream dosCreditCardFile = new DataOutputStream(osCreditCardFile);
                
                OutputStream osAddressFile = Files.newOutputStream(addressFile.toPath());
                DataOutputStream dosAddressFile = new DataOutputStream(osAddressFile);
                
                /*output streams for the objects*/
                ByteArrayOutputStream baosAccountObject = new ByteArrayOutputStream();
                ObjectOutputStream osAccountObject = new ObjectOutputStream(baosAccountObject);
              
                ByteArrayOutputStream baosCreditCardObject = new ByteArrayOutputStream();
                ObjectOutputStream osCreditCardObject = new ObjectOutputStream(baosCreditCardObject);
                
                ByteArrayOutputStream baosAddressObject = new ByteArrayOutputStream();
                ObjectOutputStream osAddressObject = new ObjectOutputStream(baosAddressObject);
                ){
            /*Write each property of the object separate to the dos*/
            /*Encode the length of the pw byte array for reference later*/
            dosAccountFile.writeShort(account.getPasswordHash().length);
            dosAccountFile.write(account.getPasswordHash(),0,account.getPasswordHash().length);
            
            dosAccountFile.writeUTF(account.getName());
            dosAccountFile.writeInt(account.getBalance());
            /*Now deal with the potentially null values*/
            if (account.getEmail()!=null) {
                dosAccountFile.writeUTF(account.getEmail());
            }
            if (account.getFullName() != null) {
                dosAccountFile.writeUTF(account.getFullName());
            }
            if (account.getPhone()!=null) {
                dosAccountFile.writeUTF(account.getPhone());
            }
            
            osAccountObject.writeObject(account);
            osAccountObject.flush();
            dosAccountFile.write(baosAccountObject.toByteArray());
             
            osCreditCardObject.writeObject(account.getCreditCard());
            osCreditCardObject.flush();
            dosCreditCardFile.write(baosCreditCardObject.toByteArray());
 
            osAddressObject.writeObject(account.getAddress());
            osAddressObject.flush();
            dosAddressFile.write(baosAddressObject.toByteArray());
        } catch (IOException e) {
            throw new AccountException("There was a problem writing to the output streams",e);
        }
        log.info("Files for Account {} persisted",account.getName());
    }
    
    /**Checks to see if an account already exists, returns true if accountName
     * already exists, otherwise false- not currently using, but keeping just in
     * case it proves useful later. 
     * The operation distinguishes between adding a new account or updating an existing
     * account by checking for a name already existing in the directory with the 
     * given Account's name. If a match is found, the hashed passwords 
     * between the two accounts are compared- if they match, it is assumed this 
     * is an update to an existing account. If they do not match, it is assumed
     * this is an attempt to add a new account with a duplicte name
     * @param account the account to check for duplicates
     * @return a <code>boolean</code> indicating whether a duplicate exists- true if
     * an account by that name already exists, otherwise false
     */
    public boolean checkForDuplicateUsername(final Account account) {
        boolean validUpdate = false;
        File[] listFiles = PARENT_DIRECTORY.listFiles();
        Account persistedAccount; 
        
        for (File directory: listFiles) {
            /*Go through each directory name and see if it matches the given
             * account's username
             * If there's a match, need to see if updating existing account
             * or if trying to provide a new account with a duplicate username
             * If the persisted object's username and password fields are equal
             * we're simply overwriting an existing account
             * If they are not equal, the given account is not the same account
             * as the existing persisted one, and there's a problem*/
            if (directory.getName().equals(account.getName())) {
                persistedAccount = this.getAccount(account.getName());
                /*...if true, need to get the persisted account object and 
                 * check for equality against the given account object
                 */
                if (Arrays.equals(persistedAccount.getPasswordHash(), 
                        account.getPasswordHash())) {
                    /*If they match, only updating account and it's fine*/
                    log.info("Passwords match, only updating");
                    validUpdate = true;
                } else {
                    /*...otherwise this is a duplicate username on a different
                     * account and thus not valid
                     */
                    validUpdate = false;
                }
                
            }
            
        }
        return validUpdate;
    }

    /** 
     * Gets a new instance of an accountDao 
     * @throws DaoFactoryException if the operation fails
     * @see edu.uw.ext.framework.dao.DaoFactory#getAccountDao()
     */ 
    @Override
    public AccountDao getAccountDao() throws DaoFactoryException {
        return new FileAccountDAO();
    }

    /*Used for testing*/
    /*public static void main(String[] args) {
        BeanFactory context = new ClassPathXmlApplicationContext("context.xml");
        
        FileAccountDAO accountDao = new FileAccountDAO();
        AccountManagerImpl accountManager;
        AccountManagerFactoryImpl accountManagerFactory = context.getBean("AccountManagerFactory", AccountManagerFactoryImpl.class);
        
        accountManager = (AccountManagerImpl) accountManagerFactory.newAccountManager(accountDao);
    
        AddressImpl address = new AddressImpl();
        CreditCardImpl creditCard = new CreditCardImpl();
        
        AddressImpl address2 = new AddressImpl();
        CreditCardImpl creditCard2 = new CreditCardImpl();
        
        AddressImpl address3 = new AddressImpl();
        CreditCardImpl creditCard3 = new CreditCardImpl();
        
        creditCard.setType("MasterChip");
        
        try {
            accountDao.reset();
            
            accountManager.createAccount("XYZ_Account", "password",
                    100000);
            accountManager.createAccount("ABC_Account", "password",
                    100000);
            
            accountManager.createAccount("DEF_Account", "password2", 100000);
            
            
            
            Account account = accountManager.getAccount("XYZ_Account");
            account.setEmail("example@me.com");
            account.setFullName("John Smith");
            account.setPhone("123-456-7890");
            account.setAddress(address);
            account.setCreditCard(creditCard2);
            //Persist the changes to the account
            accountDao.setAccount(account);
    
            Account account2 = accountManager.getAccount("ABC_Account");
            account2.setAddress(address2);
            account2.setCreditCard(creditCard2);
            accountDao.setAccount(account2);
            
            Account account3 = accountManager.getAccount("DEF_Account");
            account3.setAddress(address3);
            account3.setCreditCard(creditCard2);
            
            //Persist the changes to the account
            accountManager.persist(account3);
    
            //Now confirm changes updated
            Account acct = accountManager.getAccount("XYZ_Account");
//          accountDao.deleteAccount("XYZ_Account");
//          accountDao.reset();
//          accountDao.reset();
          System.out.println("Obtained account: " + acct.getName() );
          System.out.println("Account email: " + acct.getEmail());
          System.out.println("Account full name: " + acct.getFullName());
          
          System.out.println("CC type: " + acct.getCreditCard());
          System.out.println("Phone number: " + acct.getPhone());

            //This should throw an exception for duplicate account...
            accountManager.createAccount("XYZ_Account", "password2", 100000);
            
        } catch (AccountException e) {
            e.printStackTrace();
        }
        
        Account account5 = (Account) context.getBean("Account",AccountImpl.class);
        try {
            account5.setName("123 Company");
        } catch (AccountException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        AccountFactoryImpl accountFactory;
        accountFactory= context.getBean("AccountFactory", AccountFactoryImpl.class);
        // VVProving it's not nullVV
        System.out.println(accountFactory);
        
        accountFactory.newAccount("Bob", new byte[] {},
                800);
        System.out.println("Bean created for " + account5.getName());
    }*/
}
