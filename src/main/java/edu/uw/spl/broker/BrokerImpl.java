package edu.uw.spl.broker;

import java.util.HashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.uw.ext.framework.account.Account;
import edu.uw.ext.framework.account.AccountException;
import edu.uw.ext.framework.account.AccountManager;
import edu.uw.ext.framework.broker.Broker;
import edu.uw.ext.framework.broker.BrokerException;
import edu.uw.ext.framework.broker.OrderManager;
import edu.uw.ext.framework.exchange.ExchangeEvent;
import edu.uw.ext.framework.exchange.ExchangeListener;
import edu.uw.ext.framework.exchange.StockExchange;
import edu.uw.ext.framework.exchange.StockQuote;
import edu.uw.ext.framework.order.MarketBuyOrder;
import edu.uw.ext.framework.order.MarketSellOrder;
import edu.uw.ext.framework.order.Order;
import edu.uw.ext.framework.order.StopBuyOrder;
import edu.uw.ext.framework.order.StopSellOrder;

/**Implementation of the Broker interface. Interacts with the AccountManager 
 * to create Accounts, get/set Accounts, delete Accounts, or process Orders
 * on behalf of an Account
 * @author slajaunie
 */
public class BrokerImpl implements Broker, ExchangeListener {

    private static final Logger log = LoggerFactory.getLogger(BrokerImpl.class);
    
    /**The account manager that creates and handles operations
     * relating to an account - accessed via getAccount()*/
    private AccountManager accountManager;
    
    /**The name of this broker*/
    private String name;

    /**The exchange this broker will interact with*/
    private StockExchange exchange;
    
    /**HashMap of tickers in the exchange
     *  Key - stock symbol
    *   OrderManager - an OrderManager instance for each stock*/
    private HashMap<String,OrderManager> orderManagers;
    
    /**Dispatch filter for market orders*/
    private BiPredicate<Boolean,Order> mOrderDispatchFilter = 
            (threshold,order)-> threshold;
    
    /**Order queue for the market orders*/
    private OrderQueueImpl<Boolean,Order> marketOrders; 
            
    /**The Consumer/OrderProcessor for StopBuyOrders*/
    private Consumer<StopBuyOrder> moveBuyToMarketOrderProcessor = 
            (order)-> marketOrders.enqueue(order);
    
    /**The Consumer/OrderProcessor for StopSellOrders*/
    private Consumer<StopSellOrder> moveSellToMarketOrderProcessor = 
            (order)-> marketOrders.enqueue(order);
    
    /**The Consumer/OrderProcessor for market orders*/
    private Consumer<Order> marketOrderProcessor = (order)-> this.executeOrder(order);

    /**
     * Constructor- 
     * Instantiates a new Broker implementation
     * @param name the name of this Broker
     * @param acctMgr the AccountManager instance that will be associated with this broker
     * @param exchange The exchange this broker will interact with
     * */
    public BrokerImpl(final String name, 
                        final AccountManager acctMgr, 
                        final StockExchange exchange) {
        /*Set the internal fields...*/
        this.name = name;
        this.accountManager = acctMgr;
        this.exchange = exchange;
        
        
        
        /*....Get stock symbols in the exchange and initialize the tickers map
         * with newly-instantiated OrderManagers for each stock... 
         */
        orderManagers = new HashMap<String, OrderManager>();
        
        /*...Initialize the market order queue...*/
        marketOrders = 
                new OrderQueueImpl<Boolean, Order>(exchange.isOpen(), mOrderDispatchFilter);
        //without need for an entirely new order processor?
        marketOrders.setOrderProcessor(marketOrderProcessor);
        
        /*Instantiate an order manager for each stock in the exchange*/
        for (String stock : exchange.getTickers()) {
            StockQuote quote  = exchange.getQuote(stock);
            String ticker = quote.getTicker();
            OrderManager om = new OrderManagerImpl(ticker, quote.getPrice());
            log.info("Created order manager for {}",om.getSymbol());
            
            /*Add OrderManager to the map*/
            orderManagers.put(ticker,om);
            
            /*Confirm order manager successfully added...*/
            OrderManager confOm = orderManagers.get(ticker);
            if (confOm !=null) {
                log.info("order manager successfully added to queue");
                /*...Now add order processors - avoiding NullPointerException*/
                om.setBuyOrderProcessor(moveBuyToMarketOrderProcessor);
                om.setSellOrderProcessor(moveSellToMarketOrderProcessor);
            } else {
                log.warn("Unable to add order manager to queue");
            }
        }
        
        /*....Register this Broker to listen for exchange events*/
        exchange.addExchangeListener(this);
    }
    
