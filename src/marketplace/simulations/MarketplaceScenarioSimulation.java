package marketplace.simulations;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import marketplace.agents.HierarchicalOrchestrationBroker;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;
import marketplace.topology.aggregation.OptimisticPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 * A complex scenario to stress-test Hierarchical Aggregation and Multi-Metric
 * Routing.
 * 
 * Topology:
 * - Root (0-100)
 * - Region A (0-50, 0-50): "The Sparse Region"
 * - Leaf A1 (0-25): Contains High Cost / Low Latency
 * - Region B (50-100, 0-50): "The Dense Region"
 * - Leaf B1 (50-75): Contains Low Cost / High Latency
 * - Leaf B2 (75-100): Contains Balanced Provider
 * 
 * Objectives:
 * 1. Verify Upward Aggregation (Optimistic Policy) creates correct
 * "Frankenstein" proxies.
 * 2. Verify Single-Winner Routing sends clients to the specific best leaf.
 */
public class MarketplaceScenarioSimulation {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceScenarioSimulation.class.getName());

    public static void main(String[] args) {
        setupConfiguration();
        System.out.println("=== Starting Complex Marketplace Simulation ===\n");

        // --- 1. Topology Setup ---

        HierarchicalOrchestrationBroker root = createBroker("Root", 0, 0, 100, 100);

        // Region A
        HierarchicalOrchestrationBroker regionA = createBroker("RegionA", 0, 0, 50, 50);
        HierarchicalOrchestrationBroker leafA1 = createBroker("LeafA1", 0, 0, 25, 25);
        regionA.addChild(leafA1);
        root.addChild(regionA);

        // Region B
        HierarchicalOrchestrationBroker regionB = createBroker("RegionB", 50, 0, 100, 50);
        HierarchicalOrchestrationBroker leafB1 = createBroker("LeafB1", 50, 0, 75, 25);
        HierarchicalOrchestrationBroker leafB2 = createBroker("LeafB2", 75, 0, 100, 25);
        regionB.addChild(leafB1);
        regionB.addChild(leafB2);
        root.addChild(regionB);

        // --- 2. Provider Setup (The Offerings) ---
        long serviceId = 777;

        // P1: Premium (Fast, Expensive) in Region A
        MarketplaceProvider p1 = new MarketplaceProvider("P1_Premium_A", new Location(10, 10, 0));
        leafA1.addChild(p1);
        Map<String, Double> m1 = Map.of("latency", 5.0, "cost", 100.0);

        // P2: Budget (Slow, Cheap) in Region B (Leaf B1)
        MarketplaceProvider p2 = new MarketplaceProvider("P2_Budget_B", new Location(60, 10, 0));
        leafB1.addChild(p2);
        Map<String, Double> m2 = Map.of("latency", 200.0, "cost", 5.0);

        // P3: Balanced (Medium, Medium) in Region B (Leaf B2)
        MarketplaceProvider p3 = new MarketplaceProvider("P3_Balanced_B", new Location(80, 10, 0));
        leafB2.addChild(p3);
        Map<String, Double> m3 = Map.of("latency", 50.0, "cost", 50.0);

        // Update topology links
        leafA1.updateRegion(p1);
        leafB1.updateRegion(p2);
        leafB2.updateRegion(p3);
        regionA.updateRegion(leafA1);
        regionB.updateRegion(leafB1);
        regionB.updateRegion(leafB2);
        root.updateRegion(regionA);
        root.updateRegion(regionB);

        System.out.println("--- Phase 1: Advertising ---");
        p1.advertiseService(serviceId, m1);
        p2.advertiseService(serviceId, m2);
        p3.advertiseService(serviceId, m3);

        System.out.println("Providers Advertised.");
        System.out.println("\n--- Broker Knowledge Inspection ---");
        printBrokerState(root);

        // --- 3. Client Requests (The Stress Test) ---

        // Case 1: Client in Region A wants LOW COST.
        // - Locally, Region A only has P1 ($100).
        // - Region B advertises $5 (via P2).
        // - Result: Should route cross-region to Region B -> Leaf B1 -> P2.
        System.out.println("\n--- Phase 2: Client 1 (Frugal) in Region A ---");
        MarketplaceClient c1 = new MarketplaceClient("Client_Frugal", new Location(5, 5, 0));
        leafA1.addChild(c1);

        Map<String, Double> req1 = new HashMap<>();
        req1.put("cost", 0.0); // Strong pull towards low cost
        c1.requestService(serviceId, req1);

        // Case 2: Client in Region B wants LOW LATENCY.
        // - Locally, Region B has P2 (200ms) and P3 (50ms).
        // - Region A advertises 5ms (via P1).
        // - Result: Should route cross-region to Region A -> Leaf A1 -> P1.
        System.out.println("\n--- Phase 3: Client 2 (Gamer) in Region B ---");
        MarketplaceClient c2 = new MarketplaceClient("Client_Gamer", new Location(90, 10, 0)); // Near P3
        leafB2.addChild(c2);

        Map<String, Double> req2 = new HashMap<>();
        req2.put("latency", 0.0); // Strong pull towards low latency
        c2.requestService(serviceId, req2);

        // Case 3: Client in Region B wants BALANCED (or nearest).
        // - Currently located at 85,10 (Near P3).
        // - P3 is Balanced.
        // - Should stay local.
        System.out.println("\n--- Phase 4: Client 3 (Local) in Region B ---");
        MarketplaceClient c3 = new MarketplaceClient("Client_Local", new Location(85, 10, 0));
        leafB2.addChild(c3);

        Map<String, Double> req3 = new HashMap<>();
        // No strong constraints, mostly distance will dominate or balanced metrics
        req3.put("latency", 50.0);
        req3.put("cost", 50.0);
        c3.requestService(serviceId, req3);

        System.out.println("\n=== Simulation Complete ===");
    }

    private static HierarchicalOrchestrationBroker createBroker(String name, double x, double y, double w, double h) {
        HierarchicalOrchestrationBroker b = new HierarchicalOrchestrationBroker(name, new Location(x, y, 0),
                new Location(x + w, y + h, 0));
        b.setAggregationPolicy(new OptimisticPolicy());
        return b;
    }

    private static void setupConfiguration() {
        SimConfiguration.get().paths.enableEventTracing = true;
    }

    private static void printBrokerState(HierarchicalOrchestrationBroker broker) {
        System.out.println("State of Broker [" + broker.getName() + "]:");
        Map<TreeNode, List<simulator.events.SimulationSubscription>> store = broker.getInputSubscriptions();
        for (Map.Entry<TreeNode, List<simulator.events.SimulationSubscription>> entry : store.entrySet()) {
            TreeNode child = entry.getKey();
            for (simulator.events.SimulationSubscription sub : entry.getValue()) {
                if (sub instanceof simulator.events.SubscriptionWithLocation sl) {
                    System.out.println("  -> Child " + child.getName() + " offers: " + sl.getLocation());
                }
            }
        }
    }
}
