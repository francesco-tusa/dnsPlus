package simulator.population;

import java.util.List;
import simulator.regions.BrokerWithRegion;

/**
 * An interface for strategies that place subscribers within a topology.
 */
public interface SubscribersPlacementStrategy {

    /**
     * Generates and attaches a total number of subscribers to the leaf brokers
     * of a given topology.
     *
     * @param rootNode The root of the broker topology.
     * @param leafBrokers A list of all leaf brokers in the topology.
     * @param totalSubscribersToCreate The total number of subscribers to distribute.
     */
    void generateAndAttach(BrokerWithRegion rootNode, List<BrokerWithRegion> leafBrokers, long totalSubscribersToCreate);
}