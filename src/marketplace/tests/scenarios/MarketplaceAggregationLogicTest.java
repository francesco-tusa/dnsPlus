package marketplace.tests.scenarios;

import java.util.Map;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.regions.Region;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import utils.CustomLogger;

import marketplace.common.MarketplaceMetricSchema;
import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.topology.store.MarketplaceRegionStore;

public class MarketplaceAggregationLogicTest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceAggregationLogicTest.class.getName());

    @Override
    public String getTestName() {
        return "Marketplace Logic: Decoupled FPR Aggregation Threshold";
    }

    @Override
    public boolean run(TopologyFixture ignoredFixture) {
        System.out.println(">>> STARTING LOGIC TEST: DECOUPLED FPR AGGREGATION THRESHOLD");

        // Common Configuration
        // Latency (min), Cost (min), Reliability (max), Bandwidth (max), Energy (min)
        boolean[] flags = {true, true, false, false, true};
        TreeNode dummyBroker = new TreeNode("Florida_Broker") {};
        
        // Dummy metrics map to satisfy the ServiceOffer constructor
        Map<String, Double> dummyMetrics = Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 10.0);

        // ====================================================================
        // BASE OFFER SETUP (Provider A)
        // ====================================================================
        Location locA = new Location(0, 0, 0);
        Region physA = new Region(locA, new Location(1, 1, 0)); 
        
        MetricHyperCube cubeA = new MetricHyperCube(
                new double[]{5.0, 5.0, 99.9, 100.0, 10.0}, 
                new double[]{5.0, 5.0, 99.9, 100.0, 10.0}, 
                flags, physA);
        
        // Using anonymous subclass to access protected constructor if necessary
        ServiceOffer offerA = new ServiceOffer(1001L, dummyMetrics, cubeA, locA, "Provider_A", 1.0) {};

        // --------------------------------------------------------------------------------
        // TEST 1: SUCCESSFUL MERGE (Tight Spatial, Tight QoS)
        // --------------------------------------------------------------------------------
        System.out.println("   -> Testing Valid Merge (Low Spatial FPR, Low QoS FPR)...");
        MarketplaceRegionStore relaxedStore = new MarketplaceRegionStore(0.50);
        
        // Provider B: Physically overlaps and has very similar QoS metrics
        Location locB = new Location(0.5, 0.5, 0);
        Region physB = new Region(locB, new Location(1.5, 1.5, 0));
        MetricHyperCube cubeB = new MetricHyperCube(
                new double[]{6.0, 6.0, 99.9, 100.0, 10.0}, 
                new double[]{6.0, 6.0, 99.9, 100.0, 10.0}, 
                flags, physB);
                
        ServiceOffer offerB = new ServiceOffer(1001L, dummyMetrics, cubeB, locB, "Provider_B", 1.0) {};

        relaxedStore.addOrUpdate(dummyBroker, offerA);
        relaxedStore.addOrUpdate(dummyBroker, offerB);
        
        if (relaxedStore.size() != 1) {
            logger.severe("FAIL: Store failed to merge similar offers! Expected 1 entry, found " + relaxedStore.size());
            return false;
        }
        System.out.println("      PASS: Store merged offers successfully within acceptable FPR threshold.");

        // --------------------------------------------------------------------------------
        // TEST 2: REJECTED MERGE (SLA Violation / High QoS FPR)
        // --------------------------------------------------------------------------------
        System.out.println("   -> Testing QoS Dilution Rejection (Tight Spatial, Bad QoS)...");
        MarketplaceRegionStore qosStrictStore = new MarketplaceRegionStore(0.20);
        
        // Provider C: Exact same physical location as A, but Latency jumps to 20ms (High QoS stretch)
        MetricHyperCube cubeC = new MetricHyperCube(
                new double[]{20.0, 2.0, 99.9, 100.0, 10.0}, 
                new double[]{20.0, 2.0, 99.9, 100.0, 10.0}, 
                flags, physA);
                
        ServiceOffer offerC = new ServiceOffer(1001L, dummyMetrics, cubeC, locA, "Provider_C", 1.0) {};

        qosStrictStore.addOrUpdate(dummyBroker, offerA);
        qosStrictStore.addOrUpdate(dummyBroker, offerC);
        
        if (qosStrictStore.size() != 2) {
            logger.severe("FAIL: Store falsely merged offers with bad QoS! Expected 2 distinct entries, found " + qosStrictStore.size());
            return false;
        }
        System.out.println("      PASS: Store rejected merge because QoS SLA dilution exceeded threshold.");

        // --------------------------------------------------------------------------------
        // TEST 3: REJECTED MERGE (Physical Dead Zone / High Spatial FPR)
        // --------------------------------------------------------------------------------
        System.out.println("   -> Testing Spatial Dilution Rejection (Tight QoS, Bad Spatial)...");
        MarketplaceRegionStore spaceStrictStore = new MarketplaceRegionStore(0.20);
        
        // Provider D: Exact same QoS as A, but located far away (Creates a massive physical dead zone)
        Location locD = new Location(100, 100, 0);
        Region physD = new Region(locD, new Location(101, 101, 0));
        MetricHyperCube cubeD = new MetricHyperCube(
                new double[]{5.0, 5.0, 99.9, 100.0, 10.0}, 
                new double[]{5.0, 5.0, 99.9, 100.0, 10.0}, 
                flags, physD);
                
        ServiceOffer offerD = new ServiceOffer(1001L, dummyMetrics, cubeD, locD, "Provider_D", 1.0) {};

        spaceStrictStore.addOrUpdate(dummyBroker, offerA);
        spaceStrictStore.addOrUpdate(dummyBroker, offerD);
        
        if (spaceStrictStore.size() != 2) {
            logger.severe("FAIL: Store falsely merged offers with large spatial gap! Expected 2 distinct entries, found " + spaceStrictStore.size());
            return false;
        }
        System.out.println("      PASS: Store rejected merge because Spatial/Physical dilution exceeded threshold.");

        System.out.println(">>> FPR AGGREGATION LOGIC VERIFIED AND PASSED");
        return true;
    }
}