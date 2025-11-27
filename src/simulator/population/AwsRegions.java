package simulator.population;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.topology.analysis.TopologyAnalyzer;
import simulator.topology.geonames.PoliticalTopologyInspector;
import utils.CustomLogger;

public class AwsRegions {
    
    private static final Logger logger = CustomLogger.getLogger(AwsRegions.class.getName());

    public record AwsRegionDef(String l2Country, String l3AdminDivision, String l4City, String awsRegionName) {}

    public static final List<AwsRegionDef> AWS_REGION_LIST = Arrays.asList(
        new AwsRegionDef("United States", "Virginia", "Ashburn", "us-east-1"),
        new AwsRegionDef("United States", "Ohio", "New Albany", "us-east-2"),
        new AwsRegionDef("United States", "California", "Santa Clara", "us-west-1"),
        new AwsRegionDef("United States", "Oregon", "Boardman", "us-west-2"),
        new AwsRegionDef("Canada", "Quebec", "Montreal", "ca-central-1"),
        new AwsRegionDef("Canada", "Alberta", "Calgary", "ca-west-1"),
        new AwsRegionDef("Mexico", "Querétaro", "Querétaro", "mx-central-1"),
        new AwsRegionDef("Brazil", "São Paulo", "São Paulo", "sa-east-1"),
        new AwsRegionDef("South Africa", "Western Cape", "Cape Town", "af-south-1"),
        new AwsRegionDef("Hong Kong", "Hong Kong", "Hong Kong", "ap-east-1"),
        new AwsRegionDef("India", "Maharashtra", "Mumbai", "ap-south-1"),
        new AwsRegionDef("India", "Telangana", "Hyderabad", "ap-south-2"),
        new AwsRegionDef("Singapore", "Singapore", "Singapore", "ap-southeast-1"),
        new AwsRegionDef("Australia", "New South Wales", "Sydney", "ap-southeast-2"),
        new AwsRegionDef("Indonesia", "DKI Jakarta", "Jakarta", "ap-southeast-3"),
        new AwsRegionDef("Australia", "Victoria", "Melbourne", "ap-southeast-4"),
        new AwsRegionDef("Malaysia", "Kuala Lumpur", "Kuala Lumpur", "ap-southeast-5"),
        new AwsRegionDef("New Zealand", "Auckland Region", "Auckland", "ap-southeast-6"),
        new AwsRegionDef("Thailand", "Bangkok", "Bangkok", "ap-southeast-7"),
        new AwsRegionDef("Japan", "Tokyo Metropolis", "Tokyo", "ap-northeast-1"),
        new AwsRegionDef("South Korea", "Seoul Special City", "Seoul", "ap-northeast-2"),
        new AwsRegionDef("Japan", "Osaka Prefecture", "Osaka", "ap-northeast-3"),
        new AwsRegionDef("Germany", "Hesse", "Frankfurt", "eu-central-1"),
        new AwsRegionDef("Switzerland", "Zurich", "Zurich", "eu-central-2"),
        new AwsRegionDef("Ireland", "Leinster", "Dublin", "eu-west-1"),
        new AwsRegionDef("United Kingdom", "England", "London", "eu-west-2"),
        new AwsRegionDef("France", "Île-de-France", "Paris", "eu-west-3"),
        new AwsRegionDef("Sweden", "Stockholm County", "Stockholm", "eu-north-1"),
        new AwsRegionDef("Italian Republic", "Lombardia", "Milan", "eu-south-1"),
        new AwsRegionDef("Spain", "Aragón", "Zaragoza", "eu-south-2"),
        new AwsRegionDef("Israel", "Tel Aviv District", "Tel Aviv", "il-central-1"),
        new AwsRegionDef("Bahrain", "Capital Governorate", "Manama", "me-south-1"),
        new AwsRegionDef("United Arab Emirates", "Emirate of Dubai", "Dubai", "me-central-1")
    );

    public static List<BoundedBroker> findAwsBrokers(BoundedBroker rootNode) {
        List<BoundedBroker> allLevel2Regions = TopologyAnalyzer.findBrokersAtLevel(rootNode, 2);
        
        Map<String, BoundedBroker> l2BrokerMap = allLevel2Regions.stream()
            .collect(Collectors.toMap(TreeNode::getName, b -> b, (b1, b2) -> b1));

        return AWS_REGION_LIST.stream()
            .map(def -> resolveBroker(l2BrokerMap, def))
            .filter(b -> b != null)
            .collect(Collectors.toList());
    }

    private static BoundedBroker resolveBroker(Map<String, BoundedBroker> l2Map, AwsRegionDef def) {
        BoundedBroker l2 = l2Map.get(def.l2Country());
        if (l2 == null) return null;

        return PoliticalTopologyInspector.findBrokerByHierarchy(l2, def.l3AdminDivision(), def.l4City());
    }
}