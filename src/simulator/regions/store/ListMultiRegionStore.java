package simulator.regions.store;

import java.util.*;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

public class ListMultiRegionStore extends AbstractMultiRegionStore {

    private final Map<TreeNode, List<SubscriptionWithRegion>> map = new HashMap<>();
    private final Map<TreeNode, Region> summaryRegions = new HashMap<>();

    public ListMultiRegionStore(double threshold) {
        super(threshold);
    }

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        List<SubscriptionWithRegion> regions = map.computeIfAbsent(source, k -> new ArrayList<>());

        // 1. Filter Check (O(N))
        for (SubscriptionWithRegion existing : regions) {
            if (existing.contains(sub)) {
                return new StoreUpdate(StoreOpResult.NO_CHANGE, existing, "Filtered", 0, 0);
            }
        }

        Region accumulator = sub.getRegion();
        boolean mergedInPass;
        int absorbedCount = 0;
        int mergedCount = 0;
        double absorbedArea = 0.0;

        // 2. Absorb/Merge Loop (O(N^2) potential)
        do {
            mergedInPass = false;
            Iterator<SubscriptionWithRegion> it = regions.iterator();
            while (it.hasNext()) {
                SubscriptionWithRegion existing = it.next();

                if (accumulator.contains(existing.getRegion())) {
                    absorbedArea += existing.getArea();
                    it.remove();
                    absorbedCount++;
                } else if (shouldMerge(accumulator, existing)) {
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
        boolean isIdenticalReplacement = (mergedCount == 0 && Math.abs(accumArea - (sub.getArea() + absorbedArea)) < 1e-9);

        StoreOpResult opResult = determineResult(absorbedCount, mergedCount, isIdenticalReplacement);
        String explanation = createCleanExplanation(opResult, mergedCount, absorbedCount, isIdenticalReplacement);
        return new StoreUpdate(opResult, resultingEntry, explanation, absorbedCount, mergedCount);
    }

    private void updateSummary(TreeNode source) {
        List<SubscriptionWithRegion> list = map.get(source);
        if (list == null || list.isEmpty()) { summaryRegions.remove(source); return; }
        Region summary = new Region(list.get(0).getRegion());
        for (int i = 1; i < list.size(); i++) summary.expand(list.get(i).getRegion());
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
        return map.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public boolean isEmpty() { return map.isEmpty(); }
}