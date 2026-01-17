package marketplace.simulations;

import encryption.HEPS;
import marketplace.agents.*;
import marketplace.model.FunctionProfile;
import marketplace.network.InMemoryNetwork;
import java.util.Map;

public class HierarchySimulation {
    public static void main(String[] args) {
        System.out.println("=== Hierarchical Topology Simulation ===\n");
        
        HEPS.getInstance();
        InMemoryNetwork net = new InMemoryNetwork();

        // 1. Build Topology
        // Root Broker (The Cloud)
        BrokerAgent root = new BrokerAgent("Cloud-Broker", net); 
        
        // Core Broker (The Region) -> Parent is Cloud
        BrokerAgent core = new BrokerAgent("Region-Broker", net);
        core.setParent("Cloud-Broker");
        
        // Edge Broker (The Local Edge) -> Parent is Region
        BrokerAgent edge = new BrokerAgent("Edge-Broker", net);
        edge.setParent("Region-Broker");

        // 2. Setup Actors
        // Provider connects deep in the network at the Edge
        ProviderAgent provider = new ProviderAgent("Provider-Deep", net, "Edge-Broker");
        
        // Client connects at the top (Cloud)
        ClientAgent client = new ClientAgent("Client-Global", net, "Cloud-Broker");

        // 3. Onboard (Propagation)
        System.out.println("--- Step 1: Upstream Propagation ---");
        // We define a Global Scope service.
        FunctionProfile profile = new FunctionProfile(
            "deep-service", 
            "img:v1", 
            Map.of("scope", "GLOBAL", "cost", 10.0)
        );
        
        // Logic: Edge -> Region -> Cloud
        provider.attemptOnboard(profile);

        sleep(500);

        // 4. Request (Routing Down)
        System.out.println("\n--- Step 2: Downstream Routing ---");
        System.out.println("Client requests 'deep-service' via Cloud Broker...");
        
        // The Cloud Broker should have the service in its tree (propagated from Edge)
        // It will route the request to "Provider-Deep"
        client.requestService("deep-service", Map.of());
    }
    
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}