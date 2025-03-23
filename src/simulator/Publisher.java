package simulator;

public class Publisher extends TreeNode {
    
    private int nPublications;

    public Publisher(String name) {
        super(name);
        this.nPublications = 0;
    }

    public int getnPublications() {
        return nPublications;
    }

    public SimulationBroker getBroker() {
        return (SimulationBroker) getParent();
    }
    
    public void send(PublicationWithLocation p) {
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
