package marketplace.simulations;

import simulator.experimentation.SimulationScenario;

public class MarketplaceBatchRunner {

    public enum ExperimentType { EDGE_WORKLOAD_SWEEP, AGGREGATION_SWEEP, BUDGET_SENSITIVITY_SWEEP }

    public static void main(String[] args) {
        //ExperimentType activeExperiment = ExperimentType.BUDGET_SENSITIVITY_SWEEP;
        //ExperimentType activeExperiment = ExperimentType.EDGE_WORKLOAD_SWEEP;
        ExperimentType activeExperiment = ExperimentType.AGGREGATION_SWEEP;
        
        
        SimulationScenario activeScenario;
        String csvFilename;

        if (activeExperiment == ExperimentType.EDGE_WORKLOAD_SWEEP) {
            activeScenario = new EdgeProbabilityScenario();
            csvFilename = "edge_workload_results_";
        } else if (activeExperiment == ExperimentType.AGGREGATION_SWEEP) {
            activeScenario = new AggregationThresholdScenario();
            csvFilename = "aggregation_threshold_results_";
        } else {
            activeScenario = new BudgetProbabilityScenario();
            csvFilename = "budget_sensitivity_results_";
        }

        MarketplaceBatchOrchestrator orchestrator = new MarketplaceBatchOrchestrator();
        
        // 1. Generate the batch ID at the entry point
        String batchId = String.valueOf(System.currentTimeMillis());
        
        // 2. Inject the batch ID into the CSV filename
        csvFilename +=  batchId + ".csv";
        
        System.out.println(">>> Starting Decentralized FaaS Routing Experiment <<<");
        System.out.println("Active Scenario: " + activeScenario.getClass().getSimpleName());
        System.out.println("Batch ID: " + batchId);
        System.out.println("Output File: " + csvFilename);
        
        // 3. Pass BOTH the filename and the batchId to the orchestrator
        orchestrator.runMarketplaceBatch(activeScenario, csvFilename, batchId);
    }
}