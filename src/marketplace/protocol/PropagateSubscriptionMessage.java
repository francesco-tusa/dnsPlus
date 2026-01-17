package marketplace.protocol;

import subscribing.Subscription;
import java.util.Map;
import java.util.UUID;

public class PropagateSubscriptionMessage extends MarketplaceMessage {
    
    public enum Scope { 
        LOCAL,      // Stays on the Broker (Private)
        REGIONAL,   // Propagates to Core/Parent but stops there
        GLOBAL      // Propagates all the way to Root
    }

    private final String messageId;
    private final int ttl;
    private final Scope scope;
    
    private final Subscription subscription;
    private final Map<String, Object> capabilities;
    private final String originalProviderId;

    // Constructor for creating a NEW message
    public PropagateSubscriptionMessage(String senderBrokerId, String originalProviderId, 
                                      Subscription sub, Map<String, Object> caps, 
                                      int ttl, Scope scope) {
        super(senderBrokerId);
        this.messageId = UUID.randomUUID().toString();
        this.originalProviderId = originalProviderId;
        this.subscription = sub;
        this.capabilities = caps;
        this.ttl = ttl;
        this.scope = scope;
    }

    // Constructor for FORWARDING (Preserves ID, Decrements TTL)
    public PropagateSubscriptionMessage(String senderBrokerId, PropagateSubscriptionMessage original) {
        super(senderBrokerId);
        this.messageId = original.messageId; // Maintain ID for loop detection
        this.originalProviderId = original.getOriginalProviderId();
        this.subscription = original.getSubscription();
        this.capabilities = original.getCapabilities();
        this.scope = original.getScope();
        this.ttl = original.ttl - 1;         // Decrement TTL
    }

    public String getMessageId() { return messageId; }
    public int getTtl() { return ttl; }
    public Scope getScope() { return scope; }
    public Subscription getSubscription() { return subscription; }
    public String getOriginalProviderId() { return originalProviderId; }
    public Map<String, Object> getCapabilities() { return capabilities; }
}