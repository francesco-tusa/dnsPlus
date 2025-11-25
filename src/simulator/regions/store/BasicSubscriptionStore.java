package simulator.regions.store;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;

/**
 * A simple store implementation for brokers that do not need region logic
 * (e.g., ProximityRoutingBroker, CoordinateRoutingBroker).
 * It simply maps a neighbor node to its latest subscription.
 */
public class BasicSubscriptionStore implements SubscriptionStore {

    private final Map<TreeNode, SimulationSubscription> map = new HashMap<>();

    public void add(SimulationSubscription s) {
        if (s == null || s.getSource() == null) return;
        // TODO: I think this should not just update the existing subscription
        // maybe could keep a MBB with all the received subscriptions that 
        // can be used at publication matching time. This however would make
        // the broker region-awareness useless for this type of broker
        map.put(s.getSource(), s); 
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
            result.put(entry.getKey(), Collections.singletonList(entry.getValue()));
        }
        return result;
    }
}