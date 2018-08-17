package app;

import static app.NetExchangeTestConstants.COMMAND_IP_DEFAULT;
import static app.NetExchangeTestConstants.COMMAND_IP_PROP;
import static app.NetExchangeTestConstants.COMMAND_PORT_DEFAULT;
import static app.NetExchangeTestConstants.COMMAND_PORT_PROP;
import static app.NetExchangeTestConstants.EVENT_IP_DEFAULT;
import static app.NetExchangeTestConstants.EVENT_IP_PROP;
import static app.NetExchangeTestConstants.EVENT_PORT_DEFAULT;
import static app.NetExchangeTestConstants.EVENT_PORT_PROP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.uw.ext.framework.account.AccountManager;
import edu.uw.ext.framework.account.AccountManagerFactory;
import edu.uw.ext.framework.broker.Broker;
import edu.uw.ext.framework.broker.BrokerFactory;
import edu.uw.ext.framework.dao.AccountDao;
import edu.uw.ext.framework.dao.DaoFactory;
import edu.uw.ext.framework.exchange.NetworkExchangeProxyFactory;
import edu.uw.ext.framework.exchange.StockExchange;
import edu.uw.ext.framework.exchange.StockQuote;
import edu.uw.ext.framework.order.MarketBuyOrder;
import edu.uw.ext.framework.order.MarketSellOrder;
import edu.uw.ext.framework.order.StopBuyOrder;
import edu.uw.ext.framework.order.StopSellOrder;

/**
 * Test driver for the NetworkExchangeProxy implementation.
 *
 * @author Russ Moul
 */
public final class ExchangeProxyDriver {
    /** This class' logger. */
    private static final Logger LOG =
                         LoggerFactory.getLogger(ExchangeProxyDriver.class);

    /** Character encoding to use. */
    private static final Charset ENCODING = Charset.forName("ISO-8859-1");
    
    /** Brokerage name. */
    private static final String BROKERAGE_NAME = "RTrade";

    /** Fred test account name. */
    private static final String ACCOUNT_NAME = "fflinstone";

    /** Good Fred test password. */
    private static final String ACCOUNT_PASSWORD = "password1";

    /** One thousand dollars in cents. */
    private static final int TEN_THOUSAND_DOLLARS_IN_CENTS = 1000000;

    /** Price offset */
    private static final int PRICE_OFFSET = 5;

    /** 10 shares */
    private static final int SHARES_10 = 10;

    /** 30 shares */
    private static final int SHARES_30 = 30;

    /** 250 shares */
    private static final int SHARES_250 = 250;

    /** 400 shares */
    private static final int SHARES_400 = 400;

    /** Symbol for BA (Boeing) */
    private static final String SYMBOL_BA = "BA";

    /** Symbol for F (Ford) */
    private static final String SYMBOL_F = "F";

    /**
     * Private constructor to prevent instantiation.
     */
    private ExchangeProxyDriver() {
        super();
    }

