package edu.uw.spl.exchange;

import edu.uw.ext.framework.exchange.ExchangeAdapter;
import edu.uw.ext.framework.exchange.ExchangeEvent;
import edu.uw.ext.framework.exchange.StockExchange;

public class NetworkExchangeAdapterImpl implements ExchangeAdapter {

    private StockExchange exchange; 
    private String multicastIP;
    private int multicastPort;
    private int commandPort;
    
    public NetworkExchangeAdapterImpl(StockExchange exchange,
                                        String multicastIP,
                                        int multicastPort,
                                        int commandPort) {
        this.exchange = exchange;
        this.multicastIP = multicastIP;
        this.multicastPort = multicastPort;
        this.commandPort = commandPort;
        this.exchange.addExchangeListener(this);
    }
    
    @Override
    public void exchangeClosed(ExchangeEvent arg0) {
        /*"convert" close event to a text command and multicast to brokers
         * who are- listening? registered? accessing in some way?
         */
        // TODO Auto-generated method stub
        
    }

    @Override
    public void exchangeOpened(ExchangeEvent arg0) {
        /*"convert" open event to a text command and multicast to brokers
         * who are- listening? registered? accessing in some way?
         */
        
        
    }

    @Override
    public void priceChanged(ExchangeEvent arg0) {
        /*"convert" price change event to a text command and multicast to brokers
         * who are- listening? registered? accessing in some way?
         */
        
        
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        
    }
    
    //TODO consider a private method that handles sending the text-based events
    //via the multicast

}
