package simulator;

import java.util.List;

public class BrokerWithRegionProcessingRegion extends BrokerWithRegion {

    public BrokerWithRegionProcessingRegion(String name) {
        super(name);
    }

    public boolean matchingSubscriptionLocationOrRegion(SimulationSubscription existingSubscription, SimulationSubscription newSubscription) {
        SubscriptionWithRegion newSubscriptionWithRegion = ((SubscriptionWithRegion)newSubscription);
        SubscriptionWithRegion existingSubscriptionWithRegion = ((SubscriptionWithRegion)existingSubscription);

        // FIXME: This might require checking for intersections and do specific operations accordingly
        if (existingSubscriptionWithRegion.getRegion().contains(newSubscriptionWithRegion.getRegion())) {
            System.out.println(getName() + ": received subscription region is within existing subscription region");
            return true;
        } else {
            System.out.println(getName() + ": received subscription region is not within existing subscription region");
            return false;
        }        
    }

    @Override
    public void processPublicationLocation(SimulationPublication p, TreeNode child) {
        // just for testing, should check the region properly
        SubscriptionWithRegion s = (SubscriptionWithRegion)getChildSubscriptionEntry(child);

        if (s.getRegion().contains(p.getLocation())) {
            System.out.println(getName() + ": forwarding publication to " + child.getName());
            if (child instanceof BrokerWithRegion) {
                ((BrokerWithRegion) child).matchPublication(p);
            } else if (child instanceof Subscriber) {
                ((Subscriber) child).receive(p);
            }
        } else {
            System.out.println(getName() + ": publication location is outside " + child.getName() + "'s subscription location");
        }
    }

    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        SubscriptionWithRegion newSubscriptionWithRegion = ((SubscriptionWithRegion)newSubscription);
        List<TreeNode> children = getChildren();
        
        for (TreeNode child : children) {
            if (child != newSubscriptionWithRegion.getSource()) {
                if (child instanceof BrokerWithRegion childWithRegion && 
                    childWithRegion.getRegion().intersects(newSubscriptionWithRegion.getRegion())) {
                        SubscriptionWithRegion existingSubscriptionWithRegion = (SubscriptionWithRegion)getChildSubscriptionEntry(child);
                        if (existingSubscriptionWithRegion == null || 
                            !existingSubscriptionWithRegion.getRegion().contains(newSubscriptionWithRegion.getRegion())) {
                            // send subscription to child
                        }

                }
            }
        }
    }
}