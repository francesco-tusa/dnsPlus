package simulator.regions;

import simulator.Location;
import simulator.SimulationBroker;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;
import simulator.visualisation.TopologyVisualiser;

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
    public void processSubscription(SimulationSubscription s) {
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null) {
            visualizer.updateSubscriptionEdge(s.getSource().getName(), getName());
        }

        super.processSubscription(s);
    }

    @Override
    public void addChild(TreeNode child) {
        super.addChild(child);
        updateRegion(child);
    }

    @Override
    public BrokerWithRegion getParentBroker() {
        return (BrokerWithRegion) super.getParentBroker();
    }

    public Region getRegion() { return region; }
    public long getInternetPopulation() { return internetPopulation; }
    public void setInternetPopulation(long internetPopulation) { this.internetPopulation = internetPopulation; }
    public long getNumOfRegionUpdates() { return numOfRegionUpdates; }
    protected void increaseNumOfRegionUpdates() { numOfRegionUpdates++; }
    public SimulationSubscription getSubscriptionEntry(TreeNode source) { return getSubscriptionsTable().get(source); }

    public void updateRegion(TreeNode child) {
        boolean regionChanged = false;
        Region childRegion = null;
        if (child instanceof BrokerWithRegion broker) {
            childRegion = broker.getRegion();
        } else if (child instanceof SubscriberWithLocation subscriber) {
            childRegion = new Region(subscriber.getLocation(), subscriber.getLocation());
        }

        if (childRegion != null && childRegion.getBottomLeft() != null) {
            if (this.region.getBottomLeft() == null) {
                this.region.set(childRegion);
                regionChanged = true;
            } else {
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
