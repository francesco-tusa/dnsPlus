package marketplace.tests.scenarios;

import java.util.Map;
import java.util.logging.Logger;
import utils.CustomLogger;

import simulator.core.Location;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import simulator.topology.factories.BoundedBrokerFactory;

import marketplace.agents.MarketplaceBroker;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;
import marketplace.topology.MarketplaceBrokerFactory;
import marketplace.common.MarketplaceMetricSchema; // Import Schema
import marketplace.common.MetricLocation;         // Import MetricLocation

public class MarketplaceLogicDecompositionTest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceLogicDecompositionTest.class.getName());

    @Override
    public String getTestName() { 
        return "Marketplace Logic: Unified Propagation & Routing"; 
    }

    @Override
    public boolean run(TopologyFixture ignoredFixture) {
        System.out.println(">>> STARTING LOGIC TEST");

        if (!verifyPropagationOptimization()) return false;
        if (!verifyIntersectionLogic()) return false;
        if (!verifyRoutingPaths()) return false;

        System.out.println(">>> ALL TESTS PASSED");
        return true;
    }

    // UNIT 2: INTERSECTION & PROPAGATION
    private boolean verifyIntersectionLogic() {
        System.out.println("\n[Logic Unit 2] Verifying Intersection & Propagation Logic...");

        MarketplaceBroker cloud = createBroker("Cloud_Int", 0, 60);
        MarketplaceBroker fogLocal = createBroker("Fog_Local", 0, 5);
        MarketplaceBroker fogRemote = createBroker("Fog_Remote", 50, 55);
        cloud.addChild(fogLocal);
        cloud.addChild(fogRemote);

        // Case A: Edge Provider (Local) -> Should NOT reach Remote Fog
        System.out.println("   -> Case A: Testing Edge/Local Scope...");
        MarketplaceProvider pLocal = new MarketplaceProvider("Edge_Local_Prov", new Location(2.5, 2.5, 0));
        fogLocal.addChild(pLocal);
        pLocal.advertiseService(9001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 10.0));
        
        if (!probeBrokerState(cloud, 10.0)) return fail("Upward propagation (Local) failed.");
        if (probeBrokerState(fogRemote, 10.0)) return fail("Isolation Failed. Remote broker saw Local offer.");
        System.out.println("   -> Isolation Verified.");

        // Case B: Cloud Provider (Country/Region) -> Should reach Remote Fog
        System.out.println("   -> Case B: Testing Cloud/Country Scope...");
        MarketplaceProvider pGlobal = new MarketplaceProvider("Cloud_Global_Prov", new Location(2.5, 2.5, 0));
        fogLocal.addChild(pGlobal);
        pGlobal.advertiseService(9002, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 20.0));
        
        if (!probeBrokerState(fogRemote, 20.0)) return fail("Propagation Failed. Remote broker missing Cloud offer.");
        System.out.println("   -> Propagation Verified.");
        return true;
    }

    // UNIT 1: OPTIMIZATION
    private boolean verifyPropagationOptimization() {
        System.out.println("\n[Logic Unit 1] Verifying Propagation Optimization...");
        MarketplaceBroker cloud = createBroker("Cloud_Check", 0, 50);
        MarketplaceBroker fog   = createBroker("Fog_Check", 0, 25);
        MarketplaceBroker edge  = createBroker("Edge_Check", 0, 5);
        cloud.addChild(fog);
        fog.addChild(edge);

        MarketplaceProvider pBase = new MarketplaceProvider("Edge_Base", new Location(0.5, 0.5, 0));
        edge.addChild(pBase);
        pBase.advertiseService(1001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 50.0));
        
        if (!probeBrokerState(cloud, 50.0)) return fail("Baseline failed.");
        System.out.println("   -> Baseline established.");
        
        MarketplaceProvider pBetter = new MarketplaceProvider("Edge_Better", new Location(0.5, 0.5, 0));
        edge.addChild(pBetter);
        pBetter.advertiseService(1001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 5.0));
        
        if (!probeBrokerState(cloud, 5.0)) return fail("Improvement failed.");
        System.out.println("   -> Improvement Verified.");
        return true;
    }

    // UNIT 3: ROUTING
    private boolean verifyRoutingPaths() {
        System.out.println("\n[Logic Unit 3] Verifying Routing Paths (Cross-Branch)...");

        MarketplaceBroker cloud = createBroker("Cloud", 0, 50);
        
        // West Branch (0-20)
        MarketplaceBroker fogWest = createBroker("Fog_West", 0, 20);
        MarketplaceBroker edgeWest = createBroker("Edge_West", 0, 5);
        cloud.addChild(fogWest);
        fogWest.addChild(edgeWest);

        // East Branch (30-50)
        MarketplaceBroker fogEast = createBroker("Fog_East", 30, 50);
        MarketplaceBroker edgeEast = createBroker("Edge_East", 45, 50);
        cloud.addChild(fogEast);
        fogEast.addChild(edgeEast);

        // Providers
        // P1: Cloud Center (25,25). Dist to Client(0.5,0.5) ~ 35. Total Latency = 100+35 = 135.
        MarketplaceProvider pCloud = new MarketplaceProvider("Cloud_Prov", new Location(25, 25, 0));
        cloud.addChild(pCloud);
        pCloud.advertiseService(1001, Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, 100.0, 
            MarketplaceMetricSchema.METRIC_COST, 5.0
        ));

        // P2: West Edge (0.5,0.5). Dist to Client ~ 0. Total Latency = 10. Cost = 50.
        MarketplaceProvider pWest = new MarketplaceProvider("Edge_West_Prov", new Location(0.5, 0.5, 0));
        edgeWest.addChild(pWest);
        pWest.advertiseService(1001, Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, 10.0, 
            MarketplaceMetricSchema.METRIC_COST, 50.0
        ));

        // P3: East Edge (47.5, 47.5). Dist to Client ~ 67. Total Latency = 20+67 = 87. Cost = 10.
        // FIX: Renamed to "Cloud_East_Global" so its coverage radius (60) physically reaches the West client.
        //      Otherwise, Spatial Index filters it out before Strategy runs.
        MarketplaceProvider pEast = new MarketplaceProvider("Cloud_East_Global", new Location(47.5, 47.5, 0));
        edgeEast.addChild(pEast);
        pEast.advertiseService(1001, Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, 20.0, 
            MarketplaceMetricSchema.METRIC_COST, 10.0
        ));

        MarketplaceClient client = new MarketplaceClient("Client", new Location(0.5, 0.5, 0));
        edgeWest.addChild(client);
        
        // A. Local Preference
        // Req: Lat < 15. Only pWest (10) satisfies.
        client.requestService(1001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 15.0));
        if (pWest.getnPublications() != 1) return fail("Local Routing failed.");
        System.out.println("   -> Case A (Local): PASS");

        // B. Cloud Fallback (Cheapest)
        // Req: Cost < 8. Only pCloud (5) satisfies. pEast(10) and pWest(50) fail.
        client.requestService(1001, Map.of(MarketplaceMetricSchema.METRIC_COST, 8.0));
        if (pCloud.getnPublications() != 1) return fail("Cloud Fallback failed.");
        System.out.println("   -> Case B (Cloud Fallback): PASS");

        // C. Cross-Branch
        // FIX: Adjusted constraints to account for physical distance.
        // pCloud: Lat 135 (Fail).
        // pWest: Cost 50 (Fail).
        // pEast: Lat 87 (Pass < 120), Cost 10 (Pass < 20).
        client.requestService(1001, Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, 120.0, 
            MarketplaceMetricSchema.METRIC_COST, 20.0
        ));
        if (pEast.getnPublications() != 1) return fail("Cross-Branch Routing failed.");
        System.out.println("   -> Case C (Cross-Branch): PASS");

        return true;
    }

    private MarketplaceBroker createBroker(String name, double min, double max) {
        BoundedBrokerFactory factory = new MarketplaceBrokerFactory();
        return (MarketplaceBroker) factory.createLeafBroker(name, 
            new Location(min, min, 0), new Location(max, max, 0));
    }

    private boolean probeBrokerState(MarketplaceBroker broker, double expectedMinLatency) {
        double[] metrics = new double[MarketplaceMetricSchema.KEYS.length];
        for (int i = 0; i < metrics.length; i++) metrics[i] = 99999.0; 

        int latIdx = MarketplaceMetricSchema.getIndexOf(MarketplaceMetricSchema.METRIC_LATENCY);
        if (latIdx != -1) {
            metrics[latIdx] = expectedMinLatency + 0.0001; 
        }

        MetricLocation probe = new MetricLocation(metrics, new Location(0,0,0));
        
        var subs = broker.getInputSubscriptions();
        for (var entry : subs.values()) {
            for (var sub : entry) {
                if (sub instanceof simulator.regions.SubscriptionWithRegion swr) {
                    if (swr.getRegion() instanceof marketplace.common.MetricHyperCube mhc) {
                        if (mhc.contains(probe)) return true;
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