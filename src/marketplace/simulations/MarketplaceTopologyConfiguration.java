package marketplace.simulations;

import marketplace.config.MarketplaceConfig;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;

public class MarketplaceTopologyConfiguration extends GeoNamesTopologyConfiguration {

    public MarketplaceTopologyConfiguration() {
        super();
    }

    public boolean isCountryAllowed(String isoCodeOrName) {
        var allowed = MarketplaceConfig.get().allowedCountries;
        if (allowed.isEmpty()) return true; 

        // robust check: case-insensitive
        String input = isoCodeOrName.trim().toUpperCase();
        
        // Direct match
        if (allowed.contains(input)) return true;
        
        return false;
    }
}