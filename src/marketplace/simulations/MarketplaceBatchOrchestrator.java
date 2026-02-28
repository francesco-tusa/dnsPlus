package marketplace.simulations;

import simulator.experimentation.AbstractBatchOrchestrator;
import simulator.experimentation.SimulationScenario;
import simulator.config.SimConfiguration;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import utils.CustomLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Logger;

public class MarketplaceBatchOrchestrator extends AbstractBatchOrchestrator {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceBatchOrchestrator.class.getName());

    // --- SWEEP PARAMETERS ---
    private final int[] clientPopulations = {2000}; 
    
    // Encapsulating the providers to sweep them simultaneously
    private final ProviderScale[] providerScales = {
        new ProviderScale(10, 50, 250) // Baseline: Cloud, Fog, Edge
    };
    
    // Statistical validity
    private final int STATISTICAL_RUNS = 1;

    /**
     * Executes the batch, deferring the primary parameter sweep to the SimulationScenario.
     */
    public void runMarketplaceBatch(SimulationScenario scenario, String csvFilename) {
        
        long configuredSeed = SimConfiguration.get().simulationSeed;
        String batchSeed = (configuredSeed == -1) ? "123456789" : String.valueOf(configuredSeed);

        String batchId = String.valueOf(System.currentTimeMillis());
        String batchDirPath = "output" + File.separator + "batch_marketplace_" + batchId;
        File batchDir = new File(batchDirPath);
        
        if (!batchDir.exists()) {
            batchDir.mkdirs();
        }

        CustomLogger.setBaseOutputDirectory(batchDirPath);
        File csvFile = new File(batchDir, csvFilename);

        logger.info("===============================================================");
        logger.info(" STARTING MARKETPLACE BATCH: " + batchId);
        logger.info(" Logic: " + scenario.getClass().getSimpleName());
        logger.info(" Sweeping Knob: " + scenario.getKnobKey());
        logger.info("===============================================================");

        boolean isTracingEnabled = Boolean.parseBoolean(System.getProperty("paths.enableEventTracing", "false"));

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Header is dynamically constructed using the scenario's knob key
            writer.println("Timestamp,Threshold,CloudNodes,FogNodes,EdgeNodes,Clients,RepetitionID," + scenario.getCsvHeader());
            
            long startTime = System.currentTimeMillis();
            int globalRunId = 1;

            for (ProviderScale scale : providerScales) {
                // Defer to the Scenario's defined values (e.g., FPR Thresholds)
                for (String knobVal : scenario.getKnobValues()) {
                    for (int clients : clientPopulations) {
                        for (int run = 1; run <= STATISTICAL_RUNS; run++) {
                            
                            Properties props = new Properties();
                            props.setProperty(scenario.getKnobKey(), knobVal); // Injected from Scenario
                            props.setProperty("marketplace.providers.cloud.count", String.valueOf(scale.cloud));
                            props.setProperty("marketplace.providers.fog.count", String.valueOf(scale.fog));
                            props.setProperty("marketplace.providers.edge.count", String.valueOf(scale.edge));
                            props.setProperty("workload.replicas", String.valueOf(clients)); 
                            props.setProperty("paths.enableEventTracing", String.valueOf(isTracingEnabled));
                            
                            long runSeed = Long.parseLong(batchSeed) + run;
                            props.setProperty("simulation.seed", String.valueOf(runSeed));
                            
                            scenario.configure(props);

                            logger.info(String.format("[%s] Run %d: %s=%s | Topo=[C:%d,F:%d,E:%d] | Clients=%d | Rep=%d", 
                                          getClass().getSimpleName(), globalRunId++, scenario.getKnobKey(), knobVal, scale.cloud, scale.fog, scale.edge, clients, run));
                            
                            SimConfiguration.resetAndOverride(props);
                            
                            try {
                                PerformanceMetricsData data = scenario.runSimulation();
                                
                                if (data != null) {
                                    String scenarioMetrics = scenario.getCsvRow(data, props);
                                    String prefix = String.format("%s,%d,%d,%d,%d,%d,", 
                                            knobVal, scale.cloud, scale.fog, scale.edge, clients, run);
                                    writer.println(prefix + scenarioMetrics);
                                    writer.flush(); 
                                } else {
                                    logger.severe("Error: No metrics returned for run.");
                                }
                            } catch (Exception e) {
                                logger.severe("Simulation Failed: " + e.getMessage());
                                e.printStackTrace();
                            }

                            System.gc();
                        }
                    }
                }
            }
            
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            logger.info("Batch Finished in " + duration + "s.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ProviderScale {
        final int cloud, fog, edge;
        public ProviderScale(int cloud, int fog, int edge) {
            this.cloud = cloud;
            this.fog = fog;
            this.edge = edge;
        }
    }
}