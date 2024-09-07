package org.sample.producerconsumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class BridgeMessageBroker {
	public BlockingQueue<Integer> requestQueue = new ArrayBlockingQueue<Integer>(30);
	public BlockingQueue<Integer> responseQueue = new ArrayBlockingQueue<Integer>(30);
    public Boolean continueProducing = Boolean.TRUE;
 
    public void putRequest(Integer data) throws InterruptedException
    {
        this.requestQueue.put(data);
    }
 
    public Integer getRequest() throws InterruptedException
    {
    	//Wait up to 3 second if element is not available
        return this.requestQueue.poll(3, TimeUnit.SECONDS);
    }
    
    
    
    public void putResponse(Integer data) throws InterruptedException
    {
        this.responseQueue.put(data);
    }
 
    public Integer getResponse() throws InterruptedException
    {
    	//Wait up to 3 second if element is not available
        return this.responseQueue.poll(3, TimeUnit.SECONDS);
    }

}
