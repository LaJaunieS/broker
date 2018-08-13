package edu.uw.spl.exchange;

import edu.uw.ext.framework.exchange.ExchangeListener;
import edu.uw.ext.framework.exchange.StockExchange;
import edu.uw.ext.framework.exchange.StockQuote;
import edu.uw.ext.framework.order.Order;

public class NetworkExchangeProxyImpl implements StockExchange {

    @Override
    public void addExchangeListener(ExchangeListener arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int executeTrade(Order arg0) {
        //Tell the broker to execute a trade
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public StockQuote getQuote(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getTickers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isOpen() {
        //Tell broker exchange is open and follow dispatch order logic
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeExchangeListener(ExchangeListener arg0) {
        // TODO Auto-generated method stub
        
    }

}