    /**Attempts to close resources being used by this broker. If unable to close due
     * to an <code>AccountException</code>, will throw a <code>BrokerException</code>
     * @see edu.uw.ext.framework.broker.Broker#close()
     * @throws BrokerException if unable to close resources, or receives an exception
     */
    @Override
    public void close() throws BrokerException {
       try {
        accountManager.close();
        accountManager = null;
        exchange.removeExchangeListener(this);
       } catch (AccountException e) {
           throw new BrokerException("Broker unable to close resources",e);
       }
    }

    /**
     * Creates a new account utilizing the AccountManager. If the AccountManager throws
     * an <code>AccountException</code> will throw a <code>BrokerException</code> and return
     * null
     * @see edu.uw.ext.framework.broker.Broker#createAccount(java.lang.String, java.lang.String, int)
     * @param username the username of the account
     * @param password the password of the account
     * @param balance the initial balance of the account. 
     * @return account the newly created account, or null if an exception is thrown
     * @throws BrokerException if there was a problem creating the account
     */
    @Override
    public Account createAccount(final String username, final String password, final int balance) 
            throws BrokerException {
       Account account = null; 
       try {
           account = accountManager.createAccount(username, password, balance);
       } catch (AccountException e) {
           throw new BrokerException("Broker was unable to create account", e);
       }

       /*If for some reason account factories don't throw an exception, 
        * check if account comes back null*/
       if (account == null) {
           throw new BrokerException("Broker was unable to create account");
       }
       
       /*If account != null return it*/
       return account;
    }

    /**Removes the account from the directory via the AccountManager
     * @param username the name of the account to remove
     * @throws BrokerException if the operation failed
     */
    @Override
    public void deleteAccount(final String username) throws BrokerException {
        try {
            accountManager.deleteAccount(username);
        } catch (AccountException e) {
            throw new BrokerException("Broker was unable to delete account",e);
        }
        
    }

    /**Look up an account based on the given account name and password, and returns that account,
     * or <code>null</code> if the account was not located in the directory, or the provided
     * password is incorrect 
     * @param username the name of the account to retrieve
     * @param password the password of the account to retrieve
     * @return the account associated with the given account name, or null if the 
     * account was not located in the directory or the provided password was incorrect
     * @throws BrokerException if the operation failed
     */
    @Override
    public Account getAccount(final String username, final String password) 
                            throws BrokerException {
        boolean validated = false;
        Account account = null;
        /*First locate and validate the account...*/
        try {
            account = accountManager.getAccount(username);
            if (account != null) {
                validated = accountManager.validateLogin(username, password);
            } else {
                throw new BrokerException("Broker was unable to get account");
            }
        } catch (AccountException e) {
            throw new BrokerException("Broker was unable to get account",e);
        }
        
        /*...If account located but pw not validated, throw the exception and
         * null out account so it can't be returned*/
        if (validated == false) {
            account = null;
            throw new BrokerException("Unable to verify account credentials");
        }
        
        return account;
    }

    /**Utility function that processes a market order
     * @param order an order to be processed
     */
    private void executeOrder(final Order order) {
        try {
            accountManager.getAccount(order.getAccountId())
                            .reflectOrder(order,exchange.executeTrade(order));
        } catch (AccountException e) {
            e.printStackTrace();
        }
    }
    
    /** Places a Market Buy Order
     * @see edu.uw.ext.framework.broker.Broker#placeOrder(edu.uw.ext.framework.order.MarketBuyOrder)
     * @param order the order to be placed
     * @throws BrokerException if the operation failed
     */
    @Override
    public void placeOrder(final MarketBuyOrder order) throws BrokerException {
        this.marketOrders.enqueue(order);
    }

    /**Places a Market Sell Order
     * @see edu.uw.ext.framework.broker.Broker#placeOrder(edu.uw.ext.framework.order.MarketSellOrder)
     * @param order the order to be placed
     * @throws BrokerException if the operation failed
     */
    @Override
    public void placeOrder(final MarketSellOrder order) throws BrokerException {
        this.marketOrders.enqueue(order);
    }

