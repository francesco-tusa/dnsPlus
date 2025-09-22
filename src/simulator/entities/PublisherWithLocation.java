package simulator.entities;

import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.visualisation.TopologyVisualiser;
import utils.CustomLogger;

public class PublisherWithLocation extends TreeNode {
    private static final Logger logger = CustomLogger.getLogger(PublisherWithLocation.class.getName());
    
    private final Location location;
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
            if (p instanceof PublicationWithLocation pl) {
                pubInfo = " for location " + pl.getLocation();
            }
            logger.fine("\n" + getName() + ": sending publication" + pubInfo);
            
            // Set this publisher's node to active (bold) before sending.
            TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
            if (visualizer != null) {
                visualizer.setNodeActive(getName());
            }

            broker.processPublication(p);
            nPublications++;
        }
        else {
            System.err.println(getName() + ": there is no broker to send the publication to");
        }
    }
    
    public Location getLocation() { return location; }
    public int getnPublications() { return nPublications; }
    public SimulationBroker getBroker() { return (SimulationBroker) getParent(); }
}