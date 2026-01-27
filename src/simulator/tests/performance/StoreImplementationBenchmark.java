package simulator.tests.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.logging.Logger;
import simulator.RegionPerformanceSimulationsMain;
import simulator.config.SimConfiguration;
import utils.CustomLogger;

public class StoreImplementationBenchmark {

    private static final Logger logger = CustomLogger.getLogger(StoreImplementationBenchmark.class.getName());

    public static void main(String[] args) {
        
        String fixedSeed = "123456789"; 
        String totalSubscribers = "50000"; 
        
        logger.info("################################################################");
        logger.info("#  STARTING A/B BENCHMARK: LIST vs TREE STORE IMPLEMENTATION   #");
        logger.info("################################################################");

        // ----------------------------------------------------------------
        // RUN 1: LEGACY (LIST)
        // ----------------------------------------------------------------
        logger.info(">>> STARTING RUN 1: LEGACY [LIST] IMPLEMENTATION <<<");
        
        Properties listProps = new Properties();
        listProps.setProperty("simulation.seed", fixedSeed);
        listProps.setProperty("broker.strategy.implementation", "LIST");
        listProps.setProperty("broker.smart.threshold", "0.0"); 
        listProps.setProperty("simulation.subscribers.total", totalSubscribers);
        
        SimConfiguration.resetAndOverride(listProps);
        
        Instant start1 = Instant.now();
        try {
            RegionPerformanceSimulationsMain.main(new String[]{});
        } catch (Exception e) {
            logger.severe("CRITICAL: LIST Run Failed: " + e.getMessage());
        }
        Instant end1 = Instant.now();
        Duration duration1 = Duration.between(start1, end1);

        // Flush buffer
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // ----------------------------------------------------------------
        // RUN 2: OPTIMIZED (TREE)
        // ----------------------------------------------------------------
        logger.info(">>> STARTING RUN 2: OPTIMIZED [TREE] IMPLEMENTATION <<<");

        Properties treeProps = new Properties();
        treeProps.setProperty("simulation.seed", fixedSeed);
        treeProps.setProperty("broker.strategy.implementation", "TREE");
        treeProps.setProperty("broker.smart.threshold", "0.0");
        treeProps.setProperty("simulation.subscribers.total", totalSubscribers);

        SimConfiguration.resetAndOverride(treeProps);

        Instant start2 = Instant.now();
        try {
            RegionPerformanceSimulationsMain.main(new String[]{});
        } catch (Exception e) {
            logger.severe("CRITICAL: TREE Run Failed: " + e.getMessage());
        }
        Instant end2 = Instant.now();
        Duration duration2 = Duration.between(start2, end2);

        // ----------------------------------------------------------------
        // ACCURATE SUMMARY
        // ----------------------------------------------------------------
        logger.info("");
        logger.info("################################################################");
        logger.info("#                      BENCHMARK COMPLETE                      #");
        logger.info("################################################################");
        logger.info(String.format("RUN 1 [LIST] Duration: %d ms", duration1.toMillis()));
        logger.info(String.format("RUN 2 [TREE] Duration: %d ms", duration2.toMillis()));
    }
}