package simulator;

import java.util.logging.Logger;
import simulator.regions.BrokerWithRegion;
import simulator.simulations.functional.ConfigurableFunctionalTest;
import simulator.simulations.functional.FixedTopologyRegionFunctionalTests;
import simulator.simulations.functional.GridTopologyRegionFunctionalTests;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;
import utils.CustomLogger;

/**
 * Main entry point for all FUNCTIONAL validation tests using REGION-BASED routing brokers.
 * These tests use deterministic topologies (fixed, grid) to verify algorithm correctness.
 */
public class RegionFunctionalTestsMain {

    private static final Logger logger = CustomLogger.getLogger(RegionFunctionalTestsMain.class.getName());

    public static void main(String[] args) {
        // runManualTopologyComprehensiveTest();
        //runSubscriptionCoveringTest();
        runSubscriptionExpansionTest();
        // runGridTopologyTest();
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
}
