package experiments.cache.asynchronous;

import broker.AsynchronousMeasurementProducerCachingBroker;
import encryption.HEPS;
import broker.tree.binarybalanced.cache.asynchronous.AsynchronousBrokerWithBinaryBalancedTreeAndCache;
import java.util.logging.Logger;
import utils.CustomLogger;
import experiments.outputdata.BrokerStatsCollector;
import experiments.measurement.MeasurementProducerBroker;
import experiments.PubSubRunTasksExecutor;

/**
 *
 * @author f.tusa
 */
public class DNSWithCacheAsynchronousRun extends AsynchronousRunParallelTasksExecutor 
                                         implements PubSubRunTasksExecutor {

    private static final Logger logger = CustomLogger.getLogger(DNSWithCacheAsynchronousRun.class.getName());
    private AsynchronousMeasurementProducerCachingBroker broker;
    
    private BrokerStatsCollector brokerStatsCollector;
    

    public DNSWithCacheAsynchronousRun() {
        super(DNSWithCacheAsynchronousRun.class.getSimpleName());
        broker = new AsynchronousBrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
        brokerStatsCollector = new BrokerStatsCollector(this);
    }
    

    @Override
    public void setUp() {
        broker.startProcessing();
    }

    @Override
    public void finalise() {
        broker.stopProcessing();
    }
    
    
    @Override
    public MeasurementProducerBroker getBroker() {
        return broker;
    }
}