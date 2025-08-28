package simulator.regions;

import simulator.Location;
import simulator.SimulationBroker;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.TreeNode;

/**
 * An abstract broker that represents a geographic region and can hold population data.
 * It defers all specific routing logic to its concrete subclasses.
 * @author f.tusa
 */
public abstract class BrokerWithRegion extends SimulationBroker {
    private final Region region;
    private long numOfRegionUpdates;
    private long internetPopulation;

    public BrokerWithRegion(String name) {
        super(name);
        region = new Region();
        numOfRegionUpdates = 0;
        internetPopulation = 0;
    }

    public BrokerWithRegion(String name, Location p1, Location p2) {
        super(name);
        region = new Region(p1, p2);
        numOfRegionUpdates = 0;
        internetPopulation = 0;
    }

    @Override
    public BrokerWithRegion getParentBroker() {
        SimulationBroker parentBroker = super.getParentBroker();
        if (parentBroker instanceof BrokerWithRegion parentBrokerWithRegion) {
            return parentBrokerWithRegion;
        } else {
            return null;
        }
    }

    @Override
    public void addChild(TreeNode child) {
        super.addChild(child);
        if (child instanceof BrokerWithRegion) {
            this.updateRegion(child);
        }
    }

    public Region getRegion() {
        return region;
    }
    
    public long getInternetPopulation() {
        return internetPopulation;
    }

    public void setInternetPopulation(long internetPopulation) {
        this.internetPopulation = internetPopulation;
    }

    public long getNumOfRegionUpdates() {
        return numOfRegionUpdates;
    }

    protected void increaseNumOfRegionUpdates() {
        numOfRegionUpdates++;
    }

    public SimulationSubscription getSubscriptionEntry(TreeNode source) {
        return getSubscriptionsTable().get(source);
    }

    public void updateRegion(TreeNode child) {
        if (child instanceof BrokerWithRegion broker) {
            if (region.expand(broker.region)) {
                System.out.println(getName() + ": updated region to " + this.region);
                numOfRegionUpdates++;
                BrokerWithRegion parentBroker = getParentBroker();
                if (parentBroker != null) {
                    parentBroker.updateRegion(this);
                }
            }
        }
    }
    
    // This is the single abstract method that all concrete broker types MUST implement.
    @Override
    public abstract SimulationSubscription matchPublication(SimulationPublication p);
}