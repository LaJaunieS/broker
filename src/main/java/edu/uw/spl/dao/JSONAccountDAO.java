package edu.uw.spl.dao;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.uw.ext.framework.account.Account;
import edu.uw.ext.framework.account.AccountException;
import edu.uw.ext.framework.account.Address;
import edu.uw.ext.framework.account.CreditCard;
import edu.uw.ext.framework.dao.AccountDao;
import edu.uw.ext.framework.dao.DaoFactory;
import edu.uw.ext.framework.dao.DaoFactoryException;

import edu.uw.spl.dao.AccountDAOs;
import edu.uw.spl.account.AccountImpl;
import edu.uw.spl.account.AccountManagerFactoryImpl;
import edu.uw.spl.account.AccountManagerImpl;
import edu.uw.spl.account.CreditCardImpl;
import edu.uw.spl.account.AddressImpl;


/**Implentation which defines the methods needed to get and store accounts from a 
 * persistent storage mechanism, in JSON format
 * @author slajaunie
 *
 */
public class JSONAccountDAO extends AccountDAOs implements AccountDao, DaoFactory {

    /*parentDirectory stored in AccountDAOs object*/
    private static final Logger log = LoggerFactory.getLogger(JSONAccountDAO.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    /**
     *Instantiates a new AccountDao 
     */
    public JSONAccountDAO() {}
    
    /** 
     * Gets a new instance of an accountDao 
     * @throws DaoFactoryException if the operation fails
     * @see edu.uw.ext.framework.dao.DaoFactory#getAccountDao()
     */ 
    @Override
    public AccountDao getAccountDao() throws DaoFactoryException { 
        return new JSONAccountDAO();
    }

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
    
    
    /**Obtains the JSON data encapsulating an account and returns that data as an 
     * instance of Account, or null if the given account is not located
     * @param accountName the name of the account to look up
     * @return the given Account, or <code>null</code> if the account was not located
     * @see edu.uw.ext.framework.dao.AccountDao#getAccount(java.lang.String)
     */
    @Override
    public Account getAccount(final String accountName) {
        Account account = null;
        File accountDirectory = new File(this.getParentDirectory().toString(),accountName);
        File JSONFile = new File(accountDirectory, accountName+".json");
                
        if (!JSONFile.exists()) {
            log.info("Unable to locate directory for {}",accountName);
        } else {
                try {
                    
                    /*Map to concrete types*/
                    SimpleModule module = new SimpleModule();
                    module.addAbstractTypeMapping(Address.class, AddressImpl.class);
                    module.addAbstractTypeMapping(CreditCard.class, CreditCardImpl.class);
                    MAPPER.registerModule(module);
                    account = MAPPER.readValue(JSONFile, AccountImpl.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
               log.info("Account {} located",account.getName());
        }
        
        return account;
    }

    /** Persists new Accounts to the file Directory as a JSON file, or updates an existing account
     * @param account the account to be persisted
     * @see edu.uw.ext.framework.dao.AccountDao#setAccount(edu.uw.ext.framework.account.Account)
     * @throws AccountException if the operation failed
     */
    @Override
    public void setAccount(final Account account) throws AccountException {
        String accountName = account.getName();
        File accountDirectory = new File(this.getParentDirectory().toString(),accountName);
        File JsonFile = new File(accountDirectory.toString(), accountName + ".json");
        
        /*If account name directory doesn't exist, create it*/
        if (!accountDirectory.exists()) {
           accountDirectory.mkdir();
           log.info("Directory not found for account {}, creating",accountName);
        }
        
        /*Now map the account to Json format*/
        try {
            MAPPER.writeValue(JsonFile, account);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("Json for account {} updated",accountName);
    }

    /*Used for testing, otherwise ignore*/
    /*public static void main(String[] args) {
        try (ClassPathXmlApplicationContext context = 
                new ClassPathXmlApplicationContext("context.xml");
                )
        {
            JSONAccountDAO accountDao = new JSONAccountDAO();
            AccountManagerImpl accountManager;
            AccountManagerFactoryImpl accountManagerFactory = context.getBean("AccountManagerFactory", AccountManagerFactoryImpl.class);
            
            accountManager = (AccountManagerImpl) accountManagerFactory.newAccountManager(accountDao);
            
            Address address = context.getBean("Address",AddressImpl.class);
            CreditCard creditCard = context.getBean("CreditCard",CreditCardImpl.class);
                
            Address address2 = context.getBean("Address",AddressImpl.class);
            CreditCard creditCard2 = context.getBean("CreditCard",CreditCardImpl.class);
            
            Address address3 = context.getBean("Address",AddressImpl.class);
            CreditCard creditCard3 = context.getBean("CreditCard",CreditCardImpl.class);
            
            address.setCity("Seattle");
            address.setZipCode("98107");
            
            address2.setCity("New York");
            address2.setZipCode("12345");
            
            creditCard.setType("MasterChip");
            creditCard2.setType("VISA");
            creditCard3.setType("AMEX");
            
            
            //Instantiate and persist files of new accounts
            try {
                //Clear the directory
                accountDao.reset();
                
                //Create and persist some new accounts
                Account account1= accountManager.createAccount("XYZ_Account", "password84",
                        100000);
                Account account2 = accountManager.createAccount("ABC_Account", "password",
                        100000);
                Account account3 = accountManager.createAccount("DEF_Account", "password2", 
                        100000);
          
                //This should throw an exception as it's now a duplicate
                accountManager.createAccount("DEF_Account", "password2", 
                       100000);
                //System.out.print("Password hash for ABC_Account");
                //System.out.println(Arrays.toString(account2.getPasswordHash()));
                
                //Test that the password hash serialized and deserializes correctly
                boolean validated = accountManager.validateLogin("ABC_Account", "password");
                System.out.println("Validated? : " + validated);
                
            } catch (AccountException ex) {
                ex.printStackTrace();
            }
            
            
            try {
                //Now obtain an account and update its properties
                Account account = accountManager.getAccount("XYZ_Account");
                account.setAddress(address);
                account.setCreditCard(creditCard);
                //Persist the changes to the account
                accountManager.persist(account);

                Account account2 = accountManager.getAccount("ABC_Account");
                account2.setAddress(address2);
                account2.setCreditCard(creditCard2);
                //Persist the changes to the account
                accountManager.persist(account2);
                
                Account account3 = accountManager.getAccount("DEF_Account");
                account3.setAddress(address3);
                account3.setCreditCard(creditCard2);
                //Persist the changes to the account
                accountManager.persist(account3);
                
                //Now confirm updates took effect
                Account getAccount = accountManager.getAccount("ABC_Account");

                if (getAccount != null) {
                    boolean validated = accountManager.validateLogin("ABC_Account", "password");
                    System.out.println("Validated? : " + validated);
                } else {
                    System.out.println("Null property detected for object Account");
                }
                if (getAccount.getCreditCard() != null ) {
                    //Should print a non-null property value
                    System.out.println(getAccount.getCreditCard().getType());
                } else {
                    System.out.println("Null property detected for field creditCard");
                }
            } catch (AccountException ex2) {
                ex2.printStackTrace();
            }
        }
    }
    */
}
