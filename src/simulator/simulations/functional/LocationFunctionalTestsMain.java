package simulator.simulations.functional;

import simulator.regions.BrokerWithRegion;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;

/**
 * Main entry point for all FUNCTIONAL validation tests using LOCATION-BASED routing brokers.
 * These tests use deterministic topologies (fixed, grid) to verify algorithm correctness.
 */
public class LocationFunctionalTestsMain {

    public static void main(String[] args) {
        runManualTopologyComprehensiveTest();
        runManualTopologySubscriptionFilteringTest();
        runGridTopologyTest();
    }

    public static void runManualTopologyComprehensiveTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Functional): Manual Topology - Comprehensive Scenario (Location)");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new LocationBrokerFactory());
        ConfigurableFunctionalTest<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(FixedTopologyLocationFunctionalTests.COMPREHENSIVE_SCENARIO, "Manual Topology Comprehensive (Location)");
        validation.run(factory, config);
    }

    public static void runManualTopologySubscriptionFilteringTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Functional): Manual Topology - Subscription Filtering (Location)");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new LocationBrokerFactory());
        ConfigurableFunctionalTest<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(FixedTopologyLocationFunctionalTests.SUBSCRIPTION_FILTERING_SCENARIO, "Manual Topology Filtering (Location)");
        validation.run(factory, config);
    }

    public static void runGridTopologyTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Functional): Grid Topology - Cross-Corner Test (Location)");
        System.out.println("===============================================================");
        GridTopologyConfiguration config = new GridTopologyConfiguration(3, 0.0, 3);
        GridTopologyGenerator factory = new GridTopologyGenerator(new LocationBrokerFactory());
        ConfigurableFunctionalTest<GridTopologyConfiguration, BrokerWithRegion, GridTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(GridTopologyLocationFunctionalTests.GRID_CROSS_CORNER_PROPAGATION, "Grid Cross-Corner Test (Location)");
        validation.run(factory, config);
    }
}