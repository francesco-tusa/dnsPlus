package simulator.experimentation;

import java.util.logging.Logger;
import utils.CustomLogger;

public class RegionalBatchRunner extends AbstractBatchOrchestrator {

    private static final Logger logger = CustomLogger.getLogger(RegionalBatchRunner.class.getName());

    public static void main(String[] args) {
        logger.info("=== Starting Regional (Spatial Match) Batch ===");
        
        new RegionalBatchRunner().runBatch(
            new RegionalScenario(), 
            "results_regional.csv"
        );
    }
}