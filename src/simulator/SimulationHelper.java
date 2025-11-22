package simulator;

import java.util.function.Predicate;
import java.util.logging.Logger;

import simulator.regions.BrokerWithRegion;
import simulator.simulations.functional.ConfigurableFunctionalTest;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.factories.BrokerFactory;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;
import utils.CustomLogger;

public class SimulationHelper {

    private static final Logger logger = CustomLogger.getLogger(SimulationHelper.class.getName());

    private SimulationHelper() {}

    // Specific to BrokerWithRegion
    public static <C extends TopologyConfiguration, F extends AbstractTopologyFactory<C, BrokerWithRegion>> 
    void runFunctionalTest(
            String testName,
            F factory,
            C config,
            Predicate<BrokerWithRegion> testPredicate,
            boolean enableVisualisation) {
        
        ConfigurableFunctionalTest<C, BrokerWithRegion, F> validation =
            new ConfigurableFunctionalTest<>(testPredicate, testName);
        
        validation.setVisualisationEnabled(enableVisualisation);
        validation.run(factory, config);
    }

    public static void runGeoNamesTest(String testName, 
                                       String topologyFilePath, 
                                       BrokerFactory brokerFactory, 
                                       Predicate<BrokerWithRegion> testPredicate) {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): " + testName);
        logger.info("  Topology File: " + topologyFilePath);
        logger.info("===============================================================");

        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration(topologyFilePath);
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(brokerFactory);

        runFunctionalTest(testName, factory, config, testPredicate, false);
    }

    public static void runGridTest(String testName, 
                                   int rows, int cols, 
                                   BrokerFactory brokerFactory, 
                                   Predicate<BrokerWithRegion> testPredicate) {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): " + testName);
        logger.info("===============================================================");

        GridTopologyConfiguration config = new GridTopologyConfiguration(rows, 0.0, cols);
        GridTopologyGenerator factory = new GridTopologyGenerator(brokerFactory);

        runFunctionalTest(testName, factory, config, testPredicate, true);
    }

    public static void runFixedTest(String testName, 
                                    BrokerFactory brokerFactory, 
                                    Predicate<BrokerWithRegion> testPredicate) {
        logger.info("===============================================================");
        logger.info("  RUNNING (Functional): " + testName);
        logger.info("===============================================================");

        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator(brokerFactory);

        runFunctionalTest(testName, factory, config, testPredicate, true);
    }
}