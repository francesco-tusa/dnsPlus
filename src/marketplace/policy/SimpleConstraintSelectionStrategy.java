package marketplace.policy;

import marketplace.logic.ServiceEntry;
import java.util.List;
import java.util.Map;

public class SimpleConstraintSelectionStrategy implements ServiceSelectionStrategy {
    @Override
    public String selectBestProvider(List<ServiceEntry> candidates, Map<String, Object> requirements) {
        for (ServiceEntry candidate : candidates) {
            // Real logic would check constraints (min_ram, cost, etc.) here.
            // Returning first match for simulation.
            return candidate.getProviderId();
        }
        return null;
    }
}