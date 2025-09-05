package simulator.entities;

import java.util.HashMap;
import java.util.Map;
import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.BrokerWithRegion;
import simulator.visualisation.TopologyVisualiser;

public abstract class SimulationBroker extends TreeNode {

    private final Map<TreeNode, SimulationSubscription> subscriptionsTable = new HashMap<>();

    public SimulationBroker(String name) {
        super(name);
    }

    public abstract SimulationSubscription matchPublication(SimulationPublication p);

    public void processPublication(SimulationPublication p) {
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null && p.getSource() != null) {
            visualizer.setPublicationEdge(p.getSource().getName(), getName());
        }
        matchPublication(p);
    }

    public void processSubscription(SimulationSubscription s) {
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null && s.getSource() != null) {
            visualizer.updateSubscriptionEdge(s.getSource().getName(), getName());
        }

        BrokerWithRegion parent = getParentBroker();
        if (parent != null) {
            SimulationSubscription subscriptionToSend = s.getSubscription();
            subscriptionToSend.setSource(this);
            parent.processSubscription(subscriptionToSend);
        }
    }

    public final void addSubscription(SimulationSubscription s) {
        subscriptionsTable.put(s.getSource(), s);
    }

    public BrokerWithRegion getParentBroker() {
        TreeNode parent = getParent();
        if (parent instanceof BrokerWithRegion) {
            return (BrokerWithRegion) parent;
        }
        return null;
    }
    
    public Map<TreeNode, SimulationSubscription> getSubscriptionsTable() {
        return subscriptionsTable;
    }
}