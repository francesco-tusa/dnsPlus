package simulator.topology;

import simulator.entities.SimulationBroker;

/**
 * Abstract base class for topology factories, defining the steps for generation.
 *
 * @param <C> The specific type of TopologyConfiguration required by the subclass.
 * @param <R> A generic SimulationBroker.
 */
public abstract class AbstractTopologyFactory<C extends TopologyConfiguration, R extends SimulationBroker>
        implements TopologyFactory<C, R> {

    protected C config;
    protected R rootNode;
    protected int brokerIdCounter;
    protected int leafBrokerIdCounter;
    protected int subscriberIdCounter;
    protected int publisherIdCounter;

    @Override
    public final R generateTopology(TopologyConfiguration genericConfig) {
        initialise(genericConfig);
        return buildCoreTopology();
    }

    // These methods are now public to be called explicitly by the simulation runners.
    public abstract void attachSubscribers(R root);
    public abstract void attachPublishers(R root);

    protected abstract void initialise(TopologyConfiguration config);
    protected abstract R buildCoreTopology();

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