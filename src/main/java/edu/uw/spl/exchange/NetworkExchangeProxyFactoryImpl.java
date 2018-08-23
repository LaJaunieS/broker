package edu.uw.spl.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.exchange.NetworkExchangeProxyFactory;
import edu.uw.ext.framework.exchange.StockExchange;

public class NetworkExchangeProxyFactoryImpl 
                    implements NetworkExchangeProxyFactory {
    private static final Logger log = LoggerFactory.getLogger(NetworkExchangeProxy.class);
    
    @Override
    public StockExchange newProxy(String eventIpAddress,
                                    int eventPort, 
                                    String cmdIpAddress, 
                                    int commandPort) {
        return new NetworkExchangeProxy(eventIpAddress,eventPort,cmdIpAddress,commandPort);
    }

}
