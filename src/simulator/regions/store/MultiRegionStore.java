package simulator.regions.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    public StoreOpResult addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        List<SubscriptionWithRegion> regions = map.computeIfAbsent(source, k -> new ArrayList<>());
        Region newReg = sub.getRegion();

        // 1. Check Redundancy (Coverage)
        for (SubscriptionWithRegion existing : regions) {
            if (existing.getRegion().contains(newReg)) return StoreOpResult.NO_CHANGE;
        }

        boolean merged = false;
        // 2. Check Merge
        for (int i = regions.size() - 1; i >= 0; i--) {
            SubscriptionWithRegion existingSub = regions.get(i);
            Region existing = existingSub.getRegion();

            if (newReg.contains(existing)) {
                regions.set(i, new SubscriptionWithRegion(new Region(newReg)));
                optimizeList(regions);
                merged = true;
                break;
            }

            if (shouldMerge(existing, newReg)) {
                existing.expand(newReg);
                optimizeList(regions);
                merged = true;
                break;
            }
        }

        if (merged) {
            updateSummary(source);
            return StoreOpResult.EXPANDED;
        } else {
            // 3. Add Disjoint
            regions.add(new SubscriptionWithRegion(new Region(newReg)));
            updateSummary(source);
            return StoreOpResult.ADDED;
        }
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

    private boolean shouldMerge(Region r1, Region r2) {
        double intersection = r1.getIntersectionArea(r2);
        if (intersection <= 0) return false;
        double union = r1.getUnionArea(r2);
        return (intersection / union) >= mergeThreshold;
    }

    private void optimizeList(List<SubscriptionWithRegion> regions) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < regions.size(); i++) {
                for (int j = i + 1; j < regions.size(); j++) {
                    Region r1 = regions.get(i).getRegion();
                    Region r2 = regions.get(j).getRegion();
                    if (r1.contains(r2)) { regions.remove(j); changed = true; break; }
                    else if (r2.contains(r1)) { regions.set(i, regions.get(j)); regions.remove(j); changed = true; break; }
                    else if (shouldMerge(r1, r2)) { r1.expand(r2); regions.remove(j); changed = true; break; }
                }
                if (changed) break;
            }
        }
    }

    @Override
    public List<TreeNode> findMatches(Location loc) {
        List<TreeNode> matches = new ArrayList<>();
        for (Map.Entry<TreeNode, List<SubscriptionWithRegion>> entry : map.entrySet()) {
            TreeNode neighbor = entry.getKey();
            Region summary = summaryRegions.get(neighbor);
            
            // Optimization check
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
            List<SimulationSubscription> genericList = new ArrayList<>(entry.getValue());
            result.put(entry.getKey(), genericList);
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