package simulator.tests.scenarios.proximity.e2e;

import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.tests.framework.FunctionalTestUtils;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import simulator.topology.analysis.TopologyAnalyser;

public class ProximityGeoNamesTopologyE2ETest extends TestScenario {

    @Override
    public String getTestName() { 
        return "Proximity E2E: GeoNames Topology (Floating Point)"; 
    }

    @Override
    public boolean run(TopologyFixture fixture) {
        SimulationBroker root = fixture.getRoot();
        if (root == null) return false;

        // 1. Find a suitable Leaf Broker to attach our actors
        // Use the centralized TopologyAnalyser logic
        SimulationBroker leafBroker = TopologyAnalyser.findFirstLeafBroker(root);
        
        if (leafBroker == null) {
            logger.severe("Topology Error: No leaf broker found in GeoNames tree.");
            return false;
        }
        logger.info("Attached actors to leaf: " + leafBroker.getName());

        // 2. Setup Actors with Real-World Float Coordinates
        
        // Subscriber: London (Approx 51.5074, -0.1278)
        Location locLondon = new Location(51.5074, -0.1278, 0); 
        SubscriberWithLocation sub = new SubscriberWithLocation("SubLondon", locLondon);
        leafBroker.addChild(sub);
        
        // Publisher A: New York ("Far" - 40.7128, -74.0060)
        Location locNY = new Location(40.7128, -74.0060, 0);
        PublisherWithLocation pubNY = new PublisherWithLocation("PubNY", locNY);
        leafBroker.addChild(pubNY); 
        
        // Publisher B: Paris ("Close" - 48.8566, 2.3522)
        Location locParis = new Location(48.8566, 2.3522, 0);
        PublisherWithLocation pubParis = new PublisherWithLocation("PubParis", locParis);
        leafBroker.addChild(pubParis);

        // 3. Subscribe
        sub.send(new SubscriptionWithLocation(locLondon));

        // 4. Publish Sequence
        
        // A. Publish Far (NY) -> ACCEPTED
        pubNY.send(new PublicationWithLocation(locNY));

        // B. Publish Close (Paris) -> ACCEPTED (Improvement)
        pubParis.send(new PublicationWithLocation(locParis));

        // C. Publish Far Again (NY) -> REJECTED (Worse)
        pubNY.send(new PublicationWithLocation(locNY));

        // 5. Verify using Utils
        try {
            FunctionalTestUtils.assertReceived(sub, 2);
            logger.info("Success: GeoNames topology correctly filtered based on floating point distance.");
        } catch (AssertionError e) {
            logger.severe("Failure: " + e.getMessage());
            return false;
        }
        return true;
    }
}