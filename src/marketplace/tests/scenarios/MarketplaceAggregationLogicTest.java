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
import marketplace.common.identifiers.RoutingIdentifierFactory;
import marketplace.config.MarketplaceConfig;
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

        // BOOTSTRAP: Wake up the configuration singleton to prepare the cryptography factory
        MarketplaceConfig.get();

        boolean[] flags = {true, true, false, false, true};
        TreeNode dummyBroker = new TreeNode("Test_Broker") {};
        Map<String, Double> dummyMetrics = Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 10.0);

        boolean hypercubePassed = testHypercubeLogic(dummyBroker, dummyMetrics, flags);
        boolean cornerCasesPassed = testHypercubeCornerCases(dummyBroker, dummyMetrics, flags);

        return hypercubePassed && cornerCasesPassed;
    }

    /**
     * Factory Helper: Safely builds a ServiceOffer using the globally configured 
     * cryptographic strategy, ensuring symmetry without relying on a full Provider agent.
     */
    private ServiceOffer buildTestOffer(long oracleId, Map<String, Double> metrics, MetricHyperCube cube, Location loc, String name) {
        RoutingIdentifierFactory cryptoFactory = MarketplaceConfig.get().routingCryptography;
        return new ServiceOffer(
            oracleId, 
            cryptoFactory.createIdentifier(oracleId), 
            metrics, 
            cube, 
            loc, 
            name, 
            1.0
        ) {};
    }

    private MetricHyperCube createTestCluster(double[] cMin, double[] cMax, boolean[] flags, Region phys, Location loc) {
        double[] rMin = new double[cMin.length];
        double[] rMax = new double[cMax.length];
        
        for(int i = 0; i < cMin.length; i++) {
            if (flags[i]) { 
                rMin[i] = cMin[i];
                rMax[i] = 1000.0; 
            } else {        
                rMin[i] = 0.0;    
                rMax[i] = cMax[i];
            }
        }
        
        return new MetricHyperCube(rMin, rMax, cMin, cMax, flags, phys, 10, loc.getX(), loc.getY(), cMin);
    }

    private boolean testHypercubeLogic(TreeNode broker, Map<String, Double> metrics, boolean[] flags) {
        System.out.println("\n--- TESTING HYPERCUBE STORE (TRI-STATE GEOMETRIC FPR) ---");
        
        Location locA = new Location(0, 0, 0);
        Region physA = new Region(locA, new Location(1, 1, 0)); 
        MetricHyperCube cubeA = createTestCluster(
                new double[]{5.0, 50.0, 99.9, 100.0, 10.0}, 
                new double[]{10.0, 55.0, 99.9, 100.0, 10.0}, 
                flags, physA, locA);
        // Utilising the dynamic cryptographic factory helper
        ServiceOffer offerA = buildTestOffer(1001L, metrics, cubeA, locA, "Provider_A");

        MarketplaceRegionStore relaxedStore = new MarketplaceRegionStore(0.25);
        Location locB = new Location(0.5, 0.5, 0);
        Region physB = new Region(locB, new Location(1.5, 1.5, 0));
        MetricHyperCube cubeB = createTestCluster(
                new double[]{8.0, 52.0, 99.9, 100.0, 10.0}, 
                new double[]{12.0, 58.0, 99.9, 100.0, 10.0}, 
                flags, physB, locB);
        ServiceOffer offerB = buildTestOffer(1001L, metrics, cubeB, locB, "Provider_B");

        relaxedStore.addOrUpdate(broker, offerA);
        relaxedStore.addOrUpdate(broker, offerB);
        if (relaxedStore.size() != 1) {
            logger.severe("FAIL (Hypercube Standard): Failed to merge valid overlapping micro-clusters.");
            return false;
        }

        MarketplaceRegionStore qosStrictStore = new MarketplaceRegionStore(0.25);
        MetricHyperCube cubeC = createTestCluster(
                new double[]{150.0, 10.0, 99.9, 100.0, 10.0}, 
                new double[]{160.0, 15.0, 99.9, 100.0, 10.0}, 
                flags, physA, locA);
        ServiceOffer offerC = buildTestOffer(1001L, metrics, cubeC, locA, "Provider_C");

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

        Location locTwin1 = new Location(0, 0, 0);
        Region physTwin1 = new Region(locTwin1, new Location(1, 1, 0));
        MetricHyperCube cubeTwin1 = createTestCluster(
                new double[]{5.0, 50.0, 99.9, 100.0, 10.0}, new double[]{10.0, 55.0, 99.9, 100.0, 10.0}, flags, physTwin1, locTwin1);
        ServiceOffer offerTwin1 = buildTestOffer(1002L, metrics, cubeTwin1, locTwin1, "Twin_London");

        Location locTwin2 = new Location(100, 100, 0);
        Region physTwin2 = new Region(locTwin2, new Location(101, 101, 0));
        MetricHyperCube cubeTwin2 = createTestCluster(
                new double[]{5.0, 50.0, 99.9, 100.0, 10.0}, new double[]{10.0, 55.0, 99.9, 100.0, 10.0}, flags, physTwin2, locTwin2);
        ServiceOffer offerTwin2 = buildTestOffer(1002L, metrics, cubeTwin2, locTwin2, "Twin_Tokyo");

        store.addOrUpdate(broker, offerTwin1);
        store.addOrUpdate(broker, offerTwin2);
        if (store.size() != 2) {
            logger.severe("FAIL (Corner Case A): Broker falsely aggregated distant identical nodes.");
            return false;
        }
        System.out.println("   -> Corner Case A (Pure Spatial FPR Fragmentation): PASS");

        store = new MarketplaceRegionStore(0.25);
        Location locUni1 = new Location(0, 0, 0);
        Region physUni1 = new Region(locUni1, new Location(2, 2, 0));
        MetricHyperCube cubeUni1 = createTestCluster(
                new double[]{5.0, 90.0, 99.9, 100.0, 10.0}, new double[]{10.0, 100.0, 99.9, 100.0, 10.0}, flags, physUni1, locUni1);
        ServiceOffer offerUni1 = buildTestOffer(1003L, metrics, cubeUni1, locUni1, "Uni_Fast");

        Location locUni2 = new Location(3, 3, 0);
        Region physUni2 = new Region(locUni2, new Location(5, 5, 0)); 
        MetricHyperCube cubeUni2 = createTestCluster(
                new double[]{90.0, 5.0, 99.9, 100.0, 10.0}, new double[]{100.0, 10.0, 99.9, 100.0, 10.0}, flags, physUni2, locUni2);
        ServiceOffer offerUni2 = buildTestOffer(1003L, metrics, cubeUni2, locUni2, "Uni_Cheap");

        store.addOrUpdate(broker, offerUni1);
        store.addOrUpdate(broker, offerUni2);
        if (store.size() != 2) {
            logger.severe("FAIL (Corner Case B): Broker fabricated a 5ms for $5 Unicorn region.");
            return false;
        }
        System.out.println("   -> Corner Case B (L-Infinity Multi-Dimensional Unicorn): PASS");

        store = new MarketplaceRegionStore(0.50); 
        Location locCloud = new Location(0, 0, 0);
        Region physCloud = new Region(locCloud, new Location(100, 100, 0)); 
        MetricHyperCube cubeCloud = createTestCluster(
                new double[]{30.0, 10.0, 99.9, 100.0, 10.0}, new double[]{100.0, 20.0, 99.9, 100.0, 10.0}, flags, physCloud, locCloud);
        ServiceOffer offerCloud = buildTestOffer(1004L, metrics, cubeCloud, locCloud, "Cloud_Macro");

        Location locEdge = new Location(50, 50, 0);
        Region physEdge = new Region(locEdge, new Location(51, 51, 0)); 
        MetricHyperCube cubeEdge = createTestCluster(
                new double[]{5.0, 50.0, 99.9, 100.0, 10.0}, new double[]{10.0, 60.0, 99.9, 100.0, 10.0}, flags, physEdge, locEdge);
        ServiceOffer offerEdge = buildTestOffer(1004L, metrics, cubeEdge, locEdge, "Edge_Micro");

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