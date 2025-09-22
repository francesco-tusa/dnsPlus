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
     * Tracks which subscription regions have already been propagated to parent or
     * child brokers to prevent sending redundant messages.
     * Key: The TreeNode (parent or child) the subscription was sent to.
     * Value: The SimulationSubscription that was sent.
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

    /**
     * This method now correctly implements the abstract method from
     * SimulationBroker.
     * It contains the specific routing logic for region-based subscriptions.
     */
    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        logger.fine(String.format("%s (Region: %s): processing a subscription received from %s",
                getName(), getRegion().toShortString(), s.getSource().getName()));

        if (!(s instanceof SubscriptionWithRegion newSub)) {
            // If it's not a region subscription, we can simply stop or handle differently.
            // Based on current logic, we can just return.
            return;
        }

        addSubscription(newSub);

        if (s.getSource() != getParentBroker()) {
            propagateSubscriptionUpward(newSub);
        }

        propagateSubscriptionDownward(newSub);
    }

    private void propagateSubscriptionUpward(SubscriptionWithRegion newSub) {
        BrokerWithRegion parent = getParentBroker();
        if (parent == null)
            return;

        SubscriptionWithRegion existingSubForParent = (SubscriptionWithRegion) propagatedSubscriptions.get(parent);
        if (existingSubForParent != null && existingSubForParent.getRegion().contains(newSub.getRegion())) {
            logger.fine(getName() + ": Filtering upward propagation for " + newSub.getRegion().toShortString() +
                    ". Reason: Covered by existing propagated subscription to parent.");
            return;
        }

        logger.fine(getName() + ": Propagating subscription upwards to parent " + parent.getName());
        SimulationSubscription subscriptionToSend = newSub.getSubscription();
        subscriptionToSend.setSource(this);
        parent.processSubscription(subscriptionToSend);
        propagatedSubscriptions.put(parent, subscriptionToSend);
    }

    private void propagateSubscriptionDownward(SubscriptionWithRegion newSub) {
        for (TreeNode child : getChildren()) {
            if (child == newSub.getSource() || !(child instanceof BrokerWithRegion childBroker)) {
                continue;
            }

            if (childBroker.getRegion() != null && childBroker.getRegion().intersects(newSub.getRegion())) {
                logger.fine(String.format("%s: Subscription region %s intersects with child %s's region %s. Forwarding...",
                        getName(), newSub.getRegion().toShortString(), childBroker.getName(), childBroker.getRegion().toShortString()));

                SubscriptionWithRegion existingSubForChild = (SubscriptionWithRegion) propagatedSubscriptions
                        .get(childBroker);
                if (existingSubForChild != null && existingSubForChild.getRegion().contains(newSub.getRegion())) {
                    logger.fine(getName() + ": Filtering downward propagation to " + childBroker.getName() +
                            ". Reason: Covered by existing propagated subscription.");
                    continue;
                }

                SubscriptionWithRegion subscriptionToSend = new SubscriptionWithRegion(newSub.getRegion());
                subscriptionToSend.setSource(this);
                childBroker.processSubscription(subscriptionToSend);
                propagatedSubscriptions.put(childBroker, subscriptionToSend);
            }
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

    /**
     * This method is now redundant and should not be used.
     * The logic has been moved to propagateSubscriptionDownward and the visualiser
     * calls are handled by the parent SimulationBroker.
     */
    @Deprecated
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        if (newSubscription instanceof SubscriptionWithRegion newSub) {
            for (TreeNode child : getChildren()) {
                if (child == newSub.getSource())
                    continue;
                if (child instanceof BrokerWithRegion childBroker) {
                    if (childBroker.getRegion() != null && childBroker.getRegion().intersects(newSub.getRegion())) {
                        logger.fine(getName() + ": forwarding subscription to child " + childBroker.getName());
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