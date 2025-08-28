package simulator.clients;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import simulator.regions.BrokerWithRegion;

/**
 * A utility class to populate a given topology with subscribers and publishers.
 * This centralizes the client generation logic so it can be reused across different simulations.
 */
public class TopologyPopulater {

    /**
     * Populates the topology with clients.
     * @param rootNode The root of the broker topology.
     * @param leafBrokers A generic list of all leaf brokers in the topology.
     * @param totalSubscribers The total number of subscribers to create.
     * @param numberOfReplicas The number of publisher replicas to create.
     */
    public void populate(BrokerWithRegion rootNode, List<BrokerWithRegion> leafBrokers, long totalSubscribers, int numberOfReplicas) {
        if (rootNode == null || leafBrokers == null || leafBrokers.isEmpty()) {
            System.err.println("Cannot populate topology: root node or leaf brokers are null/empty.");
            return;
        }

        // --- Subscriber Generation ---
        ProportionalSubscriberGenerator subscriberGenerator = new ProportionalSubscriberGenerator();
        subscriberGenerator.generateAndAttach(rootNode, leafBrokers, totalSubscribers);

        // --- Publisher (Replica) Generation ---
        // Places replicas in the most populous leaf broker regions (hubs)
        HubBasedPublisherGenerator publisherGenerator = new HubBasedPublisherGenerator();
        
        List<BrokerWithRegion> hubs = leafBrokers.stream()
            .sorted(Comparator.comparingLong(BrokerWithRegion::getInternetPopulation).reversed())
            .limit(numberOfReplicas)
            .collect(Collectors.toList());

        // Note: HubBasedPublisherGenerator may need a similar generic update if it's too specific.
        // For now, we assume it can work with a List<BrokerWithRegion>.
        publisherGenerator.generateAndAttach(hubs, numberOfReplicas);
    }
}