package edu.uw.spl;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import app.ExchangeFactory;
import edu.uw.ext.exchange.TestExchange;
import edu.uw.ext.framework.account.AccountException;
import edu.uw.ext.framework.account.AccountManager;
import edu.uw.ext.framework.account.AccountManagerFactory;
import edu.uw.ext.framework.broker.Broker;
import edu.uw.ext.framework.broker.BrokerException;
import edu.uw.ext.framework.broker.BrokerFactory;
import edu.uw.ext.framework.dao.AccountDao;
import edu.uw.ext.framework.dao.DaoFactory;
import edu.uw.ext.framework.dao.DaoFactoryException;
import edu.uw.ext.framework.exchange.StockQuote;
import edu.uw.ext.framework.order.MarketBuyOrder;
import edu.uw.ext.framework.order.MarketSellOrder;
import edu.uw.ext.framework.order.Order;
import edu.uw.ext.framework.order.StopBuyOrder;
import edu.uw.ext.framework.order.StopSellOrder;
import edu.uw.spl.broker.BrokerFactoryImpl;

public class BrokerImplTest {
    
    final String ACCT_NAME = "neotheone";
    
    final String BROKER_NAME = "ABC_Brokers";
    
    final String ACCT_PASSWORD = "password";
    
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
    
    //Components needed for the tests
    DaoFactory daoFact;
    AccountDao dao;
    
    AccountManagerFactory accountManagerFactory; 
    BrokerFactory brokerFactory; 
    
    AccountManager accountManager;
    TestExchange exchange; 
    Broker broker;
    
    @Before
    public void setup() {
        
        // create the account manager, dao, and broker
        daoFact = context.getBean("DaoFactory", DaoFactory.class);
        
        accountManagerFactory = context.getBean("AccountManagerFactory", AccountManagerFactory.class);

        try {
            dao = daoFact.getAccountDao();
            dao.reset();
            accountManager = accountManagerFactory.newAccountManager(dao);
            exchange = ExchangeFactory.newTestStockExchange();
        
            brokerFactory = context.getBean("BrokerFactory",BrokerFactoryImpl.class);
            //Instantiate the broker and confirm exchange successfully added and OrderManagers queued
            broker = brokerFactory.newBroker(BROKER_NAME, accountManager, exchange);
        
            //Add a new account to this broker with a new balance
            broker.createAccount(ACCT_NAME, ACCT_PASSWORD, 100000);
        
            //A set of Orders (lifted from test.BrokerTest)
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

        } catch (DaoFactoryException | AccountException | BrokerException e) {
            e.printStackTrace();
        }
    }
        
    @Test
    public void testBrokerConstruction() {
        //just confirm the various broker components properly constructed
        assertEquals(BROKER_NAME, broker.getName());
        //get a good account
        try {
            assertEquals(ACCT_NAME, broker.getAccount(ACCT_NAME, ACCT_PASSWORD).getName());
        } catch (BrokerException e) {
            e.printStackTrace();
        }
        //get a non-existent account- should throw an exception
        try {
            broker.getAccount("bad_account","bad$password");
            fail("Should have thrown an exception");
        } catch (BrokerException e) {
            e.printStackTrace();
        }
        
    }
    
    //Test the quotes come back okay, throw expected exception
    @Test
    public void testRequestQuote() {
        try {
            StockQuote quote = broker.requestQuote(app.ExchangeFactory.SYMBOL_CDYN);
            assertEquals(app.ExchangeFactory.SYMBOL_CDYN,quote.getTicker());
            assertEquals(app.ExchangeFactory.INITIAL_PRICE_CDYN,quote.getPrice());
        } catch (BrokerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            StockQuote quote = broker.requestQuote("IPET");
            fail("Should have thrown exception");
        } catch (BrokerException e) {
            e.printStackTrace();
        }
        
    }
}
