package simulator;

import broker.GenericBroker;

public abstract class SimulationBroker extends TreeNode implements GenericBroker<SubscriptionWithLocation, PublicationWithLocation> {

    private String name;
    private int nSubscriptions;
    private int nPublications;

    private SimulationBroker parent;

    public SimulationBroker(String name) {
        this.name = name;
        this.nSubscriptions = 0;
        this.nPublications = 0;
    }

    public String getName() {
        return name;
    }

    public int getnSubscriptions() {
        return nSubscriptions;
    }

    public int getnPublications() {
        return nPublications;
    }

    public SimulationBroker getParentBroker() {
        return (SimulationBroker) parent;
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
        System.out.println("Processing subscription on node: " + name);
        nSubscriptions++;
        
        s.setSource(this);
        addSubscription(s);
        
        if (getParentBroker() != null) {
            parent.processSubscription(s);
        }
    }
}