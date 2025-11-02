// In: src/simulator/entities/SimulationBroker.java

package simulator.entities;

import java.util.ArrayList; 
import java.util.HashMap;
import java.util.List; 
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
    
    // Store the table size for each publication processing event
    private final List<Long> publicationProcessingCosts = new ArrayList<>();


    public SimulationBroker(String name) {
        super(name);
    }

    public abstract SimulationSubscription matchPublication(SimulationPublication p);

    public void processPublication(SimulationPublication p) {
        p.incrementHops(); 
        allProcessedPublicationHops.add(p.getHopCount());
        
        // --- NEW METRIC LOGIC ---
        // Log the size of the table that this publication must be processed against.
        publicationProcessingCosts.add((long) getSubscriptionsTable().size());
        // --- END NEW METRIC LOGIC ---
        
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null && p.getSource() != null) {
            boolean isUpward = (p.getSource() != getParentBroker());
            visualizer.updatePublicationEdge(p.getSource().getName(), getName(), isUpward);
        }
        matchPublication(p);
    }

    public void processSubscription(SimulationSubscription s) {
        s.incrementHops(); 
        allProcessedSubscriptionHops.add(s.getHopCount()); 
        
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null && s.getSource() != null) {
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

    public List<Integer> getAllProcessedSubscriptionHops() {
        return allProcessedSubscriptionHops;
    }

    public List<Integer> getAllProcessedPublicationHops() {
        return allProcessedPublicationHops;
    }
    
    /**
     * Gets the list of subscription table sizes for every publication processed.
     * @return A list of table size (long) data points.
     */
    public List<Long> getPublicationProcessingCosts() {
        return publicationProcessingCosts;
    }
}