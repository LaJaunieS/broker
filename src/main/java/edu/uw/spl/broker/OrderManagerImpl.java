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
    private int marketPrice; 
    
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

    /*public static void main(String[] args) {
        final String MSFT = "MSFT";
        int MSFT_PRICE_THRESHOLD = 500;
        boolean MARKET_OPEN_THRESHOLD = true;
        
        OrderManagerImpl orderManager = new OrderManagerImpl(MSFT,MSFT_PRICE_THRESHOLD);
      //Test initialized properly
        System.out.println(orderManager.getSymbol());
        orderManager.stopSellOrderQueue.setThreshold(MSFT_PRICE_THRESHOLD);
        orderManager.stopBuyOrderQueue.setThreshold(MSFT_PRICE_THRESHOLD);
        
        //Market orders holding queue
        BiPredicate<Boolean,AbstractOrder> mOrderDispatchFilter = 
                (threshold,order)-> threshold;
        
        OrderQueueImpl<Boolean,AbstractOrder> marketOrders = 
                new OrderQueueImpl<Boolean, AbstractOrder>(MARKET_OPEN_THRESHOLD, mOrderDispatchFilter);
        
        //because of true condition, should just dispatch the orders...
        marketOrders.setThreshold(MARKET_OPEN_THRESHOLD);
        
        Consumer<StopBuyOrder> moveBuyToMarketOrderProcessor = (order)-> {
            //Deqeue the order from the buy order queue and add the order to the market queue
            marketOrders.enqueue(order);
        };
        
        Consumer<StopSellOrder> moveSellToMarketOrderProcessor = (order)->{
            //Deqeue the order from the sell order queue and add the order to the market queue
            marketOrders.enqueue(order);
        };
        //set the order processors...
        orderManager.setBuyOrderProcessor(moveBuyToMarketOrderProcessor);
        orderManager.setSellOrderProcessor(moveSellToMarketOrderProcessor);
        
        //Now add some orders- shouldn't dispatch initially
        orderManager.queueOrder(new StopBuyOrder("neotheone",50,"MSFT",900));
        orderManager.queueOrder(new StopBuyOrder("neotheone",50,"MSFT",1000));
        orderManager.queueOrder(new StopBuyOrder("neotheone",30,"MSFT",600));
        orderManager.queueOrder(new StopBuyOrder("neotheone",40,"MSFT",600));
        
        orderManager.queueOrder(new StopSellOrder("neotheone",50,"MSFT",400));
        orderManager.queueOrder(new StopSellOrder("neotheone",50,"MSFT",450));
        orderManager.queueOrder(new StopSellOrder("neotheone",30,"MSFT",450));
        orderManager.queueOrder(new StopSellOrder("neotheone",40,"MSFT",350));
        
        //Test that orders added correctly, sorting correctly
        System.out.println(orderManager.stopBuyOrderQueue.toString());
        System.out.println(orderManager.stopSellOrderQueue.toString());
        
        //all but last sell order should dispatch
        orderManager.adjustPrice(400);
        
        //all but highest buy order should dispatch
        orderManager.adjustPrice(950);
        
        //confirm expected behavior worked
        System.out.println("Remaining Stop buy orders: " + orderManager.stopBuyOrderQueue.toString());
        System.out.println("Remaining stop sell orders: " + orderManager.stopSellOrderQueue.toString());
        
        //all but orders indicated above should now be in market order queue if condition
        //set to false, otherwise... 
        System.out.println("Market orders: " + marketOrders.toString());
        
        
        
    }*/
}
