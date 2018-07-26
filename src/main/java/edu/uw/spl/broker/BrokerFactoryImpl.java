package edu.uw.spl.broker;

import edu.uw.ext.framework.account.AccountManager;
import edu.uw.ext.framework.broker.Broker;
import edu.uw.ext.framework.broker.BrokerFactory;
import edu.uw.ext.framework.exchange.StockExchange;

public class BrokerFactoryImpl implements BrokerFactory {

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
