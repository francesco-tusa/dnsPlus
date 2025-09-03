package simulator.regions;

import java.util.Map;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.visualisation.TopologyVisualiser;

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

        if (s instanceof SubscriptionWithRegion) {
            SubscriptionWithRegion newSub = (SubscriptionWithRegion) s;
            // Check for covering
            for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
                // We only check against subscriptions that have already been propagated from other children
                if (entry.getKey() != getParentBroker() && entry.getKey() != s.getSource() && entry.getValue() instanceof SubscriptionWithRegion) {
                    SubscriptionWithRegion existingSub = (SubscriptionWithRegion) entry.getValue();
                    if (existingSub.getRegion().contains(newSub.getRegion())) {
                        // This is the added log message for when a subscription is filtered
                        System.out.println(getName() + ": Filtering subscription from " + s.getSource().getName() + ". Reason: Covered by existing subscription from " + entry.getKey().getName() + ".");
                        return; // Stop propagation
                    }
                }
            }
        }


        // The visualiser hook is now called automatically by the superclass method
        if (s.getSource() == getParentBroker()) {
            sendSubscriptionToChildren(s);
        } else {
            super.processSubscription(s); // Propagate upwards (includes visualisation)
            sendSubscriptionToChildren(s);
        }
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing a publication received from " + p.getSource().getName());
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode nextNode = entry.getKey();
            if (nextNode == p.getSource()) continue;

            if (entry.getValue() instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
                if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                    System.out.println(getName() + ": forwarding publication to " + nextNode.getName());
                    SimulationPublication forwardedCopy = p.getPublication();
                    forwardedCopy.setSource(this);
                    // The processPublication method in SimulationBroker will handle highlighting
                    forwardPublicationToNode(forwardedCopy, nextNode);
                }
            }
        }
        return null;
    }

    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        if (newSubscription instanceof SubscriptionWithRegion) {
            SubscriptionWithRegion newSub = (SubscriptionWithRegion) newSubscription;
            for (TreeNode child : getChildren()) {
                if (child == newSub.getSource()) continue;
                if (child instanceof BrokerWithRegion) {
                    BrokerWithRegion childBroker = (BrokerWithRegion) child;
                    if (childBroker.getRegion() != null && childBroker.getRegion().intersects(newSub.getRegion())) {
                        System.out.println(getName() + ": forwarding subscription to child " + childBroker.getName());

                        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
                        if (visualizer != null) {
                            visualizer.updateSubscriptionEdge(getName(), childBroker.getName());
                        }

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