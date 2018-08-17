
package app;

import static app.NetExchangeTestConstants.COMMAND_PORT_DEFAULT;
import static app.NetExchangeTestConstants.COMMAND_PORT_PROP;
import static app.NetExchangeTestConstants.EVENT_IP_DEFAULT;
import static app.NetExchangeTestConstants.EVENT_IP_PROP;
import static app.NetExchangeTestConstants.EVENT_PORT_DEFAULT;
import static app.NetExchangeTestConstants.EVENT_PORT_PROP;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.uw.ext.exchange.SimpleExchange;
import edu.uw.ext.framework.exchange.ExchangeAdapter;
import edu.uw.ext.framework.exchange.NetworkExchangeAdapterFactory;
import edu.uw.ext.framework.exchange.StockExchangeSpi;


/**
 * Test driver for the network ExchangeAdapter.
 *
 * @author Russ Moul
 */
public final class NetExchangeDriver {
    /** This class' logger. */
    private static final Logger LOG =
                         LoggerFactory.getLogger(NetExchangeDriver.class);

    /** Character encoding to use. */
    private static final Charset ENCODING = Charset.forName("ISO-8859-1");

    /**
     * Private constructor prevents instantiation.
     */
    private NetExchangeDriver() {
        super();
    }

    /**
     * Initialize OrderManager to be exercised.
     *
     * @param args (not used)
     *
     * @throws Exception if any exceptions are raised
     */
    public static void main(final String[] args) throws Exception {
        String eventIp = System.getProperty(EVENT_IP_PROP);
        if (eventIp == null) {
            eventIp = EVENT_IP_DEFAULT;
        }
        
        int eventPort = EVENT_PORT_DEFAULT;
        String eventPortStr = System.getProperty(EVENT_PORT_PROP);
        if (eventPortStr != null) {
            eventPort = Integer.parseInt(eventPortStr);
        }
        
        int cmdPort = COMMAND_PORT_DEFAULT;
        String cmdPortStr = System.getProperty(COMMAND_PORT_PROP);
        if (cmdPortStr != null) {
            cmdPort = Integer.parseInt(cmdPortStr);
        }
        
        // initialize the factories
        /** Spring bean factory. */
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("context.xml");
        NetworkExchangeAdapterFactory factory;
        factory = appContext.getBean("NetworkExchangeAdapterFactory", NetworkExchangeAdapterFactory.class);
        appContext.close();
        
        LOG.info("Factories initialized.");

        StockExchangeSpi exchange =
                         new SimpleExchange(new File("exchange.dat"), true);
        LOG.info("Exchange initialized.");

        System.out.println("#");
        System.out.println("#");
        System.out.println("#");
        System.out.println("# Press [Enter] to open the exchange");
        System.out.println("#       [Q-Enter] to quit");
        System.out.println("# Once running press [Enter] "
                         + "to close the exchange");
        System.out.println("#");
        System.out.println("#");
        System.out.println("#");

        

        BufferedReader in;
        in = new BufferedReader(new InputStreamReader(System.in, ENCODING));

        try (ExchangeAdapter adapter = factory.newAdapter(exchange, eventIp, eventPort, cmdPort)) {
	        while (true) {
	            String s = in.readLine();
	
	            if ((null != s && s.length() > 0)
	                   && ((s.charAt(0) == 'Q') || (s.charAt(0) == 'q'))) {
	                break;
	            }
	
	            exchange.open();
	            System.out.println("#");
	            System.out.println("#");
	            System.out.println("#");
	            System.out.println("# Press [Enter] to close the exchange");
	            System.out.println("#");
	            System.out.println("#");
	            System.out.println("#");
	            in.readLine();
	            exchange.close();
	            System.out.println("#");
	            System.out.println("#");
	            System.out.println("#");
	            System.out.println("# Press [Enter] to open the exchange");
	            System.out.println("#       [Q-Enter] to quit");
	            System.out.println("# Once running press [Enter] "
	                             + "to close the exchange");
	            System.out.println("#");
	            System.out.println("#");
	            System.out.println("#");
	        }
        }
        System.exit(0);
    }
}

