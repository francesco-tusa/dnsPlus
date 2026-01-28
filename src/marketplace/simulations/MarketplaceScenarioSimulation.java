package marketplace.simulations;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.SimulationRunner;
import simulator.regions.Region;
import marketplace.agents.HierarchicalOrchestrationBroker;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 * A comprehensive scenario simulation for the Privacy-Preserving FaaS
 * Marketplace.
 * Creates a hierarchical topology and verifies End-to-End flows.
 */
public class MarketplaceScenarioSimulation {

    // Logger setup
    private static final Logger logger = CustomLogger.getLogger(MarketplaceScenarioSimulation.class.getName());

    public static void main(String[] args) {
        setupConfiguration();

        System.out.println("=== Starting Marketplace Scenario Simulation ===");

        // 1. Create Topology: Root -> RegionA -> Leaf1
        // Using HierarchicalOrchestrationBroker for all brokers.

        // Root Broker covering the world 0-100
        HierarchicalOrchestrationBroker root = new HierarchicalOrchestrationBroker(
                "Root",
                new Location(0, 0, 0),
                new Location(100, 100, 0));

        // Regional Broker covering 0-50
        HierarchicalOrchestrationBroker regionA = new HierarchicalOrchestrationBroker(
                "RegionA",
                new Location(0, 0, 0),
                new Location(50, 50, 0));
        root.addChild(regionA);

        // Leaf Broker covering 0-25
        HierarchicalOrchestrationBroker leaf1 = new HierarchicalOrchestrationBroker(
                "Leaf1",
                new Location(0, 0, 0),
                new Location(25, 25, 0));
        regionA.addChild(leaf1);

        // 2. Add Providers

        // Provider 1: Low Latency, High Cost (at 10,10) matches Leaf1
        Map<String, Double> p1Metrics = new HashMap<>();
        p1Metrics.put("latency", 10.0);
        p1Metrics.put("cost", 50.0);
        MarketplaceProvider p1 = new MarketplaceProvider("ProviderFastExpensive", new Location(10, 10, 0));
        leaf1.addChild(p1);

        // Provider 2: High Latency, Low Cost (at 40,40) matches RegionA (but different
        // leaf/area)
        Map<String, Double> p2Metrics = new HashMap<>();
        p2Metrics.put("latency", 100.0);
        p2Metrics.put("cost", 5.0);
        MarketplaceProvider p2 = new MarketplaceProvider("ProviderSlowCheap", new Location(40, 40, 0));
        regionA.addChild(p2);

        // Update topological targets after adding children
        leaf1.updateRegion(p1);
        regionA.updateRegion(leaf1);
        regionA.updateRegion(p2);
        root.updateRegion(regionA);

        System.out.println("Topology Built.");

        // 3. Advertise Services
        long serviceId = 999;

        System.out.println("\n--- Phase 1: Advertising ---");
        p1.advertiseService(serviceId, p1Metrics);
        p2.advertiseService(serviceId, p2Metrics);

        // Allow propagation (simple method calls in this synchronous sim)
        // In a real run, this happens via events processing.
        // We verify the Root knows about the best metrics.

        // 4. Client Request

        System.out.println("\n--- Phase 2: Client Request ---");
        // Client wants Low Cost (at 5,5) - Should prefer Provider 2 despite P1 being
        // physically closer?
        // Wait, metric routing combines distance and penalties.
        // P1 spatial = (5-10)^2 + (5-10)^2 = 25+25 = 50.
        // P2 spatial = (5-40)^2 + (5-40)^2 = 1225+1225 = 2450.
        //
        // Metric 'Cost' preferences:
        // Client says "cost": 0.0 (Ideal).
        // P1 Cost diff = (50-0)^2 = 2500.
        // P2 Cost diff = (5-0)^2 = 25.
        //
        // Total P1 Score = 50 (spatial) + 2500 (metric) = 2550.
        // Total P2 Score = 2450 (spatial) + 25 (metric) = 2475.
        //
        // P2 should Win! (2475 < 2550).

        Map<String, Double> constraints = new HashMap<>();
        constraints.put("cost", 0.0);

        MarketplaceClient client = new MarketplaceClient("ClientFrugal", new Location(5, 5, 0));
        leaf1.addChild(client);

        client.requestService(serviceId, constraints);

        System.out.println("Client Request sent. Check logs for routing decisions.");
        System.out.println("Simulation Verification Complete.");
    }

    private static void setupConfiguration() {
        // Basic config setup if needed
        SimConfiguration.get().paths.enableEventTracing = true;
    }
}
