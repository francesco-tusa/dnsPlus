package marketplace.simulations.batch;

import java.util.function.Supplier;
import marketplace.simulations.MarketplaceContinuumSimulation;

public abstract class AbstractMarketplaceBatchRunner {

    protected abstract Supplier<MarketplaceContinuumSimulation> getSimulationEngineFactory();

    public void execute(ExperimentType activeExperiment) {
        // 1. Fetch the specific simulation engine from the subclass (e.g., AzureMarketplaceSimulation)
        Supplier<MarketplaceContinuumSimulation> simFactory = getSimulationEngineFactory();

        // 2. Polymorphic Resolution: The enum handles the mapping natively
        AbstractMarketplaceScenario activeScenario = activeExperiment.createScenario(simFactory);
        String baseCsvFilename = activeExperiment.getBaseCsvFilename();

        // 3. Setup File I/O and Orchestration
        MarketplaceBatchOrchestrator orchestrator = new MarketplaceBatchOrchestrator();
        String batchId = String.valueOf(System.currentTimeMillis());
        String finalCsvFilename = baseCsvFilename + batchId + ".csv";

        System.out.println("=========================================================");
        System.out.println(">>> Starting Decentralized FaaS Routing Experiment <<<");
        System.out.println("Execution Engine : " + this.getClass().getSimpleName());
        System.out.println("Active Scenario  : " + activeScenario.getClass().getSimpleName());
        System.out.println("Batch ID         : " + batchId);
        System.out.println("Output File      : " + finalCsvFilename);
        System.out.println("=========================================================");

        // 4. Run the batch
        orchestrator.runMarketplaceBatch(activeScenario, finalCsvFilename, batchId);
    }
}