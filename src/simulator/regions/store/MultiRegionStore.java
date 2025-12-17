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

        // 1. Check Coverage (Incoming is covered by Existing)
        // If an existing region already fully contains the new one, no update is needed.
        for (SubscriptionWithRegion existing : regions) {
            if (existing.getRegion().contains(sub.getRegion())) {
                String info = "Covered by: " + existing.getRegion().toLogString();
                return new StoreUpdate(StoreOpResult.NO_CHANGE, null, info, 0, 0);
            }
        }

        // 2. Greedy Accumulator Merge
        Region accumulator = new Region(sub.getRegion());
        boolean changed = false;
        boolean mergedInPass;

        int absorbedCount = 0; // Existing regions swallowed by Incoming
        int mergedCount = 0;   // Existing regions that caused Accumulator to grow
        double absorbedArea = 0.0; // To track if we are just replacing identical regions

        do {
            mergedInPass = false;
            Iterator<SubscriptionWithRegion> it = regions.iterator();
            while (it.hasNext()) {
                SubscriptionWithRegion existing = it.next();
                Region rExisting = existing.getRegion();

                // Case A: Accumulator eats Existing (Superset)
                // We remove 'existing' because 'accumulator' replaces it.
                if (accumulator.contains(rExisting)) {
                    absorbedArea += rExisting.getArea();
                    it.remove();
                    changed = true;
                    absorbedCount++;
                }
                // Case B: Merge Condition Met (Intersection or Proximity)
                else if (shouldMerge(accumulator, rExisting)) {
                    accumulator.expand(rExisting); // Accumulator GROWS here
                    it.remove();
                    changed = true;
                    mergedInPass = true;
                    mergedCount++;
                    break; // Restart iterator because accumulator changed shape
                }
            }
        } while (mergedInPass);

        // 3. Add result to store
        SubscriptionWithRegion resultingEntry = new SubscriptionWithRegion(accumulator);
        regions.add(resultingEntry);
        updateSummary(source);

        // 4. Determine OpResult (CRITICAL FIX)
        StoreOpResult opResult;
        if (!changed) {
            opResult = StoreOpResult.ADDED;
        } else {
            // Optimization: If we absorbed regions but the area didn't change (sub == existing),
            // treat it as NO_CHANGE to stop propagation "churn".
            // We check if mergedCount is 0 (pure absorption) and areas match.
            double accumArea = accumulator.getArea();
            boolean isIdenticalReplacement = (mergedCount == 0 && Math.abs(accumArea - absorbedArea) < 0.000001);

            if (isIdenticalReplacement) {
                opResult = StoreOpResult.NO_CHANGE;
            } else {
                opResult = StoreOpResult.EXPANDED;
            }
        }

        // 5. Construct Log String
        String info;
        if (opResult == StoreOpResult.NO_CHANGE && changed) {
            info = "Redundant Update (Identical Replacement)";
        } else if (!changed) {
            info = "New Disjoint Region";
        } else {
            StringBuilder sb = new StringBuilder();
            if (absorbedCount > 0) sb.append("Absorbed ").append(absorbedCount).append(" regions. ");
            if (mergedCount > 0) sb.append("Merged with ").append(mergedCount).append(" regions. ");
            sb.append("Result: ").append(accumulator.toLogString());
            info = sb.toString();
        }

        return new StoreUpdate(opResult, resultingEntry, info, absorbedCount, mergedCount);
    }

    // Updated with Debug Logging to diagnose why Merges (0) are failing
    private boolean shouldMerge(Region r1, Region r2) {
        double intersectionArea = r1.getIntersectionArea(r2);

        // Calculate Areas involved
        double area1 = r1.getArea();
        double area2 = r2.getArea();

        // The actual geometric area covered by the two regions (A U B)
        double geometricUnionArea = area1 + area2 - intersectionArea;

        // Calculate the MBR (Minimum Bounding Rectangle) of the result
        Region mbr = new Region(r1);
        mbr.expand(r2);
        double mbrArea = mbr.getArea();

        if (mbrArea <= 0) return false;

        // Calculate False Positive Region (Waste)
        double fprArea = mbrArea - geometricUnionArea;
        double fprRatio = fprArea / mbrArea;

        boolean merge = fprRatio < mergeThreshold;

        return merge;
    }

    private void updateSummary(TreeNode source) {
        List<SubscriptionWithRegion> list = map.get(source);
        if (list == null || list.isEmpty()) {
            summaryRegions.remove(source);
            return;
        }
        Region summary = new Region(list.get(0).getRegion());
        for (int i = 1; i < list.size(); i++) {
            summary.expand(list.get(i).getRegion());
        }
        summaryRegions.put(source, summary);
    }

    @Override
    public List<TreeNode> findMatches(Location loc) {
        List<TreeNode> matches = new ArrayList<>();
        for (Map.Entry<TreeNode, List<SubscriptionWithRegion>> entry : map.entrySet()) {
            TreeNode neighbor = entry.getKey();
            Region summary = summaryRegions.get(neighbor);
            if (summary != null && summary.contains(loc)) {
                for (SubscriptionWithRegion sub : entry.getValue()) {
                    if (sub.getRegion().contains(loc)) {
                        matches.add(neighbor);
                        break;
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
    public boolean isEmpty() { return map.isEmpty(); }
}