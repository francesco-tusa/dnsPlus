package simulator.events;

import java.util.ArrayList;
import java.util.List;

import simulator.core.TreeNode;

/**
 * Represents a generic subscription in the simulation.
 */
public class SimulationSubscription {
    private TreeNode source;
    protected int hopCount = 0; // New metric: counts broker hops

    public SimulationSubscription() {
        this.source = null;
    }

    public TreeNode getSource() {
        return source;
    }

    public void setSource(TreeNode source) {
        this.source = source;
    }
    
    /**
     * Increments the hop count for this message.
     */
    public void incrementHops() {
        hopCount++;
    }

    /**
     * Gets the total hop count for this message.
     * @return The hop count.
     */
    public int getHopCount() {
        return hopCount;
    }

    /**
     * A method to provide a consistent, short string representation for any subscription type.
     * This will be overridden by subclasses to provide specific details.
     * @return A string formatted for display in the visualiser.
     */
    public String toDisplayString() {
        return ""; // Default implementation returns an empty string.
    }


    /**
     * Creates a shallow copy of the subscription.
     * Subclasses should override to copy their specific fields.
     */
    public SimulationSubscription getSubscription() {
        SimulationSubscription copy = new SimulationSubscription();
        copy.setSource(this.source);
        copy.hopCount = this.hopCount; // Copy the hop count
        return copy;
    }
}