package simulator.events;

import simulator.core.TreeNode;
import simulator.events.metrics.EventMetrics;

public abstract class SimulationSubscription {

    private static long ID_COUNTER = 0;
    private final long id;
    private TreeNode source;
    
    protected EventMetrics metrics;

    public SimulationSubscription() {
        this.id = ++ID_COUNTER;
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

    public EventMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(EventMetrics metrics) {
        this.metrics = metrics;
    }
    
    /**
     * Returns a string suitable for the TopologyVisualiser labels.
     */
    public abstract String toDisplayString();

    /**
     * Prototype Pattern: Creates a deep copy of the subscription.
     */
    public abstract SimulationSubscription getSubscription();

    @Override
    public String toString() {
        return "Subscription-" + id;
    }
}