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

    // Optimization Flag: 100% saturation by parent subscription
    private boolean saturatedByParent = false;
    
    // Detailed Subscription Counters (INPUT Store)
    private long subCoveredCount = 0;
    private long subExpandedCount = 0;
    private long subAddedCount = 0;
    private long subAbsorbedCount = 0;
    private long subMergedCount = 0;

    // Detailed Subscription Counters (OUTPUT Store)
    private long outSubCoveredCount = 0;
    private long outSubExpandedCount = 0;
    private long outSubAddedCount = 0;
    private long outSubAbsorbedCount = 0;
    private long outSubMergedCount = 0;

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
        super.addChild(child); // Sets parent pointer via TreeNode
        updateRegion(child);   // Triggers expansion
    }

    public Region getRegion() { return region; }
    public long getInternetPopulation() { return internetPopulation; }
    public void setInternetPopulation(long internetPopulation) { this.internetPopulation = internetPopulation; }

    /**
     * Updates the region of this broker to include the child's region/location.
     * OPTIMIZATION:
     * 1. Check strict containment first (avoids math if point is already inside).
     * 2. Use scalar expand(Location) for subscribers (avoids object allocation).
     */
    public void updateRegion(TreeNode child) {
        boolean regionChanged = false;

        if (child instanceof SubscriberWithLocation subscriber) {
            Location subLoc = subscriber.getLocation();
            
            // 1. FASTEST PATH: If the subscriber is already inside, stop immediately.
            // This is crucial for ProportionalSubscribersPlacement where subs are usually inside.
            if (this.region.contains(subLoc)) {
                return; 
            }

            // 2. FAST PATH: Direct scalar expansion.
            // Avoids allocating Region objects or ArrayLists.
            if (this.region.expand(subLoc)) {
                regionChanged = true;
            }
        } 
        else if (child instanceof BoundedBroker broker) {
            // STANDARD PATH: Region merging for brokers
            Region childRegion = broker.getRegion();
            
            // Optimization: If child region is fully contained, do nothing.
            if (childRegion != null && childRegion.getBottomLeft() != null) {
                // If we don't have a region yet, we must set it.
                if (this.region.getBottomLeft() == null) {
                    this.region.set(childRegion);
                    regionChanged = true;
                } 
                // If we do have a region, check containment before expanding.
                else if (!this.region.contains(childRegion)) {
                    if (this.region.expand(childRegion)) {
                        regionChanged = true;
                    }
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

    public boolean isSaturatedByParent() {
        return saturatedByParent;
    }

    public void setSaturatedByParent(boolean saturated) {
        this.saturatedByParent = saturated;
    }
    
    // --- INPUT Metric Incrementers ---
    protected void recordSubCovered() { subCoveredCount++; }
    protected void recordSubExpanded() { subExpandedCount++; }
    protected void recordSubAdded() { subAddedCount++; }
    protected void recordSubAbsorbed(int count) { subAbsorbedCount += count; }
    protected void recordSubMerged(int count) { subMergedCount += count; }

    // --- OUTPUT Metric Incrementers ---
    protected void recordOutSubCovered() { outSubCoveredCount++; }
    protected void recordOutSubExpanded() { outSubExpandedCount++; }
    protected void recordOutSubAdded() { outSubAddedCount++; }
    protected void recordOutSubAbsorbed(int count) { outSubAbsorbedCount += count; }
    protected void recordOutSubMerged(int count) { outSubMergedCount += count; }

    // --- Getters (INPUT) ---
    public long getSubCoveredCount() { return subCoveredCount; }
    public long getSubExpandedCount() { return subExpandedCount; }
    public long getSubAddedCount() { return subAddedCount; }
    public long getSubAbsorbedCount() { return subAbsorbedCount; }
    public long getSubMergedCount() { return subMergedCount; }

    // --- Getters (OUTPUT) ---
    public long getOutSubCoveredCount() { return outSubCoveredCount; }
    public long getOutSubExpandedCount() { return outSubExpandedCount; }
    public long getOutSubAddedCount() { return outSubAddedCount; }
    public long getOutSubAbsorbedCount() { return outSubAbsorbedCount; }
    public long getOutSubMergedCount() { return outSubMergedCount; }
}