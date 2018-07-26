package edu.uw.spl.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.account.AccountException;
import edu.uw.ext.framework.account.AccountManager;
import edu.uw.ext.framework.account.Address;
import edu.uw.ext.framework.account.CreditCard;
import edu.uw.ext.framework.order.Order;

/**Encapsulates an Account
 * @author slajaunie
 *
 */
public class AccountImpl implements edu.uw.ext.framework.account.Account {
    /*The Account, and AccountFactory implementations classes shall 
     * enforce a minimum account name length of 8 and a minimum initial 
     * balance of $1000, furthermore the balance should be maintained in cents.*/
    
    private static final Logger log = LoggerFactory.getLogger(AccountImpl.class);
    
    /**Balance of the account, in cents (ex $1,000 = 100000)*/
    private int balance = Integer.MIN_VALUE;
    
    private Address address;
    
    private CreditCard creditCard;
    
    private String email="default";
    
    /**Full name of the account holder*/
    private String fullName="default";
    
    /**The account name/username*/
    private String name="default";
    
    /**Hashed version of a password String*/
    private byte[] passwordHash;
    
    private String phone="default";
    
    /*Account manager will interact with the DAO to control changes/updates to 
     * this Account
     */
    private transient AccountManager accountManager;
    
    /**
     *Instantiates a new account constructor 
     */
    public AccountImpl() {}
    
    /**Obtains the address associated with this account, or null if no value has been assigned
     * @see edu.uw.ext.framework.account.Account#getAddress()
     * @return an <code>Address</code> representing the address associated with this account
     */
    @Override
    public Address getAddress() {
        return this.address;
    }

    /**Obtains the current balance of this account, in cents, 
     * or null if no value has been assigned
     * @see edu.uw.ext.framework.account.Account#getBalance()
     * @return an int representing the balance of this account, in cents
     */
    @Override
    public int getBalance() {
        return this.balance;
    }

    /**Obtains the Credit card associated with this account, 
     * or null if no value has been assigned
     * @see edu.uw.ext.framework.account.Account#getCreditCard()
     * @return a <code>CreditCard</code> representing the credit card associated with this account
     */
    @Override
    public CreditCard getCreditCard() {
        return this.creditCard;
    }

    /**Obtains the email address associated with this account, 
     * or null if no value has been assigned
     * @see edu.uw.ext.framework.account.Account#getEmail()
     * @return a String representing the email address associated with this account
     */
    @Override
    public String getEmail() {
        return this.email;
    }

    /** Obtains the full name of the person associated with this account,
     * or null if no value has been assigned
     * @see edu.uw.ext.framework.account.Account#getFullName()
     * @return a String representing the full name associated with this account
     */
    @Override
    public String getFullName() {
        return this.fullName;
    }

    /**Obtains the name of this account, or null if no value has been assigned
     * @see edu.uw.ext.framework.account.Account#getName()
     * @return a <code>String</code> that is the name associated with this account
     */
    @Override
    public String getName() {
        return this.name;
    }

    /** Obtains the hashed password associated with this account
     * @see edu.uw.ext.framework.account.Account#getPasswordHash()
     * @return a <code>bye[]</code> representing the hashed password associated with this account
     */
    @Override
    public byte[] getPasswordHash() {
        byte[] copy = null;
        if (passwordHash != null) {
            /*give only an immutable defensive copy, not the actual value*/
            copy = new byte[passwordHash.length];
            System.arraycopy(passwordHash, 0, copy, 0, passwordHash.length);
        }
        return copy;
    }

    /** Obtains the phone number associated with this account, 
     * or null if no value has been assigned
     * @see edu.uw.ext.framework.account.Account#getPhone()
     * @return a String representing the phone number associated with this account
     */
    @Override
    public String getPhone() {
        return this.phone;
    }

