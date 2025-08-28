package simulator.topology;

import java.util.Random;
import simulator.regions.BrokerWithRegion;

/**
 * Abstract base class for topology factories, defining the steps for generation.
 *
 * @param <C> The specific type of TopologyConfiguration required by the subclass.
 * @param <R> The specific type of the root TreeNode, constrained to be a BrokerWithRegion.
 */
public abstract class AbstractTopologyFactory<C extends TopologyConfiguration, R extends BrokerWithRegion>
        implements TopologyFactory<C, R> { // Correctly implements the generic interface

    protected C config;
    protected R rootNode;
    protected int brokerIdCounter;
    protected int leafBrokerIdCounter;
    protected int subscriberIdCounter;
    protected int publisherIdCounter;

    @Override
    public final R generateTopology(TopologyConfiguration genericConfig) {
        initialise(genericConfig);
        this.rootNode = buildCoreTopology();
        attachSubscribers(this.rootNode);
        attachPublishers(this.rootNode);
        return this.rootNode;
    }

    protected abstract void initialise(TopologyConfiguration config);

    protected abstract R buildCoreTopology();
    
    protected abstract void attachSubscribers(R root);
    
    protected abstract void attachPublishers(R root);

    // --- Helper Methods ---
    protected String generateBrokerName() {
        return "Broker-" + brokerIdCounter++;
    }

    protected String generateLeafBrokerName() {
        return "Leaf-" + leafBrokerIdCounter++;
    }

    protected String generateSubscriberName() {
        return "Sub-" + subscriberIdCounter++;
    }
    
    protected String generatePublisherName() {
        return "Pub-" + publisherIdCounter++;
    }
}