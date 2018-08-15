package edu.uw.spl.exchange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
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

/**Provides functionality of the exchange through a network connection*/
public class NetworkExchangeAdapterImpl implements ExchangeAdapter {

    /**The stock exchange serviced by this network adapter*/
    private StockExchange exchange;
    
    /**Ip address this exchange adapter used to propagate change event messages*/
    private String eventIp;
    
    /**port this exchange adapter will use to propagate change event messages*/
    private int eventPort;
    
    /**The multicast socket*/
    private MulticastSocket multiSocket;
    
    /**The multicast group*/
    private InetAddress group;
    
    public NetworkExchangeAdapterImpl(StockExchange exchange,
                                        String eventIp,
                                        int eventPort,
                                        int cmdPort) {
        this.exchange = exchange;
        this.eventIp = eventIp;
        this.eventPort = eventPort;
        /*Attempt to initiate a connection and join group*/
        try {
            /*join the multicast group*/
            group = InetAddress.getByName(eventIp);
            multiSocket = new MulticastSocket();
            multiSocket.joinGroup(group);
            /*Start the listener thread (which will itself initiate the handler)*/
            new CommandListener(exchange,eventPort).start();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        this.exchange.addExchangeListener(this);
    }
    
    @Override
    public void exchangeClosed(ExchangeEvent event) {
        /*"convert" close event to a text command and multicast to brokers
         * who are- listening? registered? accessing in some way?
         */
        // TODO Auto-generated method stub
        this.castEvent(event);
    }

    @Override
    public void exchangeOpened(ExchangeEvent event) {
        /*"convert" open event to a text command and multicast to brokers
         * who are- listening? registered? accessing in some way?
         */
        this.castEvent(event);
        
    }

    @Override
    public void priceChanged(ExchangeEvent event) {
        /*"convert" price change event to a text command and multicast to brokers
         * who are- listening? registered? accessing in some way?
         */
        this.castEvent(event);
        
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        
    }
    
    //TODO consider a private method that handles sending the text-based events
    //via the multicast
    /*Creates a new packet using data from the event and sends to the multicast socket*/
    private void castEvent(final ExchangeEvent event) {
        byte[] buffer = new byte[1024];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                                                        this.group, this.eventPort);
        
        byte[] bytes = this.getEventString(event).getBytes();
        packet.setData(bytes);
        packet.setLength(bytes.length);
        try {
            this.multiSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
                
        
    }
    
    /*Obtains event data and builds a text-based response from that data
     * for transmitting to listeners via the socket*/
    private String getEventString(final ExchangeEvent evt) {
        StringBuilder eventString =  new StringBuilder();
        ExchangeEvent.EventType evtType = evt.getEventType();
        if (evtType.equals(ExchangeEvent.EventType.CLOSED)) {
            eventString.append(ProtocolConstants.CLOSED_EVENT);
            //build an exchange closed response and append to th e
            //string builder
        } else if (evtType.equals(ExchangeEvent.EventType.OPENED)) {
            //build an exchange open response
            eventString.append(ProtocolConstants.OPEN_EVENT);
        } else if (evtType.equals(ExchangeEvent.EventType.PRICE_CHANGED)) {
            eventString.append(ProtocolConstants.PRICE_CHANGE_EVENT);
            //command+
            eventString.append(evt.getTicker()+":").append(evt.getPrice());
        } else {
            
        }
        return eventString.toString();
    }
 

    private class CommandHandler implements Runnable {
        private final Logger log = LoggerFactory.getLogger(CommandHandler.class);
        
        private final StockExchange exchange;
        
        /**Socket for the client connection*/
        private Socket socket;
        
        private boolean handling = false;
        
        private CommandHandler(final StockExchange exchange, final Socket socket) {
            this.exchange = exchange;
            this.socket = socket;
        }
        
        
        
        @Override
        public void run() {
            // TODO Auto-generated method stub
            handling = true;
            try (   InputStreamReader isr = new InputStreamReader(socket.getInputStream());
                    BufferedReader reader = new BufferedReader(isr);
                    BufferedWriter writer = 
                            new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    ) {
                /*Receive a command...*/
                String command;
                /*....Send a response*/
                String response =  null;
                //Need to both get readLine and match it to a string in the constants-
                //two operations need to accomplish without advancing cursor
                while (handling) {
                    /*Read and capture a command from the socket input stream*/
                    while ((command = reader.readLine()) != null) {
                        switch(command) {
                            case "GET_STATE_CMD":
                                /*Return the current state of the exchange*/
                                if (exchange.isOpen()) {
                                    response = ProtocolConstants.OPEN_STATE.toString();
                                } else {
                                    response = ProtocolConstants.CLOSED_STATE.toString();
                                }
                                //return OPEN|CLOSED
                                break;
                            case "GET_TICKERS_CMD":
                                /*Return the tickers in the exchange*/
                                //return symbol:symbol:symbol....
                                String[] tickers = exchange.getTickers();
                                StringBuilder builder = new StringBuilder();
                                for (final String symbol: tickers) {
                                    builder.append(symbol)
                                        .append(ProtocolConstants.ELEMENT_DELIMITER);
                                }
                                response = builder.toString();
                                break;
                            case "GET_QUOTE_CMD":
                                /*Return the current price of the symbol in the command*/
                                final int delimiterPosition = 
                                    command.indexOf(ProtocolConstants.ELEMENT_DELIMITER.toString());
                                if (delimiterPosition != -1) {
                                    final String ticker = command.substring(delimiterPosition+1); //need to parse stock String?;
                                    final StockQuote quote = exchange.getQuote(ticker);
                                    response = Integer.toString(quote.getPrice()); //in cents
                                } else {
                                    response = "-1";
                                } 
                                //return price|-1
                                break;
                            case "EXECUTE_TRADE_CMD":
                                /*Exceute the trade on the given account and return the 
                                 * execution price
                     * Command format EXECUTE_TRADE_CMD:BUY_ORDER|SELL_ORDER:account_id:symbol:shares
                                 */
                                response = this.executeOrderCommand(command);
                                break;
                            default:
                                log.warn("Command not recognized: {}",command);
                            }
                    }
                    writer.write(response);
                    writer.flush();
                    
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        private void stopHandler() {
            handling = false;
        }
        
        private String executeOrderCommand(String command) {
            /*Scanner will parse the command*/
            final Scanner scanner = new Scanner(command);
            scanner.useDelimiter(ProtocolConstants.ELEMENT_DELIMITER.toString());
            
            /*Will be the price the stock trade executed at*/
            String response;
            
            final Order order;
            
            /*Start parsing the command*/
            /*Skip the first token since it's the command type*/
            scanner.skip(ProtocolConstants.ELEMENT_DELIMITER.toString());
            final String orderType= scanner.next();
            final String accountId = scanner.next();
            final String symbol = scanner.next();
            final int shares = scanner.nextInt();
            if (orderType.equals(ProtocolConstants.SELL_ORDER.toString())) {
                order = new MarketSellOrder(accountId,shares,symbol);
            } else {
                order = new MarketBuyOrder(accountId, shares, symbol);
            }
            /*...Execute the trade*/
            response = Integer.toString(exchange.executeTrade(order));
            return response;
        }
        
    }
    
    /*The thread that will listen for commands via the server socket*/
    private class CommandListener extends Thread {
        private final Logger log = LoggerFactory.getLogger(CommandListener.class);
        private ServerSocket serverSocket;
        private StockExchange exchange;
        private int commandPort;
        private Socket client;
        private boolean listening;
        
        private CommandListener(StockExchange exchange, int commandPort) {
            this.exchange = exchange;
            this.commandPort = commandPort;
            try { 
                serverSocket = new ServerSocket(commandPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void run() {
            listening = true;
            CommandHandler handler = null;
            try {
                while(listening) {
                    /*when a connection is accepted, open a new thread and run the handler*/
                    client = serverSocket.accept();
                    handler = new CommandHandler(exchange, client);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                log.warn("Error connecting to client socket: {}",e);
                e.printStackTrace();
            } finally {
                handler.stopHandler();
            }
        }
        
        public void close() {
            try {
                listening = false;
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
