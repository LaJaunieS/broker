package edu.uw.spl.exchange;

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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.exchange.ExchangeAdapter;
import edu.uw.ext.framework.exchange.ExchangeEvent;
import edu.uw.ext.framework.exchange.StockExchange;
import edu.uw.ext.framework.exchange.StockQuote;
import edu.uw.ext.framework.order.MarketBuyOrder;
import edu.uw.ext.framework.order.MarketSellOrder;
import edu.uw.ext.framework.order.Order;
import edu.uw.spl.exchange.NetworkExchangeAdapter.CommandHandler;

/**Provides functionality of the exchange through a network connection*/
public class NetworkExchangeAdapter implements ExchangeAdapter {
//static imports for protocol constants*/
    private static final Logger log = LoggerFactory.getLogger(NetworkExchangeAdapter.class);
    
    /**The stock exchange serviced by this network adapter*/
    private StockExchange exchange;
    
    /**Ip address this exchange adapter used to propagate change event messages*/
    private String eventIp;
    
    /**port this exchange adapter will use to propagate change event messages*/
    private int eventPort;
    
    /**The multicast socket*/
    private MulticastSocket eventSocket;
    
    /**the packet(s) by which events will be multicast to listeners*/
    private DatagramPacket datagramPacket;
    
    /**The multicast group*/
    private InetAddress group;
    
