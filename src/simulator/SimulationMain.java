package simulator;

import simulator.regions.BrokerWithRegion;
import simulator.simulations.performance.ConfigurablePerformanceSimulation;
import simulator.simulations.validation.ConfigurableValidationSimulation;
import simulator.simulations.validation.ValidationTests;
import simulator.topology.factories.BrokerFactory;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;
import simulator.topology.random.RegionProcessingRandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;

/**
 * Main entry point for running all simulations.
 * This class acts as a control panel to configure and launch different simulation scenarios.
 */
public class SimulationMain {

    public static void main(String[] args) {
        // --- CHOOSE AND CONFIGURE YOUR SIMULATION SCENARIO HERE ---
        
        // --- Run a Performance Simulation ---
        // runGridLocationPerformance();
        
        // --- Run a Validation Simulation ---
        runFixedTopologyLocationValidation();
        runFixedTopologyRegionValidation();
        //runRandomTopologyRegionValidation();
        //runRandomTopologyLocationValidation(); // New test scenario
    }

    // --- Performance Simulation Scenarios ---

    public static void runGridLocationPerformance() {
        System.out.println("==========================================================");
        System.out.println("  RUNNING (Performance): Grid with Location Brokers ");
        System.out.println("==========================================================");

        GridTopologyConfiguration config = new GridTopologyConfiguration(10, 0.25, 4);
        BrokerFactory brokerFactory = new LocationBrokerFactory();
        GridTopologyGenerator factory = new GridTopologyGenerator(brokerFactory);
        
        ConfigurablePerformanceSimulation<GridTopologyConfiguration, GridTopologyGenerator> simulation = 
            new ConfigurablePerformanceSimulation<>(1000, 5, 10.0, 0.2);
        
        simulation.run(factory, config);
    }
    
    // ... Add other performance scenarios here ...

    // --- Validation Simulation Scenarios ---

    public static void runFixedTopologyLocationValidation() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Fixed Topology with LOCATION Brokers   ");
        System.out.println("===============================================================");

        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        // Use a location-based factory for this test.
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new LocationBrokerFactory());

        ConfigurableValidationSimulation<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ValidationTests.FIXED_TOPOLOGY_SANITY_CHECK, "Fixed Topology Sanity Check (Location)");
            
        validation.run(factory, config);
    }

    public static void runFixedTopologyRegionValidation() {
        System.out.println("=============================================================");
        System.out.println("  RUNNING (Validation): Fixed Topology with REGION Brokers   ");
        System.out.println("=============================================================");

        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        // Use a region-based factory for this test.
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());

        ConfigurableValidationSimulation<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ValidationTests.FIXED_TOPOLOGY_SANITY_CHECK, "Fixed Topology Sanity Check (Region)");
            
        validation.run(factory, config);
    }

    public static void runRandomTopologyRegionValidation() {
        System.out.println("======================================================");
        System.out.println("  RUNNING (Validation): Random Topology with REGION Brokers  ");
        System.out.println("======================================================");

        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(3, 4, 4, 3, 2);
        // Use a region-based factory for this test.
        RegionProcessingRandomTopologyGenerator factory = new RegionProcessingRandomTopologyGenerator(new RegionBrokerFactory());

        ConfigurableValidationSimulation<RegionRandomTopologyConfiguration, BrokerWithRegion, RegionProcessingRandomTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ValidationTests.RANDOM_TOPOLOGY_SANITY_CHECK, "Random Topology Sanity Check (Region)");
            
        validation.run(factory, config);
    }

    public static void runRandomTopologyLocationValidation() {
        System.out.println("======================================================");
        System.out.println("  RUNNING (Validation): Random Topology with LOCATION Brokers  ");
        System.out.println("======================================================");

        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(3, 4, 4, 3, 2);
        // Use a location-based factory for this test.
        RegionProcessingRandomTopologyGenerator factory = new RegionProcessingRandomTopologyGenerator(new LocationBrokerFactory());

        ConfigurableValidationSimulation<RegionRandomTopologyConfiguration, BrokerWithRegion, RegionProcessingRandomTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ValidationTests.RANDOM_TOPOLOGY_SANITY_CHECK, "Random Topology Sanity Check (Location)");
            
        validation.run(factory, config);
    }
}