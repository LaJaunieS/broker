package edu.uw.spl.broker;

import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import edu.uw.ext.framework.broker.OrderManager;
import edu.uw.ext.framework.order.AbstractOrder;
import edu.uw.ext.framework.order.MarketBuyOrder;
import edu.uw.ext.framework.order.MarketOrder;
import edu.uw.ext.framework.order.MarketSellOrder;
import edu.uw.ext.framework.order.Order;
import edu.uw.ext.framework.order.StopBuyOrder;
import edu.uw.ext.framework.order.StopSellOrder;

public class OrderManagerImpl implements OrderManager {

    private String symbol;
    
    /**Order dispatch filters*/
    private BiPredicate<Integer,StopSellOrder> sSellOrderDispatchFilter = 
                (stockPrice,order)-> (stockPrice > 0) && stockPrice <= order.getPrice();
                
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
    
    
    public OrderManagerImpl(String symbol) {
        this.symbol = symbol;
    }
    
    public OrderManagerImpl(String symbol, int price) {
        this(symbol);
        stopSellOrderQueue = new OrderQueueImpl<Integer, StopSellOrder>(price, 
                sSellOrderDispatchFilter, 
                sSellOrderComparator);
        stopBuyOrderQueue = new OrderQueueImpl<Integer, StopBuyOrder>(price, 
                                                            sBuyOrderDispatchFilter, 
                                                            sBuyOrderComparator);
    }
    
    @Override
    public void adjustPrice(int price) {
        this.stopBuyOrderQueue.setThreshold(price);
        this.stopSellOrderQueue.setThreshold(price);
    }

    @Override
    public void queueOrder(StopBuyOrder order) {
        this.stopBuyOrderQueue.enqueue(order);
    }

    @Override
    public void queueOrder(StopSellOrder order) {
        this.stopSellOrderQueue.enqueue(order);
    }

    @Override
    public String getSymbol() {
        return this.symbol;
    }

    @Override
    public void setBuyOrderProcessor(Consumer<StopBuyOrder> processor) {
        this.stopBuyOrderQueue.setOrderProcessor(processor);
        
    }

    @Override
    public void setSellOrderProcessor(Consumer<StopSellOrder> processor) {
        this.stopSellOrderQueue.setOrderProcessor(processor);
    }

    /*Getters for testing*/
    public OrderQueueImpl<Integer, StopSellOrder> getStopSellOrderQueue() {
        return stopSellOrderQueue;
    }

    public OrderQueueImpl<Integer, StopBuyOrder> getStopBuyOrderQueue() {
        return stopBuyOrderQueue;
    }
}
