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
import marketplace.common.SkylineHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.topology.store.AbstractMarketplaceRegionStore;
import marketplace.topology.store.HypercubeRegionStore;
import marketplace.topology.store.SkylineRegionStore;

public class MarketplaceAggregationLogicTest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(MarketplaceAggregationLogicTest.class.getName());

    @Override
    public String getTestName() {
        return "Marketplace Logic: Hypercube FPR & Skyline Pareto/Tier Isolation";
    }

    @Override
    public boolean run(TopologyFixture ignoredFixture) {
        System.out.println(">>> STARTING LOGIC TEST: POLYMORPHIC AGGREGATION RULES");

        boolean[] flags = {true, true, false, false, true};
        TreeNode dummyBroker = new TreeNode("Test_Broker") {};
        Map<String, Double> dummyMetrics = Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 10.0);

        boolean hypercubePassed = testHypercubeLogic(dummyBroker, dummyMetrics, flags);
        boolean skylinePassed = testSkylineLogic(dummyBroker, dummyMetrics, flags);

        return hypercubePassed && skylinePassed;
    }

    private boolean testHypercubeLogic(TreeNode broker, Map<String, Double> metrics, boolean[] flags) {
        System.out.println("\n--- TESTING HYPERCUBE STORE (FPR DILUTION) ---");
        
        Location locA = new Location(0, 0, 0);
        Region physA = new Region(locA, new Location(1, 1, 0)); 
        MetricHyperCube cubeA = new MetricHyperCube(
                new double[]{5.0, 5.0, 99.9, 100.0, 10.0}, new double[]{5.0, 5.0, 99.9, 100.0, 10.0}, flags, physA);
        ServiceOffer offerA = new ServiceOffer(1001L, metrics, cubeA, locA, "Provider_A", 1.0) {};

        // TEST 1: SUCCESSFUL MERGE (Tight Spatial, Tight QoS)
        AbstractMarketplaceRegionStore relaxedStore = new HypercubeRegionStore(0.50);
        Location locB = new Location(0.5, 0.5, 0);
        Region physB = new Region(locB, new Location(1.5, 1.5, 0));
        MetricHyperCube cubeB = new MetricHyperCube(
                new double[]{6.0, 6.0, 99.9, 100.0, 10.0}, new double[]{6.0, 6.0, 99.9, 100.0, 10.0}, flags, physB);
        ServiceOffer offerB = new ServiceOffer(1001L, metrics, cubeB, locB, "Provider_B", 1.0) {};

        relaxedStore.addOrUpdate(broker, offerA);
        relaxedStore.addOrUpdate(broker, offerB);
        if (relaxedStore.size() != 1) {
            logger.severe("FAIL (Hypercube): Failed to merge within FPR threshold.");
            return false;
        }

        // TEST 2: REJECTED MERGE (SLA Violation / High QoS FPR)
        AbstractMarketplaceRegionStore qosStrictStore = new HypercubeRegionStore(0.20);
        MetricHyperCube cubeC = new MetricHyperCube(
                new double[]{20.0, 2.0, 99.9, 100.0, 10.0}, new double[]{20.0, 2.0, 99.9, 100.0, 10.0}, flags, physA);
        ServiceOffer offerC = new ServiceOffer(1001L, metrics, cubeC, locA, "Provider_C", 1.0) {};

        qosStrictStore.addOrUpdate(broker, offerA);
        qosStrictStore.addOrUpdate(broker, offerC);
        if (qosStrictStore.size() != 2) {
            logger.severe("FAIL (Hypercube): Falsely merged bad QoS, violating strict FPR.");
            return false;
        }

        System.out.println("PASS: Hypercube Logic verified.");
        return true;
    }

    private boolean testSkylineLogic(TreeNode broker, Map<String, Double> metrics, boolean[] flags) {
        System.out.println("\n--- TESTING SKYLINE STORE (PARETO & TIER ISOLATION) ---");
        
        Location locEdge = new Location(10, 10, 0);
        Region physEdge = new Region(locEdge, new Location(20, 20, 0)); // Area = 100
        SkylineHyperCube skyEdge = new SkylineHyperCube(new double[]{10.0, 50.0, 99.9, 100.0, 10.0}, flags, physEdge);
        ServiceOffer offerEdge = new ServiceOffer(1001L, metrics, skyEdge, locEdge, "Provider_Edge", 1.0) {};

        // TEST 1: SAME-TIER PARETO DOMINATION
        AbstractMarketplaceRegionStore skylineStore = new SkylineRegionStore(0.0);
        
        // FIX: Force Edge2 to perfectly overlap Edge1 to achieve a Spatial FPR of 0.0
        Location locEdge2 = locEdge; 
        Region physEdge2 = new Region(locEdge, new Location(20, 20, 0)); 
        
        // Edge2 is strictly better (Lat: 8.0 < 10.0, Cost: 40.0 < 50.0)
        SkylineHyperCube skyEdgeBetter = new SkylineHyperCube(new double[]{8.0, 40.0, 99.9, 100.0, 10.0}, flags, physEdge2);
        ServiceOffer offerEdgeBetter = new ServiceOffer(1001L, metrics, skyEdgeBetter, locEdge2, "Provider_Edge_2", 1.0) {};

        skylineStore.addOrUpdate(broker, offerEdge);
        skylineStore.addOrUpdate(broker, offerEdgeBetter);
        
        // Should merge into 1 node because Edge2 dominates Edge1 and area ratio is ~1.0
        if (skylineStore.size() != 1) {
            logger.severe("FAIL (Skyline): Failed to pareto-compress same-tier nodes.");
            return false;
        }

        // TEST 2: MACRO SWALLOWING MICRO (Tier Isolation Enforcement)
        skylineStore = new SkylineRegionStore(0.0);
        Location locCloud = new Location(0, 0, 0);
        Region physCloud = new Region(locCloud, new Location(1000, 1000, 0)); // Area = 1,000,000
        // Cloud completely dominates Edge in QoS and physically overlaps it fully
        SkylineHyperCube skyCloud = new SkylineHyperCube(new double[]{5.0, 10.0, 99.99, 1000.0, 10.0}, flags, physCloud);
        ServiceOffer offerCloud = new ServiceOffer(1001L, metrics, skyCloud, locCloud, "Provider_Cloud", 1.0) {};

        skylineStore.addOrUpdate(broker, offerEdge);
        skylineStore.addOrUpdate(broker, offerCloud);

        // Area Ratio is 1,000,000 / 100 = 10,000 (Way over 25.0). Must strictly isolate!
        if (skylineStore.size() != 2) {
            logger.severe("FAIL (Skyline): Cloud swallowed the Edge node! Tier isolation failed. Found: " + skylineStore.size());
            return false;
        }

        System.out.println("PASS: Skyline Logic and Tier-Isolation verified.");
        return true;
    }
}