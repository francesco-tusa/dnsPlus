package simulator;

public class LeafBrokerWithRegionProcessingRegion extends BrokerWithRegionProcessingRegion {

    public LeafBrokerWithRegionProcessingRegion(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name);
    }

    @Override
    public void addChild(TreeNode child) {
        super.addChild(child);
        System.out.println(getName() + ": added new " + child);
        updateRegion(child);
    }

    @Override
    protected void updateRegion(TreeNode child) {
        if (child instanceof Subscriber subscriber) {
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
}
