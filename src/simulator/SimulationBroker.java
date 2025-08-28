package simulator;

import java.util.HashMap;
import java.util.Map;

import broker.GenericBroker;

public abstract class SimulationBroker extends TreeNode implements GenericBroker<SimulationSubscription, SimulationPublication> {

    private int nSubscriptions;
    private int nPublications;
    private final Map<TreeNode, SimulationSubscription> subscriptionsTable;

    public SimulationBroker(String name) {
        super(name);
        this.nSubscriptions = 0;
        this.nPublications = 0;
        subscriptionsTable = new HashMap<>();
    }

    public int getnSubscriptions() {
        return nSubscriptions;
    }

    public int getnPublications() {
        return nPublications;
    }

    public SimulationBroker getParentBroker() {
        return (SimulationBroker) getParent();
    }

    public Map<TreeNode, SimulationSubscription> getSubscriptionsTable() {
        return subscriptionsTable;
    }

    @Override
    public void processPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing publication " + p);
        nPublications++;
        matchPublication(p);
    }

    @Override
    public void processSubscription(SimulationSubscription s) {
        System.out.println();
        System.out.println(getName() + ": processing subscription " + s);
        nSubscriptions++;
        addSubscription(s);
        
        SimulationBroker parentBroker = getParentBroker();
        if (parentBroker != null && s.isUpwardsForwardingEnabled()) {
            s.setSource(this);
            parentBroker.processSubscription(s);
        }
    }

    /**
     * Adds a subscription to this broker's local table.
     * This method is now protected and final to prevent incorrect overrides.
     * @param s The subscription to add.
     */
    public final void addSubscription(SimulationSubscription s) {
        getSubscriptionsTable().put(s.getSource(), s.getTableEntry());
    }

    @Override
    public abstract SimulationSubscription matchPublication(SimulationPublication p);

    public void printSubscriptionsTable() {
        System.out.println("\n" + getName() + "'s Subscription Table:");
        System.out.println("==============================");

        if (subscriptionsTable.isEmpty()) {
            System.out.println("  (Table is empty)");
        } else {
            String formatString = "  %-15s -> %s%n";

            System.out.printf(formatString, "Source Node", "Subscription Details");
            System.out.println("  ---------------   --------------------");

            for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
                TreeNode sourceNode = entry.getKey();
                SimulationSubscription tableEntry = entry.getValue();
                System.out.printf(formatString, sourceNode.getName(), tableEntry);
            }
        }
        System.out.println("==============================");
    }
}
