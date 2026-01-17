package marketplace.protocol;

import publishing.Publication;

public class ExecutionMessage extends MarketplaceMessage {
    private final String originalClientId;
    private final Object payload; // Supports both Publication (Cold) and Raw Data (Fast)

    public ExecutionMessage(String senderBrokerId, String clientId, Object payload) {
        super(senderBrokerId);
        this.originalClientId = clientId;
        this.payload = payload;
    }
    
    public String getOriginalClientId() { return originalClientId; }
    public Object getPayload() { return payload; }
}