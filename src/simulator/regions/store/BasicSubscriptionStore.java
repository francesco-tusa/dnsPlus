package simulator.regions.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;

/**
 * A simple store implementation for brokers that do not need complex region intersection logic
 * (e.g., ProximityRoutingBroker). It maps a neighbor node (child) to its latest subscription.
 * * Logic: One subscription per Neighbor. New subscriptions from the same neighbor 
 * replace the previous one (Update).
 */
public class BasicSubscriptionStore implements SubscriptionStore {

    // Maps Source Node -> Subscription
    private final Map<TreeNode, SimulationSubscription> map = new HashMap<>();

    /**
     * Adds or updates the subscription for a specific source.
     */
    public void add(SimulationSubscription s) {
        if (s == null || s.getSource() == null) return;
        map.put(s.getSource(), s); 
    }

    /**
     * Removes the subscription associated with the given source node.
     */
    public void remove(TreeNode source) {
        if (source != null) {
            map.remove(source);
        }
    }

    public SimulationSubscription get(TreeNode source) {
        return map.get(source);
    }

    public Map<TreeNode, SimulationSubscription> getAll() {
        return Collections.unmodifiableMap(map);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getAllSubscriptions() {
        Map<TreeNode, List<SimulationSubscription>> result = new HashMap<>();
        for (Map.Entry<TreeNode, SimulationSubscription> entry : map.entrySet()) {
            // Wrap single subscription in a list to match interface
            List<SimulationSubscription> list = new ArrayList<>();
            list.add(entry.getValue());
            result.put(entry.getKey(), list);
        }
        return result;
    }
}