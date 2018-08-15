package edu.uw.spl.exchange;

import java.awt.Event;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;

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

public class NetworkExchangeProxyImpl implements StockExchange {
/*
 * The operations of the StockExchange interface, except the listener registration operations, 
 * will be implemented to make requests of the exchange network adapter using the text based 
 * custom protocol
    Receive multicast messages representing exchange events, this must be done on a separate thread
    Event messages will be transformed into the appropriate event object and then propagated 
    to registered listeners
 */
    public static final Logger log = LoggerFactory.getLogger(NetworkExchangeProxyImpl.class);
    private InetAddress group;
    private MulticastSocket multiSocket;
    private Socket server;
    private NetworkEventProcessor commandProcessor;
    
    /*maintain a list of the listeners for iterating*/
    private EventListenerList listeners = new EventListenerList();
    
    /**Constructor*/
    public NetworkExchangeProxyImpl(final String eventIpAddress, 
                                    final int eventPort, 
                                    final String cmdIpAddress,
                                    final int cmdPort) {
        try {
            group = InetAddress.getByName(eventIpAddress);
            server = new Socket(cmdIpAddress, cmdPort);
            commandProcessor = new NetworkEventProcessor(eventPort,group,cmdIpAddress,cmdPort,listeners);
            //need to initialize the socket and listener list
            new Thread(commandProcessor).start();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (multiSocket != null) {
                multiSocket.close();
            }
        }
        
    }
    
    @Override
    public void addExchangeListener(ExchangeListener arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int executeTrade(Order order) {
        //Tell the broker to execute a trade
        String response;
        response = this.transmitCommand(ProtocolConstants.EXECUTE_TRADE_CMD).toString();
        //need to return the price the order was executed at - received from the server/exchange
        return Integer.parseInt(response);
    }

    //Returns a stock quote, or null if the ticker is not found
    @Override
    public StockQuote getQuote(final String ticker) {
        // TODO Auto-generated method stub
        StockQuote quote;
        String response;
        response = this.transmitCommand(ProtocolConstants.GET_QUOTE_CMD).toString();
        /*If the given symbol is found in the exchange...*/
        if (response != "-1") {
            quote = new StockQuote(ticker,Integer.parseInt(response));
        } else {
            quote = null;
        }
        
        return quote;
    }

    @Override
    public String[] getTickers() {
        // TODO Auto-generated method stub
        //send get tickers command to the server/exchange
        String response = this.transmitCommand(ProtocolConstants.GET_TICKERS_CMD).toString();
        String[] tickers = response.split(":");
        return tickers;
    }

    @Override
    public boolean isOpen() {
        //Tell broker exchange is open and follow dispatch order logic
        /*For testing- should return "OPEN_STATE|CLOSED_STATE*/
        String response = this.transmitCommand(ProtocolConstants.GET_STATE_CMD).toString();
        
        return Boolean.parseBoolean(response);
    }

    @Override
    public void removeExchangeListener(ExchangeListener arg0) {
        // TODO Auto-generated method stub
        
    }
    
    private class NetworkEventProcessor implements Runnable {

        private EventListenerList listeners;
        private MulticastSocket socket; 
        
        /**Constructor- creates a multicast socket and joins the given group
         * @param eventPort
         * @param group
         * @param cmdIpAddress
         * @param cmdPort
         * @param listeners
         */
        private NetworkEventProcessor(final int eventPort,
                                        final InetAddress group,
                                        final String cmdIpAddress,
                                        final int cmdPort,
                                        final EventListenerList listeners) {
            this.listeners = listeners;
            try {
                socket = new MulticastSocket(eventPort);
                socket.joinGroup(group);
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                socket.close();
            } 
        }
        
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //Build the buffer, the packet and have the multicast
            //socket receive the packet
            //build a listener list? then loop through the list and 
            //parse each event received to send the event message
            //Use scanner to parse? Or String.split may be sufficient?
            
            /**the components of a command received*/
            String[] commandArray;
            String commandAction;
            String commandTicker;
            String commandPrice;
            
            try {
                while(true) {
                    final byte[] buffer = new byte[256];
                    final DatagramPacket recPacket = new DatagramPacket(buffer,buffer.length);
                    socket.receive(recPacket);
                    
                    final String command =  new String(recPacket.getData(),0,recPacket.getLength());
                    commandArray = command.split(":");
                    //Change from ints to protocol constants
                    commandAction = commandArray[0];
                    commandTicker = commandArray[1]==null?null:commandArray[1];
                    commandPrice = commandArray[2]==null?null:commandArray[2];
                    
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
           
    }
    
    //Write corresponding operation for the server(NetworkExchangeAdapter)
    private ProtocolConstants transmitCommand(ProtocolConstants command) {
        //build a string and write to the output stream on the exchange socket
        //also capture the response or null
        final String ls = System.lineSeparator();
        String response = null;
        
        
        try (final BufferedReader reader = 
                            new BufferedReader(new InputStreamReader(this.server.getInputStream()));
            final PrintWriter writer = 
                    new PrintWriter(this.server.getOutputStream(),true);
            ){
            writer.write(command + ls);
            writer.flush();
            
            response = reader.readLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block- add an error message
            e.printStackTrace();
        }
        return ProtocolConstants.valueOf(response);
    }

}
