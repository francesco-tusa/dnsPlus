package experiments.outputdata;

import experiments.measurement.AsynchronousBrokerMeasurementListener;
import utils.CustomLogger;
import java.util.logging.Logger;
import java.util.logging.Level;
import experiments.PubSubRunTasksExecutor;

/**
 *
 * @author uceeftu
 */
public final class BrokerStatsCollector implements AsynchronousBrokerMeasurementListener {

        private static final Logger logger = CustomLogger.getLogger(BrokerStatsCollector.class.getName());
        private PubSubRunTasksExecutor taskRunner;
        private BrokerStats brokerStats;

        public BrokerStatsCollector(PubSubRunTasksExecutor taskRunner) {
            this.taskRunner = taskRunner;
            registerWithMeasurementProducer();
        }
        
        
        
        @Override
        public void asynchronousMeasurementPerformed(BrokerStats brokerStats) {
            this.brokerStats = brokerStats;
            logger.log(Level.SEVERE, brokerStats.toString());
        }

        @Override
        public void registerWithMeasurementProducer() {
            taskRunner.getBroker().addMeasurementListener(this);
        }    
    }
