package simulator.simulations.functional;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger; // Import Logger
import simulator.core.VisualisedSimulationRunner;
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import utils.CustomLogger; // Import CustomLogger

public class ConfigurableFunctionalTest<
    C extends TopologyConfiguration,
    R extends BrokerWithRegion,
    F extends AbstractTopologyFactory<C, R>
> extends VisualisedSimulationRunner<C, R, F> {

    private static final Logger logger = CustomLogger.getLogger(ConfigurableFunctionalTest.class.getName());

    private final Predicate<R> validationTest;
    private final String validationTestName;
    
    // Default to true to maintain existing behavior for other tests
    private boolean visualisationEnabled = true;

    public ConfigurableFunctionalTest(Predicate<R> validationTest, String validationTestName) {
        this.validationTest = validationTest;
        this.validationTestName = validationTestName;
    }
    
    /**
     * Enables or disables the visualization for this functional test.
     * @param enabled true to show the UI, false to run headless.
     */
    public void setVisualisationEnabled(boolean enabled) {
        this.visualisationEnabled = enabled;
    }

    @Override
    protected Level getLogLevel() {
        return Level.FINE;
    }
    
    /**
     * Override initialise to ensure the visualiser reference is cleared if disabled.
     */
    @Override
    protected void initialise(F factory, C config) {
        super.initialise(factory, config);
        
        if (!visualisationEnabled) {
            // Explicitly nullify the visualiser so the parent class cleanup() 
            // doesn't try to display it.
            this.visualiser = null;
            logger.info("Visualisation disabled for this test run.");
        }
    }
    
    /**
     * Overrides the new hook method. It first attaches the clients needed for the
     * functional test, and then conditionally calls the parent method for visualization.
     */
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