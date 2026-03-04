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
        return "Marketplace Logic: Geometric FPR & QoS Isolation";
    }

    @Override
    public boolean run(TopologyFixture ignoredFixture) {
        System.out.println(">>> STARTING LOGIC TEST: POLYMORPHIC AGGREGATION RULES");

        boolean[] flags = {true, true, false, false, true};
        TreeNode dummyBroker = new TreeNode("Test_Broker") {};
        Map<String, Double> dummyMetrics = Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 10.0);

        boolean hypercubePassed = testHypercubeLogic(dummyBroker, dummyMetrics, flags);
        boolean cornerCasesPassed = testHypercubeCornerCases(dummyBroker, dummyMetrics, flags);

        return hypercubePassed && cornerCasesPassed;
    }

    /**
     * Helper to correctly instantiate a Tri-State Aggregated Cluster for testing FPR logic.
     */
    private MetricHyperCube createTestCluster(double[] cMin, double[] cMax, boolean[] flags, Region phys, Location loc) {
        double[] rMin = new double[cMin.length];
        double[] rMax = new double[cMax.length];
        
        for(int i = 0; i < cMin.length; i++) {
            if (flags[i]) { // Minimize Metric
                rMin[i] = cMin[i];
                rMax[i] = 1000.0; // Simulated System Max for the Routing Envelope
            } else {        // Maximize Metric
                rMin[i] = 0.0;    // Simulated System Min for the Routing Envelope
                rMax[i] = cMax[i];
            }
        }
        
        // Uses the Tri-State Internal Aggregation Constructor:
        // (rMin, rMax, cMin, cMax, flags, physicalRegion, providerWeight, densityX, densityY, qosCenterOfMass)
        return new MetricHyperCube(rMin, rMax, cMin, cMax, flags, phys, 10, loc.getX(), loc.getY(), cMin);
    }

    private boolean testHypercubeLogic(TreeNode broker, Map<String, Double> metrics, boolean[] flags) {
        System.out.println("\n--- TESTING HYPERCUBE STORE (TRI-STATE GEOMETRIC FPR) ---");
        
        Location locA = new Location(0, 0, 0);
        Region physA = new Region(locA, new Location(1, 1, 0)); 
        // Cluster A: Latency [5, 10], Cost [50, 55]
        MetricHyperCube cubeA = createTestCluster(
                new double[]{5.0, 50.0, 99.9, 100.0, 10.0}, 
                new double[]{10.0, 55.0, 99.9, 100.0, 10.0}, 
                flags, physA, locA);
        ServiceOffer offerA = new ServiceOffer(1001L, metrics, cubeA, locA, "Provider_A", 1.0) {};

        // TEST 1: SUCCESSFUL MERGE (Tight Spatial, Overlapping QoS)
        // Store allows 25% False Positive Rate (Dead Space)
        MarketplaceRegionStore relaxedStore = new MarketplaceRegionStore(0.25);
        Location locB = new Location(0.5, 0.5, 0);
        Region physB = new Region(locB, new Location(1.5, 1.5, 0));
        // Cluster B: Latency [8, 12], Cost [52, 58] -> Merged Latency [5,12] with 0 dead space.
        MetricHyperCube cubeB = createTestCluster(
                new double[]{8.0, 52.0, 99.9, 100.0, 10.0}, 
                new double[]{12.0, 58.0, 99.9, 100.0, 10.0}, 
                flags, physB, locB);
        ServiceOffer offerB = new ServiceOffer(1001L, metrics, cubeB, locB, "Provider_B", 1.0) {};

        relaxedStore.addOrUpdate(broker, offerA);
        relaxedStore.addOrUpdate(broker, offerB);
        if (relaxedStore.size() != 1) {
            logger.severe("FAIL (Hypercube Standard): Failed to merge valid overlapping micro-clusters.");
            return false;
        }

        // TEST 2: REJECTED MERGE (Capability FPR Violation)
        MarketplaceRegionStore qosStrictStore = new MarketplaceRegionStore(0.25);
        
        // FIXED: Make Cluster C cheaper [10, 15] so it is not Pareto-dominated by A.
        // It will now bypass the filter and correctly fail the Merge evaluation due to the Latency gap.
        MetricHyperCube cubeC = createTestCluster(
                new double[]{150.0, 10.0, 99.9, 100.0, 10.0}, 
                new double[]{160.0, 15.0, 99.9, 100.0, 10.0}, 
                flags, physA, locA);
        ServiceOffer offerC = new ServiceOffer(1001L, metrics, cubeC, locA, "Provider_C", 1.0) {};

        qosStrictStore.addOrUpdate(broker, offerA);
        qosStrictStore.addOrUpdate(broker, offerC);
        
        if (qosStrictStore.size() != 2) {
            logger.severe("FAIL (Hypercube Standard): Falsely merged bad QoS, violating strict FPR limit.");
            return false;
        }

        System.out.println("PASS: Hypercube Standard Logic verified.");
        return true;
    }

    private boolean testHypercubeCornerCases(TreeNode broker, Map<String, Double> metrics, boolean[] flags) {
        System.out.println("\n--- TESTING HYPERCUBE STORE (GEOMETRIC CORNER CASES) ---");
        MarketplaceRegionStore store = new MarketplaceRegionStore(0.25);

        // CORNER CASE A: The "Identical Twins, Oceans Apart" (Pure Spatial Rejection)
        // Two nodes have identical capability ranges, but are 100 geographic units apart.
        Location locTwin1 = new Location(0, 0, 0);
        Region physTwin1 = new Region(locTwin1, new Location(1, 1, 0));
        MetricHyperCube cubeTwin1 = createTestCluster(
                new double[]{5.0, 50.0, 99.9, 100.0, 10.0}, new double[]{10.0, 55.0, 99.9, 100.0, 10.0}, flags, physTwin1, locTwin1);
        ServiceOffer offerTwin1 = new ServiceOffer(1002L, metrics, cubeTwin1, locTwin1, "Twin_London", 1.0) {};

        Location locTwin2 = new Location(100, 100, 0);
        Region physTwin2 = new Region(locTwin2, new Location(101, 101, 0));
        MetricHyperCube cubeTwin2 = createTestCluster(
                new double[]{5.0, 50.0, 99.9, 100.0, 10.0}, new double[]{10.0, 55.0, 99.9, 100.0, 10.0}, flags, physTwin2, locTwin2);
        ServiceOffer offerTwin2 = new ServiceOffer(1002L, metrics, cubeTwin2, locTwin2, "Twin_Tokyo", 1.0) {};

        store.addOrUpdate(broker, offerTwin1);
        store.addOrUpdate(broker, offerTwin2);
        if (store.size() != 2) {
            logger.severe("FAIL (Corner Case A): Broker falsely aggregated distant identical nodes.");
            return false;
        }
        System.out.println("   -> Corner Case A (Pure Spatial FPR Fragmentation): PASS");

        // CORNER CASE B: The "Multi-Dimensional Unicorn" (Cross-Metric Asymmetry)
        store = new MarketplaceRegionStore(0.25);
        Location locUni1 = new Location(0, 0, 0);
        Region physUni1 = new Region(locUni1, new Location(2, 2, 0));
        // Node 1: Fast (5-10ms) but Expensive ($90-100)
        MetricHyperCube cubeUni1 = createTestCluster(
                new double[]{5.0, 90.0, 99.9, 100.0, 10.0}, new double[]{10.0, 100.0, 99.9, 100.0, 10.0}, flags, physUni1, locUni1);
        ServiceOffer offerUni1 = new ServiceOffer(1003L, metrics, cubeUni1, locUni1, "Uni_Fast", 1.0) {};

        Location locUni2 = new Location(3, 3, 0);
        Region physUni2 = new Region(locUni2, new Location(5, 5, 0)); 
        // Node 2: Slow (90-100ms) but Cheap ($5-10)
        MetricHyperCube cubeUni2 = createTestCluster(
                new double[]{90.0, 5.0, 99.9, 100.0, 10.0}, new double[]{100.0, 10.0, 99.9, 100.0, 10.0}, flags, physUni2, locUni2);
        ServiceOffer offerUni2 = new ServiceOffer(1003L, metrics, cubeUni2, locUni2, "Uni_Cheap", 1.0) {};

        store.addOrUpdate(broker, offerUni1);
        store.addOrUpdate(broker, offerUni2);
        if (store.size() != 2) {
            logger.severe("FAIL (Corner Case B): Broker fabricated a 5ms for $5 Unicorn region.");
            return false;
        }
        System.out.println("   -> Corner Case B (L-Infinity Multi-Dimensional Unicorn): PASS");

        // CORNER CASE C: The "Trojan Horse" (Macro Swallows Micro / Tier Isolation)
        store = new MarketplaceRegionStore(0.50); // Highly relaxed 50% threshold
        Location locCloud = new Location(0, 0, 0);
        Region physCloud = new Region(locCloud, new Location(100, 100, 0)); // Area 10,000
        // Cloud: Slow [30, 100], Cheap [10, 20]
        MetricHyperCube cubeCloud = createTestCluster(
                new double[]{30.0, 10.0, 99.9, 100.0, 10.0}, new double[]{100.0, 20.0, 99.9, 100.0, 10.0}, flags, physCloud, locCloud);
        ServiceOffer offerCloud = new ServiceOffer(1004L, metrics, cubeCloud, locCloud, "Cloud_Macro", 1.0) {};

        Location locEdge = new Location(50, 50, 0);
        Region physEdge = new Region(locEdge, new Location(51, 51, 0)); // Inside cloud geography
        // Edge: Fast [5, 10], Expensive [50, 60] -> Creates a massive 60% FPR Cost Gap when merged with Cloud
        MetricHyperCube cubeEdge = createTestCluster(
                new double[]{5.0, 50.0, 99.9, 100.0, 10.0}, new double[]{10.0, 60.0, 99.9, 100.0, 10.0}, flags, physEdge, locEdge);
        ServiceOffer offerEdge = new ServiceOffer(1004L, metrics, cubeEdge, locEdge, "Edge_Micro", 1.0) {};

        store.addOrUpdate(broker, offerCloud);
        store.addOrUpdate(broker, offerEdge);
        if (store.size() != 2) {
            logger.severe("FAIL (Corner Case C): Cloud macro-region swallowed Edge micro-region despite Cost FPR.");
            return false;
        }
        System.out.println("   -> Corner Case C (Tier Isolation over Spatial Dominance): PASS");

        return true;
    }
}