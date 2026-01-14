package simulator.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.tests.fixtures.*;
import simulator.tests.framework.*;

// --- REGION IMPORTS ---
import simulator.tests.scenarios.region.logic.*;
import simulator.tests.scenarios.region.e2e.*;

// --- PROXIMITY IMPORTS ---
import simulator.tests.scenarios.proximity.logic.*;
import simulator.tests.scenarios.proximity.e2e.*;

import simulator.topology.factories.BrokerFactory;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import utils.CustomLogger;

public class RegressionSuiteRunner {
    private static final Logger logger = CustomLogger.getLogger(RegressionSuiteRunner.class.getName());

    public static void main(String[] args) {
        logger.info(">>> STARTING GLOBAL REGRESSION SUITE <<<");

        // --- GLOBAL CONFIGURATION ---
        // Explicitly disable event tracing. 
        SimConfiguration.get().paths.enableEventTracing = false;

        // --- FACTORY SETUP ---
        List<FactorySetup> regionalSetups = new ArrayList<>();
        // 1. SIMPLE: Forces Single Region Storage
        regionalSetups.add(new FactorySetup("REGIONAL-SIMPLE", new SpatialMatchBrokerFactory(true, 0.0, true)));
        // 2. SMART: Uses Multi-Region Storage with merging
        regionalSetups.add(new FactorySetup("REGIONAL-SMART", new SpatialMatchBrokerFactory(false, 0.5, true)));

        List<FactorySetup> proximitySetups = new ArrayList<>();
        // 3. PROXIMITY: Uses Location-based Storage
        proximitySetups.add(new FactorySetup("PROXIMITY", new LocationBrokerFactory()));

        // =================================================================
        // PHASE 0: PURE UNIT TESTS (Run Once)
        // =================================================================
        logger.info("\n=== PHASE 0: Pure Unit Tests (Math/Geometry) ===");
        // Run once, independent of broker strategy
        new RegionFloatingPointTest().run(null);


        // =================================================================
        // PHASE 1: LOGIC & MECHANISM VERIFICATION (Fixed Topology)
        // =================================================================
        logger.info("\n=== PHASE 1: Broker Logic & Mechanism Verification ===");

        // 1.1 Region Logic
        List<TestScenario> regionLogic = new ArrayList<>();
        regionLogic.add(new RegionCoveringLogicTest());
        regionLogic.add(new RegionExpansionLogicTest());
        
        runBatch(new FixedTopologyFixture(), regionLogic, regionalSetups);

        // 1.2 Proximity Logic
        List<TestScenario> proxLogic = new ArrayList<>();
        proxLogic.add(new ProximityAggregationLogicTest());
        proxLogic.add(new ProximityPropagationLogicTest());
        proxLogic.add(new ProximityStateResetLogicTest());
        proxLogic.add(new ProximityBrakeLogicTest());

        runBatch(new FixedTopologyFixture(), proxLogic, proximitySetups);


        // =================================================================
        // PHASE 2: END-TO-END SYSTEM TESTS (Simple Fixed Topology)
        // =================================================================
        logger.info("\n=== PHASE 2: System Sanity Checks (Fixed Topology) ===");

        List<TestScenario> regionFixedE2E = new ArrayList<>();
        regionFixedE2E.add(new RegionFixedTopologyE2ETest());
        runBatch(new FixedTopologyFixture(), regionFixedE2E, regionalSetups);

        List<TestScenario> proxFixedE2E = new ArrayList<>();
        proxFixedE2E.add(new ProximityFixedTopologyE2ETest());
        runBatch(new FixedTopologyFixture(), proxFixedE2E, proximitySetups);


        // =================================================================
        // PHASE 3: END-TO-END SYSTEM TESTS (Complex Topologies)
        // =================================================================
        logger.info("\n=== PHASE 3: Complex Topology Integration ===");

        // 3.1 GRID
        List<TestScenario> regionGridE2E = new ArrayList<>();
        regionGridE2E.add(new RegionGridTopologyE2ETest());
        runBatch(new GridTopologyFixture(), regionGridE2E, regionalSetups);

        List<TestScenario> proxGridE2E = new ArrayList<>();
        proxGridE2E.add(new ProximityGridTopologyE2ETest());
        runBatch(new GridTopologyFixture(), proxGridE2E, proximitySetups);

        // 3.2 GEONAMES
        List<TestScenario> regionGeoE2E = new ArrayList<>();
        regionGeoE2E.add(new RegionGeoNamesTopologyE2ETest());
        runBatch(new GeoNamesTopologyFixture(), regionGeoE2E, regionalSetups);

        List<TestScenario> proxGeoE2E = new ArrayList<>();
        proxGeoE2E.add(new ProximityGeoNamesTopologyE2ETest());
        runBatch(new GeoNamesTopologyFixture(), proxGeoE2E, proximitySetups);
    }

    record FactorySetup(String name, BrokerFactory factory) {}

    private static void runBatch(TopologyFixture fixture, List<TestScenario> scenarios, List<FactorySetup> setups) {
        for (FactorySetup setup : setups) {
            logger.info(String.format("\n--- [Fixture: %s] | [Algorithm: %s] ---", fixture.getName(), setup.name()));
            
            for (TestScenario test : scenarios) {
                try {
                    fixture.setup(setup.factory());
                    long start = System.currentTimeMillis();
                    boolean result = test.run(fixture);
                    long duration = System.currentTimeMillis() - start;

                    if (result) {
                        logger.info(String.format("  [PASS] %-45s (%d ms)", test.getTestName(), duration));
                    } else {
                        logger.severe(String.format("  [FAIL] %-45s", test.getTestName()));
                    }
                } catch (Exception e) {
                    logger.severe("  [ERROR] " + test.getTestName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}