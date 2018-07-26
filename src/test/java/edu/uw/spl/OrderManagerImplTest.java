package edu.uw.spl;

import static org.junit.Assert.*;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import edu.uw.ext.framework.order.Order;
import edu.uw.ext.framework.order.StopBuyOrder;
import edu.uw.ext.framework.order.StopSellOrder;
import edu.uw.spl.broker.OrderManagerImpl;
import edu.uw.spl.broker.OrderQueueImpl;

public class OrderManagerImplTest {

    final private String MSFT = "MSFT";
    
    final private int MSFT_PRICE_THRESHOLD = 500;
    
    final private boolean MARKET_OPEN_THRESHOLD = false;
    
    private OrderManagerImpl orderManager;
    
    private OrderQueueImpl<Boolean,Order> marketOrders;
    
    private BiPredicate<Boolean,Order> mOrderDispatchFilter;
    
    private Consumer<StopBuyOrder> moveBuyToMarketOrderProcessor;
    
    private Consumer<StopSellOrder> moveSellToMarketOrderProcessor;
    
    private Consumer<Order> marketOrderProcessor;
    
    @Before
    public void setup() {
        mOrderDispatchFilter = (threshold,order)-> threshold;
        marketOrderProcessor = (order)->{
            System.out.println("Market order would be processed here");
        };
        //An example market orderQueue
        marketOrders = 
                new OrderQueueImpl<Boolean, Order>(MARKET_OPEN_THRESHOLD, mOrderDispatchFilter);
        
        //if true condition, should just dispatch the orders...
        marketOrders.setThreshold(MARKET_OPEN_THRESHOLD);
        
        marketOrders.setOrderProcessor(marketOrderProcessor);
        
        
        //An example orderManager
        orderManager = new OrderManagerImpl(MSFT,MSFT_PRICE_THRESHOLD);
        moveBuyToMarketOrderProcessor = (order)-> marketOrders.enqueue(order);
        moveSellToMarketOrderProcessor = (order)-> marketOrders.enqueue(order);
        
        orderManager.setBuyOrderProcessor(moveBuyToMarketOrderProcessor);
        orderManager.setSellOrderProcessor(moveSellToMarketOrderProcessor);
        
        //Add some orders
        orderManager.queueOrder(new StopBuyOrder("neotheone",50,"MSFT",900));
        orderManager.queueOrder(new StopBuyOrder("neotheone",50,"MSFT",1000));
        orderManager.queueOrder(new StopBuyOrder("neotheone",30,"MSFT",600));
        orderManager.queueOrder(new StopBuyOrder("neotheone",40,"MSFT",600));
        
        orderManager.queueOrder(new StopSellOrder("neotheone",50,"MSFT",400));
        orderManager.queueOrder(new StopSellOrder("neotheone",50,"MSFT",450));
        orderManager.queueOrder(new StopSellOrder("neotheone",30,"MSFT",450));
        orderManager.queueOrder(new StopSellOrder("neotheone",40,"MSFT",350));
    };
    
    
    @Test
    public void testOrderManagerInstantiated() {
        assertEquals(MSFT,orderManager.getSymbol());
    }
    
    @Test
    public void testOrderManagerQueues() {
        assertEquals(4, orderManager.getStopBuyOrderQueue().length());
        assertEquals(4,orderManager.getStopSellOrderQueue().length());
    }
    
    @Test
    public void testAdjustPrice() {
        //all but last sell order should dispatch
        orderManager.adjustPrice(400);
        assertEquals(1,orderManager.getStopSellOrderQueue().length());
        
        //all but highest buy order should dispatch
        orderManager.adjustPrice(950);
        assertEquals(1, orderManager.getStopBuyOrderQueue().length());
        
    }
    
    @Test 
    public void testMarketOpen() {
        //when the market opens, all pending stop order queues should dispatch...
        marketOrders.setThreshold(Boolean.TRUE);
        assertEquals(0,marketOrders.length());
    }

}
