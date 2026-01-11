package simulator.tests.framework;

import java.util.logging.Logger;
import utils.CustomLogger;

/**
 * Abstract base class for all functional test scenarios.
 * Defines the contract for running a test against a fixture.
 * * Use FunctionalTestUtils for assertions and node retrieval.
 */
public abstract class TestScenario {
    protected final Logger logger = CustomLogger.getLogger(this.getClass().getName());

    public abstract String getTestName();
    
    /**
     * The core logic. Returns true if passed.
     */
    public abstract boolean run(TopologyFixture fixture);
}