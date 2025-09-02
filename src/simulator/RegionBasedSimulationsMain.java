package simulator;

import simulator.regions.BrokerWithRegion;
import simulator.simulations.validation.ConfigurableValidationSimulation;
import simulator.simulations.validation.GridTopologyRegionValidationTests;
import simulator.simulations.validation.ManualTopologyRegionValidationTests;
import simulator.simulations.validation.RandomTopologyRegionValidationTests;
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
        // --- Run validation tests ---
        runManualTopologyComprehensiveTest();
        //runSubscriptionCoveringTest();
        //runRandomTopologySanityCheck();
        //runGridTopologyTest();
    }

    /**
     * Runs the comprehensive validation test for the manually defined fixed topology.
     */
    public static void runManualTopologyComprehensiveTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Manual Topology - Comprehensive Scenario (Region)");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        ConfigurableValidationSimulation<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ManualTopologyRegionValidationTests.COMPREHENSIVE_SCENARIO, "Comprehensive Scenario");
        validation.run(factory, config);
    }

    /**
     * Runs the validation test for the subscription covering feature.
     */
    public static void runSubscriptionCoveringTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Manual Topology - Subscription Covering Test");
        System.out.println("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new RegionBrokerFactory());
        ConfigurableValidationSimulation<FixedTestTopologyConfiguration, BrokerWithRegion, FixedTestTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(ManualTopologyRegionValidationTests.SUBSCRIPTION_COVERING_SCENARIO, "Subscription Covering Test");
        validation.run(factory, config);
    }

    /**
     * Runs a sanity check on a randomly generated topology.
     */
    public static void runRandomTopologySanityCheck() {
        System.out.println("=======================================================");
        System.out.println("  RUNNING (Validation): Random Topology - Sanity Check (Region)");
        System.out.println("=======================================================");
        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(3, 4, 4, 3, 2);
        RegionProcessingRandomTopologyGenerator factory = new RegionProcessingRandomTopologyGenerator(new RegionBrokerFactory());
        ConfigurableValidationSimulation<RegionRandomTopologyConfiguration, BrokerWithRegion, RegionProcessingRandomTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(RandomTopologyRegionValidationTests.RANDOM_TOPOLOGY_SANITY_CHECK, "Random Topology Sanity Check");
        validation.run(factory, config);
    }

    /**
     * Runs a validation test on a 3x3 grid topology.
     */
    public static void runGridTopologyTest() {
        System.out.println("===============================================================");
        System.out.println("  RUNNING (Validation): Grid Topology - Cross-Corner Test (Region)");
        System.out.println("===============================================================");
        
        // This creates a 3x3 grid of leaf brokers with a tree depth of 3.
        GridTopologyConfiguration config = new GridTopologyConfiguration(3, 0.0, 3);
        GridTopologyGenerator factory = new GridTopologyGenerator(new RegionBrokerFactory());
        
        ConfigurableValidationSimulation<GridTopologyConfiguration, BrokerWithRegion, GridTopologyGenerator> validation =
            new ConfigurableValidationSimulation<>(GridTopologyRegionValidationTests.GRID_CROSS_CORNER_PROPAGATION, "Grid Cross-Corner Test");
            
        validation.run(factory, config);
    }
}