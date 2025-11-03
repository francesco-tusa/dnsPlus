package simulator.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger; // Import Logger

import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.BrokerWithRegion;
import simulator.visualisation.TopologyVisualiser;
import utils.CustomLogger; // Import CustomLogger

// Import the region classes to perform the expansion
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;


public abstract class SimulationBroker extends TreeNode {

    // Add logger
    private static final Logger logger = CustomLogger.getLogger(SimulationBroker.class.getName());

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
        
        // Log the size of the table that this publication must be processed against.
        publicationProcessingCosts.add((long) getSubscriptionsTable().size());
        
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

    /**
     * Adds a subscription to the broker's main subscription table.
     * This method now implements the "expand on update" logic
     * as described in your design document.
     *
     * @param s The subscription to add or merge.
     */
    public final void addSubscription(SimulationSubscription s) {
        if (s.getSource() == null) {
            throw new IllegalArgumentException("Subscription source cannot be null");
        }

        // 1. Check if an entry from this source *already* exists.
        SimulationSubscription existingSub = subscriptionsTable.get(s.getSource());

        if (existingSub == null) {
            // --- Case 1: No existing subscription. Just add it. ---
            logger.fine(String.format("%s: adding new subscription entry for %s", getName(), s.getSource().getName()));
            subscriptionsTable.put(s.getSource(), s);

        } else {
            // --- Case 2: Entry exists. We must expand it. ---
            
            // Check if both are region-based subscriptions
            if (existingSub instanceof SubscriptionWithRegion existingRegionSub &&
                s instanceof SubscriptionWithRegion newRegionSub) {
                
                // Get the regions
                Region existingRegion = existingRegionSub.getRegion();
                Region newRegion = newRegionSub.getRegion();

                // Only expand if the new region isn't already covered
                if (!existingRegion.contains(newRegion)) {
                    logger.fine(String.format("%s: expanding existing subscription for %s to include %s",
                            getName(), s.getSource().getName(), newRegion.toShortString()));
                    
                    existingRegion.expand(newRegion);
                    // We don't need to 'put' again, as we modified the Region object in-place.
                } else {
                    logger.fine(String.format("%s: filtering redundant subscription from %s",
                            getName(), s.getSource().getName()));
                }

            } else {
                // Fallback for non-region types or mismatched types: just overwrite.
                logger.fine(String.format("%s: overwriting existing subscription entry for %s", getName(), s.getSource().getName()));
                subscriptionsTable.put(s.getSource(), s);
            }
        }
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

