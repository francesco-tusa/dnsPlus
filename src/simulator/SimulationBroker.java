package simulator;

import broker.GenericBroker;

public abstract class SimulationBroker extends TreeNode implements GenericBroker<SimulationSubscription, SimulationPublication> {

    private int nSubscriptions;
    private int nPublications;

    public SimulationBroker(String name) {
        super(name);
        this.nSubscriptions = 0;
        this.nPublications = 0;
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


    /*
     *  Process the publication by calling matchPublication and
     *  then propagates it based on the broker's subscription tables
     */

    @Override
    public void processPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing publication");
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
}