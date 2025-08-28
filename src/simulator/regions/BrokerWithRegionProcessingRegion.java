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

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing a publication received from " + p.getSource().getName());

        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker != null) {
            System.out.println(getName() + ": forwarding publication to parent " + parentBroker.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parentBroker.processPublication(forwardedCopy);
        }

        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode nextNode = entry.getKey();
            SimulationSubscription tableEntry = entry.getValue();

            if (nextNode == p.getSource()) {
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
        return null;
    }

    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        if (newSubscription instanceof SubscriptionWithRegion newSub) {
            for (TreeNode child : getChildren()) {
                if (child instanceof BrokerWithRegion childBroker && childBroker != newSub.getSource()) {
                    if (childBroker.getRegion().intersects(newSub.getRegion())) {
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