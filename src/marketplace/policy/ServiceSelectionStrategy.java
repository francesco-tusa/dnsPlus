package marketplace.policy;

import marketplace.logic.ServiceEntry;
import java.util.List;
import java.util.Map;

public interface ServiceSelectionStrategy {
    String selectBestProvider(List<ServiceEntry> candidates, Map<String, Object> requirements);
}