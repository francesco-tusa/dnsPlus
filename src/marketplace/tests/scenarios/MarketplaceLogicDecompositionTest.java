package marketplace.tests.scenarios;

import java.util.Map;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.regions.SubscriptionWithRegion;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import utils.CustomLogger;
import simulator.topology.factories.BoundedBrokerFactory;

import marketplace.agents.MarketplaceBroker;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;
import marketplace.common.MetricHyperCube;
import marketplace.topology.MarketplaceBrokerFactory;

/**
 * LOGIC TEST 1: Optimal Service Propagation & Network Routing.
 * * Verifies:
 * 1. Propagation Optimization (Quiescence):
 * - Contained offers are NOT propagated (Bandwidth saving).
 * - Improved offers ARE propagated.
 * 2. Cross-Branch Routing:
 * - Requests find providers even if they are in different branches of the tree.
 */
public class MarketplaceLogicDecompositionTest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceLogicDecompositionTest.class.getName());

    @Override
    public String getTestName() {
        return "Marketplace Logic: Propagation & Cross-Branch Routing";
    }

    @Override
    public boolean run(TopologyFixture ignoredFixture) {
        System.out.println(">>> STARTING LOGIC TEST: PROPAGATION & ROUTING");

        // --- SUB-TEST 1: PROPAGATION OPTIMIZATION ---
        if (!verifyPropagationOptimization()) {
            logger.severe("FAIL: Propagation Optimization (Quiescence) failed.");
            return false;
        }

        // --- SUB-TEST 2: NETWORK ROUTING ---
        if (!verifyRoutingPaths()) {
            logger.severe("FAIL: Network Routing logic failed.");
            return false;
        }

        System.out.println(">>> PROPAGATION & ROUTING TESTS PASSED");
        return true;
    }

    // =================================================================================
    // UNIT 1: PROPAGATION OPTIMIZATION (The "Hull-Based Quiescence" Check)
    // =================================================================================
    private boolean verifyPropagationOptimization() {
        System.out.println("\n[Logic Unit 1] Verifying Propagation Optimization...");

        // 1. Setup Isolated Branch (Cloud -> Fog -> Edge)
        MarketplaceBroker cloud = createBroker("Cloud_Check", 0, 1000);
        MarketplaceBroker fog = createBroker("Fog_Check", 0, 500);
        MarketplaceBroker edge = createBroker("Edge_Check", 0, 100);

        cloud.addChild(fog);
        fog.addChild(edge);

        // 2. Baseline: Add a Standard Provider at Edge
        // Latency: 50ms.
        MarketplaceProvider pBase = new MarketplaceProvider("P_Base", new Location(10, 10, 0));
        edge.addChild(pBase);
        pBase.advertiseService(1001, Map.of("latency", 50.0));

        // Ensure Baseline is established
        if (!checkCloudState(cloud, 50.0)) {
            return fail("Cloud did not receive initial baseline [50ms].");
        }
        System.out.println("   -> Baseline established. Cloud sees 50ms.");

        // capture state
        int cloudUpdatesBefore = cloud.getInputSubscriptionCount();

        // 3. Test QUIESCENCE (Suppression)
        // We create a provider that offers 50ms again.
        // Logic: The Hull [50, 50] already covers 50.
        // Expected: Edge Broker absorbs it. Fog and Cloud receive NO update.

        System.out.println("   -> Injecting 'Contained' Offer (50ms)...");
        MarketplaceProvider pContained = new MarketplaceProvider("P_Contained", new Location(15, 15, 0));
        edge.addChild(pContained);
        pContained.advertiseService(1001, Map.of("latency", 50.0));

        int cloudUpdatesAfter = cloud.getInputSubscriptionCount();

        if (cloudUpdatesAfter > cloudUpdatesBefore) {
            return fail("Quiescence Failed. Cloud received redundant update for contained offer.");
        }
        System.out.println("   -> Quiescence Verified. Cloud received 0 updates.");

        // 4. Test IMPROVEMENT (Propagation)
        // Add "Better" Provider (Latency 5ms).
        // Logic: 5 < 50. Hull expands to [5, 50]. Must propagate.
        System.out.println("   -> Injecting 'Better' Offer (5ms)...");
        MarketplaceProvider pBetter = new MarketplaceProvider("P_Better", new Location(16, 16, 0));
        edge.addChild(pBetter);
        pBetter.advertiseService(1001, Map.of("latency", 5.0));

        // Verify Cloud sees 5ms capability
        if (!checkCloudState(cloud, 5.0)) {
            return fail("Improvement Failed. Cloud did not update to 5ms.");
        }
        System.out.println("   -> Improvement Verified. Cloud state updated.");

        return true;
    }

    // =================================================================================
    // UNIT 2: ROUTING PATHS (Network Traversal)
    // =================================================================================
    private boolean verifyRoutingPaths() {
        System.out.println("\n[Logic Unit 2] Verifying Routing Paths (Cross-Branch)...");

        // 1. Setup Multi-Branch Topology
        // Cloud
        // |-- Fog_West -- Edge_West (Client Here)
        // |-- Fog_East -- Edge_East

        MarketplaceBroker cloud = createBroker("Cloud", 0, 1000);

        MarketplaceBroker fogWest = createBroker("Fog_West", 0, 400);
        MarketplaceBroker edgeWest = createBroker("Edge_West", 0, 100);
        cloud.addChild(fogWest);
        fogWest.addChild(edgeWest);

        MarketplaceBroker fogEast = createBroker("Fog_East", 600, 1000);
        MarketplaceBroker edgeEast = createBroker("Edge_East", 900, 1000);
        cloud.addChild(fogEast);
        fogEast.addChild(edgeEast);

        // 2. Deploy Providers

        // A. Cloud Provider (Global Backup) - Lat: 100, Cost: 5
        MarketplaceProvider pCloud = new MarketplaceProvider("Prov_Cloud", new Location(500, 500, 0));
        cloud.addChild(pCloud);
        pCloud.advertiseService(1001, Map.of("latency", 100.0, "cost", 5.0));

        // B. Local Provider (Edge West) - Lat: 10, Cost: 50
        MarketplaceProvider pWest = new MarketplaceProvider("Prov_West", new Location(10, 10, 0));
        edgeWest.addChild(pWest);
        pWest.advertiseService(1001, Map.of("latency", 10.0, "cost", 50.0));

        // C. Remote Provider (Edge East) - Lat: 20, Cost: 10 ("Goldilocks")
        MarketplaceProvider pEast = new MarketplaceProvider("Prov_East", new Location(950, 950, 0));
        edgeEast.addChild(pEast);
        pEast.advertiseService(1001, Map.of("latency", 20.0, "cost", 10.0));

        // 3. Client (At Edge West)
        MarketplaceClient client = new MarketplaceClient("Client", new Location(10, 10, 0));
        edgeWest.addChild(client);

        // --- CASE A: Local Preference ---
        // Req: Latency < 15. Only pWest (10) satisfies.
        client.requestService(1001, Map.of("latency", 15.0));
        if (pWest.getnPublications() != 1)
            return fail("Local Routing failed.");
        System.out.println("   -> Case A (Local): PASS");

        // --- CASE B: Cloud Fallback ---
        // Req: Cost < 8. Only pCloud (5) satisfies.
        client.requestService(1001, Map.of("cost", 8.0));
        if (pCloud.getnPublications() != 1)
            return fail("Cloud Fallback failed.");
        System.out.println("   -> Case B (Cloud Fallback): PASS");

        // --- CASE C: Cross-Branch Traversal ---
        // Req: Latency < 30 (Excludes Cloud), Cost < 20 (Excludes West).
        // Only pEast (Lat 20, Cost 10) satisfies.
        client.requestService(1001, Map.of("latency", 30.0, "cost", 20.0));
        if (pEast.getnPublications() != 1)
            return fail("Cross-Branch Routing failed.");
        System.out.println("   -> Case C (Cross-Branch): PASS");

        return true;
    }

    // --- Helpers ---

    private MarketplaceBroker createBroker(String name, int min, int max) {
        BoundedBrokerFactory factory = new MarketplaceBrokerFactory();
        return (MarketplaceBroker) factory.createLeafBroker(name,
                new Location(min, min, 0), new Location(max, max, 0));
    }

    private boolean checkCloudState(MarketplaceBroker cloud, double expectedMinLatency) {
        // Probe the Cloud's input store to see if it knows about the metric
        double probeReq = expectedMinLatency + 0.0001;

        marketplace.common.MetricLocation probe = new marketplace.common.MetricLocation(
                new double[] { probeReq, 9999.0 }, new Location(0, 0, 0));

        // We use the public API 'getInputSubscriptions' to inspect the state
        var subs = cloud.getInputSubscriptions();
        for (var entry : subs.values()) {
            for (var sub : entry) {
                if (sub instanceof SubscriptionWithRegion swr) {
                    if (swr.getRegion() instanceof MetricHyperCube mhc) {
                        if (mhc.contains(probe))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean fail(String msg) {
        System.err.println("   -> FAIL: " + msg);
        return false;
    }
}