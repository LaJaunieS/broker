package edu.uw.spl.exchange;

import java.net.SocketException;
import java.net.UnknownHostException;

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
        ExchangeAdapter exchangeAdapter = null;
        try {
            exchangeAdapter = 
                    new NetworkExchangeAdapter(exchange,multicastIP,multicastPort,commandPort);
        } catch (UnknownHostException | SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return exchangeAdapter;
    }

}
