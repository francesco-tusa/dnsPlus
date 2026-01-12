package simulator.regions.store;

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

public class SimpleRegionStore implements RegionSubscriptionStore {
    private static final Logger logger = CustomLogger.getLogger(SimpleRegionStore.class.getName());

    private final Map<TreeNode, SubscriptionWithRegion> map = new HashMap<>();

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        SubscriptionWithRegion existing = map.get(source);

        if (existing == null) {
            SubscriptionWithRegion newEntry = new SubscriptionWithRegion(sub);
            map.put(source, newEntry);
            return new StoreUpdate(StoreOpResult.ADDED, newEntry, "Added (New Entry)");
        }

        if (existing.contains(sub)) {
            // Updated: Explicitly state "Filtered" and return the existing object for context
            return new StoreUpdate(StoreOpResult.NO_CHANGE, existing, "Covered (Filtered)");
        }

        Region currentRegion = existing.getRegion();
        currentRegion.expand(sub.getRegion());
        existing.setRegion(currentRegion);

        return new StoreUpdate(StoreOpResult.EXPANDED, existing, "Expanded (MBR Merge)");
    }

    @Override
    public List<TreeNode> findMatches(Location loc) {
        if (map.isEmpty())
            return Collections.emptyList();

        java.util.ArrayList<TreeNode> matches = new java.util.ArrayList<>();
        for (Map.Entry<TreeNode, SubscriptionWithRegion> entry : map.entrySet()) {
            if (entry.getValue().getRegion().contains(loc)) {
                matches.add(entry.getKey());
            }
        }
        return matches;
    }

    @Override
    public List<SimulationSubscription> getOutputFor(TreeNode target) {
        SubscriptionWithRegion s = map.get(target);
        return s != null ? Collections.singletonList(s) : Collections.emptyList();
    }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getAllSubscriptions() {
        Map<TreeNode, List<SimulationSubscription>> result = new HashMap<>();
        for (Map.Entry<TreeNode, SubscriptionWithRegion> entry : map.entrySet()) {
            result.put(entry.getKey(), Collections.singletonList(entry.getValue()));
        }
        return result;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}