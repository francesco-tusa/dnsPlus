package simulator;

import simulator.regions.BrokerWithRegion;
import simulator.simulations.validation.ConfigurableValidationSimulation;
import simulator.simulations.validation.ManualTopologyRegionValidationTests;
import simulator.simulations.validation.RandomTopologyValidationTests;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.random.RegionProcessingRandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;

/**
 * Main entry point for all simulations using REGION-BASED routing brokers.
 */
public class RegionBasedSimulationsMain {

    public static void main(String[] args) {
        // --- Run the primary validation tests ---
        runManualTopologyComprehensiveTest();
        runRandomTopologySanityCheck();
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
     * Runs a sanity check on a randomly generated topology.
     */
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