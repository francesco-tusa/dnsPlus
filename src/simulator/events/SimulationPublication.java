package simulator.events;

import publishing.Publication;
import simulator.core.TreeNode;

public abstract class SimulationPublication extends Publication {
    private TreeNode source; // The immediate sender of this message copy
    protected int hopCount = 0; // New metric: counts broker hops

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

    public abstract SimulationPublication getPublication();
}