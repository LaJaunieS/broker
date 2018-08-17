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


public class NetworkExchangeProxy implements StockExchange {
/*
 * The operations of the StockExchange interface, except the listener registration operations, 
 * will be implemented to make requests of the exchange network adapter using the text based 
 * custom protocol
    Receive multicast messages representing exchange events, this must be done on a separate thread
    Event messages will be transformed into the appropriate event object and then propagated 
    to registered listeners
 */
    
    
    public static final Logger log = LoggerFactory.getLogger(NetworkExchangeProxy.class);
    
    private String commandIpAddress;
    
    private int commandPort;
    
    private NetworkEventProcessor eventProcessor;
    
    private EventListenerList listenerList = new EventListenerList();
    
    private Socket commandSocket;
    
    /**Constructor*/
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
    
    @Override
    public void addExchangeListener(ExchangeListener l) {
        listenerList.add(ExchangeListener.class, l);
        
    }
    
    @Override
    public void removeExchangeListener(ExchangeListener l) {
        listenerList.remove(ExchangeListener.class, l);
        
    }
    
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

    //Returns a stock quote, or null if the ticker is not found
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

    @Override
    public String[] getTickers() {
        /*send get tickers command to the server/exchange*/
        /*Command format GET_TICKERS_CMD*/
        String response = this.transmitCommand(ProtocolConstants.GET_TICKERS_CMD);
        String[] tickers = response.split(ProtocolConstants.ELEMENT_DELIMITER.toString());
        return tickers;
    }

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

    private String transmitCommand(String command) {
        //build a string command and write to the output stream on the exchange socket
        //also capture the response or null
        
        //TODO theres a problem with the input stream- server is responding with the
        //same command as the first call ,causing problems with broker trying to 
        //populate the ticker list on initialization- coming back as [CLOSED_STATE]
        //somehow input stream isn't flushing before writing a new response?
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
            log.warn("Error transmitting command");
            e.printStackTrace();
        } 
        return response;
    }

    
    
    /*Set up to receive and fire in response to events from the exchange. Creates a multicast
     * socket and joins the multicast group broadcasting events from the exchange
     */
    private class NetworkEventProcessor implements Runnable {

        //buffer size constant
        private final int BUFFER_SIZE = 1024;
        
        private MulticastSocket eventSocket; 
        
        private String eventIpAddress;
        private int eventPort;
        private InetAddress group;
        
        /**Constructor- creates a multicast socket and joins the given group
         * @param eventIpAddress
         * @param eventPort
         */ 
        private NetworkEventProcessor(String eventIpAddress, int eventPort) {
            this.eventIpAddress = eventIpAddress;
            this.eventPort = eventPort;
            try {
                group = InetAddress.getByName(this.eventIpAddress);
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            log.info("Network Event Processor constructed at address {}, port {}",
                                                            eventIpAddress,eventPort);
        }
        
        @Override
        public void run() {
            //MulticastSocket eventSocket = null;
            // TODO Auto-generated method stub
            //Build the buffer, the packet and have the multicast
            //socket receive the packet
            //build a listener list? then loop through the list and 
            //parse each event received to send the event message
            //Use scanner to parse? Or String.split may be sufficient?
            
            /*the components of a command received*/
            log.info("Initializing event listener socket at port {}", this.eventPort);
            
            
            try {
                this.eventSocket = new MulticastSocket(eventPort);
                log.info("Event socket {} initialized at port {}",eventSocket, eventPort);
                eventSocket.joinGroup(this.group);
                log.info("Socket joined event multicast group at address {}",this.group.getHostAddress());
            } catch (IOException e2) {
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
