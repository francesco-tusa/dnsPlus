package marketplace.protocol;

import publishing.Publication;
import java.util.Map;

public class RequestMessage extends MarketplaceMessage {
    private final Publication publication; // The "Key"
    private final Map<String, Object> requirements; // Constraints

    public RequestMessage(String senderId, Publication pub, Map<String, Object> requirements) {
        super(senderId);
        this.publication = pub;
        this.requirements = requirements;
    }
    public Publication getPublication() { return publication; }
    public Map<String, Object> getRequirements() { return requirements; }
}