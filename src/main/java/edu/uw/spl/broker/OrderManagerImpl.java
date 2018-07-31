package edu.uw.spl.broker;

import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import edu.uw.ext.framework.broker.OrderManager;
import edu.uw.ext.framework.order.StopBuyOrder;
import edu.uw.ext.framework.order.StopSellOrder;

/**Implementation of an OrderManager which manages stop orders for a particular stock
 * associated with this OrderManager
 * @author slajaunie
 *
 */
public class OrderManagerImpl implements OrderManager {

    /**
     *The stock symbol for the Stock associated with this OrderManager 
     */
    private String symbol;
    
    /**The Stop Sell Order dispatch filter- will dispatch orders when the stock price 
     * meets or falls below the order's target price*/
    private BiPredicate<Integer,StopSellOrder> sSellOrderDispatchFilter = 
                (stockPrice,order)-> (stockPrice > 0) && stockPrice <= order.getPrice();
                
    /**
     *The Stop Buy Order dispatch filter- will dispatch orders when the stock price
     *meets or exceeds the order's target price
     */
    private BiPredicate<Integer,StopBuyOrder> sBuyOrderDispatchFilter = 
            (stockPrice,order)-> (stockPrice > 0) && stockPrice >= order.getPrice();
    
            
     /**Buy Order comparator- orders are ascending by price w/ buyOrders, 
     * descending by price with sellOrders- if order prices are equal, 
     * use implemented ordering for order interface, by number of shares and
     * then order id*/
    private Comparator<StopBuyOrder> sBuyOrderComparator =  
            Comparator.comparing(StopBuyOrder::getPrice)
            .reversed()
            .thenComparing(StopBuyOrder::getNumberOfShares)
            .reversed()
            .thenComparing(StopBuyOrder::getOrderId);    
    
    /**Sell Order comparator- orders are descending by price w/ buyOrders, 
     * descending by price with sellOrders- if order prices are equal, 
     * use implemented ordering for order interface, by number of shares and
     * then order id*/
    private Comparator<StopSellOrder> sSellOrderComparator =  
            Comparator.comparing(StopSellOrder::getPrice)
            .thenComparing(StopSellOrder::getNumberOfShares)
            .reversed()
            .thenComparing(StopSellOrder::getOrderId);
    

    /**
     *The Stop Sell Order Queue- Orders are sorted by price, highest to lowest, then by 
     *the Order Interface's natural ordering 
     */
    private OrderQueueImpl<Integer,StopSellOrder> stopSellOrderQueue; 
   
    /**
     *The Stop Buy Order Queue- Orders are sorted by price, lowest to highest, then by 
     *the Order Interface's natural ordering
     */
    private OrderQueueImpl<Integer,StopBuyOrder> stopBuyOrderQueue;
    
    
    /**Constructor
     * @param symbol the stock symbol to be associated with this order manager
     */
    public OrderManagerImpl(String symbol) {
        this.symbol = symbol;
    }
    
    /**Constructor
     * @param symbol the stock symbol to be associated with this order manager
     * @param price the current price of the stock at the time of instantiation
     */
    public OrderManagerImpl(String symbol, int price) {
        this(symbol);
        Thread stopSellOrderThread = new Thread(new OrderQueueImpl<Integer, StopSellOrder>(price, 
                sSellOrderDispatchFilter, 
                sSellOrderComparator),"stopSellOrderThread");
        Thread stopBuyOrderThread = new Thread(new OrderQueueImpl<Integer, StopBuyOrder>(price, 
                                                            sBuyOrderDispatchFilter, 
                                                            sBuyOrderComparator),"stopBuyOrderThread");
        
    }
    
    //TODO working...
    public void initiateDispatchThread(Runnable r) {
        //call run(), which will dispatchOrders();
        new Thread(r).start();
    }
    
    /**Adjusts the price of this order manager in response to a change in the stock's price,
     * and updates applicable thresholds in the Stop order queues
     * @see edu.uw.ext.framework.broker.OrderManager#adjustPrice(int)
     * @param price the new price of the stock/threshold of the Stop order queues
     */
    @Override
    public void adjustPrice(int price) {
        this.stopBuyOrderQueue.setThreshold(price);
        this.stopSellOrderQueue.setThreshold(price);
    }

    /**Adds a StopBuyOrder to the order queue
     * @see edu.uw.ext.framework.broker.OrderManager#queueOrder(edu.uw.ext.framework.order.StopBuyOrder)
     * @param order a StopBuyOrder to be added to the order queue
     */
    @Override
    public void queueOrder(StopBuyOrder order) {
        this.stopBuyOrderQueue.enqueue(order);
    }

    /**Adds a StopSellOrder to the order queue
     * @see edu.uw.ext.framework.broker.OrderManager#queueOrder(edu.uw.ext.framework.order.StopSellOrder)
     * @param order a StopSellOrder to be added to the order queue
     */
    @Override
    public void queueOrder(StopSellOrder order) {
        this.stopSellOrderQueue.enqueue(order);
    }

    /** Obtains the stock symbol associated with this order manager
     * @see edu.uw.ext.framework.broker.OrderManager#getSymbol()
     */
    @Override
    public String getSymbol() {
        return this.symbol;
    }

    /** Sets the order processor for StopBuyOrders which will process dispatchable orders once
     * the given threshold is reached by the order
     * @see edu.uw.ext.framework.broker.OrderManager#setBuyOrderProcessor(java.util.function.Consumer)
     * @param processor the StopBuyOrder processor
     */
    @Override
    public void setBuyOrderProcessor(Consumer<StopBuyOrder> processor) {
        this.stopBuyOrderQueue.setOrderProcessor(processor);
        
    }

    /** Sets the order processor for StopSellOrders which will process dispatchable orders once
     * the given threshold is reached by the order
     * @see edu.uw.ext.framework.broker.OrderManager#setSellOrderProcessor(java.util.function.Consumer)
     * @param processor the StopSellOrder processor
     */
    @Override
    public void setSellOrderProcessor(Consumer<StopSellOrder> processor) {
        this.stopSellOrderQueue.setOrderProcessor(processor);
    }

    /*Getters for testing*/
    /**Obtains the StopSellOrder Queue
     * @return the StopSellOrder Queue
     */
    public OrderQueueImpl<Integer, StopSellOrder> getStopSellOrderQueue() {
        return stopSellOrderQueue;
    }
    
    /**Obtains the StopSellOrder Queue
     * @return the StopSellOrder Queue
     * */
    public OrderQueueImpl<Integer, StopBuyOrder> getStopBuyOrderQueue() {
        return stopBuyOrderQueue;
    }
}
