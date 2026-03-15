package marketplace.population;

import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.population.PopulationBasedPublishersPlacement;
import marketplace.agents.MarketplaceClient;

public class MarketplaceClientPopulationPlacement extends PopulationBasedPublishersPlacement {

    @Override
    protected PublisherWithLocation createPublisher(Location loc) {
        return new MarketplaceClient(loc); 
    }
}