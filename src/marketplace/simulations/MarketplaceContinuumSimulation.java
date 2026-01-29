package marketplace.simulations;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;
import simulator.topology.factories.BoundedBrokerFactory;
import marketplace.agents.HierarchicalOrchestrationBroker;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;
import marketplace.topology.MarketplaceBrokerFactory;
import marketplace.common.MultiMetricLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import utils.CustomLogger;

/**
 * Large Scale Simulation of a Cloud-Edge Continuum.
 * 
 * Features:
 * - Hierarchical Topology (Root -> Fog -> Edge).
 * - Layered Providers (Cloud, Fog, Edge) with distinct profiles.
 * - Random Client Workload.
 * - Ground Truth Oracle for verification.
 */
public class MarketplaceContinuumSimulation {

    // --- Simulation Parameters ---
    private static final int NUM_REGIONS = 16;
    private static final int TREE_DEPTH = 3; // Root -> Region -> Leaf
    private static final int BRANCHING_FACTOR = 4;

    private static final int NUM_CLOUD_PROVIDERS = 1;
    private static final int NUM_FOG_PROVIDERS = 5;
    private static final int NUM_EDGE_PROVIDERS = 20;
    private static final int NUM_CLIENTS = 50;

    private static final long SERVICE_ID = 999;
    private static final Random random = new Random(12345); // Fixed seed for reproducibility

    // --- State Tracking ---
    private static final List<MarketplaceProvider> allProviders = new ArrayList<>();
    private static final List<MarketplaceClient> allClients = new ArrayList<>();
    // Track client requests to compare against oracle
    private static final Map<MarketplaceClient, Map<String, Double>> clientRequests = new HashMap<>();

    // Track Actual Outcomes: ClientName -> ProviderName
    private static final Map<String, String> actualMatches = new HashMap<>();

    public static void recordMatch(String clientName, String providerName) {
        actualMatches.put(clientName, providerName);
    }

    public static void main(String[] args) {
        // Enable tracing globally to ensure PublisherWithLocation populates Source Info
        SimConfiguration.get().paths.enableEventTracing = true;

        System.out.println("=== Starting Large Scale Continuum Simulation ===\n");

        // 1. Build Topology
        HierarchicalOrchestrationBroker root = buildTopology();
        List<HierarchicalOrchestrationBroker> allBrokers = collectBrokers(root);
        List<HierarchicalOrchestrationBroker> leafBrokers = filterBrokersByLevel(allBrokers, true); // Leaves
        List<HierarchicalOrchestrationBroker> intermediateBrokers = filterBrokersByLevel(allBrokers, false); // Fog/Root
        // Remove Root from intermediate list to treat it specifically as Cloud
        intermediateBrokers.remove(root);

        System.out.println("Topology Built:");
        System.out.println("  - Total Brokers: " + allBrokers.size());
        System.out.println("  - Leaf Brokers (Edge): " + leafBrokers.size());
        System.out.println("  - Intermediate Brokers (Fog): " + intermediateBrokers.size());
        System.out.println("  - Root Broker (Cloud): 1");

        // 2. Place Providers
        placeCloudProviders(root);
        placeFogProviders(intermediateBrokers);
        placeEdgeProviders(leafBrokers);

        // 3. Advertise
        System.out.println("\n--- Phase 1: Advertising ---");
        // Providers advertised during placement logic.
        // We can double check or re-advertise if we want to refresh.
        // For now, they are already advertised.
        System.out.println("All " + allProviders.size() + " providers advertised.");

        // 4. Place Clients & Generate Workload
        System.out.println("\n--- Phase 2: Client Workload ---");
        placeAndRunClients(leafBrokers);

        // 5. Oracle Analysis
        // System.out.println("\n--- Phase 3: Ground Truth Oracle ---");
        runOracleAnalysis();
    }

    // --- Topology Building ---

