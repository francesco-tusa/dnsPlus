package simulator;

import java.util.List;

public class BrokerWithRegionProcessingRegion extends BrokerWithRegion {

    public BrokerWithRegionProcessingRegion(String name) {
        super(name);
    }

    @Override
    public boolean regionsOrLocationsMatch(SubscriptionTableEntry existingSubscription, SimulationSubscription newSubscription) {
        SubscriptionWithRegion newSubscriptionWithRegion = ((SubscriptionWithRegion) newSubscription);
        SubscriptionTableEntryWithRegion existingSubscriptionWithRegion = ((SubscriptionTableEntryWithRegion) existingSubscription);

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
    public void processPublicationLocation(SimulationPublication p, TreeNode child) {
        // just for testing, should check the region properly
        SubscriptionTableEntryWithRegion s = (SubscriptionTableEntryWithRegion) getChildSubscriptionEntry(child);

        if (s.getRegion().contains(p.getLocation())) {
            System.out.println(getName() + ": forwarding publication to " + child.getName());
            if (child instanceof BrokerWithRegion) {
                ((BrokerWithRegion) child).matchPublication(p);
            } else if (child instanceof Subscriber) {
                ((Subscriber) child).receive(p);
            }
        } else {
            System.out.println(
                    getName() + ": publication location is outside " + child.getName() + "'s subscription location");
        }
    }

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

                    SubscriptionTableEntryWithRegion existingSubscriptionWithRegion = (SubscriptionTableEntryWithRegion) getChildSubscriptionEntry(child);

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

    @Override
    protected void updateSubscriptionEntry(SubscriptionTableEntry existingSubscription,
            SimulationSubscription newSubscription) {
        SubscriptionWithRegion newSubscriptionWithRegion = ((SubscriptionWithRegion) newSubscription);
        SubscriptionTableEntryWithRegion existingSubscriptionWithRegion = ((SubscriptionTableEntryWithRegion) existingSubscription);

        Region currentRegion = existingSubscriptionWithRegion.getRegion();
        Region newRegion = newSubscriptionWithRegion.getRegion();

        currentRegion.expand(newRegion);
    }
}