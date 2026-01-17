package marketplace.simulations;

import encryption.HEPS;
import marketplace.agents.BrokerAgent;
import marketplace.agents.ClientAgent;
import marketplace.agents.ProviderAgent;
import marketplace.model.FunctionProfile;
import marketplace.network.InMemoryNetwork;

import java.util.Map;

public class ComprehensiveHierarchySimulation {
    
    public static void main(String[] args) {
        System.out.println("=== Comprehensive Hierarchy & Scope Simulation ===\n");
        
        HEPS.getInstance();
        InMemoryNetwork net = new InMemoryNetwork();

        // --- 1. Infrastructure Setup (3-Tier) ---
        // Tier 1: Cloud (Root)
        BrokerAgent rootBroker = new BrokerAgent("Cloud-Broker", net);
        
        // Tier 2: Region (Parent = Cloud)
        BrokerAgent regionBroker = new BrokerAgent("Region-Broker", net);
        regionBroker.setParent("Cloud-Broker");
        
        // Tier 3: Edge (Parent = Region)
        BrokerAgent edgeBroker = new BrokerAgent("Edge-Broker", net);
        edgeBroker.setParent("Region-Broker");


        // --- 2. Actor Setup ---
        // Providers (All at the Edge)
        ProviderAgent providerGlobal   = new ProviderAgent("Provider-Global", net, "Edge-Broker");
        ProviderAgent providerRegional = new ProviderAgent("Provider-Regional", net, "Edge-Broker");
        ProviderAgent providerLocal    = new ProviderAgent("Provider-Local", net, "Edge-Broker");

        // Clients (At different levels)
        ClientAgent clientAtCloud  = new ClientAgent("Client-Cloud", net, "Cloud-Broker");
        ClientAgent clientAtRegion = new ClientAgent("Client-Region", net, "Region-Broker");
        ClientAgent clientAtEdge   = new ClientAgent("Client-Edge", net, "Edge-Broker");


        // =========================================================================
        // SCENARIO A: GLOBAL Propagation (The Happy Path)
        // =========================================================================
        System.out.println("\n--- [Scenario A] Testing GLOBAL Scope ---");
        // Expectation: Edge -> Region -> Cloud. Visible to Everyone.
        providerGlobal.attemptOnboard(new FunctionProfile(
            "service-global", "img:v1", Map.of("scope", "GLOBAL", "cost", 10)
        ));
        sleep(500);

        System.out.println("   [Check] Client at Cloud requesting 'service-global'...");
        // Should SUCCESS
        clientAtCloud.requestService("service-global", Map.of());
        sleep(500);


        // =========================================================================
        // SCENARIO B: REGIONAL Propagation (The Containment Test)
        // =========================================================================
        System.out.println("\n--- [Scenario B] Testing REGIONAL Scope ---");
        // Expectation: Edge -> Region -> STOP.
        // Visible to Region Client. INVISIBLE to Cloud Client.
        providerRegional.attemptOnboard(new FunctionProfile(
            "service-regional", "img:v1", Map.of("scope", "REGIONAL", "cost", 10)
        ));
        sleep(500);

        System.out.println("   [Check 1] Client at CLOUD requesting 'service-regional'...");
        // Should FAIL (Cloud Broker shouldn't have it)
        clientAtCloud.requestService("service-regional", Map.of()); 
        sleep(500);

        System.out.println("   [Check 2] Client at REGION requesting 'service-regional'...");
        // Should SUCCESS (Region Broker has it)
        clientAtRegion.requestService("service-regional", Map.of());
        sleep(500);


        // =========================================================================
        // SCENARIO C: LOCAL Propagation (The Privacy Test)
        // =========================================================================
        System.out.println("\n--- [Scenario C] Testing LOCAL Scope ---");
        // Expectation: Edge -> STOP.
        // Only visible to Edge Client.
        providerLocal.attemptOnboard(new FunctionProfile(
            "service-local", "img:v1", Map.of("scope", "LOCAL", "cost", 10)
        ));
        sleep(500);

        System.out.println("   [Check 1] Client at REGION requesting 'service-local'...");
        // Should FAIL
        clientAtRegion.requestService("service-local", Map.of());
        sleep(500);

        System.out.println("   [Check 2] Client at EDGE requesting 'service-local'...");
        // Should SUCCESS
        clientAtEdge.requestService("service-local", Map.of());
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}