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
    private boolean saturatedByParent = false;
    
    // Detailed Subscription Counters (INPUT Store)
    private long subCoveredCount = 0;
    private long subExpandedCount = 0;
    private long subSimpleExpandedCount = 0;
    private long subComplexExpandedCount = 0; // NEW
    private long subAddedCount = 0;
    private long subAbsorbedCount = 0;
    private long subMergedCount = 0;

    // Detailed Subscription Counters (OUTPUT Store)
    private long outSubCoveredCount = 0;
    private long outSubExpandedCount = 0;
    private long outSubSimpleExpandedCount = 0;
    private long outSubComplexExpandedCount = 0; // NEW
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
        super.addChild(child);
        updateRegion(child);
    }

    public Region getRegion() { return region; }
    public long getInternetPopulation() { return internetPopulation; }
    public void setInternetPopulation(long internetPopulation) { this.internetPopulation = internetPopulation; }

    public void updateRegion(TreeNode child) {
        boolean regionChanged = false;
        if (child instanceof SubscriberWithLocation subscriber) {
            Location subLoc = subscriber.getLocation();
            if (this.region.contains(subLoc)) return; 
            if (this.region.expand(subLoc)) regionChanged = true;
        } 
        else if (child instanceof BoundedBroker broker) {
            Region childRegion = broker.getRegion();
            if (childRegion != null && childRegion.getBottomLeft() != null) {
                if (this.region.getBottomLeft() == null) {
                    this.region.set(childRegion);
                    regionChanged = true;
                } 
                else if (!this.region.contains(childRegion)) {
                    if (this.region.expand(childRegion)) regionChanged = true;
                }
            }
        }
        if (regionChanged) {
            BoundedBroker parentBroker = getParentBroker();
            if (parentBroker != null) parentBroker.updateRegion(this);
        }
    }

    public boolean isSaturatedByParent() { return saturatedByParent; }
    public void setSaturatedByParent(boolean saturated) { this.saturatedByParent = saturated; }
    
    // --- INPUT Metric Incrementers ---
    protected void recordSubCovered() { subCoveredCount++; }
    protected void recordSubExpanded() { subExpandedCount++; }
    protected void recordSubSimpleExpanded() { subSimpleExpandedCount++; }
    protected void recordSubComplexExpanded() { subComplexExpandedCount++; } // NEW
    protected void recordSubAdded() { subAddedCount++; }
    protected void recordSubAbsorbed(int count) { subAbsorbedCount += count; }
    protected void recordSubMerged(int count) { subMergedCount += count; }

    // --- OUTPUT Metric Incrementers ---
    protected void recordOutSubCovered() { outSubCoveredCount++; }
    protected void recordOutSubExpanded() { outSubExpandedCount++; }
    protected void recordOutSubSimpleExpanded() { outSubSimpleExpandedCount++; }
    protected void recordOutSubComplexExpanded() { outSubComplexExpandedCount++; } // NEW
    protected void recordOutSubAdded() { outSubAddedCount++; }
    protected void recordOutSubAbsorbed(int count) { outSubAbsorbedCount += count; }
    protected void recordOutSubMerged(int count) { outSubMergedCount += count; }

    // --- Getters (INPUT) ---
    public long getSubCoveredCount() { return subCoveredCount; }
    public long getSubExpandedCount() { return subExpandedCount; }
    public long getSubSimpleExpandedCount() { return subSimpleExpandedCount; }
    public long getSubComplexExpandedCount() { return subComplexExpandedCount; } // NEW
    public long getSubAddedCount() { return subAddedCount; }
    public long getSubAbsorbedCount() { return subAbsorbedCount; }
    public long getSubMergedCount() { return subMergedCount; }

    // --- Getters (OUTPUT) ---
    public long getOutSubCoveredCount() { return outSubCoveredCount; }
    public long getOutSubExpandedCount() { return outSubExpandedCount; }
    public long getOutSubSimpleExpandedCount() { return outSubSimpleExpandedCount; }
    public long getOutSubComplexExpandedCount() { return outSubComplexExpandedCount; } // NEW
    public long getOutSubAddedCount() { return outSubAddedCount; }
    public long getOutSubAbsorbedCount() { return outSubAbsorbedCount; }
    public long getOutSubMergedCount() { return outSubMergedCount; }
}