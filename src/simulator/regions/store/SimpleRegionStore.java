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

public class SimpleRegionStore implements RegionSubscriptionStore {
    private static final Logger logger = CustomLogger.getLogger(SimpleRegionStore.class.getName());
    
    private final Map<TreeNode, SubscriptionWithRegion> map = new HashMap<>();

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        SubscriptionWithRegion existing = map.get(source);
        
        if (existing == null) {
            SubscriptionWithRegion newEntry = new SubscriptionWithRegion(new Region(sub.getRegion()));
            map.put(source, newEntry);
            return new StoreUpdate(StoreOpResult.ADDED, newEntry);
        }

        Region currentRegion = existing.getRegion();
        Region newRegion = sub.getRegion();

        if (currentRegion.contains(newRegion)) {
            return new StoreUpdate(StoreOpResult.NO_CHANGE, null);
        }

        currentRegion.expand(newRegion);
        return new StoreUpdate(StoreOpResult.EXPANDED, existing);
    }

    @Override
    public List<TreeNode> findMatches(Location loc) {
        List<TreeNode> matches = new ArrayList<>();
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