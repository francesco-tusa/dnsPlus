package experiments.cache.asynchronous;

import broker.AsynchronousMeasurementProducerCachingBroker;
import encryption.HEPS;
import broker.tree.binarybalanced.cache.asynchronous.AsynchronousBrokerWithBinaryBalancedTreeAndCache;
import experiments.PubSubTaskDelegator;
import experiments.outputdata.BrokerStatsCollector;
import experiments.measurement.MeasurementProducerBroker;

/**
 *
 * @author f.tusa
 */
public final class DNSWithCacheAsynchronousSequentialParallelRun extends AsynchronousRunSequentialParallelTasksExecutor implements PubSubTaskDelegator {

    private AsynchronousMeasurementProducerCachingBroker broker;
    
    private BrokerStatsCollector brokerStatsCollector;
    
    
    public DNSWithCacheAsynchronousSequentialParallelRun() {
        super(DNSWithCacheAsynchronousSequentialParallelRun.class.getSimpleName());
        broker = new AsynchronousBrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
        brokerStatsCollector = new BrokerStatsCollector(this);
    }
    

    @Override
    public void setUp() {
        broker.startProcessing();
    }

    @Override
    public void finalise() {
        super.finalise();
        broker.stopProcessing();
    }


    @Override
    public MeasurementProducerBroker getBroker() {
        return broker;
    }

    @Override
    public void taskRequestCompleted() {
        super.getRequestsLatch().countDown();
    }

    @Override
    public void taskResponseReceived() {
        super.getRepliesLatch().countDown();
    } 
}