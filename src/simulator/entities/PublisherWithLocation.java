package simulator.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.metrics.EventMetrics;
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
    
    public void send(PublicationWithLocation pub) {
        String traceId = this.getName();
        pub.setMetrics(new EventMetrics(traceId));
        
        TreeNode parent = getParent();
        
        if (parent instanceof SimulationBroker) {
            SimulationBroker broker = (SimulationBroker) parent;
            
            String pubInfo = " for location " + pub.getLocation();
            sentPublications.add(pub); 
            
            logger.fine("\n" + getName() + ": sending publication" + pubInfo);
            
            TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
            if (visualizer != null) {
                visualizer.setNodeActive(getName());
                visualizer.updatePublisherLabel(this);
            }

            broker.processPublication(pub);
            nPublications++;
        } else {
            logger.severe(getName() + ": parent is not a SimulationBroker, cannot send publication.");
        }
    }
    
    public List<PublicationWithLocation> getSentPublications() {
        return sentPublications;
    }
    
    public Location getLocation() { return location; }
    public int getnPublications() { return nPublications; }
    public SimulationBroker getBroker() { return (SimulationBroker) getParent(); }
}