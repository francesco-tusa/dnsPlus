package simulator.regions;

import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.core.TreeNode;
import utils.CustomLogger;

/**
 * Abstract base class for brokers that have a geographic region.
 * <p>This class adds the `Region` property and the logic to dynamically
 * update that region based on the regions of its children (R-tree like behavior).</p>
 */
public abstract class BoundedBroker extends SimulationBroker {
    
    private static final Logger logger = CustomLogger.getLogger(BoundedBroker.class.getName());

    private final Region region;
    private long numOfRegionUpdates;
    private long internetPopulation;
    
    // Metrics for "Aggregation by Expansion" efficiency
    private long numPropagationFilterExpansions;
    private long numMainTableExpansions;

    public BoundedBroker(String name) {
        super(name);
        region = new Region();
        numOfRegionUpdates = 0;
        internetPopulation = 0;
        numPropagationFilterExpansions = 0; 
        numMainTableExpansions = 0;
    }

    public BoundedBroker(String name, Location p1, Location p2) {
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

    /**
     * Updates this broker's region to include the region of a child.
     * This effectively builds the R-tree structure bottom-up.
     * @param child The child node (Broker or Subscriber) causing the update.
     */
    public void updateRegion(TreeNode child) {
        boolean regionChanged = false;
        Region childRegion = null;
        if (child instanceof BoundedBroker broker) {
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
            BoundedBroker parentBroker = getParentBroker();
            if (parentBroker != null) {
                parentBroker.updateRegion(this);
            }
        }
    }
}