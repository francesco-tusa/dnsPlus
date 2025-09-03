package simulator.entities;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;

public class PublisherWithLocation extends TreeNode {
    Location location;
    private int nPublications;

    public PublisherWithLocation(String name, Location location) {
        super(name);
        this.location = location;
        this.nPublications = 0;
    }
    
    public void send(SimulationPublication p) {
        SimulationBroker broker = getBroker();
        p.setSource(this);
        
        if (broker != null) {
            String pubInfo = "";
            if (p instanceof PublicationWithLocation) {
                PublicationWithLocation pl = (PublicationWithLocation) p;
                pubInfo = " for location " + pl.getLocation();
            }
            System.out.println("\n" + getName() + ": sending publication" + pubInfo);
            
            broker.processPublication(p);
            nPublications++;
        }
        else {
            System.out.println(getName() + ": there is no broker to send the publication to");
        }
    }
    
    // Unchanged getters
    public Location getLocation() { return location; }
    public int getnPublications() { return nPublications; }
    public SimulationBroker getBroker() { return (SimulationBroker) getParent(); }
}