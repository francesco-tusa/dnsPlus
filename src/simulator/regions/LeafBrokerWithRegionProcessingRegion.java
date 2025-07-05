package simulator.regions;

import simulator.Location;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;

public class LeafBrokerWithRegionProcessingRegion extends BrokerWithRegionProcessingLocation {

    public LeafBrokerWithRegionProcessingRegion(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    public void addChild(TreeNode child) {
        super.addChild(child);
        System.out.println(getName() + ": added new " + child.getName());
        updateRegion(child);
    }

    @Override
    public void updateRegion(TreeNode child) {
        if (child instanceof SubscriberWithLocation subscriber) {
            if (getRegion().expand(subscriber.getLocation())) {
                increaseNumOfRegionUpdates();
                System.out.println(getName() + ": updated region");
                BrokerWithRegion parentBroker = getParentBroker();
                if (parentBroker != null) {
                    parentBroker.updateRegion(this);
                } else {
                    System.out.println(getName() + ": topology error, parent broker not found");
                }
            }
        } 
    }

    @Override
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        System.out.println(getName() + ": I am a leaf broker and I do not have children");
    }

    /*
     * Only a leaf broker should check whether the next node is a broker or a subscriber
     */
    @Override
    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        switch (next) {
            case BrokerWithRegion brokerWithRegion -> brokerWithRegion.matchPublication(p);
            case SubscriberWithLocation subscriber -> subscriber.receive(p);
            default -> System.err.println(getName() + ": topology error");
        }
    }
}