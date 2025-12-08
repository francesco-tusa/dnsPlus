package simulator.regions.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import utils.CustomLogger;

public class MultiRegionStore implements RegionSubscriptionStore {
    private static final Logger logger = CustomLogger.getLogger(MultiRegionStore.class.getName());
    
    private final Map<TreeNode, List<SubscriptionWithRegion>> map = new HashMap<>();
    private final Map<TreeNode, Region> summaryRegions = new HashMap<>();
    private final double mergeThreshold;

    public MultiRegionStore(double threshold) {
        this.mergeThreshold = threshold;
    }

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        List<SubscriptionWithRegion> regions = map.computeIfAbsent(source, k -> new ArrayList<>());
        
        // 1. Check Coverage
        for (SubscriptionWithRegion existing : regions) {
            if (existing.getRegion().contains(sub.getRegion())) {
                return new StoreUpdate(StoreOpResult.NO_CHANGE, null);
            }
        }

        // 2. Greedy Accumulator Merge
        Region accumulator = new Region(sub.getRegion());
        boolean changed = false;
        boolean mergedInPass;

        do {
            mergedInPass = false;
            Iterator<SubscriptionWithRegion> it = regions.iterator();
            while (it.hasNext()) {
                SubscriptionWithRegion existing = it.next();
                Region rExisting = existing.getRegion();

                // Case A: Accumulator eats Existing (Redundancy reverse check)
                if (accumulator.contains(rExisting)) {
                    it.remove();
                    changed = true;
                    // No need to restart scan, just continue consuming
                }
                // Case B: Merge Condition Met (Overlap > Threshold)
                else if (shouldMerge(accumulator, rExisting)) {
                    accumulator.expand(rExisting);
                    it.remove();
                    changed = true;
                    mergedInPass = true;
                    // The accumulator grew. It might now overlap with regions we
                    // already checked in this pass. We must restart the scan 
                    // to ensure we catch everything (Cascading Merge).
                    break; 
                }
            }
        } while (mergedInPass);

        // 3. Add the final region to the list
        SubscriptionWithRegion resultingEntry = new SubscriptionWithRegion(accumulator);
        regions.add(resultingEntry);
        
        updateSummary(source);

        StoreOpResult opResult = changed ? StoreOpResult.EXPANDED : StoreOpResult.ADDED;
        return new StoreUpdate(opResult, resultingEntry);
    }

    private void updateSummary(TreeNode source) {
        List<SubscriptionWithRegion> list = map.get(source);
        if (list == null || list.isEmpty()) {
            summaryRegions.remove(source);
            return;
        }
        // Rebuild summary from scratch (O(N))
        Region summary = new Region(list.get(0).getRegion());
        for (int i = 1; i < list.size(); i++) {
            summary.expand(list.get(i).getRegion());
        }
        summaryRegions.put(source, summary);
    }

    private boolean shouldMerge(Region r1, Region r2) {
        double intersection = r1.getIntersectionArea(r2);
        if (intersection <= 0) return false;
        double union = r1.getUnionArea(r2);
        return (intersection / union) >= mergeThreshold;
    }

    @Override
    public List<TreeNode> findMatches(Location loc) {
        List<TreeNode> matches = new ArrayList<>();
        for (Map.Entry<TreeNode, List<SubscriptionWithRegion>> entry : map.entrySet()) {
            TreeNode neighbor = entry.getKey();
            Region summary = summaryRegions.get(neighbor);
            
            // OPTIMIZATION: Check Summary Region first
            // If the point is not in the summary, it cannot be in any sub-region.
            if (summary != null && summary.contains(loc)) {
                // Detailed Check: Check the specific disjoint regions
                for (SubscriptionWithRegion sub : entry.getValue()) {
                    if (sub.getRegion().contains(loc)) {
                        matches.add(neighbor);
                        break; // Found one match for this neighbor, enough to forward
                    }
                }
            }
        }
        return matches;
    }

    @Override
    public List<SimulationSubscription> getOutputFor(TreeNode target) {
        List<SubscriptionWithRegion> list = map.get(target);
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }
    
    @Override
    public Map<TreeNode, List<SimulationSubscription>> getAllSubscriptions() {
        Map<TreeNode, List<SimulationSubscription>> result = new HashMap<>();
        for (Map.Entry<TreeNode, List<SubscriptionWithRegion>> entry : map.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    @Override
    public int size() {
        int count = 0;
        for (List<SubscriptionWithRegion> list : map.values()) count += list.size();
        return count;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}