    /**
     * Tests the exchange proxy.
     *
     * @param args (not used)
     *
     * @throws Exception if any exceptions are raised
     */
    public static void main(final String[] args)
        throws Exception {
        String eventIp = System.getProperty(EVENT_IP_PROP);
        if (eventIp == null) {
            eventIp = EVENT_IP_DEFAULT;
        }
        
        int eventPort = EVENT_PORT_DEFAULT;
        String eventPortStr = System.getProperty(EVENT_PORT_PROP);
        if (eventPortStr != null) {
            eventPort = Integer.parseInt(eventPortStr);
        }
        
        String cmdIp = System.getProperty(COMMAND_IP_PROP);
        if (cmdIp == null) {
            cmdIp = COMMAND_IP_DEFAULT;
        }

        int cmdPort = COMMAND_PORT_DEFAULT;
        String cmdPortStr = System.getProperty(COMMAND_PORT_PROP);
        if (cmdPortStr != null) {
            cmdPort = Integer.parseInt(cmdPortStr);
        }

        DaoFactory daoFact;
        AccountDao dao;

        Broker broker;
        StockExchange exchange;
        AccountManager mAcctMngr;
        BrokerFactory mBrokerFactory;


        // initialize the factories
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("context.xml");
        Object obj = appContext.getBean("AccountManagerFactory", AccountManagerFactory.class);
        AccountManagerFactory mAccountManagerFactory;
        mAccountManagerFactory = (AccountManagerFactory) obj;

        obj = appContext.getBean("BrokerFactory", BrokerFactory.class);
        mBrokerFactory = (BrokerFactory) obj;

        obj = appContext.getBean("NetworkExchangeProxyFactory", NetworkExchangeProxyFactory.class);
        LOG.info("Factories initialized.");

        NetworkExchangeProxyFactory proxyFact;
        proxyFact = (NetworkExchangeProxyFactory) obj;

        exchange = proxyFact.newProxy(eventIp, eventPort, cmdIp, cmdPort);
        LOG.info("Connected to exchange proxy.");

        // create the account manager, dao, and broker
        daoFact = (DaoFactory) appContext.getBean("DaoFactory", DaoFactory.class);
        appContext.close();
        dao = daoFact.getAccountDao();
        dao.reset();
        LOG.info("DAO initialized.");

        mAcctMngr = mAccountManagerFactory.newAccountManager(dao);
        LOG.info("Account manager initialized.");
        
//        LOG.info("Brokerage name: {}", BROKERAGE_NAME);
//        LOG.info("Account Manager: {}",mAcctMngr);
//        LOG.info("Exchange: {}",exchange);
//        
        broker = mBrokerFactory.newBroker(BROKERAGE_NAME, mAcctMngr, exchange);
        LOG.info("Broker initialized.");

        broker.createAccount(ACCOUNT_NAME, ACCOUNT_PASSWORD,
                             TEN_THOUSAND_DOLLARS_IN_CENTS);

        // test getting a quote
        StockQuote quote = broker.requestQuote(SYMBOL_BA);

        if (LOG.isInfoEnabled()) {
        	LOG.info("Obtained quote for " + SYMBOL_BA + ": " + quote + ".");
        }

        broker.placeOrder(new MarketBuyOrder(ACCOUNT_NAME,
                                             SHARES_250, SYMBOL_BA));
        broker.placeOrder(new MarketSellOrder(ACCOUNT_NAME,
                                              SHARES_400, SYMBOL_F));
        LOG.info("Placed market buy and sell orders.");

        BufferedReader in;
        in = new BufferedReader(new InputStreamReader(System.in, ENCODING));
        System.out.println("#");
        System.out.println("#");
        System.out.println("#");
        System.out.println("# Open the exchange... "
                         + "queued market orders should execute, then...");

        System.out.println("# Press [Enter] to add stop orders and continue");
        System.out.println("#");
        System.out.println("#");
        System.out.println("#");
        in.readLine();

        int price;

        // place orders for Boeing
        quote = broker.requestQuote(SYMBOL_BA);
        price = quote.getPrice();
        broker.placeOrder(new StopBuyOrder(ACCOUNT_NAME, SHARES_10, SYMBOL_BA,
                price - PRICE_OFFSET));
        broker.placeOrder(new StopSellOrder(ACCOUNT_NAME, SHARES_30, SYMBOL_BA,
                price + PRICE_OFFSET));
        if(LOG.isInfoEnabled()) {
        	LOG.info("Placed stop buy and sell orders for " + SYMBOL_BA + ".");
        }

        // place orders for Ford
        quote = broker.requestQuote("F");
        price = quote.getPrice();
        broker.placeOrder(new StopBuyOrder(ACCOUNT_NAME, SHARES_10, SYMBOL_F,
                price - PRICE_OFFSET));
        broker.placeOrder(new StopSellOrder(ACCOUNT_NAME, SHARES_30, SYMBOL_F,
                price + PRICE_OFFSET));
        if(LOG.isInfoEnabled()) {
        	LOG.info("Placed stop buy and sell orders for " + SYMBOL_F + ".");
        }

        System.out.println("#");
        System.out.println("#");
        System.out.println("#");
        System.out.println(
            "# Watch the exchange to verify the orders execute then,");
        System.out.println("# Press [Enter] to exit");
        System.out.println("#");
        System.out.println("#");
        System.out.println("#");
        in.readLine();
        System.exit(0);
    }
}

