package simulator;

import java.util.List;

public class BrokerWithRegionProcessingRegion extends BrokerWithRegion {

    public BrokerWithRegionProcessingRegion(String name) {
        super(name);
    }

    @Override
    public boolean regionsOrLocationsMatch(SimulationSubscription existingSubscription, SimulationSubscription newSubscription) {
        SubscriptionWithRegion newSubscriptionWithRegion = ((SubscriptionWithRegion) newSubscription);
        SubscriptionWithRegion existingSubscriptionWithRegion = ((SubscriptionWithRegion) existingSubscription);

        // FIXME: This might require checking for intersections and do specific
        // operations accordingly
        if (existingSubscriptionWithRegion.getRegion().contains(newSubscriptionWithRegion.getRegion())) {
            //System.out.println(getName() + ": received subscription region is within existing subscription region");
            return true;
        } else {
            //System.out.println(getName() + ": received subscription region is not within existing subscription region");
            return false;
        }
    }

    @Override
    public void processPublicationLocation(SimulationPublication p, TreeNode nextBroker) {
        SubscriptionWithRegion tableEntry = (SubscriptionWithRegion) getSubscriptionEntry(nextBroker);

        if (tableEntry.getRegion().contains(p.getLocation())) {
            System.out.println(getName() + ": forwarding publication to " + nextBroker.getName());
            switch (nextBroker) {
                case BrokerWithRegion brokerWithRegion -> brokerWithRegion.matchPublication(p);
                case Subscriber subscriber -> subscriber.receive(p);
                default -> {}
            }
        } else {
            System.out.println(
                    getName() + ": publication location is outside " + nextBroker.getName() + "'s subscription location");
        }
    }

    @Override
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        SubscriptionWithRegion newSubscriptionWithRegion = ((SubscriptionWithRegion) newSubscription);
        List<TreeNode> children = getChildren();

        for (TreeNode child : children) {
            //System.out.println(getName() + ": current child: " + child.getName());
            //System.out.println(getName() + ": subscription source: " + newSubscriptionWithRegion.getSource().getName());

            if (child instanceof BrokerWithRegion childWithRegion
                    && childWithRegion != newSubscriptionWithRegion.getSource()) {
                if (childWithRegion.getRegion().intersects(newSubscriptionWithRegion.getRegion())) {
                    System.out.println(getName() + ": found " + childWithRegion.getName() +
                            " with intersecting " + childWithRegion.getRegion() +
                            " for " + newSubscription);

                    SubscriptionWithRegion existingSubscriptionWithRegion = (SubscriptionWithRegion) getSubscriptionEntry(child);

                    if (existingSubscriptionWithRegion == null ||
                            !existingSubscriptionWithRegion.getRegion()
                                    .contains(newSubscriptionWithRegion.getRegion())) {
                        System.out.println(getName() + ": sending subscription to " + childWithRegion.getName());

                        // sending a 'copy' of the subscription down the subtree
                        SubscriptionWithRegion subscriptionToSend = new SubscriptionWithRegion(newSubscriptionWithRegion.getRegion());
                        subscriptionToSend.setSource(this);
                        childWithRegion.addSubscription(subscriptionToSend);
                    }
                // the two 'else' below are just for debugging the simulation    
                } else {
                    System.out.println(getName() + ": no intersections found with " + child.getName() + "'s region");
                }
            } else {
                System.out.println(getName() + ": skipping " + child.getName() + " as it sent the subscription");
            }

        }
    }

    /*
     * Updates the region of the entry found in the subscription table and the region of the current subscription
     * by aggregating them.
     */
    @Override
    protected void updateSubscriptions(SimulationSubscription existingSubscription, SimulationSubscription newSubscription) {
        SubscriptionWithRegion newSubscriptionWithRegion = ((SubscriptionWithRegion) newSubscription);
        SubscriptionWithRegion existingSubscriptionWithRegion = ((SubscriptionWithRegion) existingSubscription);

        Region existingSubscriptionRegion = existingSubscriptionWithRegion.getRegion();
        Region newSubscriptionRegion = newSubscriptionWithRegion.getRegion();

        Region existingSubscriptionRegionCopy = new Region(existingSubscriptionRegion);

        existingSubscriptionRegion.expand(newSubscriptionRegion);
        newSubscriptionRegion.expand(existingSubscriptionRegionCopy);

        System.out.println(getName() + ": updated table with " + existingSubscriptionWithRegion);
        System.out.println(getName() + ": updated current subscription with " + newSubscriptionWithRegion);
    }
}