package simulator.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import simulator.config.BrokerConfig.StrategyType;
import simulator.tests.fixtures.*;
import simulator.tests.framework.*;
import simulator.tests.scenarios.*;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import utils.CustomLogger;

public class RegressionSuiteRunner {
    private static final Logger logger = CustomLogger.getLogger(RegressionSuiteRunner.class.getName());

    public static void main(String[] args) {
        logger.info(">>> STARTING REGRESSION SUITE <<<");

        // 0. Unit Tests (Run Once)
        logger.info("\n=== Unit Tests (Topology/Strategy Independent) ===");
        new RegionFloatingPointTest().run(null);

        // -----------------------------------------------------------
        // 1. Define Strategies to Test
        // -----------------------------------------------------------
        List<FactorySetup> setups = new ArrayList<>();
        
        // Setup A: SIMPLE Strategy (Single Region, Clipping Enabled)
        // We enable clipping (true) to test the IntersectionPolicy logic even with a Simple Store.
        setups.add(new FactorySetup(
            StrategyType.SIMPLE, 
            new SpatialMatchBrokerFactory(true, 0.0, true)
        ));

        // Setup B: SMART Strategy (Multi-Region, Threshold 0.5, Clipping Enabled)
        setups.add(new FactorySetup(
            StrategyType.SMART, 
            new SpatialMatchBrokerFactory(false, 0.5, true) 
        ));

        // -----------------------------------------------------------
        // 2. Fixed Topology Tests
        // -----------------------------------------------------------
        List<RegionTestScenario> fixedTests = new ArrayList<>();
        fixedTests.add(new SubscriptionCoveringTest());
        fixedTests.add(new SubscriptionExpansionTest());
        fixedTests.add(new ComprehensiveFixedScenario());
        
        runBatch(new FixedTopologyFixture(), fixedTests, setups);

        // -----------------------------------------------------------
        // 3. Grid Topology Tests
        // -----------------------------------------------------------
        List<RegionTestScenario> gridTests = new ArrayList<>();
        gridTests.add(new GridCrossCornerTest());
        
        runBatch(new GridTopologyFixture(), gridTests, setups);

        // -----------------------------------------------------------
        // 4. GeoNames Topology Tests
        // -----------------------------------------------------------
        // Checks complex routing on the real-world dataset
        List<RegionTestScenario> geoTests = new ArrayList<>();
        geoTests.add(new GeoNamesRegressionTest());
        
        runBatch(new GeoNamesTopologyFixture(), geoTests, setups);
    }

    // Helper Record
    record FactorySetup(StrategyType name, SpatialMatchBrokerFactory factory) {}

    private static void runBatch(TopologyFixture fixture, List<RegionTestScenario> scenarios, List<FactorySetup> setups) {
        for (FactorySetup setup : setups) {
            logger.info(String.format("\n=== Environment: %s | Strategy: %s ===", fixture.getName(), setup.name()));
            
            for (RegionTestScenario test : scenarios) {
                try {
                    // Re-initialize topology for every test to ensure isolation
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