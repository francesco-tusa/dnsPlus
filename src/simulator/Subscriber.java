package simulator;

public class Subscriber extends TreeNode {
    
    private int nSubscriptions;
    private int nPublications;

    public Subscriber(String name) {
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

    public SimulationBroker getBroker() {
        return (SimulationBroker) getParent();
    }

    public void receive(PublicationWithLocation p) {
        System.out.println(getName() + ": received publication");
        nPublications++;
    }
    
    public void send(SubscriptionWithLocation s) {
        SimulationBroker broker = getBroker();
        s.setSource(this);

        if (broker != null) {
            System.out.println(getName() + ": sending subscription to " + broker.getName());
            broker.processSubscription(s);
            nSubscriptions++;
        } 
        
        else {
            System.out.println(getName() + ": there is no broker to send the subscription to");
        }
    } 
}
