package marketplace.model;

import java.util.Map;

public class ServiceOffer {
    // The identity used for Crypto Subscription
    private final String serviceName; 
    // The capabilities exposed to the Broker (e.g., "region=us-east")
    private final Map<String, Object> capabilities;

    public ServiceOffer(String serviceName, Map<String, Object> capabilities) {
        this.serviceName = serviceName;
        this.capabilities = capabilities;
    }

    public String getServiceName() { return serviceName; }
    public Map<String, Object> getCapabilities() { return capabilities; }
}