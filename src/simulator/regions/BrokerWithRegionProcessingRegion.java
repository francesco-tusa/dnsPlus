package simulator.regions;

import java.util.Map;
import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;

/**
 * A broker that implements region-based routing. It forwards subscriptions to
 * children with intersecting regions and forwards publications to nodes with
 * matching subscription regions.
 */
public class BrokerWithRegionProcessingRegion extends BrokerWithRegion {

    public BrokerWithRegionProcessingRegion(String name) {
        super(name);
    }

    public BrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    public void processSubscription(SimulationSubscription s) {
        System.out.println(getName() + ": processing a subscription received from " + s.getSource().getName());
        
        addSubscription(s);

        if (s.getSource() == getParentBroker()) {
            sendSubscriptionToChildren(s);
        } else {
            sendSubscriptionToChildren(s);
            super.processSubscription(s);
        }
    }

    /**
     * This is the main fix. The logic is now separated to handle upward and
     * downward propagation correctly, preventing routing loops.
     */
    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing a publication received from " + p.getSource().getName());

        // A publication can only go UP from a child, or DOWN from a parent.
        if (p.getSource() != getParentBroker()) {
            // This is an UPWARD message from a child.
            // Action: Propagate it further up to our parent.
            propagatePublicationUpward(p);
        } else {
            // This is a DOWNWARD message from our parent.
            // Action: Only check if our children or subscribers need this publication.
            processPublicationForDownwardPropagation(p);
        }
        
        return null;
    }

    private void propagatePublicationUpward(SimulationPublication p) {
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker != null) {
            System.out.println(getName() + ": forwarding publication to parent " + parentBroker.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parentBroker.processPublication(forwardedCopy);
        }
    }

    private void processPublicationForDownwardPropagation(SimulationPublication p) {
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode nextNode = entry.getKey();
            SimulationSubscription tableEntry = entry.getValue();

            // Do not send the publication back to the parent who sent it to us.
            if (nextNode == getParentBroker()) {
                continue;
            }

            if (tableEntry instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
                if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                    System.out.println(getName() + ": publication location is within " + nextNode.getName() + "'s subscribed region.");
                    SimulationPublication forwardedCopy = p.getPublication();
                    forwardedCopy.setSource(this);
                    forwardPublicationToNode(forwardedCopy, nextNode);
                }
            }
        }
    }

    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        if (newSubscription instanceof SubscriptionWithRegion newSub) {
            for (TreeNode child : getChildren()) {
                if (child instanceof BrokerWithRegion childBroker && child != newSub.getSource()) {
                    if (childBroker.getRegion() != null && childBroker.getRegion().intersects(newSub.getRegion())) {
                        System.out.println(getName() + ": forwarding subscription to child " + childBroker.getName());
                        SubscriptionWithRegion subscriptionToSend = new SubscriptionWithRegion(newSub.getRegion());
                        subscriptionToSend.setSource(this);
                        subscriptionToSend.setOriginalSource(newSub.getOriginalSource());
                        childBroker.processSubscription(subscriptionToSend);
                    }
                }
            }
        }
    }

    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        if (next instanceof BrokerWithRegion broker) {
            broker.processPublication(p);
        } else if (next instanceof SubscriberWithLocation subscriber) {
            subscriber.receive(p);
        }
    }
}