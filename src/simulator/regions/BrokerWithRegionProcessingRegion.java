package simulator.regions;

import java.util.List;

import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.TreeNode;

public class BrokerWithRegionProcessingRegion extends BrokerWithRegion {

    public BrokerWithRegionProcessingRegion(String name) {
        super(name);
    }


    public BrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
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
    public void processPublicationLocation(SimulationPublication p, TreeNode next) {
        SubscriptionWithRegion tableEntry = (SubscriptionWithRegion) getSubscriptionEntry(next);

        if (tableEntry.getRegion().contains(((PublicationWithLocation) p).getLocation())) {
            SimulationPublication forwardedPublication = p.getPublication();
            forwardedPublication.setSource(this);
            forwardPublicationToNode(forwardedPublication, next);
        } else {
            System.out.println(
                    getName() + ": publication location is outside " + next.getName() + "'s subscription region");
        }
    }

    @Override
    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        if (next instanceof BrokerWithRegionProcessingRegion brokerWithRegion) {
            System.out.println(getName() + ": forwarding publication to broker " + next.getName());
            brokerWithRegion.matchPublication(p);
        } else {
            System.err.println(getName() + ": topology error");
        }
    }
         

    @Override
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        SubscriptionWithRegion newSubscriptionWithRegion = ((SubscriptionWithRegion) newSubscription);
        List<TreeNode> children = getChildren();

        for (TreeNode child : children) {
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

        System.out.println(getName() + ": updated table with " + existingSubscriptionWithRegion);
        existingSubscriptionRegion.expand(newSubscriptionRegion);
        newSubscriptionRegion.expand(existingSubscriptionRegionCopy);        
        System.out.println(getName() + ": updated current subscription with " + newSubscriptionWithRegion);
    }
}