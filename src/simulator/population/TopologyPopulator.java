package simulator.population;

import java.util.List;
import java.util.logging.Logger; // Import Logger
import simulator.regions.BrokerWithRegion;
import utils.CustomLogger; // Import CustomLogger

/**
 * A utility class to populate a given topology with subscribers and publishers
 * using specified placement strategies.
 */
public class TopologyPopulator {

    private static final Logger logger = CustomLogger.getLogger(TopologyPopulator.class.getName()); // Get logger

    private final SubscribersPlacementStrategy subscribersStrategy;
    private final PublishersPlacementStrategy publishersStrategy;

    /**
     * Creates a new populator with defined strategies for placing clients.
     * @param subscribersStrategy The strategy for placing subscribers.
     * @param publishersStrategy The strategy for placing publishers.
     */
    public TopologyPopulator(SubscribersPlacementStrategy subscribersStrategy, PublishersPlacementStrategy publishersStrategy) {
        if (subscribersStrategy == null || publishersStrategy == null) {
            throw new IllegalArgumentException("Placement strategies cannot be null.");
        }
        this.subscribersStrategy = subscribersStrategy;
        this.publishersStrategy = publishersStrategy;
    }

    /**
     * Populates the topology with clients.
     * @param rootNode The root of the broker topology.
     * @param leafBrokers A generic list of all leaf brokers in the topology.
     * @param totalSubscribers The total number of subscribers to create.
     * @param totalPublishers The number of publishers to create.
     */
    public void populate(BrokerWithRegion rootNode, List<BrokerWithRegion> leafBrokers, long totalSubscribers, int totalPublishers) {
        if (rootNode == null || leafBrokers == null || leafBrokers.isEmpty()) {
            logger.severe("Cannot populate topology: root node or leaf brokers are null/empty.");
            return;
        }

        // --- Subscriber Generation ---
        subscribersStrategy.generateAndAttach(rootNode, leafBrokers, totalSubscribers);

        // --- Publisher (Replica) Generation ---
        publishersStrategy.generateAndAttach(rootNode, leafBrokers, totalPublishers);
    }
}