package marketplace.simulations;

import encryption.HEPS;
import marketplace.agents.BrokerAgent;
import marketplace.agents.ClientAgent;
import marketplace.agents.ProviderAgent;
import marketplace.model.FunctionProfile;
import marketplace.network.InMemoryNetwork;

import java.util.Map;

public class ParetoSelectionSimulation {
    public static void main(String[] args) {
        System.out.println("=== Privacy-Preserving FaaS Marketplace: Pareto Front Test ===\n");
        
        HEPS.getInstance();
        InMemoryNetwork net = new InMemoryNetwork();

        BrokerAgent broker = new BrokerAgent("Broker-Node", net);
        ClientAgent client = new ClientAgent("Client-X", net, "Broker-Node");

        // --- Step 1: Define Providers with Conflicting Metrics ---

        // 1. The "Budget" Option (Cheap but Slow)
        ProviderAgent pBudget = new ProviderAgent("Provider-Budget", net, "Broker-Node");
        FunctionProfile profileBudget = new FunctionProfile(
            "img-resize", "docker/resize:v1", 
            Map.of("cost", 10.0, "latency", 100.0, "reliability", 0.99)
        );

        // 2. The "Performance" Option (Expensive but Fast)
        ProviderAgent pFast = new ProviderAgent("Provider-Speed", net, "Broker-Node");
        FunctionProfile profileFast = new FunctionProfile(
            "img-resize", "docker/resize:v1", 
            Map.of("cost", 50.0, "latency", 20.0, "reliability", 0.999)
        );

        // 3. The "Dominated" Option (Expensive AND Slow) -> Should NEVER be picked
        ProviderAgent pBad = new ProviderAgent("Provider-Bad", net, "Broker-Node");
        FunctionProfile profileBad = new FunctionProfile(
            "img-resize", "docker/resize:v1", 
            Map.of("cost", 60.0, "latency", 110.0, "reliability", 0.90) 
            // Worse cost than Budget, Worse latency than Speed. Strictly dominated.
        );

        // --- Step 2: Onboard Everyone ---
        System.out.println("\n--- Onboarding Providers ---");
        pBudget.attemptOnboard(profileBudget);
        pFast.attemptOnboard(profileFast);
        pBad.attemptOnboard(profileBad);

        sleep(500);

        // --- Step 3: Client Request (Loose Constraints) ---
        System.out.println("\n--- Client Request ---");
        // We set high limits so EVERYONE passes the hard constraint check.
        // This forces the Broker to rely on Pareto optimization to pick the winner.
        Map<String, Object> requirements = Map.of(
            "max_cost", 200.0,  
            "max_latency", 500.0
        );
        
        System.out.println("Requesting 'img-resize' with loose constraints (Cost < 200, Latency < 500)...");
        client.requestService("img-resize", requirements);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}