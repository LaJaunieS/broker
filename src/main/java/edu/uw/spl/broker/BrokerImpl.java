package edu.uw.spl.broker;

import java.util.HashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import app.ExchangeFactory;
import edu.uw.ext.exchange.TestExchange;
import edu.uw.ext.framework.account.Account;
import edu.uw.ext.framework.account.AccountException;
import edu.uw.ext.framework.account.AccountManager;
import edu.uw.ext.framework.account.AccountManagerFactory;
import edu.uw.ext.framework.broker.Broker;
import edu.uw.ext.framework.broker.BrokerException;
import edu.uw.ext.framework.broker.BrokerFactory;
import edu.uw.ext.framework.broker.OrderManager;
import edu.uw.ext.framework.dao.AccountDao;
import edu.uw.ext.framework.dao.DaoFactory;
import edu.uw.ext.framework.dao.DaoFactoryException;
import edu.uw.ext.framework.exchange.ExchangeEvent;
import edu.uw.ext.framework.exchange.ExchangeListener;
import edu.uw.ext.framework.exchange.StockExchange;
import edu.uw.ext.framework.exchange.StockQuote;
import edu.uw.ext.framework.order.MarketBuyOrder;
import edu.uw.ext.framework.order.MarketSellOrder;
import edu.uw.ext.framework.order.Order;
import edu.uw.ext.framework.order.StopBuyOrder;
import edu.uw.ext.framework.order.StopSellOrder;
import edu.uw.spl.account.AccountManagerFactoryImpl;
import app.ExchangeFactory.*;

/**Implementation of the Broker interface. Interacts with the AccountManager 
 * to create Accounts, get/set Accounts, delete Accounts, or process Orders
 * on behalf of an Account
 */
public class BrokerImpl implements Broker, ExchangeListener {

    private static final Logger log = LoggerFactory.getLogger(BrokerImpl.class);
    
//    /**The account associated with this broker*/
//    private Account account; 
//    
    //TODO confirm we need all these member fields- can they be local fields 
    //in the methods?
    /**The account manager that creates and handles operations
     * relating to an account - accessed via getAccount()*/
    private AccountManager accountManager;
    
    /**The name of this broker*/
    private String name;

    /**The exchange this broker will interact with*/
    private StockExchange exchange;
    
    /**TreeMap of tickers in the exchange
     *  Key - stock symbol
    *   OrderManager - an OrderManager instance for each stock*/
    //TODO consider other map implementations?
    private HashMap<String,OrderManager> orderManagers;
    
    /**Dispatch filter for market orders*/
    private BiPredicate<Boolean,Order> mOrderDispatchFilter = 
            (threshold,order)-> exchange.isOpen();
    
    /**Order queue for the market orders*/
    private OrderQueueImpl<Boolean,Order> marketOrders; 
            
    /*The Consumers/OrderProcessors*/
    private Consumer<StopBuyOrder> moveBuyToMarketOrderProcessor = 
            (order)-> marketOrders.enqueue(order);
        
    private Consumer<StopSellOrder> moveSellToMarketOrderProcessor = 
            (order)-> marketOrders.enqueue(order);
    
