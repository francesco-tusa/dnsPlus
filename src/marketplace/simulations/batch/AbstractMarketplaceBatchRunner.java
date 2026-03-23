package marketplace.simulations.batch;

import java.util.function.Supplier;

import marketplace.simulations.MarketplaceContinuumSimulation;

public abstract class AbstractMarketplaceBatchRunner {

    public enum ExperimentType {
        EDGE_WORKLOAD_SWEEP,
        AGGREGATION_SWEEP,
        BUDGET_SENSITIVITY_SWEEP
    }

    /**
     * Subclasses MUST implement this to inject their specific workload generation 
     * and capability components (e.g., Azure FaaS vs. Legacy Synthetic).
     */
    protected abstract Supplier<MarketplaceContinuumSimulation> getSimulationEngineFactory();

    /**
     * Core execution pipeline. Resolves the scenario, sets up deterministic file naming,
     * and hands off to the orchestrator.
     */
    public void execute(ExperimentType activeExperiment) {
        // 1. Fetch the specific simulation engine from the subclass
        Supplier<MarketplaceContinuumSimulation> simFactory = getSimulationEngineFactory();

        AbstractMarketplaceScenario activeScenario;
        String baseCsvFilename;

        // 2. Resolve the experiment scenario
        if (activeExperiment == ExperimentType.EDGE_WORKLOAD_SWEEP) {
            activeScenario = new EdgeProbabilityScenario(simFactory);
            baseCsvFilename = "edge_workload_results_";
        } else if (activeExperiment == ExperimentType.AGGREGATION_SWEEP) {
            activeScenario = new AggregationThresholdScenario(simFactory);
            baseCsvFilename = "aggregation_threshold_results_";
        } else {
            activeScenario = new BudgetProbabilityScenario(simFactory);
            baseCsvFilename = "budget_sensitivity_results_";
        }

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