package marketplace.simulations;

public class MarketplaceBatchRunner {
    public static void main(String[] args) {
        MarketplaceBatchOrchestrator orchestrator = new MarketplaceBatchOrchestrator();
        MarketplaceScenario scenario = new MarketplaceScenario();
        
        // Run the comprehensive parameter sweep
        orchestrator.runMarketplaceBatch(scenario, "marketplace_continuum_results.csv");
    }
}