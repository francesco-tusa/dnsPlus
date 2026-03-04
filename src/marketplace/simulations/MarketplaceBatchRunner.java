package marketplace.simulations;

public class MarketplaceBatchRunner {
    public static void main(String[] args) {
        MarketplaceBatchOrchestrator orchestrator = new MarketplaceBatchOrchestrator();
        MarketplaceScenario scenario = new MarketplaceScenario();
        
        // 1. Generate the batch ID at the entry point
        String batchId = String.valueOf(System.currentTimeMillis());
        
        // 2. Inject the batch ID into the CSV filename
        String csvFilename = "marketplace_continuum_results_" + batchId + ".csv";
        
        System.out.println("Starting Marketplace Batch: " + batchId);
        System.out.println("Output File: " + csvFilename);
        
        // 3. Pass BOTH the filename and the batchId to the orchestrator
        orchestrator.runMarketplaceBatch(scenario, csvFilename, batchId);
    }
}