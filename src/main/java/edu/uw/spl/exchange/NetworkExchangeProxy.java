package edu.uw.spl.exchange;

import java.awt.Event;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.swing.event.EventListenerList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.broker.Broker;
import edu.uw.ext.framework.exchange.ExchangeEvent;
import edu.uw.ext.framework.exchange.ExchangeListener;
import edu.uw.ext.framework.exchange.StockExchange;
import edu.uw.ext.framework.exchange.StockQuote;
import edu.uw.ext.framework.order.Order;
import edu.uw.spl.broker.BrokerImpl;
import edu.uw.spl.exchange.ProtocolConstants;

/**Operates as a client for interacting with an exchange via a network. The methods encode
 * the method request as a text-based procotol, per ProtocolConstants, and send the command to the 
 * NetworkExchangeAdapter implementation; the methods then receive the response, decode it, 
 * and return a result
 * The information exchanged will either be Commands based on the text-based protocol,
 * (to get a quote or execute a trade, etc), or change Events (price change, market open/closed).
 * The NetowrkExchangeAdapter implementation will send multicast messages representing exchange events
 *  via a separate thread. Event messages will be transformed into the appropriate event object 
 *  and then propagated to registered listeners*/
public class NetworkExchangeProxy implements StockExchange {
    
    public static final Logger log = LoggerFactory.getLogger(NetworkExchangeProxy.class);
    
    /**The TCP IP address used to send commands*/
    private String commandIpAddress;

    /**The port where the exchange accepts command requests*/
    private int commandPort;
    
    /**The network event processor*/
    private NetworkEventProcessor eventProcessor;
    
    /**The event listener list that holds listeners for exchange events*/
    private EventListenerList listenerList = new EventListenerList();
    
    /**The socket where commands will be transmitted*/
    private Socket commandSocket;
    
    /**Constructor
     * @param eventIpAddress the multicast IP address to connect to
     * @param eventPort the multicast port to connect to
     * @param cmdIpAddress the address where the exchange accepts commands
     * @param cmdPort the port where the exchange accepts commands
     */
    public NetworkExchangeProxy(final String eventIpAddress, 
                                    final int eventPort, 
                                    final String cmdIpAddress,
                                    final int cmdPort) {
        this.commandIpAddress = cmdIpAddress;
        this.commandPort = cmdPort;
        
        this.eventProcessor= new NetworkEventProcessor(eventIpAddress, eventPort);
        
        log.info("Exchange Proxy constructed");
        
        new Thread(this.eventProcessor).start();
        
        try {
            this.commandSocket = new Socket(this.commandIpAddress, this.commandPort);
            log.info("Command Socket connected at port {}",this.commandSocket.getPort());
            log.info("Command Socked opened after initilization? {}",!(this.commandSocket.isClosed()));
        } catch (IOException e) {
            log.warn("Error connecting command socket at port {}",cmdPort);
            e.printStackTrace();
        }
        
    }
    
    /**Add an exchange listener for exchange events
     * @see edu.uw.ext.framework.exchange.StockExchange#addExchangeListener(edu.uw.ext.framework.exchange.ExchangeListener)
     * @param l the listener to be added
     */
    @Override
    public void addExchangeListener(ExchangeListener l) {
        listenerList.add(ExchangeListener.class, l);
        
    }
    
    /**Add an exchange listener for exchange events
     * @see edu.uw.ext.framework.exchange.StockExchange#removeExchangeListener(edu.uw.ext.framework.exchange.ExchangeListener)
     * @param l the listener to be added
     */
    @Override
    public void removeExchangeListener(ExchangeListener l) {
        listenerList.remove(ExchangeListener.class, l);
        
    }
    