    /**Places a Stop Buy Order
     * @see edu.uw.ext.framework.broker.Broker#placeOrder(edu.uw.ext.framework.order.StopBuyOrder)
     * @param order the order to be placed
     * @throws BrokerException if the Order Manager is unable to locate the stock associated
     * with the given order, or the operation fails
     */
    @Override
    public void placeOrder(final StopBuyOrder order) throws BrokerException {
        OrderManager om = this.orderManagers.get(order.getStockTicker());
        if (om == null) {
            throw new BrokerException("Unable to locate stock symbol for this order");
        } else {
            om.queueOrder(order);
            log.info("StopBuyOrder queued with order manager for {}",om.getSymbol());
        }
    }


    /**Places a Stop Sell Order
     * @see edu.uw.ext.framework.broker.Broker#placeOrder(edu.uw.ext.framework.order.StopBuyOrder)
     * @param order the order to be placed
     * @throws BrokerException if the Order Manager is unable to locate the stock associated
     * with the given order, or the operation fails
     */
    @Override
    public void placeOrder(final StopSellOrder order) throws BrokerException {
        OrderManager om = this.orderManagers.get(order.getStockTicker());
        if (om == null) {
            throw new BrokerException("Unable to locate stock symbol for this order");
        } else {
            om.queueOrder(order);
            log.info("StopSellOrder queued with order manager for {}",om.getSymbol());
        }
    }

    /**Obtains a Stock Quote for the given stock, containing the stock's symbol and 
     * current price
     * @see edu.uw.ext.framework.broker.Broker#requestQuote(java.lang.String)
     * @param ticker the stock's ticker symbol
     * @throws BrokerException if unable to locate the requested stock in the exchange
     */
    @Override
    public StockQuote requestQuote(final String ticker) 
            throws BrokerException {
        /*Get a quote using the given stock symbol
         * and use returned information to instantiate a 
         * new StockQuote (ticker and price)
         */
        StockQuote stockQuote = exchange.getQuote(ticker);
        if (stockQuote == null) {
            throw new BrokerException("Requested stock not listed");
        }
        return stockQuote;
    }
    
    /*ExchangeListener methods...*/
    
    /**Updates the Market Order queue's threshold to reflect the exchange has closed, upon 
     * receiving a close event from the exchange
     * @see edu.uw.ext.framework.exchange.ExchangeListener#exchangeClosed(edu.uw.ext.framework.exchange.ExchangeEvent)
     * @param evt the close event from the exchange
     */
    @Override
    public void exchangeClosed(ExchangeEvent evt) {
        //emit event to lsteners that exchange is closed
        this.marketOrders.setThreshold(Boolean.FALSE);
        log.info("******Exchange is closed******");
    }

    /**Updates the Market Order queue's threshold to reflect the exchange has opened, upon 
     * receiving a open event from the exchange
     * @see edu.uw.ext.framework.exchange.ExchangeListener#exchangeClosed(edu.uw.ext.framework.exchange.ExchangeEvent)
     * @param evt the open event from the exchange
     */
    @Override
    public void exchangeOpened(ExchangeEvent evt) {
      //emit event to lsteners that exchange is open
        //ExchangeEvent.newOpenedEvent(this.exchange);
        this.marketOrders.setThreshold(Boolean.TRUE);
        log.info("*****Exchange is open******");
    }

    /**Updates the Stop Order queues' thresholds to reflect the price of a stock has changed, upon 
     * receiving a price change event from the exchange
     * @see edu.uw.ext.framework.exchange.ExchangeListener#exchangeClosed(edu.uw.ext.framework.exchange.ExchangeEvent)
     * @param evt the price change event from the exchange
     */
    @Override
    public void priceChanged(ExchangeEvent evt) {
        //emit event to listeners that price of the order manager's stock has changed
        //TODO Check for null returns
        OrderManager om = this.orderManagers.get(evt.getTicker());
        om.adjustPrice(evt.getPrice());
    }

    /**Obtains the name of this broker
     * @see edu.uw.ext.framework.broker.Broker#getName()
     * @return a string reflecting the name of this broker
     */
    @Override
    public String getName() {
        return this.name;
    }
}
