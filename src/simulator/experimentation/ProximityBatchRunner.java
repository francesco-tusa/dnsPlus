package simulator.experimentation;

import java.util.logging.Logger;
import utils.CustomLogger;

public class ProximityBatchRunner extends AbstractBatchOrchestrator {

    private static final Logger logger = CustomLogger.getLogger(ProximityBatchRunner.class.getName());

    public static void main(String[] args) {
        logger.info("=== Starting Proximity (Closest) Batch ===");
        
        new ProximityBatchRunner().runBatch(
            new ProximityScenario(), 
            "results_closest.csv"
        );
    }
}