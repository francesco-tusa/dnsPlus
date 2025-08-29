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

        // If the subscription is from a parent, propagate it down to children.
        if (s.getSource() == getParentBroker()) {
            sendSubscriptionToChildren(s);
        }
        // If the subscription is from a client or a child, propagate it up to the
        // parent.
        else {
            // The call to super.processSubscription handles the upward propagation.
            super.processSubscription(s);
        }
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing a publication received from " + p.getSource().getName());

        // 1. Always propagate the publication upwards to the parent.
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker != null && p.getSource() != parentBroker) {
            System.out.println(getName() + ": Propagating publication upwards to " + parentBroker.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parentBroker.processPublication(forwardedCopy);
        }

        // 2. Always process the publication for downward propagation to children.
        processPublicationForDownwardPropagation(p);
        
        return null;
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