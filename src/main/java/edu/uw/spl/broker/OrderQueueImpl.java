package edu.uw.spl.broker;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.broker.OrderQueue;
import edu.uw.ext.framework.order.Order;

/**
 * Implementation of OrderQueue, using a TreeSet to store Orders
 * @author slajaunie
 * @param T the dispatch threshold type
 * @param E the type of order contained in the queue
 */
public class OrderQueueImpl<T, E extends edu.uw.ext.framework.order.Order> 
                                implements OrderQueue<T, E>, Runnable{

    private static final Logger log = LoggerFactory.getLogger(OrderQueueImpl.class);
    
    /**
     * The order queue
     * */
    private TreeSet<E> orderQueue;
    
    /**The threshold value- if StopBuy/StopSell queue, it will be an int (the target/stop price);
     * if a Market Order queue, it will be a boolean (market open/closed?)*/
    private T threshold; 
    
    /**Lock controlling access to the queue*/
    private Lock lock = new ReentrantLock();
    
    /**Lock condition used for initiating processing orders*/
    private Condition condition = lock.newCondition();
    
    /**
     * Dispatches orders for processing that meet the given threshold 
     */
    private BiPredicate<T,E> dispatchFilter;
    
    /**The Consumer that processes orders when they become dispatchable*/
    private Consumer<E> orderProcessor;
    
    /**
     * The market order implementation constructor- 
     * 
     * The threshold will be a boolean testing whether market is open or closed
     * @param threshold the initial threshold
     * @param dispatchFilter the dispatch filter that will be used to control dispatching orders
     * from the queue
     *  
     * */
    public OrderQueueImpl( T threshold, BiPredicate<T,E> dispatchFilter) {
        this.orderQueue = new TreeSet<>();
        this.threshold = threshold;
        this.dispatchFilter = dispatchFilter;
    }
    
    /**
     * The StopBuy/StopSell order implementation constructor- 
     * 
     * The threshold will be an Integer that is the stop price
     * @param threshold the initial threshold
     * @param dispatchFilter the dispatch filter that will be used to control dispatching orders
     * from the queue
     * @param cmp the Comparator controlling the ordering of orders added to this queue
     * */
    
    public OrderQueueImpl( T threshold, BiPredicate<T,E> dispatchFilter, Comparator<E> cmp) {
        /*Initialize the treeSet with the comparator argument*/
        this.orderQueue = new TreeSet<E>(cmp);
        this.threshold = threshold;
        this.dispatchFilter = dispatchFilter;
    }
    
    /** Adds the specified order to the queue. Dispatches any dispatchable orders after
     * placing the given order into the queue 
     * @see edu.uw.ext.framework.broker.OrderQueue#enqueue(edu.uw.ext.framework.order.Order)
     * @param order an order to place in the queue
     */
    @Override
    public void enqueue(final E order) {
        /*Dispatch any dispatchable orders after adding the new order to the queue*/
        try {
            lock.lock();
            orderQueue.add(order);
            dispatchOrders();
        } finally {
            lock.unlock();
        }
    }

    
    /**Removes the highest dispatchable order in the queue. If there are orders in the 
     * queue but they do not meet the dispatch threshold order will not be removed and 
     * null will be returned.
     * @see edu.uw.ext.framework.broker.OrderQueue#dequeue()
     * @return the first dispatchable order in the queue, or null if there are no dispatchable 
     * orders in the queue
     */
    @Override
    public E dequeue() {
        /*If the first order in the list meets the dispatch threshold
         * remove the first order from the list 
         * Return the order or null, if no more orders in the list*/
        
        //TODO- add lock/unlock logic from enqueue
        E order = null;
        lock.lock();
        try {
            if (orderQueue.isEmpty()) {
                log.info("queue is empty, nothing to deqeue");
            } else {
                order = orderQueue.first();
                /*If order isn't null (ie list wasn't empty) test with dispatch filter...*/
                if (this.dispatchFilter.test(this.threshold, order)){
                    log.info("{} removed from queue",order.toString());
                    if (this.dispatchFilter !=null) 
                        orderQueue.remove(order);
                } else {
                    /*...Otherwise don't remove the order from the list, don't dispatch 
                     * and return null*/
                    log.info("{} not dequeued at this time",order.toString());
                    order = null;
                };
            }
        } finally {
            lock.unlock();
        }
        
        return order;
        
    }

    
    @Override
    public void run() {
        E order; 
        
        while (true ) {
            
            try {
                /*acquire the lock...*/
                lock.lock();
                /*Thread will wait/release the lock until items are in the queue, at which point
                 * dispatchOrders() will signal the waiting thread    
                 */
                while ((order = this.dequeue()) ==null) {
                    condition.await();
                    log.info("Thread waiting for signal...");
                }
                dispatchOrders();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                /*Release the lock once done*/
                lock.unlock();
            }
        } 
    }
    
    /** Executes the callback for each dispatchable order. Each dispatchable order is in turn 
     * removed from the queue and passed to the callback. If no callback is registered (ie, null)
     * the order is simply removed from the queue.
     * @see edu.uw.ext.framework.broker.OrderQueue#dispatchOrders()
     * 
     */
    @Override
    public void dispatchOrders() {
        /*capture the first dispatchable order in the queue, if any, derived
         * from invoking the dispatch filter
         *  Send dispatchable orders to the market order queue*/
        /*  If dequeue tests return a null order, nothing to dispatch and do nothing*/
        /*...also dequeue the next order for processing, or return if order is null*/
        E order;
        lock.lock();
        
        try {
            while ((order = this.dequeue()) !=null) {
                /*send dispatchable orders to the market order queue- if no orderProcessor
                is set, will just dequeue the order*/
                if (this.orderProcessor!=null) {
                    this.orderProcessor.accept(order);
                    log.info("dispatched order {}",order.toString());
                } else {
                    log.info("Order processor is null, nothing dispatched");
                }
            }
            /*Signal another thread to wake up*/
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    /** Obtains the current threshold value
     * @see edu.uw.ext.framework.broker.OrderQueue#getThreshold()
     * @return the current threshold value
     * 
     */
    @Override
    public T getThreshold() {
        return this.threshold;
    }

    /**Sets the threshold value
     * @see edu.uw.ext.framework.broker.OrderQueue#setThreshold(java.lang.Object)
     * @param threshold the new threshold value
     */
    @Override
    public void setThreshold(final T threshold) {
        this.threshold = threshold;
        dispatchOrders();
    }

    /**Sets the order processor that will process dispatchable orders
     * @see edu.uw.ext.framework.broker.OrderQueue#setOrderProcessor(java.util.function.Consumer)
     */
    @Override
    public void setOrderProcessor(final Consumer<E> proc) {
        this.orderProcessor = proc;
    }
    
    /**Outputs a formatted string of the contents of the order queue, 
     * printing a string representing every order in the queue
     * @see java.lang.Object#toString()
     * @return a formatted string of the order queue
     */
    @Override 
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (E order: this.orderQueue) {
            builder.append(order.toString());
            builder.append("; ");
        }
        return builder.toString();
    }
    
    /**Outputs the number of orders currently contained in the order queue
     * @return an int representing the number of orders currently contained in the order queue
     */
    public int length() {
        return this.orderQueue.size();
    }

    /**Returns a shallow copy of the TreeSet containing the orders in this queue
     * @return a shallow copy of the TreeSet containing the orders in this queue
     */
    public TreeSet<E> getOrderQueue() {
        return (TreeSet<E>) orderQueue.clone();
    }

    
}
