package simulator;

import java.util.HashMap;
import java.util.Map;
import simulator.regions.BrokerWithRegion;
import simulator.visualisation.TopologyVisualiser;

public abstract class SimulationBroker extends TreeNode {

    private final Map<TreeNode, SimulationSubscription> subscriptionsTable = new HashMap<>();

    public SimulationBroker(String name) {
        super(name);
    }

    public abstract SimulationSubscription matchPublication(SimulationPublication p);

    public void processPublication(SimulationPublication p) {
        // --- Generic Visualisation Hook for Heat Map ---
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null) {
            // Permanently set the edge to red to indicate it was used by a publication
            visualizer.setPublicationEdge(p.getSource().getName(), getName());
        }
        // --- End Visualisation Hook ---

        matchPublication(p); // Call the subclass-specific matching logic
    }

    public void processSubscription(SimulationSubscription s) {
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

    public void printSubscriptionsTable() {
        System.out.println("\n" + getName() + "'s Subscription Table:");
        System.out.println("==============================");
        System.out.println("  Source Node      -> Subscription Details");
        System.out.println("  ---------------    --------------------");
        if (subscriptionsTable.isEmpty()) {
            System.out.println("  (empty)");
        } else {
            for (Map.Entry<TreeNode, SimulationSubscription> entry : subscriptionsTable.entrySet()) {
                System.out.printf("  %-15s -> %s\n", entry.getKey().getName(), entry.getValue().toString());
            }
        }
        System.out.println("==============================");
    }

    // Unchanged getters
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
    public int getnSubscriptions() {
        return getSubscriptionsTable().size();
    }
}
