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

    private int numberOfPublications;
    private int numberOfSubscriptions;
    
    private BrokerStatsCollector brokerStatsCollector;
    
    
    public DNSWithCacheAsynchronousSequentialParallelRun(int nPublications, int nSubscriptions) {
        super(DNSWithCacheAsynchronousSequentialParallelRun.class.getSimpleName());
        broker = new AsynchronousBrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
        numberOfPublications = nPublications;
        numberOfSubscriptions = nSubscriptions;
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
    
    @Override
    public int getNumberOfPublications() {
        return numberOfPublications;
    }

    @Override
    public int getNumberOfSubscriptions() {
        return numberOfSubscriptions;
    }
    
    
}