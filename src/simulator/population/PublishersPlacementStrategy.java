package simulator.population;

import java.util.List;
import simulator.regions.BoundedBroker;

/**
 * An interface for strategies that place publishers within a topology.
 */
public interface PublishersPlacementStrategy {

    /**
     * Generates and attaches a total number of publishers to the leaf brokers
     * of a given topology.
     *
     * @param rootNode The root of the broker topology.
     * @param leafBrokers A list of all leaf brokers in the topology.
     * @param totalPublishersToCreate The total number of publishers to create.
     */
    void generateAndAttach(BoundedBroker rootNode, List<BoundedBroker> leafBrokers, long totalPublishersToCreate);
}