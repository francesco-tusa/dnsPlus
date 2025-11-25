package simulator;

import java.util.function.Predicate;
import java.util.logging.Logger;

import simulator.regions.BoundedBroker;
import simulator.simulations.functional.ConfigurableFunctionalTest;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.factories.BoundedBrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;
import utils.CustomLogger;

public class SimulationHelper {

    private static final Logger logger = CustomLogger.getLogger(SimulationHelper.class.getName());

    private SimulationHelper() {}

    // Specific to BrokerWithRegion
    public static <C extends TopologyConfiguration, F extends AbstractTopologyFactory<C, BoundedBroker>> 
    void runFunctionalTest(
            String testName,
            F factory,
            C config,
            Predicate<BoundedBroker> testPredicate,
            boolean enableVisualisation) {
        
        ConfigurableFunctionalTest<C, BoundedBroker, F> validation =
            new ConfigurableFunctionalTest<>(testPredicate, testName);
        
        validation.setVisualisationEnabled(enableVisualisation);
        validation.run(factory, config);
    }

    public static void runGeoNamesTest(String testName, 
                                       BoundedBrokerFactory brokerFactory, 
                                       Predicate<BoundedBroker> testPredicate) {

        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyGenerator factory = new GeoNamesTopologyGenerator(brokerFactory);

        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): " + testName);
        logger.info("  Topology File: " + config.getTopologyFilePath());
        logger.info("===============================================================");

        runFunctionalTest(testName, factory, config, testPredicate, false);
    }

    public static void runGridTest(String testName, 
                                   int rows, int cols, 
                                   BoundedBrokerFactory brokerFactory, 
                                   Predicate<BoundedBroker> testPredicate) {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): " + testName);
        logger.info("===============================================================");

        GridTopologyConfiguration config = new GridTopologyConfiguration(rows, 0.0, cols);
        GridTopologyGenerator factory = new GridTopologyGenerator(brokerFactory);

        runFunctionalTest(testName, factory, config, testPredicate, true);
    }

    public static void runFixedTest(String testName, 
                                    BoundedBrokerFactory brokerFactory, 
                                    Predicate<BoundedBroker> testPredicate) {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): " + testName);
        logger.info("===============================================================");

        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(brokerFactory);

        runFunctionalTest(testName, factory, config, testPredicate, true);
    }
}