package simulator.regions.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

public class MultiRegionStore implements RegionSubscriptionStore {

    private final Map<TreeNode, List<SubscriptionWithRegion>> map = new HashMap<>();
    private final Map<TreeNode, Region> summaryRegions = new HashMap<>();
    private final double mergeThreshold;

    public MultiRegionStore(double threshold) {
        this.mergeThreshold = threshold;
    }

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        List<SubscriptionWithRegion> regions = map.computeIfAbsent(source, k -> new ArrayList<>());

        for (SubscriptionWithRegion existing : regions) {
            if (existing.contains(sub)) {
                return new StoreUpdate(StoreOpResult.NO_CHANGE, null, "Covered", 0, 0);
            }
        }

        Region accumulator = sub.getRegion();
        boolean mergedInPass;
        int absorbedCount = 0;
        int mergedCount = 0;
        double absorbedArea = 0.0;

        do {
            mergedInPass = false;
            Iterator<SubscriptionWithRegion> it = regions.iterator();
            while (it.hasNext()) {
                SubscriptionWithRegion existing = it.next();

                // Case A: Accumulator eats Existing
                if (existing.isContainedIn((float) accumulator.getMinLon(), (float) accumulator.getMaxLon(),
                        (float) accumulator.getMinLat(), (float) accumulator.getMaxLat())) {
                    absorbedArea += existing.getArea();
                    it.remove();
                    absorbedCount++;
                }
                // Case B: Merge Condition
                else if (shouldMerge(accumulator, existing)) {
                    accumulator.expand(existing.getRegion());
                    it.remove();
                    mergedInPass = true;
                    mergedCount++;
                    break;
                }
            }
        } while (mergedInPass);

        SubscriptionWithRegion resultingEntry = new SubscriptionWithRegion(accumulator);
        regions.add(resultingEntry);
        updateSummary(source);

        double accumArea = accumulator.getArea();
        boolean isIdenticalReplacement = (mergedCount == 0
                && Math.abs(accumArea - (sub.getArea() + absorbedArea)) < 1e-6);

        StoreOpResult opResult;
        if (absorbedCount == 0 && mergedCount == 0) {
            opResult = StoreOpResult.ADDED;
        } else if (isIdenticalReplacement) {
            opResult = StoreOpResult.NO_CHANGE;
        } else {
            opResult = StoreOpResult.EXPANDED;
        }

        return new StoreUpdate(opResult, resultingEntry, accumulator.toLogString(), absorbedCount, mergedCount);
    }

    private boolean shouldMerge(Region acc, SubscriptionWithRegion existing) {
        float aMinL = (float) acc.getMinLon(), aMaxL = (float) acc.getMaxLon(),
                aMinT = (float) acc.getMinLat(), aMaxT = (float) acc.getMaxLat();

        float interArea = existing.getIntersectionArea(aMinL, aMaxL, aMinT, aMaxT);
        float area1 = Region.fastArea(aMinL, aMaxL, aMinT, aMaxT);
        float area2 = existing.getArea();

        float geometricUnion = area1 + area2 - interArea;
        float mbrArea = Region.fastMBRArea(aMinL, aMaxL, aMinT, aMaxT,
                existing.getMinLon(), existing.getMaxLon(),
                existing.getMinLat(), existing.getMaxLat());

        return (mbrArea > 0) && ((mbrArea - geometricUnion) / mbrArea < mergeThreshold);
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
        for (List<SubscriptionWithRegion> list : map.values())
            count += list.size();
        return count;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}