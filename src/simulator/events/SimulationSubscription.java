package simulator.events;

import simulator.core.TreeNode;
import simulator.events.metrics.EventMetrics;
import subscribing.AbstractSubscription;

public abstract class SimulationSubscription extends AbstractSubscription {

    private static long ID_COUNTER = 0;
    private final long id;
    private TreeNode source;
    
    private int hops = 0;
    
    protected EventMetrics metrics;

    public SimulationSubscription() {
        this.id = ++ID_COUNTER;
    }

    public long getId() { return id; }

    public TreeNode getSource() { return source; }
    public void setSource(TreeNode source) { this.source = source; }

    public EventMetrics getMetrics() { return metrics; }
    public void setMetrics(EventMetrics metrics) { this.metrics = metrics; }

    public int getHops() { return hops; }
    public void setHops(int hops) { this.hops = hops; }
    
    public void incrementHops() { 
        this.hops++; 
    }

    public void copyStateFrom(SimulationSubscription other) {
        this.hops = other.hops;
        
        if (other.metrics != null) {
            this.metrics = new EventMetrics(other.metrics);
        }
    }

    public abstract String toDisplayString();
    public abstract SimulationSubscription getSubscription();

    @Override
    public String toString() { return "Subscription-" + id; }
}