    /**Incorporates the effect on an order in the balance
     * @see edu.uw.ext.framework.account.Account#reflectOrder(edu.uw.ext.framework.order.Order, int)
     * @param order the order to be reflected in the account
     * @param executionPrice the price at which the order was executed, in cents
     */
    @Override
    public void reflectOrder(final Order order, final int executionPrice) {
        try {
            balance += order.valueOfOrder(executionPrice);
            if (this.accountManager != null) {
                this.accountManager.persist(this);
            } else {
                log.error("Account manager has not been initialized.",
                             new Exception());
            }
        } catch (final AccountException ex) {
            log.error(String.format("Failed to persist account %s after adjusting for order.", name),
                         ex);
        }
        /*earlier implementation*/
        //this.balance += executionPrice;
        
    }

    /** Sets the account manager responsible for persisting/managing this account. 
     * This may be invoked exactly once on any given account, any subsequent invocations 
     * should be ignored. The account manager member should not be serialized with 
     * implementing class object.
     * @see edu.uw.ext.framework.account.Account#registerAccountManager(edu.uw.ext.framework.account.AccountManager)
     */
    @Override
    public void registerAccountManager(final AccountManager m) {
        if (this.accountManager == null) {
            this.accountManager = m;
        }
    }

    /**Sets the address associated with this account
     * @see edu.uw.ext.framework.account.Account#setAddress(edu.uw.ext.framework.account.Address)
     * @param address the address to be associated with this account
     */
    @Override
    public void setAddress(final Address address) {
        this.address = address;
    }

    /**Sets the balance on this account, in cents
     * @see edu.uw.ext.framework.account.Account#setBalance(int)
     * @param balance the balance on this account
     */
    @Override
    public void setBalance(final int balance) {
        this.balance = balance;
    }

    /**Sets the credit card to be associated with this account
     * @see edu.uw.ext.framework.account.Account#setCreditCard(edu.uw.ext.framework.account.CreditCard)
     * @param creditCard the credit card to be associated with this account
     */
    @Override
    public void setCreditCard(final CreditCard creditCard) {
        this.creditCard = creditCard;
    }

    /**Sets the email to be associated with this account
     * @see edu.uw.ext.framework.account.Account#setEmail(java.lang.String)
     * @param email the email to be associated with this account
     */
    @Override
    public void setEmail(final String email) {
        this.email = email;
    }

    /**Sets the full name of the accountholder associated with this account
     * @see edu.uw.ext.framework.account.Account#setFullName(java.lang.String)
     * @param fullName the full name of the accountholder associated with this account
     */
    @Override
    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    /**Sets the name of this account, which must be a minimum of 8 characters in length
     * @see edu.uw.ext.framework.account.Account#setName(java.lang.String)
     * @param name the name associated with this account
     * @throws AccountException if the account name is not excepted. Throws if the account
     * name is less than 8 characters in length
     */
    @Override
    public void setName(final String name) throws AccountException {
        /*Note AccountFactory also handles specific requirements 
         * to instantiate a new Account that may throw an 
         * account exception
         */
        if (name.length()<8) {
            throw new AccountException("Account name must be equal to or greater than"
                    + "8 characters in length");
        } else {
            this.name = name;
        }
    }

    /**Sets the hashed password associated with this account
     * @see edu.uw.ext.framework.account.Account#setPasswordHash(byte[])
     * @param passwordHash the hashed password associated with this account
     */
    @Override
    public void setPasswordHash(final byte[] passwordHash) {
        byte[] copy = null;
        if (passwordHash != null) {
            /*give only an immutable defensive copy, not the actual value*/
            copy = new byte[passwordHash.length];
            System.arraycopy(passwordHash, 0, copy, 0, passwordHash.length);
        }
        this.passwordHash = copy;
        
    }

    /**Sets the phone number associated with this account
     * @see edu.uw.ext.framework.account.Account#setPhone(java.lang.String)
     * @param phone the phone number to be associated with this account
     */
    @Override
    public void setPhone(final String phone) {
        this.phone = phone;
    }

}