    /**Propagate events to the listeners based on the type of event received from the exchange
     * @param event the event received from the exchange
     */
    private void fireExchangeEvent(final ExchangeEvent event) {
        ExchangeListener[] listeners = this.listenerList.getListeners(ExchangeListener.class);
        if (listeners.length != 0) {
            for (ExchangeListener listener : listeners) {
                log.info("EVENT TYPE: {}",event.getEventType().toString());
                switch (event.getEventType()) {
                case OPENED:
                    listener.exchangeOpened(event);
                    break;

                case CLOSED:
                    listener.exchangeClosed(event);
                    break;

                case PRICE_CHANGED:
                    listener.priceChanged(event);
                    break;

                default:
                    log.warn("Attempted to fire an unknown exchange event: "
                                 + event.getEventType());
                    break;
                }
            }
        } else {
            log.info("No listeners currently waiting for events...");
        }
    }

    
    /**Instructs the brokers/exchange to execute a trade
     * @see edu.uw.ext.framework.exchange.StockExchange#executeTrade(edu.uw.ext.framework.order.Order)
     * @param order the order to be executed
     * @return an int reflecting the price at which the trade occurred
     */
    @Override
    public int executeTrade(Order order) {
        //Tell the broker to execute a trade
        //Command format EXECUTE_TRADE_CMD:BUY_ORDER|SELL_ORDER:accountId:symbol:shares
        final String orderType = order.isBuyOrder()? ProtocolConstants.BUY_ORDER: 
                                                    ProtocolConstants.SELL_ORDER;
        final String acctId = order.getAccountId();
        final String ticker = order.getStockTicker();
        final int shares = order.getNumberOfShares();
        final String response;
        
        //Build a command
        final String command = String.join(ProtocolConstants.ELEMENT_DELIMITER,
                                        ProtocolConstants.EXECUTE_TRADE_CMD,
                                        orderType,
                                        acctId,
                                        ticker,
                                        Integer.toString(shares));
        response = this.transmitCommand(command);
        //return the price the order was executed at, as received from the server/exchange
        //response from server will be "-1" if stock symbol not found
        return Integer.parseInt(response);
    }

    /**Sends a GET_QUOTE_CMD:ticker to the exchange, and returns a stock quote, 
     * or null if the ticker is not found
     * @see edu.uw.ext.framework.exchange.StockExchange#getQuote(java.lang.String)
     * @param ticker the stock symbol for which a quote is obtained
     * @return a StockQuote that reflects the current price of the given ticker
     */
    @Override
    public StockQuote getQuote(final String ticker) {
        /*Command format: GET_QUOTE_CMD:symbol*/
        StockQuote quote;
        final String response;
        int price; 
        
        /*Build a command*/
        final String command = String.join(ProtocolConstants.ELEMENT_DELIMITER,
                                        ProtocolConstants.GET_QUOTE_CMD,
                                        ticker);
        /*Transmit the command and capture the response*/
        response = this.transmitCommand(command);
        price = Integer.parseInt(response);
        /*If the given symbol is found in the exchange...*/
        if (price >= 0) {
            quote = new StockQuote(ticker,price);
            /*...otherwise the quote will be null*/
        } else {
            quote = null;
        }

        return quote;
    }

    /**Sends a GET_TICKERS_CMD to the exchange, and return all tickers contained in the exchange,
     * or null if there was an error obtaining the information from the exchange
     * @see edu.uw.ext.framework.exchange.StockExchange#getTickers()
     * @return a <code>String[]</code> containing all tickers in the exchange
     */
    @Override
    public String[] getTickers() {
        /*send get tickers command to the server/exchange*/
        /*Command format GET_TICKERS_CMD*/
        String response = this.transmitCommand(ProtocolConstants.GET_TICKERS_CMD);
        String[] tickers = response.split(ProtocolConstants.ELEMENT_DELIMITER.toString());
        return tickers;
    }

    /**Sends a GET_STATE_CMD to the exchange, and returns a boolean reflecting the current
     * state of the exchange (open/closed)
     * @see edu.uw.ext.framework.exchange.StockExchange#isOpen()
     * @return a <code>boolean</code>- true if the exchange is open, false if it is closed
     */
    @Override
    public boolean isOpen() {
        /*Send exchange state query command*/
        /*Command format GET_STATE_CMD*/
        /*Should respond "OPEN_STATE|CLOSED_STATE*/
        String response = this.transmitCommand(ProtocolConstants.GET_STATE_CMD);
        log.info("Exchange is open?: {}",response);
        
        final boolean state = ProtocolConstants.OPEN_STATE.equals(response);
        return state;
    }

    /**Formats a command consistent with the text-based protocol (in ProtocolConstants)
     * and transmits it to the exchange. Returns a string representing the text-based
     * protocol response from the server
     * @param command the command to be transmitted
     * @return a String representing the text-based response from the server
     */
    private String transmitCommand(String command) {
        //build a string command and write to the output stream on the exchange socket
        //also capture the response or null
        String response = null;
        try {
            final OutputStream os = this.commandSocket.getOutputStream();
            final PrintWriter writer = new PrintWriter(os,true);
            
            final InputStreamReader reader = new InputStreamReader(this.commandSocket.getInputStream(),
                    ProtocolConstants.ENCODING);
            final BufferedReader br = new BufferedReader(reader);


            /*Write the command to the output stream*/
            log.info("Transmitting command: {}",command);
            writer.println(command);
            writer.flush();
            /*Read the response back from the input stream*/
            response = br.readLine();
            
            log.info("Transmitted, response: {}",response);
            
        } catch (IOException e) {
            log.warn("Error transmitting command: {}",command);
            e.printStackTrace();
        } 
        return response;
    }

    
    
