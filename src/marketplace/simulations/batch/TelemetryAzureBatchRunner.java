package marketplace.simulations.batch;

import java.util.function.Supplier;
import marketplace.simulations.AbstractMarketplaceContinuumSimulation;
import marketplace.simulations.telemetry.TelemetryAzureDataGenerator;

public class TelemetryAzureBatchRunner extends AbstractMarketplaceBatchRunner {

    @Override
    protected Supplier<AbstractMarketplaceContinuumSimulation> getSimulationEngineFactory() {
        // Inject the specialized telemetry wrapper as the simulation engine
        return () -> new TelemetryAzureDataGenerator();
    }

    public static void main(String[] args) {
        TelemetryAzureBatchRunner runner = new TelemetryAzureBatchRunner();
        
        // Execute the flattened 3D grid sweep
        runner.execute(ExperimentType.FEDERATED_LEARNING_3D_SWEEP);
    }
}