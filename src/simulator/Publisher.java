package simulator;

public class Publisher extends TreeNode {
    Location location;
    private int nPublications;

    public Publisher(String name, Location location) {
        super(name);
        this.location = location;
        this.nPublications = 0;
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
            System.out.println(getName() + ": sending publication " + p.getLocation() + " to " + broker.getName());
            broker.processPublication(p);
            nPublications++;
        } 
        
        else {
            System.out.println(getName() + ": there is no broker to send the publication to");
        }
    } 
}
