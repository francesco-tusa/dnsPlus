package simulator.simulations.functional;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.core.VisualisedSimulationRunner;
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import utils.CustomLogger;

public class ConfigurableFunctionalTest<
    C extends TopologyConfiguration,
    R extends BrokerWithRegion,
    F extends AbstractTopologyFactory<C, R>
> extends VisualisedSimulationRunner<C, R, F> {

    private static final Logger logger = CustomLogger.getLogger(ConfigurableFunctionalTest.class.getName());

    private final Predicate<R> validationTest;
    private final String validationTestName;
    
    private boolean visualisationEnabled = true;

    public ConfigurableFunctionalTest(Predicate<R> validationTest, String validationTestName) {
        this.validationTest = validationTest;
        this.validationTestName = validationTestName;
    }
    
    public void setVisualisationEnabled(boolean enabled) {
        this.visualisationEnabled = enabled;
    }

    @Override
    protected Level getLogLevel() {
        return Level.FINE;
    }
    
    @Override
    protected void initialise(F factory, C config) {
        super.initialise(factory, config);
        if (!visualisationEnabled) {
            this.visualiser = null;
            logger.info("Visualisation disabled for this test run.");
        }
    }
    
    @Override
    protected void attachClientsAndVisualize() {
        logger.info("\n--- Attaching clients for functional test ---");
        topologyFactory.attachSubscribers(this.rootNode);
        topologyFactory.attachPublishers(this.rootNode);
        
        if (visualisationEnabled) {
            super.attachClientsAndVisualize();
        }
    }

    @Override
    protected void executeScenarios() {
        logger.info("\n--- Executing Functional Test Scenario: " + this.validationTestName + " ---");
        if (this.rootNode == null) {
            logger.severe("Functional test failed: Root node is null.");
            return;
        }

        boolean success = validationTest.test(this.rootNode);

        logger.info("\n--- Functional Test Result ---");
        if (success) {
            logger.info("SUCCESS: The '" + this.validationTestName + "' check passed.");
        } else {
            logger.info("FAILED: The '" + this.validationTestName + "' check did not pass.");
        }
    }
}