    private static HierarchicalOrchestrationBroker buildTopology() {
        // Create a custom configuration by overriding getters to bypass final field
        // restrictions
        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration() {
            @Override
            public int getNumRegions() {
                return NUM_REGIONS;
            }

            @Override
            public int getTreeDepth() {
                return TREE_DEPTH;
            }

            @Override
            public int getMaxBranchingFactor() {
                return BRANCHING_FACTOR;
            }

            @Override
            public double getWorldWidth() {
                return 1000;
            }

            @Override
            public double getWorldHeight() {
                return 1000;
            }
        };

        MarketplaceBrokerFactory factory = new MarketplaceBrokerFactory();
        RandomTopologyGenerator generator = new RandomTopologyGenerator(factory);

        // Use generateTopology which fits the interface
        return (HierarchicalOrchestrationBroker) generator.generateTopology(config);
    }

    private static List<HierarchicalOrchestrationBroker> collectBrokers(HierarchicalOrchestrationBroker root) {
        List<HierarchicalOrchestrationBroker> list = new ArrayList<>();
        collectRecursive(root, list);
        return list;
    }

    private static void collectRecursive(TreeNode node, List<HierarchicalOrchestrationBroker> list) {
        if (node instanceof HierarchicalOrchestrationBroker) {
            HierarchicalOrchestrationBroker hb = (HierarchicalOrchestrationBroker) node;
            list.add(hb);
            for (Object child : hb.getChildren()) {
                if (child instanceof TreeNode) {
                    collectRecursive((TreeNode) child, list);
                }
            }
        }
    }

    private static List<HierarchicalOrchestrationBroker> filterBrokersByLevel(
            List<HierarchicalOrchestrationBroker> brokers, boolean isLeaf) {
        List<HierarchicalOrchestrationBroker> result = new ArrayList<>();
        for (HierarchicalOrchestrationBroker b : brokers) {
            // A simple heuristic: if it has children that are Brokers, it's intermediate.
            // If it has NO children that are brokers, it's a leaf.
            boolean hasBrokerChild = false;
            for (Object child : b.getChildren()) {
                if (child instanceof BoundedBroker) {
                    hasBrokerChild = true;
                    break;
                }
            }

            if (isLeaf && !hasBrokerChild) {
                result.add(b);
            } else if (!isLeaf && hasBrokerChild) {
                result.add(b);
            }
        }
        return result;
    }

    // --- Provider Placement ---

    private static void placeCloudProviders(HierarchicalOrchestrationBroker root) {
        for (int i = 0; i < NUM_CLOUD_PROVIDERS; i++) {
            Location loc = root.getRegion().getCenter(); // Cloud is conceptually "Central"
            MarketplaceProvider p = new MarketplaceProvider("Cloud_P" + i, loc);
            root.addChild(p);

            Map<String, Double> metrics = new HashMap<>();
            metrics.put("cost", 0.01); // Cheap
            metrics.put("latency", 100.0); // Slow
            metrics.put("capacity", 1000.0);

            // We store description for later re-advertising
            p.advertiseService(SERVICE_ID, metrics);
            allProviders.add(p);
        }
    }

    private static void placeFogProviders(List<HierarchicalOrchestrationBroker> brokers) {
        if (brokers.isEmpty())
            return;
        for (int i = 0; i < NUM_FOG_PROVIDERS; i++) {
            HierarchicalOrchestrationBroker b = brokers.get(random.nextInt(brokers.size()));
            Location loc = b.getRegion().getCenter();
            MarketplaceProvider p = new MarketplaceProvider("Fog_P" + i, loc);
            b.addChild(p);
            b.updateRegion(p);

            Map<String, Double> metrics = new HashMap<>();
            metrics.put("cost", 0.10); // Moderate
            metrics.put("latency", 20.0); // Moderate
            metrics.put("capacity", 100.0);

            p.advertiseService(SERVICE_ID, metrics);
            allProviders.add(p);
        }
    }

