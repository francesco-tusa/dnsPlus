package simulator;

public class Subscriber extends TreeNode {
    private Location location;
    private int nSubscriptions;
    private int nPublications;

    public Subscriber(String name, Location location) {
        super(name);
        this.location = location;
        this.nSubscriptions = 0;
        this.nPublications = 0;
    }
    
    public Location getLocation() {
        return location;
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
            System.out.println();
            System.out.println(getName() + ": sending subscription " + s.getLocation() + " to " + broker.getName());
            broker.processSubscription(s);
            nSubscriptions++;
        } 
        
        else {
            System.out.println(getName() + ": there is no broker to send the subscription to");
        }
    }
}
