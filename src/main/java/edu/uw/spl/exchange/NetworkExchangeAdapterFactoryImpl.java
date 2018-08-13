package edu.uw.spl.exchange;

import edu.uw.ext.framework.exchange.ExchangeAdapter;
import edu.uw.ext.framework.exchange.NetworkExchangeAdapterFactory;
import edu.uw.ext.framework.exchange.StockExchange;

public class NetworkExchangeAdapterFactoryImpl implements 
                                            NetworkExchangeAdapterFactory {

    @Override
    public ExchangeAdapter newAdapter(StockExchange exchange, 
                                        String multicastIP, 
                                        int multicastPort, 
                                        int commandPort) {
        return new NetworkExchangeAdapterImpl(exchange,multicastIP,multicastPort,commandPort);
    }

}
