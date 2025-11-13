package simulator.regions;

import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.events.TrackableEvent;
import simulator.core.TreeNode;
import utils.CustomLogger;

public abstract class BrokerWithRegion extends SimulationBroker {
    
    private static final Logger logger = CustomLogger.getLogger(BrokerWithRegion.class.getName());

    private final Region region;
    private long numOfRegionUpdates;
    private long internetPopulation;
    
    private long numPropagationFilterExpansions;
    private long numMainTableExpansions;

    public BrokerWithRegion(String name) {
        super(name);
        region = new Region();
        numOfRegionUpdates = 0;
        internetPopulation = 0;
        numPropagationFilterExpansions = 0; 
        numMainTableExpansions = 0;
    }

    public BrokerWithRegion(String name, Location p1, Location p2) {
        super(name);
        region = new Region(p1, p2);
        numOfRegionUpdates = 0;
        internetPopulation = 0;
        numPropagationFilterExpansions = 0;
        numMainTableExpansions = 0;
    }

    @Override
    public void addChild(TreeNode child) {
        super.addChild(child);
        updateRegion(child);
    }

    public Region getRegion() { return region; }
    public long getInternetPopulation() { return internetPopulation; }
    public void setInternetPopulation(long internetPopulation) { this.internetPopulation = internetPopulation; }
    public long getNumOfRegionUpdates() { return numOfRegionUpdates; }

    public long getNumPropagationFilterExpansions() {
        return numPropagationFilterExpansions;
    }

    public void incrementPropagationFilterExpansions() {
        this.numPropagationFilterExpansions++;
    }

    public long getNumMainTableExpansions() {
        return numMainTableExpansions;
    }

    public void incrementMainTableExpansions() {
        this.numMainTableExpansions++;
    }

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
            logger.fine(getName() + ": updated region to " + this.region);
            numOfRegionUpdates++;
            BrokerWithRegion parentBroker = getParentBroker();
            if (parentBroker != null) {
                parentBroker.updateRegion(this);
            }
        }
    }

    @Override
    protected void captureRegionMetric(TrackableEvent event) {
        if (getRegion() != null) {
            event.addBrokerRegionToPath(getRegion().toShortString());
        } else {
            event.addBrokerRegionToPath("[]");
        }
    }
}
