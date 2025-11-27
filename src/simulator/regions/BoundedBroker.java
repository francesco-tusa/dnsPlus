package simulator.regions;

import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.core.TreeNode;
import utils.CustomLogger;

public abstract class BoundedBroker extends SimulationBroker {
    
    private static final Logger logger = CustomLogger.getLogger(BoundedBroker.class.getName());

    private final Region region;
    private long internetPopulation;
    
    // Detailed Subscription Counters
    private long subCoveredCount = 0;
    private long subExpandedCount = 0;
    private long subAddedCount = 0;

    public BoundedBroker(String name) {
        super(name);
        region = new Region();
        internetPopulation = 0;
    }

    public BoundedBroker(String name, Location p1, Location p2) {
        super(name);
        region = new Region(p1, p2);
        internetPopulation = 0;
    }

    @Override
    public void addChild(TreeNode child) {
        super.addChild(child);
        updateRegion(child);
    }

    public Region getRegion() { return region; }
    public long getInternetPopulation() { return internetPopulation; }
    public void setInternetPopulation(long internetPopulation) { this.internetPopulation = internetPopulation; }

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
            BoundedBroker parentBroker = getParentBroker();
            if (parentBroker != null) {
                parentBroker.updateRegion(this);
            }
        }
    }
    
    // --- Metric Incrementers ---
    protected void recordSubCovered() { subCoveredCount++; }
    protected void recordSubExpanded() { subExpandedCount++; }
    protected void recordSubAdded() { subAddedCount++; }

    // --- Getters ---
    public long getSubCoveredCount() { return subCoveredCount; }
    public long getSubExpandedCount() { return subExpandedCount; }
    public long getSubAddedCount() { return subAddedCount; }
}