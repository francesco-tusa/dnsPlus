package simulator;

import broker.GenericBroker;

public abstract class SimulationBroker extends TreeNode implements GenericBroker<SubscriptionWithLocation, PublicationWithLocation> {

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


    @Override
    public SubscriptionWithLocation matchPublication(PublicationWithLocation p) {
        throw new UnsupportedOperationException("Unimplemented method 'matchPublication'");
    }

    @Override
    public void processPublication(PublicationWithLocation p) {
        throw new UnsupportedOperationException("Unimplemented method 'processPublication'");
    }

    /* 
        Process the subscription by calling addSubscription and 
        then propagates it toward the root of the tree
    */
    @Override
    public void processSubscription(SubscriptionWithLocation s) {
        System.out.println(getName() + ": processing subscription");
        nSubscriptions++;

        addSubscription(s);
        
        SimulationBroker parentBroker = getParentBroker();
        if (parentBroker != null) {
            s.setSource(this);
            parentBroker.processSubscription(s);
        }
    }
}