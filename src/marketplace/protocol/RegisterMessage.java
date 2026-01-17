package marketplace.protocol;

import subscribing.Subscription;
import java.util.Map;

public class RegisterMessage extends MarketplaceMessage {
    private final Subscription subscription; // The "Lock"
    private final Map<String, Object> capabilities; // Metadata

    public RegisterMessage(String senderId, Subscription sub, Map<String, Object> capabilities) {
        super(senderId);
        this.subscription = sub;
        this.capabilities = capabilities;
    }
    public Subscription getSubscription() { return subscription; }
    public Map<String, Object> getCapabilities() { return capabilities; }
}