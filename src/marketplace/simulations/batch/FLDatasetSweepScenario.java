package marketplace.simulations.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import marketplace.simulations.AbstractMarketplaceContinuumSimulation;

public class FLDatasetSweepScenario extends AbstractMarketplaceScenario {

    private final List<String> gridCoordinates = new ArrayList<>();

    public FLDatasetSweepScenario(Supplier<AbstractMarketplaceContinuumSimulation> factory) {
        super(factory);
        
        // --- 4D GRID DEFINITION ---
        int[] topologyScales = {1000, 2500, 5000};
        int[] workloadScales = {10000, 50000, 100000};
        double[] epsilonRates = {0.05, 0.10, 0.20};
        
        double[] aggregationThresholds = {0.0, 0.25, 0.75}; 

        // Flatten the 4D grid into a 1D list of configurations for the Batch Orchestrator
        for (int topo : topologyScales) {
            for (int work : workloadScales) {
                for (double eps : epsilonRates) {
                    for (double agg : aggregationThresholds) {
                        gridCoordinates.add(topo + "_" + work + "_" + eps + "_" + agg);
                    }
                }
            }
        }
    }

    @Override
    public String[] getKnobValues() {
        return gridCoordinates.toArray(new String[0]);
    }

    @Override
    public String getKnobKey() {
        return "marketplace.fl.grid_coordinate"; 
    }

    @Override
    protected void configureSpecific(Properties props) {
        super.configureSpecific(props);

        String coordinate = props.getProperty(getKnobKey());
        if (coordinate == null || coordinate.isEmpty()) {
            return;
        }

        // Parse the 4 parameters
        String[] parts = coordinate.split("_");
        int edgeNodes = Integer.parseInt(parts[0]);
        int clients = Integer.parseInt(parts[1]);
        double epsilon = Double.parseDouble(parts[2]);
        double aggThreshold = Double.parseDouble(parts[3]);

        int fogNodes = Math.max(1, edgeNodes / 10);
        int cloudNodes = Math.max(1, fogNodes / 10);

        props.setProperty("marketplace.providers.cloud.count", String.valueOf(cloudNodes));
        props.setProperty("marketplace.providers.fog.count", String.valueOf(fogNodes));
        props.setProperty("marketplace.providers.edge.count", String.valueOf(edgeNodes));
        props.setProperty("workload.replicas", String.valueOf(clients));
        props.setProperty("workload.subscribers.count", String.valueOf(cloudNodes + fogNodes + edgeNodes));
        
        props.setProperty("marketplace.ml.exploration_rate", String.valueOf(epsilon));
        props.setProperty("marketplace.routing.strategy", "EPSILON_GREEDY");
        props.setProperty("marketplace.enableTracing", "true");
        
        props.setProperty("broker.smartThreshold", String.valueOf(aggThreshold));
        
        // Update the folder structure to safely reflect all 4 dimensions
        String runFolderName = String.format("Topo%d_Clients%d_Eps%.2f_Agg%.2f", 
                                             edgeNodes, clients, epsilon, aggThreshold);
        String runPath = "FL_Dataset" + java.io.File.separator + runFolderName;
        System.setProperty("marketplace.ml.telemetry_base_path", runPath);
    }
}