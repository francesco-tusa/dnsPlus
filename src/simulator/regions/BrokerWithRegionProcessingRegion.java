package simulator.regions;

import java.util.Map;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.visualisation.TopologyVisualiser;
import utils.CustomLogger;

public class BrokerWithRegionProcessingRegion extends BrokerWithRegion {

    private static final Logger logger = CustomLogger.getLogger(BrokerWithRegionProcessingRegion.class.getName());

    public BrokerWithRegionProcessingRegion(String name) {
        super(name);
    }

    public BrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    public void processSubscription(SimulationSubscription s) {
        logger.fine(getName() + ": processing a subscription received from " + s.getSource().getName());

        boolean isCovered = false;
        // Check for coverage only if the subscription is coming from below.
        if (s instanceof SubscriptionWithRegion newSub && s.getSource() != getParentBroker()) {
            for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
                // Check against other subscriptions that also came from below.
                if (entry.getKey() != getParentBroker()
                        && entry.getValue() instanceof SubscriptionWithRegion existingSub) {
                    if (existingSub.getRegion().contains(newSub.getRegion())) {
                        logger.fine(getName() + ": Filtering upward propagation for subscription from "
                                + s.getSource().getName() + ". Reason: Covered by existing subscription from "
                                + entry.getKey().getName() + ".");
                        isCovered = true;
                        break; // A covering subscription was found.
                    }
                }
            }
        }

        // Crucially, always add the subscription to the local table for routing
        // publications.
        addSubscription(s);

        if (s.getSource() == getParentBroker()) {
            // If the subscription came from the parent, only propagate it downwards.
            sendSubscriptionToChildren(s);
        } else {
            // If the subscription came from a client or child...
            // ...only propagate it upwards if it was not covered.
            if (!isCovered) {
                super.processSubscription(s); // This propagates to the parent.
            }
            // ...and always propagate it downwards to other interested children.
            sendSubscriptionToChildren(s);
        }
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        logger.fine(getName() + ": processing a publication received from " + p.getSource().getName());
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode nextNode = entry.getKey();
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

    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        if (newSubscription instanceof SubscriptionWithRegion newSub) {
            for (TreeNode child : getChildren()) {
                if (child == newSub.getSource())
                    continue;
                if (child instanceof BrokerWithRegion childBroker) {
                    if (childBroker.getRegion() != null && childBroker.getRegion().intersects(newSub.getRegion())) {
                        logger.fine(getName() + ": forwarding subscription to child " + childBroker.getName());
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