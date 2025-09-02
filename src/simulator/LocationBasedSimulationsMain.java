package simulator;

import simulator.regions.BrokerWithRegion;
import simulator.simulations.validation.ConfigurableValidationSimulation;
import simulator.simulations.validation.GridTopologyLocationValidationTests;
import simulator.simulations.validation.ManualTopologyLocationValidationTests;
import simulator.simulations.validation.RandomTopologyLocationValidationTests;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;
import simulator.topology.random.RegionProcessingRandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;

/**
 * Main entry point for all simulations using LOCATION-BASED routing brokers.
 */
public class LocationBasedSimulationsMain {

    public static void main(String[] args) {
        // --- Run all validation tests for LOCATION-based brokers ---
        runManualTopologyComprehensiveTest();
        //runManualTopologyFilteringTest();
        //runGridTopologyTest();
        //runRandomTopologySanityCheck();
    }

    /**
     * Runs the comprehensive validation test for the manually defined fixed topology.
     */
    public static void runManualTopologyComprehensiveTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Manual Topology - Comprehensive Scenario (Location)");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new LocationBrokerFactory());
        ConfigurableValidationSimulation<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ManualTopologyLocationValidationTests.COMPREHENSIVE_SCENARIO, "Manual Topology Comprehensive (Location)");
        validation.run(factory, config);
    }

    /**
     * Runs the publication filtering validation test for the manually defined fixed topology.
     */
    public static void runManualTopologyFilteringTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Manual Topology - Publication Filtering (Location)");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new LocationBrokerFactory());
        ConfigurableValidationSimulation<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ManualTopologyLocationValidationTests.COMPREHENSIVE_SCENARIO, "Manual Topology Filtering (Location)");
        validation.run(factory, config);
    }

    /**
     * Runs a validation test on a 3x3 grid topology using location-based routing.
     */
    public static void runGridTopologyTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Grid Topology - Cross-Corner Test (Location)");
        System.out.println("===============================================================");

        GridTopologyConfiguration config = new GridTopologyConfiguration(3, 0.0, 3);
        GridTopologyGenerator factory = new GridTopologyGenerator(new LocationBrokerFactory());

        ConfigurableValidationSimulation<GridTopologyConfiguration, BrokerWithRegion, GridTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(GridTopologyLocationValidationTests.GRID_CROSS_CORNER_PROPAGATION, "Grid Cross-Corner Test (Location)");
            
        validation.run(factory, config);
    }

    /**
     * Runs a sanity check on a randomly generated topology using location-based routing.
     */
    public static void runRandomTopologySanityCheck() {
        System.out.println("=======================================================");
        System.out.println("  RUNNING (Validation): Random Topology - Sanity Check (Location)");
        System.out.println("=======================================================");
        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(3, 4, 4, 3, 2);
        // NOTE: The generator is for regions, but we inject a LocationBrokerFactory to build the correct broker types.
        RegionProcessingRandomTopologyGenerator factory = new RegionProcessingRandomTopologyGenerator(new LocationBrokerFactory());
        ConfigurableValidationSimulation<RegionRandomTopologyConfiguration, BrokerWithRegion, RegionProcessingRandomTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(RandomTopologyLocationValidationTests.SANITY_CHECK, "Random Topology Sanity Check");
        validation.run(factory, config);
    }
}