    /**Constructor
     * @param exchange the exchange used ot service the network requests
     * @param multicastIp the ipAdress used to propagate price changes
     * @param multicastPort the ip port used to propagate price changes
     * @param commandPort the ports for listening for commands
     * @throws UnknownHostException
     * @throws SocketException
     */
    public NetworkExchangeAdapter(StockExchange exchange,
                                        String eventIp,
                                        int eventPort,
                                        int commandPort) throws UnknownHostException, SocketException {
        this.exchange = exchange;
        InetAddress multicastGroup = InetAddress.getByName(eventIp);
        
        final byte[] buf = {};
        
        datagramPacket = new DatagramPacket(buf, 0, multicastGroup, eventPort);
        
        
        
        /*Attempt to initiate a connection and join group*/
        try {
            eventSocket = new MulticastSocket();
            eventSocket.setTimeToLive(4);
            /*join the multicast group*/
            group = InetAddress.getByName(eventIp);
            
            eventSocket.joinGroup(group);
            if (log.isInfoEnabled()) {
                log.info("Multicasting events to address {}",group.getHostAddress());
            }
            /*Start the listener thread (which will itself initiate the handler)*/
            /*Consider using executor*/
            /*Will be listening for TCP connections to process commands*/
            new Thread(new CommandListener(commandPort, exchange)).start();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        this.exchange.addExchangeListener(this);
    }
    
    /**The exchange has closed- notify clients and remove price change listener
     * @see edu.uw.ext.framework.exchange.ExchangeListener#exchangeClosed(edu.uw.ext.framework.exchange.ExchangeEvent)
     */
    @Override
    public void exchangeClosed(ExchangeEvent event) {
        log.info("****Market closed****");
        this.multicastEvent(ProtocolConstants.CLOSED_EVENT.toString());
        this.exchange.removeExchangeListener(this);
    }

    /** The exchange has opened- add listeners to receive price change events and multicast them
     * to brokers
     * @see edu.uw.ext.framework.exchange.ExchangeListener#exchangeOpened(edu.uw.ext.framework.exchange.ExchangeEvent)
     */
    @Override
    public void exchangeOpened(ExchangeEvent event) {
        log.info("****Market open****");
        this.multicastEvent(ProtocolConstants.OPEN_EVENT.toString());
        this.exchange.addExchangeListener(this);
        
    }

    /** A price change for a ticker has occurred. Multicast the event to brokers
     * @see edu.uw.ext.framework.exchange.ExchangeListener#priceChanged(edu.uw.ext.framework.exchange.ExchangeEvent)
     */
    @Override
    public void priceChanged(ExchangeEvent event) {
        final CharSequence symbol = event.getTicker();
        final int price = event.getPrice();
        final String msg = String.join(ProtocolConstants.ELEMENT_DELIMITER,
                ProtocolConstants.PRICE_CHANGE_EVENT,
                symbol,
                Integer.toString(price));
        log.info("price changed event: {}: {}", event.getTicker(),event.getPrice());
        this.multicastEvent(msg);
        
    }

    /**Closes the adapter
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }
    
    /**Creates a new packet using data from the event and sends to the multicast socket
     * @param msg the message/command to be multicasted to listeners*/
    private synchronized void multicastEvent(final String msg) {
        byte[] buffer;
        try {
            buffer = msg.getBytes(ProtocolConstants.ENCODING.toString());
            datagramPacket.setData(buffer);
            datagramPacket.setLength(buffer.length);
            
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            this.eventSocket.send(datagramPacket);
            log.info("Sent event for: {}",msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
     

    /**Receives and executes commands from a client
     * @author slajaunie
     *
     */
    class CommandHandler implements Runnable {
        private final Logger log = LoggerFactory.getLogger(CommandHandler.class);
        
        /**The exchange the client will be interacting with*/
        private final StockExchange exchange;
        
        /**Socket for the client connection*/
        private Socket socket;
        
        /**Constructor- Assigns an exchange and a socket for receiving/transmitting commands
         * @param exchange the exchange the client will be interacting with
         * @param socket the socket for the client connection
         */
        public CommandHandler(final StockExchange exchange, final Socket socket) {
            this.exchange = exchange;
            this.socket = socket;
        }
        
        /**Receives and executes the command received from the client via an input stream,
         *  and writes a response to the output stream 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            // TODO Auto-generated method stub
            String command;
            String response = ProtocolConstants.INVALID_COMMAND;
            String[] tickers = exchange.getTickers();
            
            try (final InputStreamReader reader = new InputStreamReader(this.socket.getInputStream());
                    final BufferedReader br = new BufferedReader(reader);
                    
                    final PrintWriter writer = 
                            new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
                    ){
                
                /*Receive a command...*/
                command = br.readLine();
                /*....Send a response*/
                /*An array consisting of the individual portions of a command*/
                //Need to both get readLine and match it to a string in the constants-
                //two operations need to accomplish without advancing cursor
                    /*Read and capture a command from the socket input stream*/
                while (command !=null) {
                    log.info("Received command: {}",command);
                    
                    final String[] elements = command.split(ProtocolConstants.ELEMENT_DELIMITER.toString());
                    final String cmd = elements[ProtocolConstants.CMD_ELEMENT];
                    
                    switch(cmd) {
                        case "GET_STATE_CMD":
                            /*Return the current state of the exchange*/
                            log.info("Get state command received");
                            response = doGetState();
                        
                            break;
                        case "GET_TICKERS_CMD":
                            //see notes:
                            log.info("Get tickers command received");
                            
                            /*Server will format into a string response, ie BA:F:PFG...*/
                            response = doGetTickers();
                        
                            break;
                        case "GET_QUOTE_CMD":
                            /*Return the current price of the symbol in the command*/
                            String ticker = elements[ProtocolConstants.QUOTE_CMD_TICKER_ELEMENT];
                            log.info("Get quote command received for ticker {}",ticker);
                            response = doGetQuote(ticker);
                            
                            break;
                        case "EXECUTE_TRADE_CMD":
                            /*Exceute the trade on the given account and return the 
                             * execution price
                 * Command format EXECUTE_TRADE_CMD:BUY_ORDER|SELL_ORDER:account_id:symbol:shares
                             */
                            log.info("Execute trade command received for {}: {} for {}, {} shares",
                                                    elements[ProtocolConstants.EXECUTE_TRADE_CMD_ACCOUNT_ELEMENT],
                                                    elements[ProtocolConstants.EXECUTE_TRADE_CMD_ORDER_TYPE_ELEMENT],
                                                    elements[ProtocolConstants.EXECUTE_TRADE_CMD_TICKER_ELEMENT],
                                                    elements[ProtocolConstants.EXECUTE_TRADE_CMD_SHARES_ELEMENT]);
                            
                            response = doExecuteTrade(elements);
                            
                            break;
                        default:
                            log.warn("Command not recognized: {}",command);
                            /*Will just send the default response (invalid command)*/
                            writer.println(response);
                            writer.flush();
                        
                        }
                        log.info("Responded to client command with {}",response);
                        writer.println(response);
                        writer.flush();
                    
                        command = br.readLine();
                    }
                    
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        this.socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                
            }
            //close the sockets
            
            /**Prepares a response to a get state command consistent with the text-based protocol
             * @return a String- the open state response if the exchange is open, the closed state
             * response if the exchange is closed
             */
            private String doGetState() {
                final String response = exchange.isOpen() ? ProtocolConstants.OPEN_STATE.toString():
                                                            ProtocolConstants.CLOSED_STATE.toString();
                return response;
            }
            
            /**Prepares a response to a get tickers command consistent with the text-based protocol
             * @return a String consisting of the get tickers response
             */
            private String doGetTickers() {
                final String[] tickers = exchange.getTickers();
                final String response = String.join(ProtocolConstants.ELEMENT_DELIMITER,
                                                    tickers);
                return response;
            }
            
            /**Prepares a response to a get quote command consistent with the text-based protocol
             * @param ticker the ticker for which a quote will be returned
             * @return a String consisting of the get quote response, or a response indicating that
             * the stock was not found
             */
            private String doGetQuote(final String ticker) {
                final StockQuote quote = exchange.getQuote(ticker);
                int price = (quote == null)? Integer.parseInt(ProtocolConstants.INVALID_STOCK):
                                            quote.getPrice();
                String response = Integer.toString(price);
                log.info("Sending quote: {} at {}",quote.getTicker(),quote.getPrice());
                return response;
            }
            
            /**Executes the trade in response to an execute trade command consistent with 
             * the text-based protocol
             * @param elements an array consisting of the individual components of the 
             * execute trade command (command, order type, price) as parsed by the handler
             * @return a String consisting of the exchange's execute trade response, the price 
             * at which the trade was executed
             */
            private String doExecuteTrade(final String[] elements) {
                /*parse the individual components of the command*/
                /*command format: EXECUTE_TRADE_CMD:BUY_ORDER|SELL_ORDER:accountid:symbol:shares*/
                final Order order;
                final String orderType = elements[ProtocolConstants.EXECUTE_TRADE_CMD_ORDER_TYPE_ELEMENT];
                final String acctId = elements[ProtocolConstants.EXECUTE_TRADE_CMD_ACCOUNT_ELEMENT];
                final String ticker = elements[ProtocolConstants.EXECUTE_TRADE_CMD_TICKER_ELEMENT];
                final int qty = Integer.parseInt(elements[ProtocolConstants.EXECUTE_TRADE_CMD_SHARES_ELEMENT]);
                final String response; 
                
                int price;
                
                if (ProtocolConstants.BUY_ORDER.toString().equals(orderType)) {
                    order = new MarketBuyOrder(acctId, qty, ticker);
                    log.info("New market buy order created for order");
                } else {
                    order=  new MarketSellOrder(acctId, qty, ticker);
                    log.info("New market buy order created for order");

                }
                
                price = exchange.executeTrade(order);
                log.info("Executed trade: {}",order.toString());
                if (price == 0) {
                    response =  ProtocolConstants.INVALID_STOCK;
                } else {
                    response = Integer.toString(price);
                    
                }
                return response;
            }
        }
    

        /**Listens for and accepts connections from a client; passes connections to the 
         * CommandHandler for the reading and processing of commands
         * @author slajaunie
         *
         */
        private class CommandListener implements Runnable {
            private final Logger log = LoggerFactory.getLogger(CommandListener.class);
            
            private ServerSocket serverSocket;
            private StockExchange exchange;
            private int commandPort;
            private Socket client;
            private boolean listening;
            /*Consider using executor service with cached thread pool*/
            
            private CommandListener(int commandPort, StockExchange exchange) {
                this.exchange = exchange;
                this.commandPort = commandPort;
                try {
                    serverSocket = new ServerSocket(commandPort);
                    log.info("Command socket opened at port {}",commandPort);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
            
            /**Listens for and accepts connections from a client; then runs a CommandHandler
             * in a separate thread to process commands received from the client
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                listening = true;
                CommandHandler handler = null;
                try {
                    while(listening) {
                        Socket client = null;
                        log.info("Listening for commands at port {}...",this.commandPort);
                        
                        /*when a connection is accepted, open a new thread and run the handler*/
                        client = serverSocket.accept();
                        log.info("Client command connection accepted at port {}",this.commandPort);
                        handler = new CommandHandler(this.exchange, client);
                        new Thread(handler).start();
                    }
                } catch (IOException ex) {
                    if (serverSocket!= null && serverSocket.isClosed()) {
                        log.warn("There was an error accepting the command connection", ex);
                    }
                }
                finally {
                    listening = false;
                    if (serverSocket != null && serverSocket.isClosed()) {
                            this.close();
                            serverSocket = null;
                        
                    }
                    //if providing an excecutor initiate shutdown()
                    this.close();
                }
            }
            
            /**
             *Closes the socket
             */
            public void close() {
                if (serverSocket != null || client != null) {
                    try {
                        client.close();
                        serverSocket.close();
                    } catch (IOException e) {
                        log.warn("Error closing resources");
                        e.printStackTrace();
                    } finally {
                        client = null;
                        serverSocket = null;
                    }

                }
            }
        }
}
