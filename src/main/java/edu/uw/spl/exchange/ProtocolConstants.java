package edu.uw.spl.exchange;

public enum ProtocolConstants {
    
    /**A buy order*/
    BUY_ORDER("BUY_ORDER"),
    /**A sell order*/
    SELL_ORDER("SELL_ORDER"),
    
    /*Events*/
    /**The index of the event type element*/
    EVENT_ELEMENT("0"),
    /**Event indicating the exchange is open*/
    OPEN_EVENT("OPEN_EVENT"),
    /**Event indicating the exchange has closed*/
    CLOSED_EVENT("CLOSED_EVENT"),
    /**Event indicating a stock price has changed*/
    PRICE_CHANGE_EVENT("PRICE_CHANGE_EVENT"),
    /**The index of the price element*/
    PRICE_CHANGE_EVENT_PRICE_ELEMENT("2"),
    
    /*State*/
    /**Indicates the exchange is open*/
    OPEN_STATE("OPEN_STATE"),
    /**Indicates the exchange has closed*/
    CLOSED_STATE("CLOSED_STATE"),
    
    
    /*Commands*/
    /**The index of the command element*/
    CMD_ELEMENT("0"),
    /**Command to execute a trade*/
    EXECUTE_TRADE_CMD("EXECUTE_TRADE_CMD"),
    /**The index of the account id element in the execute trade command*/
    EXECUTE_TRADE_CMD_ACCOUNT_ELEMENT("2"),
    /**The index of the ticker element in the execute trade command*/
    EXECUTE_TRADE_CMD_TICKER_ELEMENT("3"),
    /**The index of the shares element in the execute trade command*/
    EXECUTE_TRADE_CMD_SHARES_ELEMENT("4"),
    /**The index of the order type element in the execute trade command*/
    EXECUTE_TRADE_CMD_ORDER_TYPE_ELEMENT("1"),
    /**Command requesting a stock price quote*/
    GET_QUOTE_CMD("GET_QUOTE_CMD"),
    /**The index of the ticker element in the price quote command*/
    QUOTE_CMD_TICKER_ELEMENT("1"),
    /**Command requesting the state of the exchange(open or closed)*/
    GET_STATE_CMD("GET_STATE_CMD"),
    /**Command requesting the ticker symbols for all traded stocks*/
    GET_TICKERS_CMD("GET_TICKERS_CMD"),
    
    /*Misc.*/
    /**The maximum number of commands used in the protocol*/
    ELEMENT_DELIMITER(":"),
    /**Character encoding to use*/
    ENCODING("UTF-8"),
    /**An invalid stock price- this response indicates stock is not on the exchange*/
    INVALID_STOCK("INVALID_STOCK");
    
    
    private String value;
    
    private ProtocolConstants(String value) {
        this.value = value;
    }
    
    public String toString() {
        return this.value;
    }
}
