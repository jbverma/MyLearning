package org.sample.producerconsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SampleProducerConsumer {
	public static void main(String[] args) {
		try {
			BridgeMessageBroker bridgeMessageBroker = new BridgeMessageBroker();

			ExecutorService executorThreadPool = Executors.newFixedThreadPool(3);

			executorThreadPool.execute(new BridgeMessageConsumer("SyncMessageConsumer-1", bridgeMessageBroker));
			executorThreadPool.execute(new BridgeMessageConsumer("SyncMessageConsumer-2", bridgeMessageBroker));
			Future producerStatus = executorThreadPool.submit(new BridgeMessageProducer(bridgeMessageBroker));

			//This will wait for the producer to finish its execution.
			producerStatus.get();

			// Get response from response queue
			for (Integer i = 1; i < 50 + 1; ++i)
			{
				Thread.sleep(100);
				Integer data = bridgeMessageBroker.getResponse();

				System.out.println("Response received for request: " + data);
			}

			executorThreadPool.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
