package simulator.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import utils.CustomLogger;
import simulator.visualisation.TopologyVisualiser;

public class PublisherWithLocation extends TreeNode {
    private static final Logger logger = CustomLogger.getLogger(PublisherWithLocation.class.getName());
    
    private final Location location;
    private int nPublications;
    private final List<PublicationWithLocation> sentPublications = new ArrayList<>();

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
                sentPublications.add(pl); // Add to history
            }
            logger.fine("\n" + getName() + ": sending publication" + pubInfo);
            
            TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
            if (visualizer != null) {
                visualizer.setNodeActive(getName());
                // Update the label with the full history
                visualizer.updatePublisherLabel(this);
            }

            broker.processPublication(p);
            nPublications++;
        }
        else {
            System.err.println(getName() + ": there is no broker to send the publication to");
        }
    }
    
    public List<PublicationWithLocation> getSentPublications() {
        return sentPublications;
    }
    
    public Location getLocation() { return location; }
    public int getnPublications() { return nPublications; }
    public SimulationBroker getBroker() { return (SimulationBroker) getParent(); }
}