package simulator.events;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import simulator.core.TreeNode;
import simulator.events.metrics.EventMetrics;

/**
 * This class implements TrackableEvent and composes EventMetrics
 * to handle shared hop counting and path tracking.
 */
public abstract class SimulationPublication implements TrackableEvent {

    private static final AtomicLong nextId = new AtomicLong(0);
    private final long id;
    private TreeNode source;
    
    protected EventMetrics metrics;

    public SimulationPublication() {
        this.id = nextId.getAndIncrement();
        this.metrics = new EventMetrics();
    }

    public long getId() {
        return id;
    }

    public TreeNode getSource() {
        return source;
    }

    public void setSource(TreeNode source) {
        this.source = source;
    }
    
    @Override
    public int getHops() {
        return this.metrics.getHops();
    }

    @Override
    public void incrementHops() {
        this.metrics.incrementHops();
    }
    
    @Override
    public void addBrokerToPath(String brokerName) {
        this.metrics.addBrokerToPath(brokerName);
    }

    @Override
    public List<String> getBrokerPath() {
        return this.metrics.getBrokerPath();
    }

    @Override
    public void addBrokerRegionToPath(String regionInfo) {
        this.metrics.addBrokerRegionToPath(regionInfo);
    }

    @Override
    public List<String> getBrokerRegionPath() {
        return this.metrics.getBrokerRegionPath();
    }

    public abstract SimulationPublication getPublication();

    public abstract String toDisplayString();
}