    private static void placeEdgeProviders(List<HierarchicalOrchestrationBroker> brokers) {
        if (brokers.isEmpty())
            return;
        for (int i = 0; i < NUM_EDGE_PROVIDERS; i++) {
            HierarchicalOrchestrationBroker b = brokers.get(random.nextInt(brokers.size()));
            Location loc = b.getRegion().getCenter(); // Or random in region
            MarketplaceProvider p = new MarketplaceProvider("Edge_P" + i, loc);
            b.addChild(p);
            b.updateRegion(p);

            Map<String, Double> metrics = new HashMap<>();
            metrics.put("cost", 0.50); // Expensive
            metrics.put("latency", 1.0); // Fast
            metrics.put("capacity", 10.0);

            p.advertiseService(SERVICE_ID, metrics);
            allProviders.add(p);
        }
    }

    // --- Client Workload ---

    private static void placeAndRunClients(List<HierarchicalOrchestrationBroker> leafBrokers) {
        for (int i = 0; i < NUM_CLIENTS; i++) {
            HierarchicalOrchestrationBroker leaf = leafBrokers.get(random.nextInt(leafBrokers.size()));
            Location loc = leaf.getRegion().getCenter();
            MarketplaceClient c = new MarketplaceClient("Client_" + i, loc);
            leaf.addChild(c);
            allClients.add(c);

            Map<String, Double> req = new HashMap<>();
            // Random preference: 50% want Speed, 50% want Cost
            if (random.nextBoolean()) {
                req.put("latency", 0.0); // Want 0 latency
            } else {
                req.put("cost", 0.0); // Want 0 cost
            }

            clientRequests.put(c, req);
            c.requestService(SERVICE_ID, req);
        }
    }

    // --- Oracle ---

    private static void runOracleAnalysis() {
        System.out.println("\n--- Phase 3: Comparative Analysis (Oracle vs Actual) ---");
        System.out.println("| Client          | Preference | Optimal (Oracle) | Actual (Sim)     | Status |");
        System.out.println("|-----------------|------------|------------------|------------------|--------|");

        int exactMatches = 0;
        int total = 0;

        for (MarketplaceClient c : allClients) {
            Map<String, Double> req = clientRequests.get(c);
            if (req == null)
                continue;
            total++;

            ProviderScore best = findBestProvider(c, allProviders, req);
            String actualProvider = actualMatches.getOrDefault(c.getName(), "NONE");

            String pref = req.containsKey("latency") ? "Latency" : "Cost   ";
            String optimalName = (best != null) ? best.p.getName() : "NONE";

            // Status Check
            String status = "MISMATCH";
            if (optimalName.equals(actualProvider)) {
                status = "MATCH";
                exactMatches++;
            } else if (actualProvider.equals("NONE") && optimalName.equals("NONE")) {
                status = "MATCH"; // Both agree no provider found
                exactMatches++;
            }

            // Format
            System.out.println(String.format("| %-15s | %-10s | %-16s | %-16s | %-6s |",
                    c.getName(), pref, optimalName, actualProvider, status));
        }
        System.out.println("---------------------------------------------------------------------------");
        System.out.println(String.format("Accuracy: %d / %d (%.2f%%)", exactMatches, total,
                (total > 0 ? (double) exactMatches / total * 100 : 0)));
    }

    private static ProviderScore findBestProvider(MarketplaceClient c, List<MarketplaceProvider> providers,
            Map<String, Double> req) {
        ProviderScore best = null;
        double minScore = Double.MAX_VALUE;
        // Create a temporary object to perform the distance calculation
        MultiMetricLocation requestLoc = new MultiMetricLocation(c.getLocation(), req);

        for (MarketplaceProvider p : providers) {
            Map<String, Double> pMetrics = p.getLastAdvertisedMetrics();
            if (pMetrics == null)
                continue;

            MultiMetricLocation providerLoc = new MultiMetricLocation(p.getLocation(), pMetrics);
            double score = requestLoc.distanceSquared(providerLoc);

            // Strict Threshold Check (Matches Simulator)
            if (score <= MultiMetricLocation.MAX_ACCEPTABLE_DISTANCE) {
                if (score < minScore) {
                    minScore = score;
                    best = new ProviderScore(p, score);
                }
            }
        }
        return best;
    }

    static class ProviderScore {
        MarketplaceProvider p;
        double score;

        public ProviderScore(MarketplaceProvider p, double s) {
            this.p = p;
            this.score = s;
        }
    }
}
