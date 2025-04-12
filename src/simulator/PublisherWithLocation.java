package simulator;

public class PublisherWithLocation extends TreeNode {
    Location location;
    private int nPublications;

    public PublisherWithLocation(String name, Location location) {
        super(name);
        this.location = location;
        this.nPublications = 0;
    }

    public Location getLocation() {
        return location;
    }

    public int getnPublications() {
        return nPublications;
    }

    public SimulationBroker getBroker() {
        return (SimulationBroker) getParent();
    }
    
    public void send(SimulationPublication p) {
        SimulationBroker broker = getBroker();
        p.setSource(this);
        
        if (broker != null) {
            System.out.println();
            System.out.println(getName() + ": sending publication " + p + " to " + broker.getName());
            broker.processPublication(p);
            nPublications++;
        } 
        
        else {
            System.out.println(getName() + ": there is no broker to send the publication to");
        }
    } 
}
