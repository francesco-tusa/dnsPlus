package simulator.simulations.functional;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import utils.CustomLogger;

public class GeoNamesPropagationTest {

    private static final Logger logger = CustomLogger.getLogger(GeoNamesPropagationTest.class.getName());

    public static final Predicate<BoundedBroker> COMPREHENSIVE_REGRESSION_TEST = root -> {
        logger.info("\n>>> STARTING COMPREHENSIVE REGRESSION TEST (GeoNames Subset) <<<");
        boolean allPassed = true;

        // ----------------------------------------------------------------
        // SETUP
        // ----------------------------------------------------------------
        BoundedBroker dhaka = findBrokerByNamePartial(root, "Dhaka"); 
        BoundedBroker sylhet = findBrokerByNamePartial(root, "Sylhet"); 
        BoundedBroker rajshahi = findBrokerByNamePartial(root, "Rajshahi"); 
        BoundedBroker chittagong = findBrokerByNamePartial(root, "Chittagong");
        BoundedBroker beijing = findBrokerByNamePartial(root, "Beijing");
        BoundedBroker bangladesh = findBrokerByNamePartial(root, "Bangladesh");

        if (dhaka == null || sylhet == null || rajshahi == null || chittagong == null || beijing == null || bangladesh == null) {
            logger.severe("FAILURE: Could not find all required brokers.");
            return false;
        }
        
        Location dhakaCenter = dhaka.getRegion().getKeyPoints().get(8);
        SubscriberWithLocation subLocal = new SubscriberWithLocation("Sub-Dhaka-Local", dhakaCenter);
        dhaka.addChild(subLocal);
        dhaka.updateRegion(subLocal);

        SubscriberWithLocation subRemote = new SubscriberWithLocation("Sub-Dhaka-Remote", dhakaCenter);
        dhaka.addChild(subRemote);

        // ----------------------------------------------------------------
        // PHASE 1: Downward Intersection (Dhaka -> Bangladesh -> Sylhet)
        // ----------------------------------------------------------------
        logger.info("\n[PHASE 1] Testing Downward Intersection (Dhaka -> Bangladesh -> Sylhet)...");

        // Adjusted coordinates to be EXCLUSIVELY inside Sylhet (East of 91.25) 
        // to avoid intersecting Dhaka and causing side-effect propagations.
        // Sylhet: ~[90.77 - 92.45]. Dhaka: ~[89.31 - 91.25].
        Region regionSylhet = new Region(new Location(91.5, 23.0, 0), new Location(92.0, 24.0, 0));
        logger.info("Sub-Dhaka-Local subscribing to: " + regionSylhet.toLogString());
        subLocal.send(new SubscriptionWithRegion(regionSylhet));

        boolean sylhetReceivedSub = false;
        Map<TreeNode, List<SimulationSubscription>> sylhetSubs = sylhet.getInputSubscriptions();
        if (sylhetSubs.containsKey(bangladesh)) {
            for (SimulationSubscription s : sylhetSubs.get(bangladesh)) {
                if (s instanceof SubscriptionWithRegion sub && sub.getRegion().contains(regionSylhet.getBottomLeft())) {
                     sylhetReceivedSub = true;
                     break;
                }
            }
        }
        
        if (sylhetReceivedSub) logger.info("SUCCESS: Bangladesh propagated subscription DOWN to Sylhet.");
        else { logger.severe("FAILURE: Sylhet did not receive subscription."); allPassed = false; }

        // ----------------------------------------------------------------
        // PHASE 1.B: Downward Redundancy (Rajshahi -> Bangladesh -x-> Sylhet)
        // ----------------------------------------------------------------
        logger.info("\n[PHASE 1.B] Testing Downward Redundancy...");
        
        Location rajshahiCenter = rajshahi.getRegion().getKeyPoints().get(8);
        SubscriberWithLocation subRajshahi = new SubscriberWithLocation("Sub-Rajshahi", rajshahiCenter);
        rajshahi.addChild(subRajshahi);
        rajshahi.updateRegion(subRajshahi);
        
        // Small region strictly contained in the Phase 1 region
        Region smallSylhet = new Region(new Location(91.6, 23.2, 0), new Location(91.8, 23.8, 0));
        logger.info("Sub-Rajshahi subscribing to Contained Region: " + smallSylhet.toLogString());
        
        long expansionsBefore = bangladesh.getNumPropagationFilterExpansions();
        subRajshahi.send(new SubscriptionWithRegion(smallSylhet));
        long expansionsAfter = bangladesh.getNumPropagationFilterExpansions();
        
        if (expansionsAfter == expansionsBefore) {
            logger.info("SUCCESS: Bangladesh filtered the redundant downward subscription.");
        } else {
            logger.severe("FAILURE: Bangladesh propagated redundant subscription! Expansions went from " + expansionsBefore + " to " + expansionsAfter);
            allPassed = false;
        }

        // ----------------------------------------------------------------
        // PHASE 2: Aggregation & Filtering
        // ----------------------------------------------------------------
        logger.info("\n[PHASE 2] Testing Aggregation & Filtering...");
        
        Region regionA = new Region(new Location(116.0, 39.0, 0), new Location(116.1, 39.1, 0));
        Region regionB = new Region(new Location(116.9, 39.9, 0), new Location(117.0, 40.0, 0));
        SubscriberWithLocation subChitA = new SubscriberWithLocation("Sub-Chit-A", chittagong.getRegion().getKeyPoints().get(8));
        SubscriberWithLocation subChitB = new SubscriberWithLocation("Sub-Chit-B", chittagong.getRegion().getKeyPoints().get(8));
        chittagong.addChild(subChitA);
        chittagong.addChild(subChitB);
        subChitA.send(new SubscriptionWithRegion(regionA));
        subChitB.send(new SubscriptionWithRegion(regionB));

        boolean aggregationPassed = false;
        Region expectedUnion = new Region(regionA); expectedUnion.expand(regionB);
        List<SimulationSubscription> chitPropagated = chittagong.getPropagatedSubscriptions().get(bangladesh);
        if (chitPropagated != null && !chitPropagated.isEmpty()) {
            if (chitPropagated.get(0) instanceof SubscriptionWithRegion swr && swr.getRegion().equals(expectedUnion)) {
                aggregationPassed = true;
            }
        }
        if (aggregationPassed) logger.info("SUCCESS: Aggregation verified.");
        else { logger.severe("FAILURE: Aggregation failed."); allPassed = false; }

        Region largeBeijing = new Region(new Location(115.0, 38.0, 0), new Location(118.0, 42.0, 0)); 
        logger.info("Sub-Dhaka-Remote subscribing to: " + largeBeijing.toLogString());
        subRemote.send(new SubscriptionWithRegion(largeBeijing));
        
        Region state1 = getPropagatedRegion(dhaka, bangladesh);
        
        Region smallBeijing = new Region(new Location(116.4, 39.9, 0), new Location(116.5, 40.0, 0));
        SubscriberWithLocation subDhakaSmall = new SubscriberWithLocation("Sub-Dhaka-Small", dhaka.getRegion().getKeyPoints().get(8));
        dhaka.addChild(subDhakaSmall);
        subDhakaSmall.send(new SubscriptionWithRegion(smallBeijing));

        Region state2 = getPropagatedRegion(dhaka, bangladesh);
        if (state1 != null && state1.equals(state2)) logger.info("SUCCESS: Filtering verified.");
        else { logger.severe("FAILURE: Filtering failed."); allPassed = false; }

        // ----------------------------------------------------------------
        // PHASE 3: Internal Routing
        // ----------------------------------------------------------------
        logger.info("\n[PHASE 3] Testing Internal Routing (Sylhet -> Bangladesh -> Dhaka)...");

        PublisherWithLocation pubSylhet = new PublisherWithLocation("Pub-Sylhet", sylhet.getRegion().getKeyPoints().get(8));
        sylhet.addChild(pubSylhet);
        
        // Publish inside the Phase 1 subscription area
        Location locInternal = new Location(91.6, 23.5, 0); 
        logger.info("Pub-Sylhet sending Publication at: " + locInternal.toString());
        pubSylhet.send(new PublicationWithLocation(locInternal));
        
        int countLocal = subLocal.getnPublications();
        int countRemote = subRemote.getnPublications();
        
        if (countLocal == 1 && countRemote == 0) {
            logger.info("SUCCESS: Internal routing verified.");
        } else {
            logger.severe(String.format("FAILURE: Internal routing mismatch. Local: %d (Exp: 1), Remote: %d (Exp: 0)", countLocal, countRemote));
            allPassed = false;
        }

        // ----------------------------------------------------------------
        // PHASE 4: Outlier Test
        // ----------------------------------------------------------------
        logger.info("\n[PHASE 4] Testing Aggregation False Positive (Tibet Gap)...");
        
        PublisherWithLocation pubTibet = new PublisherWithLocation("Pub-Tibet", sylhet.getRegion().getKeyPoints().get(8));
        sylhet.addChild(pubTibet);
        
        Location outlierLoc = new Location(100.0, 30.0, 0); 
        logger.info("Pub-Tibet sending Outlier at: " + outlierLoc.toString());
        pubTibet.send(new PublicationWithLocation(outlierLoc));
        
        int countLocal2 = subLocal.getnPublications();
        int countRemote2 = subRemote.getnPublications();
        
        if (countLocal2 == 1 && countRemote2 == 0) {
            logger.info("SUCCESS: Outlier correctly filtered.");
        } else {
            logger.severe(String.format("FAILURE: Subscriber received outlier! Local: %d, Remote: %d", countLocal2, countRemote2));
            allPassed = false;
        }

        // ----------------------------------------------------------------
        // PHASE 5: Remote Propagation
        // ----------------------------------------------------------------
        logger.info("\n[PHASE 5] Testing Remote Propagation (Beijing -> Dhaka)...");
        
        PublisherWithLocation pubBeijing = new PublisherWithLocation("Pub-Beijing", beijing.getRegion().getKeyPoints().get(8));
        beijing.addChild(pubBeijing);
        
        Location validLoc = new Location(116.0, 40.0, 0); 
        logger.info("Pub-Beijing sending Valid Publication at: " + validLoc.toString());
        pubBeijing.send(new PublicationWithLocation(validLoc));
        
        int countLocal3 = subLocal.getnPublications();
        int countRemote3 = subRemote.getnPublications();
        
        logger.info(String.format("Final Counts -> Local: %d (Exp: 1), Remote: %d (Exp: 1)", countLocal3, countRemote3));
        
        if (countLocal3 == 1 && countRemote3 == 1) {
            logger.info("SUCCESS: Remote publication delivered correctly.");
        } else {
            logger.severe("FAILURE: Remote publication failed.");
            allPassed = false;
        }
        
        // ----------------------------------------------------------------
        // STEP 6: Nearby Branch Routing
        // ----------------------------------------------------------------
        logger.info("\n[STEP 6] Testing Nearby Branch Routing (Rajshahi -> Dhaka)...");
        
        Location rajshahiPoint = rajshahi.getRegion().getKeyPoints().get(8); 
        Region regionRajshahi = new Region(
            new Location(rajshahiPoint.getX() - 0.1, rajshahiPoint.getY() - 0.1, 0),
            new Location(rajshahiPoint.getX() + 0.1, rajshahiPoint.getY() + 0.1, 0)
        );
        logger.info("Sub-Dhaka-Local subscribing to Rajshahi Region: " + regionRajshahi.toLogString());
        subLocal.send(new SubscriptionWithRegion(regionRajshahi));
        
        PublisherWithLocation pubRajshahi = new PublisherWithLocation("Pub-Rajshahi", rajshahi.getRegion().getKeyPoints().get(8));
        rajshahi.addChild(pubRajshahi);
        
        logger.info("Pub-Rajshahi sending Publication at: " + rajshahiPoint.toString());
        pubRajshahi.send(new PublicationWithLocation(rajshahiPoint));
        
        int countLocalFinal = subLocal.getnPublications();
        int countRemoteFinal = subRemote.getnPublications();
        
        logger.info(String.format("Final Counts -> Local: %d (Exp: 2), Remote: %d (Exp: 1)", countLocalFinal, countRemoteFinal));
        
        boolean nearbyPassed = (countLocalFinal == 2 && countRemoteFinal == 1);
        
        if (nearbyPassed) {
            logger.info("SUCCESS: Nearby Branch Routing verified.");
        } else {
            logger.severe("FAILURE: Nearby Branch Routing failed.");
            allPassed = false;
        }

        return allPassed;
    };
    
    private static Region getPropagatedRegion(BoundedBroker source, BoundedBroker target) {
        List<SimulationSubscription> subs = source.getPropagatedSubscriptions().get(target);
        if (subs != null && !subs.isEmpty()) {
            if (subs.get(0) instanceof SubscriptionWithRegion swr) return swr.getRegion();
        }
        return null;
    }
    
    private static BoundedBroker findBrokerByNamePartial(TreeNode root, String partialName) {
        if (root == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof BoundedBroker && current.getName().contains(partialName)) {
                return (BoundedBroker) current;
            }
            if (current.getChildren() != null) queue.addAll(current.getChildren());
        }
        return null;
    }
}