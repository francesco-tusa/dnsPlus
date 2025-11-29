package simulator.simulations.functional;

import java.util.function.Predicate;
import java.util.logging.Logger;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.BoundedBroker;
import simulator.topology.analysis.TopologyAnalyzer;
import utils.CustomLogger;

public class FixedTopologyLocationFunctionalTests {

    private static final Logger logger = CustomLogger.getLogger(FixedTopologyLocationFunctionalTests.class.getName());

    public static final Predicate<BoundedBroker> COMPREHENSIVE_SCENARIO = root -> {
        logger.info("\n>>> SCENARIO: Testing Multi-Client Closest Publication Filtering (Location). <<<");

        SubscriberWithLocation sub2 = TopologyAnalyzer.findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation sub8 = TopologyAnalyzer.findNodeByName(root, "sub8", SubscriberWithLocation.class);
        PublisherWithLocation p1 = TopologyAnalyzer.findNodeByName(root, "pub1", PublisherWithLocation.class);
        PublisherWithLocation p2 = TopologyAnalyzer.findNodeByName(root, "pub2", PublisherWithLocation.class);

        if (sub2 == null || sub8 == null || p1 == null || p2 == null) {
            logger.severe("Test failed: Could not find all required nodes.");
            return false;
        }

        sub2.send(new SubscriptionWithLocation(sub2.getLocation()));
        sub8.send(new SubscriptionWithLocation(sub8.getLocation()));

        p1.send(new PublicationWithLocation(p1.getLocation())); 
        p2.send(new PublicationWithLocation(p2.getLocation())); 
        
        p1.send(new PublicationWithLocation(new simulator.core.Location(6.0, 3.0, 0.0)));
        p2.send(new PublicationWithLocation(new simulator.core.Location(19.0, 5.0, 0.0)));
        p1.send(new PublicationWithLocation(new simulator.core.Location(15.0, 15.0, 0.0)));

        boolean success = sub2.getnPublications() == 2 && sub8.getnPublications() == 3;
        
        if (success) logger.info("SUCCESS: The Multi-Client Closest Publication test passed.");
        else logger.severe("FAILED: The Multi-Client Closest Publication test did not pass.");
        
        return success;
    };

    public static final Predicate<BoundedBroker> SUBSCRIPTION_FILTERING_SCENARIO = root -> {
        logger.info("\n>>> SCENARIO: Testing Upper-Level Proxy Subscription Filtering (Location). <<<");

        SubscriberWithLocation s2 = TopologyAnalyzer.findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s3 = TopologyAnalyzer.findNodeByName(root, "sub3", SubscriberWithLocation.class);
        BoundedBroker child2 = TopologyAnalyzer.findNodeByName(root, "child2", BoundedBroker.class);
        PublisherWithLocation p2 = TopologyAnalyzer.findNodeByName(root, "pub2", PublisherWithLocation.class);

        if (s2 == null || s3 == null || child2 == null || p2 == null) {
            logger.severe("Test failed: Could not find all required nodes.");
            return false;
        }

        s2.send(new SubscriptionWithLocation(s2.getLocation()));
        s3.send(new SubscriptionWithLocation(s3.getLocation()));

        int intermediateBrokerSubscriptionCount = child2.getInputSubscriptionCount();
        
        long rootSubscriptionsFromChild2 = root.getInputSubscriptions().keySet().stream()
                .filter(node -> node.getName().equals("child2"))
                .count();

        logger.info("\n--- Mid-point Check ---");
        logger.info("  - Broker 'child2' table size: " + intermediateBrokerSubscriptionCount + " (Expected: 2)");
        logger.info("  - Root inputs from 'child2': " + rootSubscriptionsFromChild2 + " (Expected: 1)");

        boolean filteringSuccess = (intermediateBrokerSubscriptionCount == 2) && (rootSubscriptionsFromChild2 == 1);

        p2.send(new PublicationWithLocation(p2.getLocation()));

        boolean deliverySuccess = s2.getnPublications() == 1 && s3.getnPublications() == 1;
        boolean finalSuccess = filteringSuccess && deliverySuccess;

        if (finalSuccess) logger.info("SUCCESS: The Upper-Level Proxy Filtering test passed.");
        else logger.severe("FAILED: The Upper-Level Proxy Filtering test did not pass.");

        return finalSuccess;
    };
}