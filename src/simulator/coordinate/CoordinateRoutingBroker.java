package simulator.coordinate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.store.BasicSubscriptionStore;

/* This Broker operates in a loosely-regionally-aware overlay, where each broker does not 
 * know the region it covers. It just sees, when running, the coordinates announced 
 * by the publications it receives
 */

public class CoordinateRoutingBroker extends SimulationBroker {

    // Use the new store
    protected final BasicSubscriptionStore inputStore = new BasicSubscriptionStore();

    public CoordinateRoutingBroker(String name) {
        super(name);
    }

    @Override
    public void addSubscription(SimulationSubscription s) {
        inputStore.add(s);
    }
    
    @Override
    public int getSubscriptionCount() {
        return inputStore.size();
    }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions() {
        return inputStore.getAllSubscriptions();
    }

    @Override
    public Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions() {
        // Stub: Coordinate routing might implement specific propagation logic later
        return Collections.emptyMap();
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        return null;
    }

    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        addSubscription(s);
        // Future: logic for coordinate routing
    }
}