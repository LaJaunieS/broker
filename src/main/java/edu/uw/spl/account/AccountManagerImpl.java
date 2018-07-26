package edu.uw.spl.account;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.uw.ext.framework.account.Account;

import edu.uw.ext.framework.account.AccountException;
import edu.uw.ext.framework.account.AccountFactory;
import edu.uw.ext.framework.dao.AccountDao;
import edu.uw.ext.framework.account.AccountManagerFactory;
import edu.uw.ext.framework.account.AccountManager;

import edu.uw.spl.account.AccountFactoryImpl;

/**Manages interactions, such as adds, updates, or deletes, between accounts and the DAO.
 * Additionally encrypts new accounts' passwords into a <code>byte[]</code> 
 * @author slajaunie
 */
public class AccountManagerImpl 
            implements AccountManager {
    /*Interacts with the AccountDAO to perform operations on the Accounts*/
    /*Responsible for protecting passwords via MessageDigest*/
    /*The MessageDigest class must be used to hash the passwords prior to 
     * placing them in an Account object.  The AccountManager will use 
     * an instance of an AccountDao implementation to persist and retrieve accounts.*/
    
    private AccountDao accountDao = null;
    private AccountFactory accountFactory;
    
    /**character encoding to use when converting strings to/from bytes*/
    private final String ENCODING = "ISO-8859-1";
    
    /**hashing algorithm*/
    private final String ALGORITHM = "SHA-256";
            
    /**
     *Instantiates a new account manager
     */
    public AccountManagerImpl() {}

    /**Obtains the Account DAO used by this account manger, or null if no account DAO
     * has been set
     * @return the account DAO used by this account manager, or null if no account DAO has 
     * been set
     */
    public AccountDao getAccountDAO() {
        
        return this.accountDao;
    }
    
    /**Sets the Account DAO used by this account manager
     * @param accountDao the account DAO to be used by this account manager
     */
    public void setAccountDAO(final AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    /**Releases any resources used by the AccountManager implementation.
     * @throws AccountException if unable to close resources
     */
    @Override
    public void close() throws AccountException {
        /*close any open resources via the DAO*/
        try {
            accountDao.close();
            accountDao=null;
        } catch (AccountException ex) {
            throw new AccountException("Unable to release resources",ex);
        }
        
    }

    /**Creates a new account and persists it via the DAO to the file directory. 
     * The DAO will determine if this is a duplicate account to an account that already
     * exists, and if so, will throw an AccountException
     * @param accountName the name of the account to add
     * @param password the password used to gain access to the account
     * @param balance the initial balance of the account, in cents
     * @throws AccountException if the account is a duplicate of an account
     * that already exists, or otherwise unable to create an account
     * @return the newly created account
     */
    @Override
    public Account createAccount(final String accountName, 
                                final String password, 
                                final int balance)
                                        throws AccountException {
        /*The account to create and persist via the DAO*/
        Account account = null;
        
        AccountFactory accountFactory = new AccountFactoryImpl();
        /*Will be supplied to the AccountFactory method*/
        byte[] hashedPassword = null;
        
        /*Hash the password*/
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(ALGORITHM);
            md.update(password.getBytes());
            hashedPassword = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        
        /*The account factory will verify parameters meet requirements for new accounts*/
        account = accountFactory.newAccount(accountName, hashedPassword, balance);
        
        /*Check if there was an issue, making account == null at this point*/
        if (account == null) {
            throw new AccountException("There was a problem instantiating the new account");
        }
        /*Confirm whether a duplicate account exists*/
        Account possibleDuplicateAccount = this.getAccount(accountName);
        if (possibleDuplicateAccount != null) {
            throw new AccountException("Unable to create duplicate account");
        } else {
            /*persist the account*/
            try {
                this.persist(account);
                account.registerAccountManager(this);
            } catch (AccountException ex) {
                throw new AccountException("Unable to persist account",ex);
            }
        }
        
        return account;
    }

    /**Removes the account from the directory via the DAO
     * @param accountName the name of the account to remove
     * @throws AccountException if the operation failed
     */
    @Override
    public void deleteAccount(final String accountName) throws AccountException {
        /*interact with the DAO to delete an account*/
        try {
            accountDao.deleteAccount(accountName);
        } catch (AccountException e) {
            throw new AccountException(e.getMessage());
        }
        
    }

    /**Lookup an account based on the given account name, and returns that account,
     * or <code>null</code> if the account was not located in the directory 
     * @param accountName the name of the account to retrieve
     * @return the account associated with the given account name, or null if the 
     * account was not located in the directory
     * @throws AccountException if the operation failed
     */
    @Override
    public Account getAccount(final String accountName) throws AccountException {
        /* get an account via the DAO*/
        Account account = accountDao.getAccount(accountName);
        if (account != null) {
            account.registerAccountManager(this);
        }
        /*Prefer to throw exception on null return but tests won't pass*/
//        if (account == null) {
//            throw new AccountException("The account was not found");
//        }
        return account;
        
    }

    /**Persists an account to the file directory
     * @param account the account to persist
     * @throws AccountException if the operation fails
     */
    @Override
    public void persist(final Account account) throws AccountException {
        /* interact with the DAO to persist the account*/
        try {
            accountDao.setAccount(account);
        } catch (AccountException ex) {
            ex.printStackTrace();
            throw new AccountException("Uanble to persist Account: " 
                                                        + ex.getMessage(),ex);
        }
    }
    
    /**Checks whether a login is valid. The Account must be located in the directory
     * and the password must match the stored password
     * @param accountName the name of the account to validate
     * @param password the password on the given account
     * @return true if the login was validated, otherwise false
     * @throws AccountException if the given account could not be located
     */
    @Override
    public boolean validateLogin(final String accountName, 
                                final String password) 
                                                    throws AccountException {
        boolean validated = false;
        try {
            MessageDigest mdInput = MessageDigest.getInstance(ALGORITHM);
            mdInput.update(password.getBytes());
            byte[] inputHashed = mdInput.digest();
            Account targetAccount = this.getAccount(accountName);
            /*If the account doesn't exist, return false and quit...*/
            if (targetAccount != null) {
                /*...If it does exist, verify the passwords match*/
                validated = MessageDigest.isEqual(inputHashed, 
                        targetAccount.getPasswordHash());
            } else {
                /*if targetAccount is null, couldn't locate the account*/
                validated = false;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new AccountException(
                    String.format("Unable to find hash algorithm %s",ALGORITHM),e);
        }
         
        return validated;
    }

//    /**Instantiates a new Account Manager instance
//     * @param accountDAO the DAO to be used by the account manager
//     * @return the newly instantiated account manager
//     */
//    @Override
//    public AccountManager newAccountManager(final AccountDao accountDAO) {
//        AccountManagerImpl accountManager = new AccountManagerImpl();
//        accountManager.setAccountDAO(accountDao);
//        return accountManager;
//    }
    
}
