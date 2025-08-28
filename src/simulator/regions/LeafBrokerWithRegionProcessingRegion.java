package simulator.regions;

import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;

import java.util.Map;

/**
 * The leaf-level broker for the region-based routing strategy.
 * It is responsible for delivering publications to its subscribers and for
 * defining the initial region based on the locations of its subscribers.
 */
public class LeafBrokerWithRegionProcessingRegion extends BrokerWithRegionProcessingRegion {

    public LeafBrokerWithRegionProcessingRegion(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    public void addChild(TreeNode child) {
        super.addChild(child);
        System.out.println(getName() + ": added new " + child.getName());
        // When a subscriber is added, update the leaf's region and propagate the change upwards.
        updateRegion(child);
    }

    @Override
    public void updateRegion(TreeNode child) {
        if (child instanceof SubscriberWithLocation subscriber) {
            if (getRegion().expand(subscriber.getLocation())) {
                increaseNumOfRegionUpdates();
                System.out.println(getName() + ": updated region");
                BrokerWithRegion parentBroker = getParentBroker();
                if (parentBroker != null) {
                    parentBroker.updateRegion(this);
                }
            }
        } 
    }

    /**
     * Overrides the parent method. A leaf broker has no child brokers to forward subscriptions to,
     * so this method does nothing.
     */
    @Override
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        // Intentionally empty as leaf nodes do not have child brokers.
    }

    /**
     * Overrides the parent method. For a leaf broker, the next node for a publication
     * can only be a subscriber. This method delivers the publication.
     */
    @Override
    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        if (next instanceof SubscriberWithLocation subscriber) {
            System.out.println(getName() + ": Delivering publication to subscriber " + subscriber.getName());
            subscriber.receive(p);
        } else {
            System.err.println(getName() + ": Topology error. Leaf broker tried to forward publication to a non-subscriber node: " + next.getName());
        }
    }

    /**
     * Overrides the matching logic for a leaf broker.
     * A leaf broker's primary job is to check its directly connected subscribers.
     */
    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing a publication received from " + p.getSource().getName());

        // Propagate publication upwards to the parent first.
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker != null) {
            System.out.println(getName() + ": forwarding publication to parent " + parentBroker.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parentBroker.processPublication(forwardedCopy);
        }

        // Check for matches with directly connected subscribers.
        for (TreeNode child : getChildren()) {
            if (child instanceof SubscriberWithLocation) {
                // A leaf broker doesn't use a subscription table for its children; it checks them directly.
                // We'll create a temporary subscription to represent the subscriber's interest.
                SubscriptionWithRegion subRegion = new SubscriptionWithRegion(this.getRegion()); // The subscriber is in this broker's region.
                
                if (p instanceof PublicationWithLocation pubLocation) {
                    if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                        forwardPublicationToNode(p, child);
                    }
                }
            }
        }
        return null;
    }
}