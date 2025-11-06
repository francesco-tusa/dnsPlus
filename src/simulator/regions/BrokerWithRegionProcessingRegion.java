package simulator.regions;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import utils.CustomLogger;

public class BrokerWithRegionProcessingRegion extends BrokerWithRegion {

    private static final Logger logger = CustomLogger.getLogger(BrokerWithRegionProcessingRegion.class.getName());

    /**
     * Tracks the *union* of all subscription regions propagated to a neighbor.
     * Key: The TreeNode (parent or child) the subscription was sent to.
     * Value: A SubscriptionWithRegion containing the *merged* region.
     */
    private final Map<TreeNode, SimulationSubscription> propagatedSubscriptions = new HashMap<>();

    public BrokerWithRegionProcessingRegion(String name) {
        super(name);
    }

    public BrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    public Map<TreeNode, SimulationSubscription> getPropagatedSubscriptions() {
        return propagatedSubscriptions;
    }

    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        logger.fine(String.format("%s (Region: %s): processing a subscription received from %s",
                getName(), getRegion().toShortString(), s.getSource().getName()));

        if (!(s instanceof SubscriptionWithRegion newSub)) {
            return;
        }

        addSubscription(newSub); // Store this subscription for publication matching

        // Propagate upward (unless it came from the parent)
        if (s.getSource() != getParentBroker()) {
            propagateSubscriptionUpward(newSub);
        }

