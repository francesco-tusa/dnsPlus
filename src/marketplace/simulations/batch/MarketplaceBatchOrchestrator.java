package marketplace.simulations.batch;

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
    
    // Baseline continuum topology. Used as the default UNLESS the scenario knob overrides it.
    private final ProviderScale[] providerScales = {
        new ProviderScale(25, 250, 2500)   
    };
    
    // Sweeping across algorithms
    private final String[] routingStrategies = {
        //"BASELINE",
        "WEIGHTED_UTILITY"
    };
    
    private final int STATISTICAL_RUNS = 1; // Kept lower for HE feasibility batches

    public void runMarketplaceBatch(SimulationScenario scenario, String csvFilename, String batchId) {
        
        long configuredSeed = SimConfiguration.get().simulationSeed;
        String batchSeed = (configuredSeed == -1) ? "123456789" : String.valueOf(configuredSeed);

        String batchDirPath = "output" + File.separator + "batch_marketplace_" + batchId;
        File batchDir = new File(batchDirPath);
        
        if (!batchDir.exists()) batchDir.mkdirs();

        CustomLogger.setBaseOutputDirectory(batchDirPath);
        File csvFile = new File(batchDir, csvFilename);

        boolean isTracingEnabled = Boolean.parseBoolean(System.getProperty("paths.enableEventTracing", "false"));

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println(scenario.getCsvHeader());
            int globalRunId = 1;

            // Strategy Loop
            for (String strategy : routingStrategies) {
                // Topology Loop
                for (ProviderScale scale : providerScales) {
                    // Scenario Knob Loop
                    for (String knobVal : scenario.getKnobValues()) {
                        for (int clients : clientPopulations) {
                            for (int run = 1; run <= STATISTICAL_RUNS; run++) {
                                
                                Properties props = new Properties();
                                
                                // 1. Inject Baseline Strategy & Topology
                                props.setProperty("marketplace.routing.strategy", strategy);
                                props.setProperty("marketplace.providers.cloud.count", String.valueOf(scale.cloud));
                                props.setProperty("marketplace.providers.fog.count", String.valueOf(scale.fog));
                                props.setProperty("marketplace.providers.edge.count", String.valueOf(scale.edge));
                                props.setProperty("workload.replicas", String.valueOf(clients)); 
                                
                                // 2. Inject Knob (This cleanly overwrites baseline if the knob targets the topology)
                                props.setProperty(scenario.getKnobKey(), knobVal); 
                                
                                props.setProperty("marketplace.repetition.id", String.valueOf(run));
                                props.setProperty("paths.enableEventTracing", String.valueOf(isTracingEnabled));
                                
                                long runSeed = Long.parseLong(batchSeed) + run;
                                props.setProperty("simulation.seed", String.valueOf(runSeed));
                                
                                // 3. Execute Scenario Configuration (e.g., calculates proportional tiers)
                                scenario.configure(props);

                                // 4. Read back the precise state to calculate total providers safely
                                int finalCloud = Integer.parseInt(props.getProperty("marketplace.providers.cloud.count", "0"));
                                int finalFog = Integer.parseInt(props.getProperty("marketplace.providers.fog.count", "0"));
                                int finalEdge = Integer.parseInt(props.getProperty("marketplace.providers.edge.count", "0"));
                                
                                int totalProviders = finalCloud + finalFog + finalEdge;
                                props.setProperty("workload.subscribers.count", String.valueOf(totalProviders));

                                logger.info(String.format("[%s] Run %d: Strategy=%s | %s=%s | Topo=[C:%d,F:%d,E:%d]", 
                                              getClass().getSimpleName(), globalRunId++, strategy, scenario.getKnobKey(), knobVal, finalCloud, finalFog, finalEdge));
                                
                                // 5. Bootstrap Simulation Substrate
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
                                System.gc(); // Force memory clearance between heavy tree computations
                            }
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