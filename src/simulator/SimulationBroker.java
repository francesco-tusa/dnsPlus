package simulator;

import java.util.HashMap;
import java.util.Map;

import broker.GenericBroker;

public abstract class SimulationBroker extends TreeNode implements GenericBroker<SimulationSubscription, SimulationPublication> {

    private int nSubscriptions;
    private int nPublications;
    private Map<TreeNode, SimulationSubscription> subscriptionsTable;

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

    protected Map<TreeNode, SimulationSubscription> getSubscriptionsTable() {
        return subscriptionsTable;
    }


    /*
     *  Process the publication by calling matchPublication,
     *  which defines the logic to propagate it 
     *  based on the broker's subscription tables
     */

    @Override
    public void processPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing publication " + p);
        nPublications++;
        matchPublication(p);
    }

    /* 
     *   Process the subscription by calling addSubscription and 
     *   then propagates it (recursively) toward the root of the tree unless 
     *   upwards subscription propagation was disabled
     */
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

        public void printSubscriptionsTable() {
        System.out.println("\n" + getName() + "'s Subscription Table:");
        System.out.println("==============================");

        if (subscriptionsTable.isEmpty()) {
            System.out.println("  (Table is empty)");
        } else {
            String formatString = "  %-15s -> %s%n";

            System.out.printf(formatString, "Source Node", "Subscription Details");
            System.out.println("  ---------------   --------------------");

            for (Map.Entry<TreeNode, SimulationSubscription> entry : subscriptionsTable.entrySet()) {
                TreeNode sourceNode = entry.getKey();
                SimulationSubscription tableEntry = entry.getValue();
                System.out.printf(formatString, sourceNode.getName(), tableEntry);
            }
        }
        System.out.println("==============================");
    }

}