    private Consumer<Order> marketOrderProcessor = (order)->{
            this.executeOrder(order);
    };
            
                        
    /**
     * Constructor
     * 
     * */
    public BrokerImpl(String name, AccountManager acctMgr, StockExchange exchange) {
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
        //Russ puts this in a separate methods initializeOrderMaangers()
        for (String stock : exchange.getTickers()) {
            StockQuote quote  = exchange.getQuote(stock);
            String ticker = quote.getTicker();
            //TODO local crreateOrderManager method?
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
    
    @Override
    public void close() throws BrokerException {
       try {
        accountManager.close();
        accountManager = null;
       } catch (AccountException e) {
           e.printStackTrace();
       }
    }

    @Override
    public Account createAccount(String username, String password, int balance) throws BrokerException {
       Account account = null; 
       try {
           account = accountManager.createAccount(username, password, balance);
       } catch (AccountException e) {
           throw new BrokerException("Broker was unable to create account", e);
       }

       /*If account factories don't throw an exception, check if account comes back null*/
       if (account == null) {
           throw new BrokerException("Broker was unable to create account");
       }
       
       /*If account != null return it*/
       return account;
       
    }

    @Override
    public void deleteAccount(String username) throws BrokerException {
        //TODO need to catch for nulls?
        try {
            accountManager.deleteAccount(username);
        } catch (AccountException e) {
            throw new BrokerException("Broker was unable to delete account",e);
        }
        
    }

    @Override
    public Account getAccount(String username, String password) throws BrokerException {
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

    private void executeOrder(Order order) {
        try {
            accountManager.getAccount(order.getAccountId())
                            .reflectOrder(order,exchange.executeTrade(order));
        } catch (AccountException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void placeOrder(MarketBuyOrder order) throws BrokerException {
        this.marketOrders.enqueue(order);
    }

    @Override
    public void placeOrder(MarketSellOrder order) throws BrokerException {
        this.marketOrders.enqueue(order);
    }

    @Override
    public void placeOrder(StopBuyOrder order) throws BrokerException {
        OrderManager om = this.orderManagers.get(order.getStockTicker());
        om.queueOrder(order);
        log.info("StopBuyOrder queued with order manager for {}",om.getSymbol());
        //TODO need to check for null return
    }

    @Override
    public void placeOrder(StopSellOrder order) throws BrokerException {
        OrderManager om = this.orderManagers.get(order.getStockTicker());
        om.queueOrder(order);
      //TODO need to check for null return ie stockTicker not found
        log.info("StopSellOrder queued with order manager for {}",om.getSymbol());
    }

    @Override
    public StockQuote requestQuote(String ticker) throws BrokerException {
        /*Get a quote using the given stock symbol
         * and use returned information to instantiate a 
         * new StockQuote (ticker and price)
         */
        StockQuote stockQuote = exchange.getQuote(ticker);
        
        return stockQuote;
    }
    
    /*ExchangeListener methods...*/
    @Override
    public void exchangeClosed(ExchangeEvent evt) {
        //emit event to lsteners that exchange is closed
        this.marketOrders.setThreshold(Boolean.FALSE);
        log.info("******Exchange is closed******");
    }

    @Override
    public void exchangeOpened(ExchangeEvent evt) {
      //emit event to lsteners that exchange is open
        //ExchangeEvent.newOpenedEvent(this.exchange);
        this.marketOrders.setThreshold(Boolean.TRUE);
        log.info("*****Exchange is open******");
    }

    @Override
    public void priceChanged(ExchangeEvent evt) {
        //emit event to listeners that price of the order manager's stock has changed
        //TODO Check for null returns
        OrderManager om = this.orderManagers.get(evt.getTicker());
        om.adjustPrice(evt.getPrice());
    }


    @Override
    public String getName() {
        return this.name;
    }
    
    /*TESTING*/
    /*public static void main(String[] args) {
        //Objectives- 
         // Create a new Broker successfully
         // Create a new Account successfully that the broker will interact with
         // Create at least one order manager to handle orders from the account via the broker
         // Correctly log and capture the events 
         //
        final String ACCT_NAME = "neotheone"; 
        
        final int PRICE_DELTA = 5;

        //Below initial price for BA (Boeing) 
        final int BELOW_INITIAL_PRICE_TYRL = app.ExchangeFactory.INITIAL_PRICE_TYRL - PRICE_DELTA;

        //Above initial price for BA (Boeing) 
        final int ABOVE_INITIAL_PRICE_TYRL = app.ExchangeFactory.INITIAL_PRICE_TYRL + PRICE_DELTA;

        //Above initial price for F (Ford) 
        final int ABOVE_INITIAL_PRICE_CDYN = app.ExchangeFactory.INITIAL_PRICE_CDYN + PRICE_DELTA;

        //A small price adjustment//
        final int SMALL_PRICE_OFFSET = 10;

        //A large price adjustment
        final int LARGE_PRICE_OFFSET = 500;
        
        Order[] expectedOrderSequence;

        BeanFactory context = new ClassPathXmlApplicationContext("context.xml");
        
        // create the account manager, dao, and broker
        DaoFactory daoFact = context.getBean("DaoFactory", DaoFactory.class);
        
        AccountDao dao;
        AccountManager accountManager;
        AccountManagerFactory accountManagerFactory = context.getBean("AccountManagerFactory", AccountManagerFactory.class);

        try {
            dao = daoFact.getAccountDao();
            dao.reset();
            accountManager = accountManagerFactory.newAccountManager(dao);
        TestExchange exchange = ExchangeFactory.newTestStockExchange();
        
        AccountManagerFactoryImpl acctMgrFactory = 
                context.getBean("AccountManagerFactory",AccountManagerFactoryImpl.class);
        BrokerFactory brokerFactory = 
                context.getBean("BrokerFactory",BrokerFactoryImpl.class);
        //Instantiate the broker and confirm exchange successfully added and OrderManagers queued
        //cast to expose new methods for now
        BrokerImpl broker = (BrokerImpl) brokerFactory.newBroker("ABC_Broker", accountManager, exchange);
        
        //Add a new account to this broker with a new balance
        broker.createAccount(ACCT_NAME, "password", 100000);
        
        //A set of Orders (lifted from BrokerTest)
        Order[] tmp = {
                new MarketSellOrder(ACCT_NAME, 400, app.ExchangeFactory.SYMBOL_CDYN),
                new MarketBuyOrder(ACCT_NAME, 250, app.ExchangeFactory.SYMBOL_TYRL),
                new MarketSellOrder(ACCT_NAME, 100, app.ExchangeFactory.SYMBOL_TYRL),
                new StopSellOrder(ACCT_NAME, 30, app.ExchangeFactory.SYMBOL_TYRL,
                    BELOW_INITIAL_PRICE_TYRL),
                new StopBuyOrder(ACCT_NAME, 10, app.ExchangeFactory.SYMBOL_CDYN,
                    ABOVE_INITIAL_PRICE_CDYN),
                new StopBuyOrder(ACCT_NAME, 10, app.ExchangeFactory.SYMBOL_TYRL,
                    ABOVE_INITIAL_PRICE_TYRL),
            };
                
            expectedOrderSequence = tmp;
            
            //Place a bunch of orders
            broker.placeOrder((MarketSellOrder) expectedOrderSequence[2]);
            broker.placeOrder((StopBuyOrder) expectedOrderSequence[4]);
            broker.placeOrder((StopBuyOrder) expectedOrderSequence[5]);
            broker.placeOrder((MarketSellOrder) expectedOrderSequence[0]);
            broker.placeOrder((StopSellOrder) expectedOrderSequence[3]);

            broker.placeOrder((MarketBuyOrder) expectedOrderSequence[1]);
            
            //Get market orders - need to open market to dispatch
            System.out.println(broker.marketOrders.toString());
            
            System.out.println(broker.exchange.isOpen());
            //open the exchange
            exchange.open();
            
            //Get market orders - market opened, so should dequeue/dispatch
            System.out.println(broker.marketOrders.toString());
            
            
        } catch (DaoFactoryException | AccountException | BrokerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
        
    }*/
        
}
