package simulator;

import java.util.function.Predicate;
import java.util.logging.Logger; // Import Logger
import simulator.regions.BoundedBroker;
import simulator.simulations.functional.ConfigurableFunctionalTest;
import simulator.simulations.functional.FixedTopologyLocationFunctionalTests;
import simulator.simulations.functional.GridTopologyLocationFunctionalTests;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;
import utils.CustomLogger; // Import CustomLogger

/**
 * Main entry point for all FUNCTIONAL validation tests using LOCATION-BASED routing brokers.
 * These tests use deterministic topologies (fixed, grid) to verify algorithm correctness.
 */
public class LocationFunctionalTestsMain {

    private static final Logger logger = CustomLogger.getLogger(LocationFunctionalTestsMain.class.getName());


    public static void main(String[] args) {
        runManualTopologyComprehensiveTest();
        //runManualTopologySubscriptionFilteringTest();
        //runGridTopologyTest();
    }

    public static void runManualTopologyComprehensiveTest() {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): Manual Topology - Comprehensive Scenario (Location)");
        logger.info("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new LocationBrokerFactory());
        ConfigurableFunctionalTest<FixedTestTopologyConfiguration, BoundedBroker, FixedTestTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(FixedTopologyLocationFunctionalTests.COMPREHENSIVE_SCENARIO, "Manual Topology Comprehensive (Location)");
        validation.run(factory, config);
    }

    public static void runManualTopologySubscriptionFilteringTest() {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): Manual Topology - Subscription Filtering (Location)");
        logger.info("===============================================================");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(new LocationBrokerFactory());
        ConfigurableFunctionalTest<FixedTestTopologyConfiguration, BoundedBroker, FixedTestTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(FixedTopologyLocationFunctionalTests.SUBSCRIPTION_FILTERING_SCENARIO, "Manual Topology Filtering (Location)");
        validation.run(factory, config);
    }

    public static void runGridTopologyTest() {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): Grid Topology - Cross-Corner Test (Location)");
        logger.info("===============================================================");
        GridTopologyConfiguration config = new GridTopologyConfiguration(3, 0.0, 3);
        GridTopologyGenerator factory = new GridTopologyGenerator(new LocationBrokerFactory());
        ConfigurableFunctionalTest<GridTopologyConfiguration, BoundedBroker, GridTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(GridTopologyLocationFunctionalTests.GRID_CROSS_CORNER_PROPAGATION, "Grid Cross-Corner Test (Location)");
        validation.run(factory, config);
    }


    // TODO: Should include here a complete regression test for location based algorithm, like we do in RegionFunctionalTestsMain
    public static void runGeoNamesTest() {        
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyGenerator factory = new GeoNamesTopologyGenerator(new LocationBrokerFactory());

        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): GeoNames Topology - Location Propagation");
        logger.info("  Topology File: " + config.getTopologyFilePath());
        logger.info("===============================================================");
        
        Predicate<BoundedBroker> placeholderTest = root -> {
            logger.info("Location-based GeoNames test is not yet implemented. Topology loaded successfully.");
            return true; 
        };

        ConfigurableFunctionalTest<GeoNamesTopologyConfiguration, BoundedBroker, GeoNamesTopologyGenerator> validation =
            new ConfigurableFunctionalTest<>(placeholderTest, "GeoNames Location Propagation");
            
        validation.setVisualisationEnabled(false);
        
        validation.run(factory, config);
    }

}