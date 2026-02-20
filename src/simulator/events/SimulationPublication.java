package simulator.events;

import publishing.AbstractPublication;
import simulator.core.TreeNode;
import simulator.events.metrics.EventMetrics;

public class SimulationPublication extends AbstractPublication {

    private static long ID_COUNTER = 0;
    private final long id;
    private TreeNode source;
    
    private int hops = 0;
    protected EventMetrics metrics;
    protected boolean isRoutingDown = false;


    public SimulationPublication() {
        this.id = ++ID_COUNTER;
    }

    public long getId() { return id; }

    public TreeNode getSource() { return source; }
    public void setSource(TreeNode source) { this.source = source; }

    public EventMetrics getMetrics() { return metrics; }
    public void setMetrics(EventMetrics metrics) { this.metrics = metrics; }

    public int getHops() { return hops; }
    public void setHops(int hops) { this.hops = hops; }

    public void markAsRoutingDown() { this.isRoutingDown = true; }
    public boolean isRoutingDown() { return isRoutingDown; }
    
    public void incrementHops() { 
        this.hops++; 
    }

    public void copyStateFrom(SimulationPublication other) {
        this.hops = other.hops;
        this.metrics = other.metrics;
        this.isRoutingDown = other.isRoutingDown;
    }

    public SimulationPublication getPublication() {
        return new SimulationPublication();
    }
    
    public String toDisplayString() { return toString(); }
    
    @Override
    public String toString() { return "Publication-" + id; }
}