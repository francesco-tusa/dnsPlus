package simulator.regions;

import java.util.Map;
import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;

public class BrokerWithRegionProcessingRegion extends BrokerWithRegion {

    public BrokerWithRegionProcessingRegion(String name) {
        super(name);
    }

    public BrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    /**
     * This is the corrected subscription logic. It distinguishes between
     * subscriptions received from a parent and those from a child to prevent
     * infinite loops, which was the cause of the StackOverflowError.
     */
    @Override
    public void processSubscription(SimulationSubscription s) {
        System.out.println(getName() + ": processing a subscription received from " + s.getSource().getName());
        addSubscription(s);

        // If the subscription is from a parent, propagate it ONLY down to children.
        if (s.getSource() == getParentBroker()) {
            sendSubscriptionToChildren(s);
        } 
        // If the subscription is from a client or a child, propagate it BOTH up and down.
        else {
            super.processSubscription(s);
            sendSubscriptionToChildren(s);
        }
    }

    /**
     * Correctly handles publication routing. It checks all subscription entries
     * and forwards the publication to every matching node (parent or child),
     * except for the immediate source of the message.
     */
    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing a publication received from " + p.getSource().getName());
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode nextNode = entry.getKey();
            
            // Do not forward the publication back to the immediate source.
            if (nextNode == p.getSource()) {
                continue;
            }

            if (entry.getValue() instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
                if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                    System.out.println(getName() + ": forwarding publication to " + nextNode.getName());
                    SimulationPublication forwardedCopy = p.getPublication();
                    forwardedCopy.setSource(this);
                    forwardPublicationToNode(forwardedCopy, nextNode);
                }
            }
        }
        return null;
    }

    /**
     * Forwards a subscription to all children with intersecting regions, crucially
     * ensuring it does not send it back to the source child.
     */
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        if (newSubscription instanceof SubscriptionWithRegion newSub) {
            for (TreeNode child : getChildren()) {
                // The critical check to prevent infinite loops.
                if (child == newSub.getSource()) {
                    continue;
                }

                if (child instanceof BrokerWithRegion childBroker) {
                    if (childBroker.getRegion() != null && childBroker.getRegion().intersects(newSub.getRegion())) {
                        System.out.println(getName() + ": forwarding subscription to child " + childBroker.getName());
                        SubscriptionWithRegion subscriptionToSend = new SubscriptionWithRegion(newSub.getRegion());
                        subscriptionToSend.setSource(this);
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
