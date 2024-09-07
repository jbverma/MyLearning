package org.sample.producerconsumer;

public class BridgeMessageProducer implements Runnable{
	private BridgeMessageBroker bridgeMessageBroker;
	 
    public BridgeMessageProducer(BridgeMessageBroker bridgeMessageBroker)
    {
        this.bridgeMessageBroker = bridgeMessageBroker;
    }
 
 
    @Override
    public void run()
    {
        try
        {
            for (Integer i = 1; i < 50 + 1; ++i)
            {
                System.out.println("Producer produced: " + i);
                Thread.sleep(100);
                bridgeMessageBroker.putRequest(i);
            }
 
            this.bridgeMessageBroker.continueProducing = Boolean.FALSE;
            System.out.println("Producer finished it's job... terminating");
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }
 
    }

}
