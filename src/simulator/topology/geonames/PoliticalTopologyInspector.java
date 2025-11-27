package simulator.topology.geonames;

import java.util.LinkedList;
import java.util.Queue;
import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.topology.analysis.TopologyAnalyzer;

/**
 * Utility class for inspecting topologies that follow the Political structure.
 */
public class PoliticalTopologyInspector {

    /**
     * Finds a broker by following a strict political hierarchy.
     */
    public static BoundedBroker findBrokerByHierarchy(BoundedBroker countryBroker, String admin1Name, String cityName) {
        if (countryBroker == null) return null;

        // Try to find specific Admin1 region under the country
        BoundedBroker admin1Broker = TopologyAnalyzer.findBrokerByName(countryBroker, admin1Name);
        if (admin1Broker == null) {
            return countryBroker; // Fallback to Country
        }

        // Try to find specific City/Admin2 under Admin1
        BoundedBroker cityBroker = TopologyAnalyzer.findBrokerByName(admin1Broker, cityName);
        return (cityBroker != null) ? cityBroker : admin1Broker;
    }

    /**
     * Finds a broker by a partial name match.
     */
    public static BoundedBroker findBrokerByNamePartial(TreeNode root, String partialName) {
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