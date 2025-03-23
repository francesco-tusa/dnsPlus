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
    
    public void send(PublicationWithLocation s) {
        SimulationBroker broker = getBroker();
        s.setSource(this);
        
        if (broker != null) {
            System.out.println(getName() + ": sending publication to " + broker.getName());
            broker.processPublication(s);
            nPublications++;
        } 
        
        else {
            System.out.println(getName() + ": there is no broker to send the publication to");
        }
    } 
}
