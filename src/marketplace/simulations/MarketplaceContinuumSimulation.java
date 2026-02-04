package marketplace.simulations;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;
import marketplace.agents.MarketplaceBroker;
import marketplace.topology.MarketplaceBrokerFactory;
import marketplace.common.MetricHyperCube;
import marketplace.common.MetricLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
        MarketplaceBroker root = buildTopology();
        List<MarketplaceBroker> allBrokers = collectBrokers(root);
        List<MarketplaceBroker> leafBrokers = filterBrokersByLevel(allBrokers, true); // Leaves
        List<MarketplaceBroker> intermediateBrokers = filterBrokersByLevel(allBrokers, false); // Fog/Root
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
        System.out.println("All " + allProviders.size() + " providers advertised.");

        // 4. Place Clients & Generate Workload
        System.out.println("\n--- Phase 2: Client Workload ---");
        placeAndRunClients(leafBrokers);

        // 5. Oracle Analysis
        runOracleAnalysis();
    }

    // --- Topology Building ---

    private static MarketplaceBroker buildTopology() {
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

        return (MarketplaceBroker) generator.generateTopology(config);
    }

    private static List<MarketplaceBroker> collectBrokers(MarketplaceBroker root) {
        List<MarketplaceBroker> list = new ArrayList<>();
        collectRecursive(root, list);
        return list;
    }

    private static void collectRecursive(TreeNode node, List<MarketplaceBroker> list) {
        if (node instanceof MarketplaceBroker) {
            MarketplaceBroker hb = (MarketplaceBroker) node;
            list.add(hb);
            for (Object child : hb.getChildren()) {
                if (child instanceof TreeNode) {
                    collectRecursive((TreeNode) child, list);
                }
            }
        }
    }

    private static List<MarketplaceBroker> filterBrokersByLevel(
            List<MarketplaceBroker> brokers, boolean isLeaf) {
        List<MarketplaceBroker> result = new ArrayList<>();
        for (MarketplaceBroker b : brokers) {
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

    private static void placeCloudProviders(MarketplaceBroker root) {
        for (int i = 0; i < NUM_CLOUD_PROVIDERS; i++) {
            Location loc = root.getRegion().getCenter(); // Cloud is conceptually "Central"
            MarketplaceProvider p = new MarketplaceProvider("Cloud_P" + i, loc);
            root.addChild(p);

            Map<String, Double> metrics = new HashMap<>();
            metrics.put("cost", 0.01); // Cheap
            metrics.put("latency", 100.0); // Slow
            metrics.put("capacity", 1000.0); // High Capacity

            p.advertiseService(SERVICE_ID, metrics);
            allProviders.add(p);
        }
    }

    private static void placeFogProviders(List<MarketplaceBroker> brokers) {
        if (brokers.isEmpty())
            return;
        for (int i = 0; i < NUM_FOG_PROVIDERS; i++) {
            MarketplaceBroker b = brokers.get(random.nextInt(brokers.size()));
            Location loc = b.getRegion().getCenter();
            MarketplaceProvider p = new MarketplaceProvider("Fog_P" + i, loc);
            b.addChild(p);
            // No manual updateRegion needed with SpatialMatchBroker

            Map<String, Double> metrics = new HashMap<>();
            metrics.put("cost", 0.10); // Moderate
            metrics.put("latency", 20.0); // Moderate
            metrics.put("capacity", 100.0);

            p.advertiseService(SERVICE_ID, metrics);
            allProviders.add(p);
        }
    }

    private static void placeEdgeProviders(List<MarketplaceBroker> brokers) {
        if (brokers.isEmpty())
            return;
        for (int i = 0; i < NUM_EDGE_PROVIDERS; i++) {
            MarketplaceBroker b = brokers.get(random.nextInt(brokers.size()));
            Location loc = b.getRegion().getCenter(); // Or random in region
            MarketplaceProvider p = new MarketplaceProvider("Edge_P" + i, loc);
            b.addChild(p);

            Map<String, Double> metrics = new HashMap<>();
            metrics.put("cost", 0.50); // Expensive
            metrics.put("latency", 1.0); // Fast
            metrics.put("capacity", 10.0);

            p.advertiseService(SERVICE_ID, metrics);
            allProviders.add(p);
        }
    }

    // --- Client Workload ---

    private static void placeAndRunClients(List<MarketplaceBroker> leafBrokers) {
        for (int i = 0; i < NUM_CLIENTS; i++) {
            MarketplaceBroker leaf = leafBrokers.get(random.nextInt(leafBrokers.size()));
            Location loc = leaf.getRegion().getCenter();
            MarketplaceClient c = new MarketplaceClient("Client_" + i, loc);
            leaf.addChild(c);
            allClients.add(c);

            Map<String, Double> req = new HashMap<>();
            // Random preference: 50% want Speed (Latency < 50), 50% want Cost (Cost < 0.2)
            // Note: We use thresholds now because we use Containment.
            if (random.nextBoolean()) {
                // Needs FAST response
                req.put("latency", 15.0); // Strict latency requirement
                // req.put("cost", 100.0); // Loose cost
            } else {
                // Needs CHEAP response
                // req.put("latency", 1000.0); // Loose latency
                req.put("cost", 0.05); // Strict cost requirement
            }

            clientRequests.put(c, req);
            c.requestService(SERVICE_ID, req);
        }
    }

    // --- Oracle ---

    private static void runOracleAnalysis() {
        System.out.println("\n--- Phase 3: Comparative Analysis (Oracle vs Actual) ---");
        System.out
                .println("| Client          | Preference | Optimal (Score)   | Actual (Score)    | Gap (%) | Status |");
        System.out
                .println("|-----------------|------------|-------------------|-------------------|---------|--------|");

        int exactMatches = 0;
        int total = 0;
        double totalGap = 0.0;
        int validMatches = 0;

        // Build a lookup map for providers
        Map<String, MarketplaceProvider> providerMap = new HashMap<>();
        for (MarketplaceProvider p : allProviders) {
            providerMap.put(p.getName(), p);
        }

        for (MarketplaceClient c : allClients) {
            Map<String, Double> req = clientRequests.get(c);
            if (req == null)
                continue;
            total++;

            ProviderScore best = findBestProvider(c, allProviders, req);
            String actualProviderName = actualMatches.getOrDefault(c.getName(), "NONE");
            MarketplaceProvider actualProvider = providerMap.get(actualProviderName);

            String pref = req.containsKey("latency") ? "Lat" : "Cost";

            double optimalScore = (best != null) ? best.score : Double.MAX_VALUE;
            String optimalStr = (best != null) ? String.format("%s (%.1f)", best.p.getName(), optimalScore) : "NONE";

            double actualScore = Double.MAX_VALUE;
            if (actualProvider != null) {
                actualScore = calculateScore(c, actualProvider, req);
            }
            String actualStr = (actualProvider != null)
                    ? String.format("%s (%.1f)", actualProvider.getName(), actualScore)
                    : "NONE";

            // Status Check
            String status = "MISMATCH";
            if (actualProviderName.equals((best != null) ? best.p.getName() : "NONE")) {
                status = "MATCH";
                exactMatches++;
            }

            // Gap Calculation
            String gapStr = "-";
            if (best != null && actualProvider != null) {
                double diff = actualScore - optimalScore;
                double ratio = (optimalScore > 0.0001) ? (diff / optimalScore) * 100.0 : 0.0;
                gapStr = String.format("%.1f%%", ratio);

                // Only count gap for valid matches
                if (actualScore < Double.MAX_VALUE) {
                    totalGap += ratio;
                    validMatches++;
                }
            } else if (best == null && actualProvider == null) {
                status = "MATCH";
                exactMatches++;
                gapStr = "0.0%";
            }

            // Format
            System.out.println(String.format("| %-15s | %-10s | %-17s | %-17s | %-7s | %-6s |",
                    c.getName(), pref, optimalStr, actualStr, gapStr, status));
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println(String.format("Exact Accuracy: %d / %d (%.2f%%)", exactMatches, total,
                (total > 0 ? (double) exactMatches / total * 100 : 0)));
        if (validMatches > 0) {
            System.out.println(String.format("Avg Optimality Gap: %.2f%% (Lower is better)", totalGap / validMatches));
        }
    }

    private static ProviderScore findBestProvider(MarketplaceClient c, List<MarketplaceProvider> providers,
            Map<String, Double> req) {
        ProviderScore best = null;
        double minScore = Double.MAX_VALUE;

        for (MarketplaceProvider p : providers) {
            double score = calculateScore(c, p, req);

            // Valid Match Check
            if (score < Double.MAX_VALUE) {
                if (score < minScore) {
                    minScore = score;
                    best = new ProviderScore(p, score);
                }
            }
        }
        return best;
    }

    private static double calculateScore(MarketplaceClient c, MarketplaceProvider p, Map<String, Double> req) {
        Map<String, Double> pMetrics = p.getLastAdvertisedMetrics();
        if (pMetrics == null)
            return Double.MAX_VALUE;

        // 1. Reconstruct MetricHyperCube (Provider)
        double pLat = pMetrics.getOrDefault("latency", 0.0);
        double pCost = pMetrics.getOrDefault("cost", 0.0);
        double[] pValues = new double[] { pLat, pCost };
        boolean[] flags = new boolean[] { true, true };
        MetricHyperCube pRegion = new MetricHyperCube(pValues, pValues, flags);

        // 2. Reconstruct MetricLocation (Requirement)
        double rLat = req.getOrDefault("latency", Double.MAX_VALUE);
        double rCost = req.getOrDefault("cost", Double.MAX_VALUE);
        double[] rValues = new double[] { rLat, rCost };
        MetricLocation rLoc = new MetricLocation(rValues);

        // 3. Check Containment (Validity)
        if (!pRegion.contains(rLoc)) {
            return Double.MAX_VALUE; // Not a match
        }

        // 4. Return Score (Optimization Objective)
        // If Request specifies Latency < X, we assume they care about Latency
        // minimization?
        // Or strictly meeting threshold?
        // Let's assume implied preference based on the TIGHTEST constraint.
        // For simplicity: If Latency constraint < 100, optimize Latency. Else optimize
        // Cost.

        if (rLat < 100.0) {
            return pLat;
        } else {
            return pCost;
        }
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
