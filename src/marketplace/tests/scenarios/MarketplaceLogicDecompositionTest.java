package marketplace.tests.scenarios;

import java.util.Map;
import java.util.logging.Logger;
import utils.CustomLogger;

import simulator.core.Location;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import simulator.topology.factories.BoundedBrokerFactory;

import marketplace.agents.AbstractMarketplaceBroker;
import marketplace.agents.MarketplaceClient;
import marketplace.agents.MarketplaceProvider;
import marketplace.topology.MarketplaceBrokerFactory;
import marketplace.common.MarketplaceMetricSchema; 
import marketplace.common.MetricLocation;         

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

    // UNIT 1: OPTIMIZATION
    private boolean verifyPropagationOptimization() {
        System.out.println("\n[Logic Unit 1] Verifying Propagation Optimization (HyperCube Aggregation)...");
        AbstractMarketplaceBroker cloud = createBroker("Cloud_Check", 0, 50);
        AbstractMarketplaceBroker fog   = createBroker("Fog_Check", 0, 25);
        AbstractMarketplaceBroker edge  = createBroker("Edge_Check", 0, 5);
        cloud.addChild(fog);
        fog.addChild(edge);

        // Baseline: A standard Fog node (15.0ms intrinsic) exists in the region
        MarketplaceProvider pBase = new MarketplaceProvider("Fog_Base", new Location(0.5, 0.5, 0));
        edge.addChild(pBase);
        // FIX: advertiseService instead of configureService
        pBase.advertiseService(1001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 15.0), 3.0);
        
        // Probe exactly where the provider is located
        if (!probeBrokerState(cloud, 15.0, new Location(0.5, 0.5, 0))) return fail("Baseline failed. Cloud broker did not receive 15.0ms bound.");
        System.out.println("   -> Baseline established (15.0ms).");
        
        // Improvement: A lightning-fast Edge WASM node (5.0ms intrinsic) comes online
        MarketplaceProvider pBetter = new MarketplaceProvider("Edge_Better", new Location(0.5, 0.5, 0));
        edge.addChild(pBetter);
        // FIX: advertiseService instead of configureService
        pBetter.advertiseService(1001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 5.0), 0.25);
        
        if (!probeBrokerState(cloud, 5.0, new Location(0.5, 0.5, 0))) return fail("Improvement failed. Cloud HyperCube did not optimize to 5.0ms.");
        System.out.println("   -> Improvement Verified. HyperCube successfully aggregated the dominant metric.");
        return true;
    }

    // UNIT 2: INTERSECTION & PROPAGATION
    private boolean verifyIntersectionLogic() {
        System.out.println("\n[Logic Unit 2] Verifying Intersection & Propagation Logic (Spatial Isolation)...");

        AbstractMarketplaceBroker cloud = createBroker("Cloud_Int", 0, 60);
        AbstractMarketplaceBroker fogLocal = createBroker("Fog_Local", 0, 10);
        
        // Move Remote Fog closer so a realistic 20-degree Cloud can intersect it
        AbstractMarketplaceBroker fogRemote = createBroker("Fog_Remote", 30, 40); 
        cloud.addChild(fogLocal);
        cloud.addChild(fogRemote);

        // Case A: Edge Provider (Local) -> Should NOT reach Remote Fog
        System.out.println("   -> Case A: Testing Edge/Local Spatial Isolation...");
        MarketplaceProvider pLocal = new MarketplaceProvider("Edge_Local_Prov", new Location(5.0, 5.0, 0));
        fogLocal.addChild(pLocal);
        
        // Strict Metro boundary (0.25)
        pLocal.advertiseService(9001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 5.0), 0.25);
        
        if (!probeBrokerState(cloud, 5.0, new Location(5.0, 5.0, 0))) return fail("Upward propagation (Local) failed.");
        if (probeBrokerState(fogRemote, 5.0, new Location(5.0, 5.0, 0))) return fail("Isolation Failed. Remote broker saw localized Edge offer.");
        System.out.println("   -> Isolation Verified. Edge offer was correctly contained.");

        // Case B: Cloud Provider (Global) -> Should intersect and reach Remote Fog
        System.out.println("   -> Case B: Testing Cloud/Country Spatial Reach...");
        
        // Place Cloud provider at (15, 15). With a 20.0 radius, coverage is [-5.0 to 35.0].
        // This successfully intersects the fogRemote region of [30, 40].
        MarketplaceProvider pGlobal = new MarketplaceProvider("Cloud_Global_Prov", new Location(15.0, 15.0, 0));
        cloud.addChild(pGlobal); 
        
        // Enforce the strict 20.0 degree simulation maximum
        pGlobal.advertiseService(9002, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 25.0), 20.0);
        
        // Probe at (32.0, 32.0) which is inside both fogRemote and the Cloud's coverage area
        if (!probeBrokerState(fogRemote, 25.0, new Location(32.0, 32.0, 0))) return fail("Propagation Failed. Remote broker missing Cloud offer.");
        System.out.println("   -> Propagation Verified. Cloud offer successfully spanned the topology.");
        return true;
    }

    // UNIT 3: ROUTING
    private boolean verifyRoutingPaths() {
        System.out.println("\n[Logic Unit 3] Verifying Routing Paths (Cross-Branch Stratification)...");

        AbstractMarketplaceBroker cloud = createBroker("Cloud", 0, 50);
        
        AbstractMarketplaceBroker fogWest = createBroker("Fog_West", 0, 10);
        AbstractMarketplaceBroker edgeWest = createBroker("Edge_West", 5, 10);
        cloud.addChild(fogWest);
        fogWest.addChild(edgeWest);

        AbstractMarketplaceBroker fogEast = createBroker("Fog_East", 10, 30);
        AbstractMarketplaceBroker edgeEast = createBroker("Edge_East", 10, 20);
        cloud.addChild(fogEast);
        fogEast.addChild(edgeEast);

        // --- THE SUPPLY TIERS ---

        // P1 (CLOUD): Total Latency = 20 + (~22.6 dist * 1.665) = 57.6ms. Cost ($5).
        MarketplaceProvider pCloud = new MarketplaceProvider("AWS_Cloud_East", new Location(25.0, 25.0, 0));
        cloud.addChild(pCloud);
        pCloud.advertiseService(1001, Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, 20.0, 
            MarketplaceMetricSchema.METRIC_COST, 5.0
        ), 20.0); // Realistic 20.0 limit

        // P2 (EDGE): Total Latency = 5 + 0 = 5.0ms. Cost ($50).
        MarketplaceProvider pWest = new MarketplaceProvider("Metro_Edge_Local", new Location(9.0, 9.0, 0));
        edgeWest.addChild(pWest);
        pWest.advertiseService(1001, Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, 5.0, 
            MarketplaceMetricSchema.METRIC_COST, 50.0
        ), 0.25); 

        // P3 (FOG): Total Latency = 15 + (~2.8 dist * 1.665) = 19.7ms. Cost ($15).
        MarketplaceProvider pEast = new MarketplaceProvider("Telco_Fog_Remote", new Location(11.0, 11.0, 0));
        edgeEast.addChild(pEast);
        pEast.advertiseService(1001, Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, 15.0, 
            MarketplaceMetricSchema.METRIC_COST, 15.0
        ), 3.0); 

        // Client is placed near the border at (9.0, 9.0)
        MarketplaceClient client = new MarketplaceClient("Client", new Location(9.0, 9.0, 0));
        edgeWest.addChild(client);
        
        // --- THE DEMAND CLASSES ---

        // A. Latency < 10. Only Edge (5.0ms) passes.
        client.requestService(1001, Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 10.0));
        if (pWest.getnPublications() != 1) return fail("Class 1 Edge Routing failed.");
        System.out.println("   -> Case A (Edge/Local Routing): PASS");

        // B. Cost < 10. Only Cloud ($5) passes.
        client.requestService(1001, Map.of(MarketplaceMetricSchema.METRIC_COST, 10.0));
        if (pCloud.getnPublications() != 1) return fail("Class 2 Cloud Routing failed.");
        System.out.println("   -> Case B (Cloud/Global Routing): PASS");

        // C. Cross-Branch. Latency < 30, Cost < 20. 
        // Cloud fails latency (57.6ms). Edge fails cost ($50). 
        // Fog perfectly passes both (19.7ms, $15).
        client.requestService(1001, Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, 30.0, 
            MarketplaceMetricSchema.METRIC_COST, 20.0
        ));
        if (pEast.getnPublications() != 1) return fail("Class 3 Fog Cross-Branch Routing failed.");
        System.out.println("   -> Case C (Fog/Cross-Branch Routing): PASS");

        return true;
    }

    private AbstractMarketplaceBroker createBroker(String name, double min, double max) {
        BoundedBrokerFactory factory = new MarketplaceBrokerFactory();
        return (AbstractMarketplaceBroker) factory.createLeafBroker(name, 
            new Location(min, min, 0), new Location(max, max, 0));
    }

    // REFACTORED: src.zip/marketplace/tests/scenarios/MarketplaceLogicDecompositionTest.java

    private boolean probeBrokerState(AbstractMarketplaceBroker broker, double expectedMinLatency, Location probeLoc) {
        double[] metrics = new double[MarketplaceMetricSchema.KEYS.length];
        
        // Fill un-probed dimensions with exact defaults so they fall inside the interval bounds
        for (int i = 0; i < metrics.length; i++) {
            String key = MarketplaceMetricSchema.KEYS[i];
            boolean minimize = MarketplaceMetricSchema.DIRECTIONS.get(key);
            // Default to the extreme bounds of the System Limits
            metrics[i] = minimize ? MarketplaceMetricSchema.getSystemMax(key) : 0.0;
        }

        // Apply the specific latency probe we are testing for
        int latIdx = MarketplaceMetricSchema.getIndexOf(MarketplaceMetricSchema.METRIC_LATENCY);
        if (latIdx != -1) {
            metrics[latIdx] = expectedMinLatency + 0.0001; 
        }

        MetricLocation probe = new MetricLocation(metrics, probeLoc);
        
        var subs = broker.getInputSubscriptions();
        for (var entry : subs.values()) {
            for (var sub : entry) {
                if (sub instanceof simulator.regions.SubscriptionWithRegion swr) {
                    if (swr.getRegion() instanceof marketplace.common.MetricHyperCube mhc) {
                        if (mhc.contains(probe)) return true;
                    } else if (swr.getRegion() instanceof marketplace.common.SkylineHyperCube shc) {
                        if (shc.contains(probe)) return true;
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