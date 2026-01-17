package marketplace.logic;

import subscribing.Subscription;
import java.util.Map;

public class ServiceEntry {
    private final String providerId; 
    private final Subscription subscription; 
    private final Map<String, Object> capabilities;

    public ServiceEntry(String providerId, Subscription sub, Map<String, Object> caps) {
        this.providerId = providerId;
        this.subscription = sub;
        this.capabilities = caps;
    }
    public String getProviderId() { return providerId; }
    public Subscription getSubscription() { return subscription; }
    public Map<String, Object> getCapabilities() { return capabilities; }
}