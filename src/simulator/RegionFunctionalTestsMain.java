package simulator;

import simulator.regions.BrokerWithRegion;
import simulator.simulations.functional.ConfigurableFunctionalTest;
import simulator.simulations.functional.FixedTopologyRegionFunctionalTests;
import simulator.simulations.functional.GridTopologyRegionFunctionalTests;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;

/**
 * Main entry point for all FUNCTIONAL validation tests using REGION-BASED routing brokers.
 * These tests use deterministic topologies (fixed, grid) to verify algorithm correctness.
 */
public class RegionFunctionalTestsMain {

    public static void main(String[] args) {
        runManualTopologyComprehensiveTest();
        //runSubscriptionCoveringTest();
        //runGridTopologyTest();
    }

    public static void runManualTopologyComprehensiveTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Functional): Manual Topology - Comprehensive Scenario (Region)");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        ConfigurableFunctionalTest<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(FixedTopologyRegionFunctionalTests.COMPREHENSIVE_SCENARIO, "Comprehensive Scenario");
        validation.run(factory, config);
    }

    public static void runSubscriptionCoveringTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Functional): Manual Topology - Subscription Covering Test");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        ConfigurableFunctionalTest<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(FixedTopologyRegionFunctionalTests.SUBSCRIPTION_COVERING_SCENARIO, "Subscription Covering Test");
        validation.run(factory, config);
    }

    public static void runGridTopologyTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Functional): Grid Topology - Cross-Corner Test (Region)");
        System.out.println("===============================================================");
        GridTopologyConfiguration config = new GridTopologyConfiguration(3, 0.0, 3);
        GridTopologyGenerator factory = new GridTopologyGenerator(new RegionBrokerFactory());
        ConfigurableFunctionalTest<GridTopologyConfiguration, BrokerWithRegion, GridTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(GridTopologyRegionFunctionalTests.GRID_CROSS_CORNER_PROPAGATION, "Grid Cross-Corner Test");
        validation.run(factory, config);
    }
}