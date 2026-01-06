package simulator.tests.framework;

import java.util.logging.Logger;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import utils.CustomLogger;

/**
 * Abstract base class for all functional test scenarios.
 * Compatible with both Region-based and Location-based topologies.
 */
public abstract class TestScenario {
    protected final Logger logger = CustomLogger.getLogger(this.getClass().getName());

    public abstract String getTestName();
    
    /**
     * The core logic. Returns true if passed.
     */
    public abstract boolean run(TopologyFixture fixture);

    // --- Common Assertions & Helpers ---
    
    protected void assertReceived(SubscriberWithLocation sub, int expected) {
        logger.info(String.format("   [Check] Subscriber '%s': Checking inbox. Expected=%d, Actual=%d", 
            sub.getName(), expected, sub.getnPublications()));

        if (sub.getnPublications() != expected) {
            throw new AssertionError("Subscriber '" + sub.getName() + "' expected " + expected + 
                                     " pubs but got " + sub.getnPublications());
        }
    }
    
    protected SubscriberWithLocation requireSubscriber(TopologyFixture fixture, String name) {
        // Find generic SubscriberWithLocation (works for both Bounded and Coordinate brokers)
        SubscriberWithLocation sub = fixture.findNode(name, SubscriberWithLocation.class);
        if (sub == null) throw new IllegalStateException("Test Setup Failed: Missing subscriber " + name);
        return sub;
    }
    
    protected PublisherWithLocation requirePublisher(TopologyFixture fixture, String name) {
        PublisherWithLocation pub = fixture.findNode(name, PublisherWithLocation.class);
        if (pub == null) throw new IllegalStateException("Test Setup Failed: Missing publisher " + name);
        return pub;
    }
}