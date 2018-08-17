package edu.uw.spl.exchange;

public class ProtocolConstants {
    //TODO*******change to static constants???? prefer to keep enum bc type safety************
    
    /*Order types*/
    /**A buy order*/
    public static final String BUY_ORDER = "BUY_ORDER";
    
    /**A sell order*/
    public static final String SELL_ORDER = "SELL_ORDER";
    
    /*Events*/
    /**The index of the event type element*/
    public static final int EVENT_ELEMENT = 0;
    /**Event indicating the exchange is open*/
    public static final  String OPEN_EVENT = "OPEN_EVENT";
    /**Event indicating the exchange has closed*/
    public static final String CLOSED_EVENT = "CLOSED_EVENT";
    /**Event indicating a stock price has changed*/
    public static final CharSequence PRICE_CHANGE_EVENT = "PRICE_CHANGE_EVENT";
    /**The index of the price element*/
    public static final int PRICE_CHANGE_EVENT_PRICE_ELEMENT = 2;
    
    public static final int PRICE_CHANGE_EVENT_TICKER_ELEMENT = 1;
    
    /*State*/
    /**Indicates the exchange is open*/
    public static final String OPEN_STATE ="OPEN_STATE";
    /**Indicates the exchange has closed*/
    public static final String CLOSED_STATE ="CLOSED_STATE";
    
    
    /*Commands*/
    /**The index of the command element*/
    public static final int CMD_ELEMENT = 0;
    /**Command to execute a trade*/
    public static final String EXECUTE_TRADE_CMD ="EXECUTE_TRADE_CMD";
    
    public static final int EXECUTE_TRADE_CMD_TYPE_ELEMENT = 1;
    /**The index of the account id element in the execute trade command*/
    public static final int EXECUTE_TRADE_CMD_ACCOUNT_ELEMENT = 2;
    /**The index of the ticker element in the execute trade command*/
    public static final int EXECUTE_TRADE_CMD_TICKER_ELEMENT = 3;
    /**The index of the shares element in the execute trade command*/
    public static final int EXECUTE_TRADE_CMD_SHARES_ELEMENT = 4;
    /**The index of the order type element in the execute trade command*/
    public static final int EXECUTE_TRADE_CMD_ORDER_TYPE_ELEMENT = 1;
    /**Command requesting a stock price quote*/
    public static final String GET_QUOTE_CMD = "GET_QUOTE_CMD";
    /**The index of the ticker element in the price quote command*/
    public static final int QUOTE_CMD_TICKER_ELEMENT = 1;
    /**Command requesting the state of the exchange(open or closed)*/
    public static final String GET_STATE_CMD= "GET_STATE_CMD";
    /**Command requesting the ticker symbols for all traded stocks*/
    public static final String GET_TICKERS_CMD = "GET_TICKERS_CMD";
    
    /*Misc.*/
    /**The maximum number of commands used in the protocol*/
    public static final CharSequence ELEMENT_DELIMITER = ":";
    /**Character encoding to use*/
    public static final String ENCODING = "UTF-8";
    /**An invalid stock price- this response indicates stock is not on the exchange*/
    public static final String INVALID_STOCK = "-1";
    /**Default response if a sent command is not recognized*/
    public static final String INVALID_COMMAND = "INVALID_COMMAND";
    
    private String value;
    
    private ProtocolConstants(String value) {
        this.value = value;
    }
    
    public String toString() {
        return this.value;
    }
}
