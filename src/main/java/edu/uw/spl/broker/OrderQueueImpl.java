package edu.uw.spl.broker;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.broker.OrderQueue;
import edu.uw.ext.framework.order.Order;

public class OrderQueueImpl<T, E extends Order> 
                                implements OrderQueue<T, E> {

    private static final Logger log = LoggerFactory.getLogger(OrderQueueImpl.class);
    
    /**
     * The order queue
     * */
    private TreeSet<E> orderQueue;
    
    /**The threshold value- if StopBuy/StopSell queue, it will be an int (the target/stop price);
     * if a Market Order queue, it will be a boolean (market open/closed?)*/
    private T threshold; 
    
    private final BiPredicate<T,E> dispatchFilter;
    
    /**The Consumer that processes orders when they become dispatchable*/
    private Consumer<E> orderProcessor;
    
    /**
     * Constructor
     * 
     * The market order implementation constructor- 
     * The threshold will be a boolean testing whether market is open or close*/
    public OrderQueueImpl( T threshold, BiPredicate<T,E> dispatchFilter) {
        this.orderQueue = new TreeSet<>();
        this.threshold = threshold;
        this.dispatchFilter = dispatchFilter;
    }
    
    /**
     * Constructor
     * 
     * The StopBuy/StopSell order implementation constructor- 
     * The threshold will be an Integer that is the stop price*/
    public OrderQueueImpl( T threshold, BiPredicate<T,E> dispatchFilter, Comparator<E> cmp) {
        /*Initialize the treeSet with the comparator argument*/
        this.orderQueue = new TreeSet<E>(cmp);
        this.threshold = threshold;
        this.dispatchFilter = dispatchFilter;
    }
    
    @Override
    public void enqueue(final E order) {
        /*Dispatch any dispatchable orders after adding the new order to the queue*/
        orderQueue.add(order);
        dispatchOrders();
    }

    @Override
    public E dequeue() {
        /*If the first order in the list meets the dispatch threshold
         * remove the first order from the list 
         * Return the order or null, if no more orders in the list*/
        E order = null;
        if (orderQueue.isEmpty()) {
            log.info("queue is empty, nothing to deqeue");
        } else {
            order = orderQueue.first();
            /*If order isn't null (ie list wasn't empty) test with dispatch filter...*/
            if (this.dispatchFilter.test(this.threshold, order)){
                log.info("{} removed from queue",order.toString());
                orderQueue.remove(order);
            } else {
                /*...Otherwise don't remove the order from the list, don't dispatch 
                 * and return null*/
                log.info("{} not dequeued at this time",order.toString());
                order = null;
            };
            }
        
        return order;
    }

    @Override
    public void dispatchOrders() {
        /*capture the first dispatchable order in the queue, if any, derived
         * from invoking the dispatch filter*/
        E order; 
        /*Send dispatchable orders to the market order queue*/
        /*If dequeue tests return a null order, nothing to dispatch and do nothing*/
        /*...also dequeue the next order for processing, or exit if order is null*/
        while ((order = this.dequeue()) !=null) {
            /*send dispatchable orders to the market order queue- if no orderProcessor
            is set, will just deqeue the order*/
            if (this.orderProcessor!=null) {
                this.orderProcessor.accept(order);
                log.info("dispatched order {}",order.toString());
            } else {
                log.info("Nothing to dispatch at this time");
            }
        }
    }

    @Override
    public T getThreshold() {
        return this.threshold;
    }

    @Override
    public void setThreshold(final T threshold) {
        this.threshold = threshold;
        dispatchOrders();
    }

    @Override
    public void setOrderProcessor(Consumer<E> proc) {
        this.orderProcessor = proc;
        
    }
    
    @Override 
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (E order: this.orderQueue) {
            builder.append(order.toString());
            builder.append("; ");
        }
        return builder.toString();
    }
}
