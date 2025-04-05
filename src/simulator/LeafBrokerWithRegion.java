package simulator;

public class LeafBrokerWithRegion extends BrokerWithRegion {

    public LeafBrokerWithRegion(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegion(String name, Location p1, Location p2) {
        super(name);
    }

    @Override
    public void addChild(TreeNode child) {
        super.addChild(child);
        updateRegion(child);
    }

    @Override
    protected void updateRegion(TreeNode child) {
        if (child instanceof Subscriber subscriber) {
            getRegion().expand(subscriber.getLocation());
        }

        super.updateRegion(child);
    }

}
