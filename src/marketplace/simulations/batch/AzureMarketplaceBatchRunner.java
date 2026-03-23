package marketplace.simulations.batch;

import java.util.function.Supplier;

import marketplace.simulations.AzureMarketplaceSimulation;
import marketplace.simulations.MarketplaceContinuumSimulation;

public class AzureMarketplaceBatchRunner extends AbstractMarketplaceBatchRunner {

    @Override
    protected Supplier<MarketplaceContinuumSimulation> getSimulationEngineFactory() {
        // Inject the Azure-specific workload traces
        return () -> new AzureMarketplaceSimulation();
    }

    public static void main(String[] args) {
        AzureMarketplaceBatchRunner runner = new AzureMarketplaceBatchRunner();
        
        // Define which experiment you want to run for the Azure traces
        runner.execute(ExperimentType.EDGE_WORKLOAD_SWEEP);
    }
}