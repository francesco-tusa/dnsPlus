package simulator.topology.geonames;

import simulator.regions.BoundedBroker;
import simulator.topology.analysis.TopologyAnalyzer;

/**
 * Utility class for inspecting topologies that follow the Political structure.
 * Relies on TopologyAnalyzer for the actual graph traversal.
 */
public class PoliticalTopologyInspector {

    /**
     * Finds a broker by following a strict political hierarchy: Country -> Admin1 -> City.
     */
    public static BoundedBroker findBrokerByHierarchy(BoundedBroker countryBroker, String admin1Name, String cityName) {
        if (countryBroker == null) return null;

        // 1. Try to find specific Admin1 region under the country using optimized search
        BoundedBroker admin1Broker = TopologyAnalyzer.findBrokerByName(countryBroker, admin1Name);
        if (admin1Broker == null) {
            return countryBroker; // Fallback to Country if Admin1 not found
        }

        // 2. Try to find specific City/Admin2 under Admin1
        BoundedBroker cityBroker = TopologyAnalyzer.findBrokerByName(admin1Broker, cityName);
        
        // Return City if found, otherwise return the State/Province (Admin1)
        return (cityBroker != null) ? cityBroker : admin1Broker;
    }
}