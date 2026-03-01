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

import marketplace.config.MarketplaceConfig;

public class MarketplaceBatchOrchestrator extends AbstractBatchOrchestrator {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceBatchOrchestrator.class.getName());

    private final int[] clientPopulations = {1000000}; 
    private final ProviderScale[] providerScales = {
        new ProviderScale(1000, 5000, 25000) // Baseline: Cloud, Fog, Edge
    };
    private final int STATISTICAL_RUNS = 1;

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

        boolean isTracingEnabled = Boolean.parseBoolean(System.getProperty("paths.enableEventTracing", "false"));

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            
            // FIX 1: Delegate header generation strictly to the Scenario. No manual prefixes.
            writer.println(scenario.getCsvHeader());
            
            int globalRunId = 1;

            for (ProviderScale scale : providerScales) {
                for (String knobVal : scenario.getKnobValues()) {
                    for (int clients : clientPopulations) {
                        for (int run = 1; run <= STATISTICAL_RUNS; run++) {
                            
                            Properties props = new Properties();
                            props.setProperty(scenario.getKnobKey(), knobVal); 
                            props.setProperty("marketplace.providers.cloud.count", String.valueOf(scale.cloud));
                            props.setProperty("marketplace.providers.fog.count", String.valueOf(scale.fog));
                            props.setProperty("marketplace.providers.edge.count", String.valueOf(scale.edge));
                            
                            props.setProperty("workload.replicas", String.valueOf(clients)); 
                            
                            int totalProviders = scale.cloud + scale.fog + scale.edge;
                            props.setProperty("workload.subscribers.count", String.valueOf(totalProviders));
                            
                            props.setProperty("marketplace.repetition.id", String.valueOf(run));
                            
                            props.setProperty("paths.enableEventTracing", String.valueOf(isTracingEnabled));
                            
                            long runSeed = Long.parseLong(batchSeed) + run;
                            props.setProperty("simulation.seed", String.valueOf(runSeed));
                            
                            scenario.configure(props);

                            logger.info(String.format("[%s] Run %d: %s=%s | Topo=[C:%d,F:%d,E:%d] | Clients=%d | Rep=%d", 
                                          getClass().getSimpleName(), globalRunId++, scenario.getKnobKey(), knobVal, scale.cloud, scale.fog, scale.edge, clients, run));
                            
                            // Synchronize BOTH configuration substrates
                            SimConfiguration.resetAndOverride(props);
                            MarketplaceConfig.resetAndOverride(props); 
                            
                            try {
                                PerformanceMetricsData data = scenario.runSimulation();
                                if (data != null) {
                                    writer.println(scenario.getCsvRow(data, props));
                                    writer.flush(); 
                                }
                            } catch (Exception e) {
                                logger.severe("Simulation Failed: " + e.getMessage());
                            }

                            System.gc();
                        }
                    }
                }
            }
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