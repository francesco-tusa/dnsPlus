package simulator.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import simulator.tests.fixtures.*;
import simulator.tests.framework.*;
import simulator.tests.scenarios.*;
import simulator.topology.factories.BrokerFactory;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import utils.CustomLogger;

public class RegressionSuiteRunner {
    private static final Logger logger = CustomLogger.getLogger(RegressionSuiteRunner.class.getName());

    public static void main(String[] args) {
        logger.info(">>> STARTING GLOBAL REGRESSION SUITE <<<");

        // 0. Unit Tests
        logger.info("\n=== Unit Tests ===");
        new RegionFloatingPointTest().run(null);

        // 1. Run Region-Based Suite
        runRegionSuite();

        // 2. Run Location-Based Suite
        runLocationSuite();
    }

    // --- Suite 1: Region Logic (SpatialMatchBroker) ---
    private static void runRegionSuite() {
        logger.info("\n\n################################################");
        logger.info("### RUNNING REGION-BASED REGRESSION TESTS    ###");
        logger.info("################################################");

        List<FactorySetup> setups = new ArrayList<>();
        setups.add(new FactorySetup("SIMPLE (Clip)", new SpatialMatchBrokerFactory(true, 0.0, true)));
        setups.add(new FactorySetup("SMART (0.5)", new SpatialMatchBrokerFactory(false, 0.5, true)));

        // 1. Fixed Topology Tests
        List<TestScenario> fixedScenarios = new ArrayList<>();
        fixedScenarios.add(new SubscriptionCoveringTest());
        fixedScenarios.add(new SubscriptionExpansionTest());
        fixedScenarios.add(new ComprehensiveFixedScenario());
        runBatch(new FixedTopologyFixture(), fixedScenarios, setups);

        // 2. Grid Topology Tests
        List<TestScenario> gridScenarios = new ArrayList<>();
        gridScenarios.add(new GridCrossCornerTest());
        runBatch(new GridTopologyFixture(), gridScenarios, setups);

        // 3. GeoNames Topology Tests
        List<TestScenario> geoScenarios = new ArrayList<>();
        geoScenarios.add(new GeoNamesRegressionTest());
        runBatch(new GeoNamesTopologyFixture(), geoScenarios, setups);
    }

    // --- Suite 2: Location Logic (ProximityRoutingBroker) ---
    private static void runLocationSuite() {
        logger.info("\n\n################################################");
        logger.info("### RUNNING LOCATION-BASED REGRESSION TESTS  ###");
        logger.info("################################################");

        List<FactorySetup> setups = new ArrayList<>();
        setups.add(new FactorySetup("PROXIMITY", new LocationBrokerFactory()));

        List<TestScenario> scenarios = new ArrayList<>();
        scenarios.add(new SubscriptionAggregationTest());
        scenarios.add(new ProximityPropagationTest());
        scenarios.add(new ProximityNetworkLogicTest());
        scenarios.add(new ProximityBrakeTest());

        // Run against all topologies
        runBatch(new FixedTopologyFixture(), scenarios, setups);
        runBatch(new GridTopologyFixture(), scenarios, setups);
        runBatch(new GeoNamesTopologyFixture(), scenarios, setups);
    }

    // --- Execution Engine ---

    record FactorySetup(String name, BrokerFactory factory) {}

    private static void runBatch(TopologyFixture fixture, List<TestScenario> scenarios, List<FactorySetup> setups) {
        for (FactorySetup setup : setups) {
            logger.info(String.format("\n=== Environment: %s | Strategy: %s ===", fixture.getName(), setup.name()));
            
            for (TestScenario test : scenarios) {
                try {
                    fixture.setup(setup.factory());
                    
                    long start = System.currentTimeMillis();
                    boolean result = test.run(fixture);
                    long duration = System.currentTimeMillis() - start;

                    if (result) {
                        logger.info(String.format("  [PASS] %-40s (%d ms)", test.getTestName(), duration));
                    } else {
                        logger.severe(String.format("  [FAIL] %-40s", test.getTestName()));
                    }
                } catch (Exception e) {
                    logger.severe("  [ERROR] " + test.getTestName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}