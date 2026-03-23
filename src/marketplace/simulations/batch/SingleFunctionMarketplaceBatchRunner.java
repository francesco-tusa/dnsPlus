package marketplace.simulations.batch;

import java.util.function.Supplier;

import marketplace.simulations.MarketplaceContinuumSimulation;
import marketplace.simulations.SingleFunctionMarketplaceSimulation;

public class SingleFunctionMarketplaceBatchRunner extends AbstractMarketplaceBatchRunner {

    @Override
    protected Supplier<MarketplaceContinuumSimulation> getSimulationEngineFactory() {
        return () -> new SingleFunctionMarketplaceSimulation();
    }

    public static void main(String[] args) {
        SingleFunctionMarketplaceBatchRunner runner = new SingleFunctionMarketplaceBatchRunner();
        
        // Define which experiment you want to run for the Legacy synthetic models
        runner.execute(ExperimentType.EDGE_WORKLOAD_SWEEP);
    }
}