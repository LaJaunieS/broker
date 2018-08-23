package edu.uw.spl.exchange;

import java.net.SocketException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.exchange.ExchangeAdapter;
import edu.uw.ext.framework.exchange.NetworkExchangeAdapterFactory;
import edu.uw.ext.framework.exchange.StockExchange;

public class NetworkExchangeAdapterFactoryImpl implements 
                                            NetworkExchangeAdapterFactory {
    private static final Logger log = LoggerFactory.getLogger(NetworkExchangeAdapter.class);
    
    @Override
    public ExchangeAdapter newAdapter(final StockExchange exchange, 
                                        final String multicastIP, 
                                        final int multicastPort, 
                                        final int commandPort) {
        ExchangeAdapter exchangeAdapter = null;
        try {
            exchangeAdapter = 
                    new NetworkExchangeAdapter(exchange,multicastIP,multicastPort,commandPort);
        } catch (SocketException e) {
            log.error("Unable to establish socket connection",e);
        } catch (UnknownHostException e) {
            log.error("Unable to resolve multicast address",e);
        }
        return exchangeAdapter;
    }

}
