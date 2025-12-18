package simulator.population;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.topology.geonames.PoliticalTopologyInspector;
import utils.CustomLogger;

public class AwsRegions {
    
    private static final Logger logger = CustomLogger.getLogger(AwsRegions.class.getName());

    public record AwsRegionDef(String l2Country, String l3AdminDivision, String l4City, String awsRegionName, double lat, double lon) {}

    public static final List<AwsRegionDef> AWS_REGION_LIST = Arrays.asList(
        new AwsRegionDef("United States", "Virginia", "Ashburn", "us-east-1", 39.0438, -77.4874),
        new AwsRegionDef("United States", "Ohio", "New Albany", "us-east-2", 40.0811, -82.8088),
        new AwsRegionDef("United States", "California", "Santa Clara", "us-west-1", 37.3541, -121.9552),
        new AwsRegionDef("United States", "Oregon", "Boardman", "us-west-2", 45.8399, -119.7006),
        new AwsRegionDef("Canada", "Quebec", "Montreal", "ca-central-1", 45.5017, -73.5673),
        new AwsRegionDef("Canada", "Alberta", "Calgary", "ca-west-1", 51.0447, -114.0719),
        new AwsRegionDef("Mexico", "Querétaro", "Querétaro", "mx-central-1", 20.5888, -100.3899),
        new AwsRegionDef("Brazil", "São Paulo", "São Paulo", "sa-east-1", -23.5505, -46.6333),
        new AwsRegionDef("South Africa", "Western Cape", "Cape Town", "af-south-1", -33.9249, 18.4241),
        new AwsRegionDef("Hong Kong", "Hong Kong", "Hong Kong", "ap-east-1", 22.3193, 114.1694),
        new AwsRegionDef("India", "Maharashtra", "Mumbai", "ap-south-1", 19.0760, 72.8777),
        new AwsRegionDef("India", "Telangana", "Hyderabad", "ap-south-2", 17.3850, 78.4867),
        new AwsRegionDef("Singapore", "Singapore", "Singapore", "ap-southeast-1", 1.3521, 103.8198),
        new AwsRegionDef("Australia", "New South Wales", "Sydney", "ap-southeast-2", -33.8688, 151.2093),
        new AwsRegionDef("Indonesia", "DKI Jakarta", "Jakarta", "ap-southeast-3", -6.2088, 106.8456),
        new AwsRegionDef("Australia", "Victoria", "Melbourne", "ap-southeast-4", -37.8136, 144.9631),
        new AwsRegionDef("Malaysia", "Kuala Lumpur", "Kuala Lumpur", "ap-southeast-5", 3.1390, 101.6869),
        new AwsRegionDef("New Zealand", "Auckland Region", "Auckland", "ap-southeast-6", -36.8485, 174.7633),
        new AwsRegionDef("Thailand", "Bangkok", "Bangkok", "ap-southeast-7", 13.7563, 100.5018),
        new AwsRegionDef("Japan", "Tokyo Metropolis", "Tokyo", "ap-northeast-1", 35.6762, 139.6503),
        new AwsRegionDef("South Korea", "Seoul Special City", "Seoul", "ap-northeast-2", 37.5665, 126.9780),
        new AwsRegionDef("Japan", "Osaka Prefecture", "Osaka", "ap-northeast-3", 34.6937, 135.5023),
        new AwsRegionDef("Germany", "Hesse", "Frankfurt", "eu-central-1", 50.1109, 8.6821),
        new AwsRegionDef("Switzerland", "Zurich", "Zurich", "eu-central-2", 47.3769, 8.5417),
        new AwsRegionDef("Ireland", "Leinster", "Dublin", "eu-west-1", 53.3498, -6.2603),
        new AwsRegionDef("United Kingdom", "England", "London", "eu-west-2", 51.5074, -0.1278),
        new AwsRegionDef("France", "Île-de-France", "Paris", "eu-west-3", 48.8566, 2.3522),
        new AwsRegionDef("Sweden", "Stockholm County", "Stockholm", "eu-north-1", 59.3293, 18.0686),
        new AwsRegionDef("Italian Republic", "Lombardia", "Milan", "eu-south-1", 45.4642, 9.1900),
        new AwsRegionDef("Spain", "Aragón", "Zaragoza", "eu-south-2", 41.6488, -0.8891),
        new AwsRegionDef("Israel", "Tel Aviv District", "Tel Aviv", "il-central-1", 32.0853, 34.7818),
        new AwsRegionDef("Bahrain", "Capital Governorate", "Manama", "me-south-1", 26.2285, 50.5860),
        new AwsRegionDef("United Arab Emirates", "Emirate of Dubai", "Dubai", "me-central-1", 25.2048, 55.2708)
    );

    public static List<BoundedBroker> findAwsBrokers(BoundedBroker rootNode) {
        // 1. Prepare map for Political lookup (Country Level)
        List<BoundedBroker> allLevel2Regions = TopologyAnalyser.findBrokersAtLevel(rootNode, 2);
        Map<String, BoundedBroker> l2BrokerMap = allLevel2Regions.stream()
            .collect(Collectors.toMap(TreeNode::getName, b -> b, (b1, b2) -> b1));

        return AWS_REGION_LIST.stream()
            .map(def -> resolveBroker(l2BrokerMap, rootNode, def))
            .filter(b -> b != null)
            .collect(Collectors.toList());
    }

    private static BoundedBroker resolveBroker(Map<String, BoundedBroker> l2Map, BoundedBroker rootNode, AwsRegionDef def) {
        BoundedBroker l2 = l2Map.get(def.l2Country());
        
        // --- Strategy A: Political Hierarchy (Preferred for Political Topology) ---
        if (l2 != null) {
            BoundedBroker candidate = PoliticalTopologyInspector.findBrokerByHierarchy(l2, def.l3AdminDivision(), def.l4City());
            
            // Only accept if we found a node MORE specific than the Country itself.
            if (candidate != null && !candidate.equals(l2)) {
                return candidate;
            }
        }

        // --- Strategy B: Geometric Fallback (Essential for R-Tree) ---
        // Uses the new generic method in TopologyAnalyzer
        Location targetLoc = new Location(def.lon(), def.lat(), 0);
        BoundedBroker geoMatch = TopologyAnalyser.findLeafBrokerAtLocation(rootNode, targetLoc);
        
        if (geoMatch != null) {
            logger.finer("Found AWS region " + def.awsRegionName() + " (" + def.l4City() + ") via geometric match in broker: " + geoMatch.getName());
            return geoMatch;
        }

        // --- Strategy C: Last Resort ---
        // If Geometric also fails (e.g., coordinate outside bounds), fall back to the Country node if available.
        if (l2 != null) {
            logger.warning("AWS Region " + def.awsRegionName() + " (" + def.l4City() + ") not found by name or geometry. Falling back to Country: " + l2.getName());
            return l2;
        }

        logger.warning("Could not find broker for AWS Region: " + def.awsRegionName() + " (" + def.l4City() + ")");
        return null;
    }
}