        // Propagate downward (to all children except the source)
        propagateSubscriptionDownward(newSub);
    }

    /**
     * Correctly propagates a subscription upward, expanding the region if needed.
     */
    private void propagateSubscriptionUpward(SubscriptionWithRegion newSub) {
        BrokerWithRegion parent = getParentBroker();
        if (parent == null)
            return;
        
        // Call the refactored helper method
        propagateOrExpandSubscription(newSub, parent, "upward");
    }

    /**
     * Correctly propagates a subscription downward, expanding the region if needed.
     */
    private void propagateSubscriptionDownward(SubscriptionWithRegion newSub) {
        for (TreeNode child : getChildren()) {
            // Skip the source of the subscription and any non-broker children
            if (child == newSub.getSource() || !(child instanceof BrokerWithRegion childBroker)) {
                continue;
            }

            // Only propagate if the child's region intersects with the subscription
            if (childBroker.getRegion() != null && childBroker.getRegion().intersects(newSub.getRegion())) {
                logger.fine(String.format("%s: Subscription region %s intersects with child %s's region %s. Forwarding...",
                        getName(), newSub.getRegion().toShortString(), childBroker.getName(), childBroker.getRegion().toShortString()));
                
                // Call the refactored helper method
                propagateOrExpandSubscription(newSub, childBroker, "downward");
            }
        }
    }

    /**
     * Checks if a new subscription is covered by an existing propagated subscription
     * for a given neighbor. If not, it expands the existing region and propagates
     * the new, larger subscription.
     *
     * @param newSub The new subscription to process.
     * @param neighbor The neighboring broker (parent or child) to propagate to.
     * @param direction A string for logging (e.g., "upward", "downward").
     */
    private void propagateOrExpandSubscription(SubscriptionWithRegion newSub, TreeNode neighbor, String direction) {
        
        SubscriptionWithRegion existingSub = (SubscriptionWithRegion) propagatedSubscriptions.get(neighbor);

        if (existingSub == null) {
            // --- Case 1: First Time ---
            // This is the first subscription we are propagating to this neighbor.
            logger.fine(String.format("%s: Propagating first subscription %s to %s %s",
                    getName(), direction, neighbor.getName(), newSub.getRegion().toShortString()));
            
            // Create a copy to send
            SubscriptionWithRegion subscriptionToSend = (SubscriptionWithRegion) newSub.getSubscription();
            subscriptionToSend.setSource(this);
            
            // Send to the neighbor
            if (neighbor instanceof BrokerWithRegion broker) {
                broker.processSubscription(subscriptionToSend);
            }
            
            // Store what we sent
            propagatedSubscriptions.put(neighbor, subscriptionToSend);

        } else if (existingSub.getRegion().contains(newSub.getRegion())) {
            // --- Case 2: Filter (Redundant) ---
            // The subscription we *already* sent to this neighbor
            // is larger than (or equal to) this new one.
            logger.fine(String.format("%s: Filtering %s propagation to %s for %s. Reason: Covered by existing sub %s.",
                    getName(), direction, neighbor.getName(), newSub.getRegion().toShortString(), existingSub.getRegion().toShortString()));
            return; // Filter

        } else {
            // --- Case 3: Expand (Not Covered) ---
            // The new subscription is not covered by what we sent before.
            // We must create a new, larger region that is the UNION of the old and new.
            
            incrementPropagationFilterExpansions(); // for performance metric
            
            logger.fine(String.format("%s: Expanding %s propagated region for %s to include %s",
                    getName(), direction, neighbor.getName(), newSub.getRegion().toShortString()));

            // 1. Create a new Region object from the one we stored
            Region expandedRegion = new Region(existingSub.getRegion());
            // 2. Expand it to include the new region
            expandedRegion.expand(newSub.getRegion());

            // 3. Create a new subscription based on this expanded region
            SubscriptionWithRegion expandedSubscription = new SubscriptionWithRegion(expandedRegion);
            expandedSubscription.setSource(this);

            // 4. Propagate the new, larger subscription
            if (neighbor instanceof BrokerWithRegion broker) {
                broker.processSubscription(expandedSubscription);
            }

            // 5. Store the new, larger subscription in our map, replacing the old one
            propagatedSubscriptions.put(neighbor, expandedSubscription);
        }
    }

    /**
     * Adds a subscription to the main table, implementing region-expansion logic.
     * This logic is specific to region-based routing.
     * @param s The subscription to add or merge.
     */
    @Override
    public void addSubscription(SimulationSubscription s) {
        if (s.getSource() == null) {
            throw new IllegalArgumentException("Subscription source cannot be null");
        }

        Map<TreeNode, SimulationSubscription> mainTable = getSubscriptionsTable();
        SimulationSubscription existingSub = mainTable.get(s.getSource());

        if (existingSub == null) {
            // --- Case 1: No existing subscription. Just add it. ---
            logger.fine(String.format("%s: adding new subscription entry for %s", getName(), s.getSource().getName()));
            mainTable.put(s.getSource(), s);

        } else {
            // --- Case 2: Entry exists. Check for region expansion. ---
            if (existingSub instanceof SubscriptionWithRegion existingRegionSub &&
                s instanceof SubscriptionWithRegion newRegionSub) {
                
                Region existingRegion = existingRegionSub.getRegion();
                Region newRegion = newRegionSub.getRegion();

                if (!existingRegion.contains(newRegion)) {
                    // --- Increment the counter from the parent class ---
                    incrementMainTableExpansions(); 
                    
                    logger.fine(String.format("%s: expanding existing subscription for %s to include %s",
                            getName(), s.getSource().getName(), newRegion.toShortString()));
                    
                    existingRegion.expand(newRegion);
                } else {
                    logger.fine(String.format("%s: filtering redundant subscription from %s",
                            getName(), s.getSource().getName()));
                }

            } else {
                // Fallback for non-region types or mismatched types: just overwrite.
                logger.fine(String.format("%s: overwriting existing subscription entry for %s", getName(), s.getSource().getName()));
                mainTable.put(s.getSource(), s);
            }
        }
    }


    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        logger.fine(getName() + ": processing a publication received from " + p.getSource().getName());
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode nextNode = entry.getKey();
            // This is the critical check to prevent reflection loops
            if (nextNode == p.getSource())
                continue;

            if (entry.getValue() instanceof SubscriptionWithRegion subRegion
                    && p instanceof PublicationWithLocation pubLocation) {
                
                if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                    logger.fine(getName() + ": forwarding publication to " + nextNode.getName());
                    SimulationPublication forwardedCopy = p.getPublication();
                    forwardedCopy.setSource(this);
                    forwardPublicationToNode(forwardedCopy, nextNode);
                }
            }
        }
        return null;
    }

    /**
     * This method is deprecated as its logic is now contained in propagateSubscriptionDownward.
     */
    @Deprecated
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        // This logic is now incorrect as it does not implement region expansion.
        // See propagateSubscriptionDownward for the correct implementation.
    }

    /**
     * Helper to forward a publication to the correct node type.
     */
    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        if (next instanceof BrokerWithRegion broker) {
            broker.processPublication(p);
        } else if (next instanceof SubscriberWithLocation subscriber) {
            subscriber.receive(p);
        }
    }
}
