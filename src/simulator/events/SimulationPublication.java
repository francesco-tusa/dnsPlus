package simulator.events;

import simulator.core.TreeNode;
import simulator.events.metrics.EventMetrics;

public class SimulationPublication {

    private static long ID_COUNTER = 0;
    private final long id;
    private TreeNode source;
    
    protected EventMetrics metrics;

    public SimulationPublication() {
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
     * Creates a copy of the publication for forwarding.
     * Subclasses MUST override this to preserve payload data.
     */
    public SimulationPublication getPublication() {
        return new SimulationPublication();
    }
    
    /**
     * Returns a string suitable for the TopologyVisualiser labels.
     */
    public String toDisplayString() {
        return toString();
    }
    
    @Override
    public String toString() {
        return "Publication-" + id;
    }
}