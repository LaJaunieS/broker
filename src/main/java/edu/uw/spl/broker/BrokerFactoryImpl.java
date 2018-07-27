package edu.uw.spl.broker;

import edu.uw.ext.framework.account.AccountManager;
import edu.uw.ext.framework.broker.Broker;
import edu.uw.ext.framework.broker.BrokerFactory;
import edu.uw.ext.framework.exchange.StockExchange;

/**Implentation of a Broker Factory which instantiates a new Broker
 * @author slajaunie
 *
 */
public class BrokerFactoryImpl implements BrokerFactory {

    /**
     *The default constructor 
     */
    public BrokerFactoryImpl() {}
    
    /**Instantiates a new Broker object
     * @see edu.uw.ext.framework.broker.BrokerFactory#newBroker(java.lang.String, edu.uw.ext.framework.account.AccountManager, edu.uw.ext.framework.exchange.StockExchange)
     * @param name the name of this broker
     * @param acctMgr the account manager that will be associated with this broker
     * @param exch the exchange this broker will interact with
     * @return the newly instantiated broker
     */
    @Override
    public Broker newBroker(String name, AccountManager acctMgr, StockExchange exch) {
        BrokerImpl broker = new BrokerImpl(name,acctMgr,exch);
        /*Either here or in Broker constructor, get list of alll the stocks in the exchange
         * and create an order manager for each
         * Map that gets the stock symbol and maps to a current price from the exchange
         */
        return broker;
    }

}
