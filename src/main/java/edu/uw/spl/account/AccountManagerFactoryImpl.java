package edu.uw.spl.account;

import edu.uw.ext.framework.account.AccountManager;
import edu.uw.ext.framework.dao.AccountDao;

/**Creates new account managers
 * @author slajaunie
 *
 */
public class AccountManagerFactoryImpl 
    implements edu.uw.ext.framework.account.AccountManagerFactory {
    /*Creates instance(s) of AccountManager using corresponding interface,
     *  separate from client*/
    
    /**
     *Instantiates a new AccountManagerFactory
     */
    public AccountManagerFactoryImpl() {}
    
    /**Instantiates a new account manager instance and sets the account manager's dao
     * @see edu.uw.ext.framework.account.AccountManagerFactory#newAccountManager(edu.uw.ext.framework.dao.AccountDao)
     * @param dao the data access object to be used by the account manager
     * @return the newly instantiated account manager
     */
    @Override
    public AccountManager newAccountManager(final AccountDao dao) {
        AccountManagerImpl accountManager = new AccountManagerImpl();
        accountManager.setAccountDAO(dao);
        return accountManager;
    }

}
