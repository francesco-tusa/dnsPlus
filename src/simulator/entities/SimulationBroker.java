// In: src/simulator/entities/SimulationBroker.java

package simulator.entities;

import java.util.ArrayList; // Import added
import java.util.HashMap;
import java.util.List; // Import added
import java.util.Map;
import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.BrokerWithRegion;
import simulator.visualisation.TopologyVisualiser;

public abstract class SimulationBroker extends TreeNode {

    private final Map<TreeNode, SimulationSubscription> subscriptionsTable = new HashMap<>();
    
    // Store hop counts for ALL processed messages
    private final List<Integer> allProcessedSubscriptionHops = new ArrayList<>();
    private final List<Integer> allProcessedPublicationHops = new ArrayList<>();

    public SimulationBroker(String name) {
        super(name);
    }

    public abstract SimulationSubscription matchPublication(SimulationPublication p);

    public void processPublication(SimulationPublication p) {
        p.incrementHops(); // Increment hop count for the publication
        allProcessedPublicationHops.add(p.getHopCount()); // Store this hop event
        
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null && p.getSource() != null) {
            boolean isUpward = (p.getSource() != getParentBroker());
            visualizer.updatePublicationEdge(p.getSource().getName(), getName(), isUpward);
        }
        matchPublication(p);
    }

    public void processSubscription(SimulationSubscription s) {
        s.incrementHops(); // Increment hop count for the subscription
        allProcessedSubscriptionHops.add(s.getHopCount()); // Store this hop event
        
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null && s.getSource() != null) {
            
            // A subscription is "upward" if its source is one of this broker's children.
            // Otherwise, it must be coming "downward" from the parent.
            boolean isUpward = getChildren().contains(s.getSource());
            visualizer.updateSubscriptionEdge(s.getSource().getName(), getName(), isUpward);
        }
        
        propagateSubscription(s);
    }

    // New abstract method to be implemented by subclasses
    protected abstract void propagateSubscription(SimulationSubscription s);

    public final void addSubscription(SimulationSubscription s) {
        subscriptionsTable.put(s.getSource(), s);
    }

    public BrokerWithRegion getParentBroker() {
        TreeNode parent = getParent();
        if (parent instanceof BrokerWithRegion) {
            return (BrokerWithRegion) parent;
        }
        return null;
    }
    
    public Map<TreeNode, SimulationSubscription> getSubscriptionsTable() {
        return subscriptionsTable;
    }

    /**
     * Gets the list of hop counts for every subscription message processed by this broker.
     * @return A list of hop count integers.
     */
    public List<Integer> getAllProcessedSubscriptionHops() {
        return allProcessedSubscriptionHops;
    }

    /**
     * Gets the list of hop counts for every publication message processed by this broker.
     * @return A list of hop count integers.
     */
    public List<Integer> getAllProcessedPublicationHops() {
        return allProcessedPublicationHops;
    }
}