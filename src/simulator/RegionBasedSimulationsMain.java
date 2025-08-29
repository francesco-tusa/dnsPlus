package simulator;

import simulator.regions.BrokerWithRegion;
import simulator.simulations.performance.ConfigurablePerformanceSimulation;
import simulator.simulations.validation.ConfigurableValidationSimulation;
import simulator.simulations.validation.ManualTopologyRegionValidationTests;
import simulator.simulations.validation.RandomTopologyValidationTests;
import simulator.topology.factories.BrokerFactory;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;
import simulator.topology.random.RegionProcessingRandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;

/**
 * Main entry point for all simulations using REGION-BASED routing brokers.
 */
public class RegionBasedSimulationsMain {

    public static void main(String[] args) {
        // --- Run all validation tests for REGION-based brokers ---
        // runManualTopologySanityCheck();
        // runManualTopologyOverlapValidation();
           runManualTopologyComplexValidation();
           runRandomTopologySanityCheck();

        // --- Run performance tests for REGION-based brokers ---
        // runGridRegionPerformance();
    }

    // --- Performance Scenarios ---
    public static void runGridRegionPerformance() {
        System.out.println("==========================================================");
        System.out.println("  RUNNING (Performance): Grid with Region Brokers ");
        System.out.println("==========================================================");
        GridTopologyConfiguration config = new GridTopologyConfiguration(10, 0.25, 4);
        GridTopologyGenerator factory = new GridTopologyGenerator(new RegionBrokerFactory());
        ConfigurablePerformanceSimulation<GridTopologyConfiguration, GridTopologyGenerator> simulation = 
            new ConfigurablePerformanceSimulation<>(1000, 5, 10.0, 0.2);
        simulation.run(factory, config);
    }
    
    // --- Validation Scenarios ---
    public static void runManualTopologySanityCheck() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Manual Topology - Sanity Check (Region)");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        ConfigurableValidationSimulation<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ManualTopologyRegionValidationTests.SANITY_CHECK_NO_OVERLAP, "Sanity Check (No Overlap)");
        validation.run(factory, config);
    }
    
    public static void runManualTopologyOverlapValidation() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Manual Topology - Overlap Propagation (Region)");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        ConfigurableValidationSimulation<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ManualTopologyRegionValidationTests.CROSS_BRANCH_PROPAGATION, "Cross-Branch Propagation");
        validation.run(factory, config);
    }

    public static void runManualTopologyComplexValidation() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Manual Topology - Complex Scenario (Region)");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        ConfigurableValidationSimulation<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ManualTopologyRegionValidationTests.COMBINED_COMPLEX_SCENARIO, "Combined Complex Scenario");
        validation.run(factory, config);
    }

    public static void runRandomTopologySanityCheck() {
        System.out.println("=======================================================");
        System.out.println("  RUNNING (Validation): Random Topology - Sanity Check (Region)");
        System.out.println("=======================================================");
        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(3, 4, 4, 3, 2);
        RegionProcessingRandomTopologyGenerator factory = new RegionProcessingRandomTopologyGenerator(new RegionBrokerFactory());
        ConfigurableValidationSimulation<RegionRandomTopologyConfiguration, BrokerWithRegion, RegionProcessingRandomTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(RandomTopologyValidationTests.RANDOM_TOPOLOGY_SANITY_CHECK, "Random Topology Sanity Check");
        validation.run(factory, config);
    }
}
