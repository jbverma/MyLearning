package org.sample.producerconsumer;

public class BridgeMessageConsumer implements Runnable{
	
	private String name;
    private BridgeMessageBroker bridgeMessageBroker;
 
 
    public BridgeMessageConsumer(String name, BridgeMessageBroker bridgeMessageBroker)
    {
        this.name = name;
        this.bridgeMessageBroker = bridgeMessageBroker;
    }
 
 
    @Override
    public void run()
    {
        try
        {
            Integer data = bridgeMessageBroker.getRequest();            
            while (bridgeMessageBroker.continueProducing || data != null)
            {
                Thread.sleep(1000);

                // Process request
                System.out.println("Consumer " + this.name + " processed data for request: " + data);
                // Add response
                bridgeMessageBroker.putResponse(data);

                // Get next request
                data = bridgeMessageBroker.getRequest();
            }
 
 
            System.out.println("Comsumer " + this.name + " finished it's job... terminating");
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }
    }

}