    /**Joins the multicast group transmitting exchange events, and then processed events received
     * from the exchange by propagating those events to registered listeners. 
     */
    private class NetworkEventProcessor implements Runnable {

        /**buffer size constant*/
        private final int BUFFER_SIZE = 1024;
        
        /**The socket for propagating exchange events*/
        private MulticastSocket eventSocket; 
        
        /**the multicast IP address for propagating exchange events*/
        private String eventIpAddress;
        
        /**the port for propagating exchange events*/
        private int eventPort;
        
        /**the Multicast group which will receive exchange events*/
        private InetAddress group;
        
        /**Constructor- creates a multicast socket and joins the given group
         * @param eventIpAddress the multicast IP address for propagating exchange events
         * @param eventPort the port for propagating exchange events
         */ 
        private NetworkEventProcessor(String eventIpAddress, int eventPort) {
            this.eventIpAddress = eventIpAddress;
            this.eventPort = eventPort;
            try {
                group = InetAddress.getByName(this.eventIpAddress);
            } catch (UnknownHostException e) {
                log.warn("Unable to locate IP address for host, event multicast group not initialized");
                e.printStackTrace();
            }
            log.info("Network Event Processor constructed at address {}, port {}",
                                                            eventIpAddress,eventPort);
        }
        
        /**Accepts and processes market and price change events received from the exchange
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            //Build the buffer, the packet and have the multicast
            //socket receive the packet
            
            log.info("Initializing event listener socket at port {}", this.eventPort);
            
            
            try {
                this.eventSocket = new MulticastSocket(this.eventPort);
                log.info("Event socket {} initialized at port {}",eventSocket, eventPort);
                eventSocket.joinGroup(this.group);
                log.info("Socket joined event multicast group at address {}",this.group.getHostAddress());
            } catch (IOException e2) {
                log.warn("Unable to initialize event multicast socket, or join the multicast group"
                        + "at port {}",eventPort);
                e2.printStackTrace();
            } 
            //handle the events and fire listeners in response to text commands
            //received from the packet (parse first)
            final byte[] buffer =new byte[this.BUFFER_SIZE];
            final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            String msg= null;
            String[] elements = null;
            String eventType = null;
            
            while(true) {
                /*Receive UDP packets (containing events) from a broker*/
                try {
                    eventSocket.receive(packet);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    /*build a command string from the packet data*/
                    msg = new String(packet.getData(),
                                                    packet.getOffset(),
                                                    packet.getLength(),
                                                    ProtocolConstants.ENCODING);
                    
                } catch (UnsupportedEncodingException e) {
                    log.warn("Unable to encode event message bytes using {}",ProtocolConstants.ENCODING);
                    e.printStackTrace();
                } 
                //need to handle potential null pointer exceptions
                if (msg == null) {
                    log.warn("No message received");
                }
                /*Split up the commands in order to parse*/
                elements = msg.split(ProtocolConstants.ELEMENT_DELIMITER.toString());
                eventType = elements[ProtocolConstants.CMD_ELEMENT]; 
                switch(eventType) {
                    case "OPEN_EVENT":
                        fireExchangeEvent(ExchangeEvent.newOpenedEvent(this));
                        log.info("Received market open event, notifying listeners");
                        break;
                    case "CLOSED_EVENT":
                        fireExchangeEvent(ExchangeEvent.newClosedEvent(this));
                        log.info("Received market closed event, notifying listeners");
                        break;
                    case "PRICE_CHANGE_EVENT":
                        /*expected format: PRICE_CHANGE_EVENT:ticker:price*/
                        final String ticker = elements[ProtocolConstants.PRICE_CHANGE_EVENT_TICKER_ELEMENT];
                        final String priceStr = elements[ProtocolConstants.PRICE_CHANGE_EVENT_PRICE_ELEMENT];
                        log.info("Received price change event for {}:{}",ticker,priceStr);
                        /*default will be -1, will be changed if set to a valid ticker*/
                        int price = -1;
                        price = Integer.parseInt(priceStr);
                        fireExchangeEvent(ExchangeEvent.newPriceChangedEvent(this,ticker,price));
                        
                        break;
                    default: 
                        log.warn("Invalid event command- command not recognized");
                }
            }    
        }
    }
}
