package simulator.experimentation;

import simulator.config.SimConfiguration;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import utils.CustomLogger; 

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Logger; 

public abstract class AbstractBatchOrchestrator {

    private static final Logger logger = CustomLogger.getLogger(AbstractBatchOrchestrator.class.getName());

    // Global Scale Parameters
    //protected static final int[] PUBLISHERS = {1, 10, 1000, 100000, 1000000};
    protected static final int[] PUBLISHERS = {1000};

    //protected static final int[] SUBSCRIBERS = {10, 1000, 10000, 1000000};
    protected static final int[] SUBSCRIBERS = {10000};

    protected void runBatch(SimulationScenario scenario, String csvFilename) {
        
        long configuredSeed = SimConfiguration.get().simulationSeed;
        String batchSeed;
        
        if (configuredSeed == -1) {
            logger.warning("Configured seed is -1 (Random). Forcing fixed seed for batch consistency.");
            batchSeed = "123456789"; 
        } else {
            batchSeed = String.valueOf(configuredSeed);
        }

        // 1. Setup Batch Directory
        String batchId = String.valueOf(System.currentTimeMillis());
        String batchDirPath = "output" + File.separator + "batch_" + batchId;
        File batchDir = new File(batchDirPath);
        
        if (!batchDir.exists()) {
            batchDir.mkdirs();
        }

        // 2. Redirect Logs to this folder
        CustomLogger.setBaseOutputDirectory(batchDirPath);
        
        // 3. Place CSV inside the batch folder
        File csvFile = new File(batchDir, csvFilename);

        logger.info("===============================================================");
        logger.info(" STARTING BATCH EXPERIMENT: " + batchId);
        logger.info(" Logic: " + scenario.getClass().getSimpleName());
        logger.info(" Results: " + csvFile.getAbsolutePath());
        logger.info(" Logs: " + batchDirPath + File.separator + "<Run_Timestamp>");
        logger.info(" Using Batch Seed: " + batchSeed);
        logger.info("===============================================================");

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println(scenario.getCsvHeader());

            long startTime = System.currentTimeMillis();
            int runId = 1;

            for (int pubs : PUBLISHERS) {
                for (int subs : SUBSCRIBERS) {
                    for (String knobVal : scenario.getKnobValues()) {
                        
                        Properties props = new Properties();
                        props.setProperty("workload.replicas", String.valueOf(pubs));
                        props.setProperty("workload.subscribers.count", String.valueOf(subs));
                        props.setProperty(scenario.getKnobKey(), knobVal);
                        props.setProperty("simulation.seed", batchSeed);
                        
                        scenario.configure(props);

                        logger.info(String.format("[%s] Run %d: Pubs=%d, Subs=%d, %s=%s", 
                                          getClass().getSimpleName(), runId++, pubs, subs, scenario.getKnobKey(), knobVal));
                        
                        // Force Eager Initialization (sets Seed and new Config)
                        SimConfiguration.resetAndOverride(props);
                        
                        try {
                            PerformanceMetricsData data = scenario.runSimulation();
                            
                            if (data != null) {
                                writer.println(scenario.getCsvRow(data, props));
                                writer.flush(); 
                            } else {
                                logger.severe("Error: No metrics returned for run " + (runId - 1));
                            }
                        } catch (Exception e) {
                            logger.severe("Simulation Failed: " + e.getMessage());
                            e.printStackTrace();
                        }

                        // Aggressive cleanup between heavy runs
                        System.gc();
                    }
                }
            }
            
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            logger.info("Batch Finished in " + duration + "s.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}