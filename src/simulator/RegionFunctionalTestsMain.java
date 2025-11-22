package simulator;

import java.util.function.Predicate;
import java.util.logging.Logger;
import simulator.regions.BrokerWithRegion;
import simulator.simulations.functional.ConfigurableFunctionalTest;
import simulator.simulations.functional.FixedTopologyRegionFunctionalTests;
import simulator.simulations.functional.GeoNamesPropagationTest;
import simulator.simulations.functional.GridTopologyRegionFunctionalTests;
import simulator.simulations.functional.RegionFpFunctionalTests;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;
import utils.CustomLogger;

/**
 * Main entry point for all FUNCTIONAL validation tests using REGION-BASED routing brokers.
 * These tests use deterministic topologies (fixed, grid) to verify algorithm correctness.
 */
public class RegionFunctionalTestsMain {

    private static final Logger logger = CustomLogger.getLogger(RegionFunctionalTestsMain.class.getName());

    // --- CONFIGURATION FLAG ---
    // Set to TRUE to run the test on the full 800k node topology.
    // Set to FALSE to run on the verified subset (Bangladesh-China).
    private static final boolean USE_FULL_TOPOLOGY = true;

    public static void main(String[] args) {
        // Run the most fundamental test first
        // runRegionFloatingPointTest();

        // runManualTopologyComprehensiveTest();
        // runSubscriptionCoveringTest();
        // runSubscriptionExpansionTest();
        // runGridTopologyTest();

        // Run a comprehensive end-to-end test using a subset
        // of the GeoNames world topolgy
        runGeoNamesTest();
    }


    public static void runRegionFloatingPointTest() {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): Region Class Floating-Point Logic Test");
        logger.info("===============================================================");
        
        Predicate<BrokerWithRegion> testWrapper = rootBroker -> {
            return RegionFpFunctionalTests.ALL_REGION_TESTS.test(null);
        };

        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        
        ConfigurableFunctionalTest<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(testWrapper, "Region Class FP Test");
        
        validation.run(factory, config);
    }

    public static void runManualTopologyComprehensiveTest() {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): Manual Topology - Comprehensive Scenario (Region)");
        logger.info("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        ConfigurableFunctionalTest<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(FixedTopologyRegionFunctionalTests.COMPREHENSIVE_SCENARIO, "Comprehensive Scenario");
        validation.run(factory, config);
    }

    public static void runSubscriptionCoveringTest() {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): Manual Topology - Subscription Covering Test");
        logger.info("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        ConfigurableFunctionalTest<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(FixedTopologyRegionFunctionalTests.SUBSCRIPTION_COVERING_SCENARIO, "Subscription Covering Test");
        validation.run(factory, config);
    }

    public static void runSubscriptionExpansionTest() {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): Manual Topology - Subscription EXPANSION Test");
        logger.info("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        ConfigurableFunctionalTest<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(FixedTopologyRegionFunctionalTests.SUBSCRIPTION_EXPANSION_SCENARIO, "Subscription Expansion Test");
        validation.run(factory, config);
    }

    public static void runGridTopologyTest() {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): Grid Topology - Cross-Corner Test (Region)");
        logger.info("===============================================================");
        GridTopologyConfiguration config = new GridTopologyConfiguration(3, 0.0, 3);
        GridTopologyGenerator factory = new GridTopologyGenerator(new RegionBrokerFactory());
        ConfigurableFunctionalTest<GridTopologyConfiguration, BrokerWithRegion, GridTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(GridTopologyRegionFunctionalTests.GRID_CROSS_CORNER_PROPAGATION, "Grid Cross-Corner Test");
        validation.run(factory, config);
    }

    public static void runGeoNamesTest() {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): GeoNames Propagation & Metrics");
        logger.info("  Mode: " + (USE_FULL_TOPOLOGY ? "FULL TOPOLOGY" : "SUBSET TOPOLOGY"));
        logger.info("===============================================================");
        
        String topologyFile;
        if (USE_FULL_TOPOLOGY) {
            topologyFile = "output/geonames_topology.json";
        } else {
            topologyFile = "output/geonames_subset_bangladesh_beijing.json";
        }
        
        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration(topologyFile);
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new RegionBrokerFactory());
        
        ConfigurableFunctionalTest<FileBasedTopologyConfiguration, BrokerWithRegion, FileBasedTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(GeoNamesPropagationTest.COMPREHENSIVE_REGRESSION_TEST, "GeoNames Propagation");
        
        validation.setVisualisationEnabled(false);
        
        validation.run(factory, config);
    }
}