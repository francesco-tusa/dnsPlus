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
     *  then propagates it toward the root of the tree
     */

    @Override
    public void processPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing publication");
        nPublications++;

        matchPublication(p);

        SimulationBroker parentBroker = getParentBroker();
        if (parentBroker != null) {
            p.setSource(this);
            parentBroker.processPublication(p);
        }
    }

    /* 
     *   Process the subscription by calling addSubscription and 
     *   then propagates it toward the root of the tree
     */
    @Override
    public void processSubscription(SimulationSubscription s) {
        System.out.println(getName() + ": processing subscription");
        nSubscriptions++;

        addSubscription(s);
        
        SimulationBroker parentBroker = getParentBroker();
        if (parentBroker != null && s.isForwardingEnabled()) {
            s.setSource(this);
            parentBroker.processSubscription(s);
        }
    }
}