package edu.uw.spl.exchange;

import edu.uw.ext.framework.exchange.NetworkExchangeProxyFactory;
import edu.uw.ext.framework.exchange.StockExchange;

public class NetworkExchangeProxyFactoryImpl 
                    implements NetworkExchangeProxyFactory {

    @Override
    public StockExchange newProxy(String eventIpAddress,
                                    int eventPort, 
                                    String cmdIpAddress, 
                                    int commandPort) {
        return new NetworkExchangeProxy(eventIpAddress,eventPort,cmdIpAddress,commandPort);
    }

}
