package simulator.regions;

import simulator.Location;
import simulator.SimulationBroker;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
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

    /**
     * Updates this broker's region to encompass the region of a child node.
     * This method now handles both child brokers and subscribers, and correctly
     * initializes the region if it was previously undefined.
     * @param child The child node (either a BrokerWithRegion or SubscriberWithLocation).
     */
    public void updateRegion(TreeNode child) {
        boolean regionChanged = false;
        
        Region childRegion = null;
        if (child instanceof BrokerWithRegion broker) {
            childRegion = broker.getRegion();
        } else if (child instanceof SubscriberWithLocation subscriber) {
            // Treat a subscriber's single location as a point-region
            childRegion = new Region(subscriber.getLocation(), subscriber.getLocation());
        }

        if (childRegion != null && childRegion.getBottomLeft() != null) {
            // If the current broker's region is not yet initialized, set it.
            if (this.region.getBottomLeft() == null) {
                this.region.set(childRegion);
                regionChanged = true;
            } 
            // Otherwise, expand the existing region.
            else {
                if (this.region.expand(childRegion)) {
                    regionChanged = true;
                }
            }
        }

        if (regionChanged) {
            System.out.println(getName() + ": updated region to " + this.region);
            numOfRegionUpdates++;
            BrokerWithRegion parentBroker = getParentBroker();
            if (parentBroker != null) {
                parentBroker.updateRegion(this);
            }
        }
    }
    
    @Override
    public abstract SimulationSubscription matchPublication(SimulationPublication p);
}
