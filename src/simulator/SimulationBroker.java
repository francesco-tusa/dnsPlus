package simulator;

import java.util.HashMap;
import java.util.Map;
import simulator.regions.BrokerWithRegion;

/**
 * Represents a generic broker in the simulation.
 */
public abstract class SimulationBroker extends TreeNode {

    private final Map<TreeNode, SimulationSubscription> subscriptionsTable = new HashMap<>();

    public SimulationBroker(String name) {
        super(name);
    }

    /**
     * Retrieves the parent of this broker, if it is also a broker.
     * @return The parent as a BrokerWithRegion, or null if there is no parent or it's not a broker.
     */
    public BrokerWithRegion getParentBroker() {
        TreeNode parent = getParent();
        if (parent instanceof BrokerWithRegion) {
            return (BrokerWithRegion) parent;
        }
        return null;
    }

    public abstract SimulationSubscription matchPublication(SimulationPublication p);

    public void processPublication(SimulationPublication p) {
        matchPublication(p);
    }

    /**
     * Propagates a subscription upwards to the parent broker.
     * This method's sole responsibility is upward propagation. The subclass is
     * responsible for adding the subscription to its own table first.
     */
    public void processSubscription(SimulationSubscription s) {
        BrokerWithRegion parent = getParentBroker();
        if (parent != null) {
            SimulationSubscription subscriptionToSend = s.getSubscription();
            subscriptionToSend.setSource(this);
            parent.processSubscription(subscriptionToSend);
        }
    }

    /**
     * Adds a subscription to this broker's local table.
     * This is final to ensure consistent, non-recursive behavior.
     */
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

    public Map<TreeNode, SimulationSubscription> getSubscriptionsTable() {
        return subscriptionsTable;
    }

    public int getnSubscriptions() {
        return getSubscriptionsTable().size();
    }
}
