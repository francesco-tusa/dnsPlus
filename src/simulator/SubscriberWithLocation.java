package simulator;

public class SubscriberWithLocation extends TreeNode {
    private Location location;
    private int nSubscriptions;
    private int nPublications;
    private PublicationWithLocation lastReceivedPublication; // Added for validation

    public SubscriberWithLocation(String name, Location location) {
        super(name);
        this.location = location;
        this.nSubscriptions = 0;
        this.nPublications = 0;
        this.lastReceivedPublication = null;
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

    /**
     * Receives a publication and updates the internal counter and cache.
     * @param p The publication received from a broker.
     */
    public void receive(SimulationPublication p) {
        System.out.println(getName() + ": received publication " + p);
        nPublications++;
        if (p instanceof PublicationWithLocation) {
            this.lastReceivedPublication = (PublicationWithLocation) p;
        }
    }
    
    public void send(SimulationSubscription s) {
        SimulationBroker broker = getBroker();
        s.setSource(this);

        if (broker != null) {
            System.out.println();
            System.out.println(getName() + ": sending subscription " + s + " to " + broker.getName());
            broker.processSubscription(s);
            nSubscriptions++;
        } 
        
        else {
            System.out.println(getName() + ": topology error, there is no broker to send the subscription to");
        }
    }
    
    /**
     * Gets the last publication this subscriber received. Used for validation.
     * @return The last received publication, or null if none.
     */
    public PublicationWithLocation getLastReceivedPublication() {
        return lastReceivedPublication;
    }

    @Override
    public String toString() {
        return "Subscriber [name=" + getName() + ", location=" + location + "]";
    }
}
