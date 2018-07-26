package edu.uw.spl.account;

import edu.uw.ext.framework.account.Account;
import edu.uw.ext.framework.account.AccountException;
import edu.uw.ext.framework.account.AccountFactory;

/**Used to instantiate new instances of <code>Account</code>
 * @author slajaunie
 *
 */
public class AccountFactoryImpl implements AccountFactory{

    /**
     *Instantiates a new Account Factory
     */
    public AccountFactoryImpl() {}
    
    /**Creates new instances of Accounts. Defines the requirements for the creation of a valid
     * account: 
     * <ol><li>The account name must be a minimum of 8 characters in length</li>
     * <li>The initial balance must be at least $1000, in cents</li>
     * </ol>
     * @see edu.uw.ext.framework.account.AccountFactory#newAccount(java.lang.String, byte[], int)
     * @param initialBalance - the balance of the new account, in cents
     * @param accountName - the name of the new account
     * @param hashedPassword - a byte array representing the encryped password String
     * @return the newly created account
     */
    @Override
    public Account newAccount(final String accountName, 
                                final byte[] hashedPassword, 
                                final int initialBalance) {
        Account account = new AccountImpl();
        try {
            /*if either criteria not met, don't create an account*/
            if (accountName.length()<8 | initialBalance < 100000) {
                account = null;
                if (accountName.length()<8) {
                    String msg = "Property name must be equal to or greater" 
                                    + "than 8 characters in length";
                    throw new AccountException(msg);
                } else if (initialBalance < 100000) {
                    throw new AccountException("Initial balance must be a minimum of $1000");
                }
            } else {
                account.setName(accountName);
                account.setBalance(initialBalance);
                account.setPasswordHash(hashedPassword);
            }
        } catch (AccountException e) {
            e.printStackTrace();
        }
        return account;
    }

}
