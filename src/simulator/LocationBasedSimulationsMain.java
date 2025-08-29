package simulator;

import simulator.regions.BrokerWithRegion;
import simulator.simulations.performance.ConfigurablePerformanceSimulation;
import simulator.simulations.validation.ConfigurableValidationSimulation;
import simulator.simulations.validation.GridTopologyValidationTests;
import simulator.simulations.validation.ManualTopologyLocationValidationTests;
import simulator.topology.factories.BrokerFactory;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;

/**
 * Main entry point for all simulations using LOCATION-BASED routing brokers.
 */
public class LocationBasedSimulationsMain {

    public static void main(String[] args) {
        // --- Run all validation tests for LOCATION-based brokers ---
        runManualTopologySanityCheck();
        runGridGuaranteedMatchValidation();

        // --- Run performance tests for LOCATION-based brokers ---
        // runGridLocationPerformance();
    }

    // --- Performance Scenarios ---
    public static void runGridLocationPerformance() {
        System.out.println("==========================================================");
        System.out.println("  RUNNING (Performance): Grid with Location Brokers ");
        System.out.println("==========================================================");
        GridTopologyConfiguration config = new GridTopologyConfiguration(10, 0.25, 4);
        GridTopologyGenerator factory = new GridTopologyGenerator(new LocationBrokerFactory());
        ConfigurablePerformanceSimulation<GridTopologyConfiguration, GridTopologyGenerator> simulation = 
            new ConfigurablePerformanceSimulation<>(1000, 5, 10.0, 0.2);
        simulation.run(factory, config);
    }
    
    // --- Validation Scenarios ---
    public static void runManualTopologySanityCheck() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Manual Topology - Sanity Check (Location)");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new LocationBrokerFactory());
        ConfigurableValidationSimulation<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ManualTopologyLocationValidationTests.SANITY_CHECK, "Manual Topology Sanity Check (Location)");
        validation.run(factory, config);
    }

    public static void runGridGuaranteedMatchValidation() {
        System.out.println("===========================================================");
        System.out.println("  RUNNING (Validation): Grid Topology - Guaranteed Match (Location)");
        System.out.println("===========================================================");
        GridTopologyConfiguration config = new GridTopologyConfiguration(3, 0.1, 3);
        GridTopologyGenerator factory = new GridTopologyGenerator(new LocationBrokerFactory());
        ConfigurableValidationSimulation<GridTopologyConfiguration, BrokerWithRegion, GridTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(GridTopologyValidationTests.GUARANTEED_MATCH, "Guaranteed Match Test");
        validation.run(factory, config);
    }
}
