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
            return new StoreUpdate(StoreOpResult.ADDED, newEntry, "New Entry");
        }

        if (existing.contains(sub)) {
            return new StoreUpdate(StoreOpResult.NO_CHANGE, existing, "Filtered");
        }

        Region currentRegion = existing.getRegion();
        currentRegion.expand(sub.getRegion());
        existing.setRegion(currentRegion);

        return new StoreUpdate(StoreOpResult.EXPANDED, existing, "MBR Merge");
    }
    
    @Override
    public int findMatches(Location loc, Map<TreeNode, List<SimulationSubscription>> resultsBuffer) {
        if (map.isEmpty()) {
            return 0;
        }

        int ops = 0;
        for (Map.Entry<TreeNode, SubscriptionWithRegion> entry : map.entrySet()) {
            ops++; // Counting the geometric bounding-box check
            if (entry.getValue().getRegion().contains(loc)) {
                // Return the specific aggregated subscription that successfully matched the QoS/Spatial constraints
                resultsBuffer.put(entry.getKey(), Collections.singletonList(entry.getValue()));
            }
        }
        return ops;
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