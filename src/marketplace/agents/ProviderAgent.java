package marketplace.agents;

import encryption.BlindingSubscriber;
import encryption.HEPS;
import subscribing.Subscription;
import marketplace.model.FunctionProfile;
import marketplace.model.ServiceOffer;
import marketplace.protocol.*;
import marketplace.network.NetworkInterface;
import java.util.HashMap;
import java.util.Map;

public class ProviderAgent extends BlindingSubscriber implements NetworkInterface.MessageReceiver {
    
    private final NetworkInterface network;
    private final String brokerAddress;
    private final Map<String, String> localRegistry = new HashMap<>();

    public ProviderAgent(String name, NetworkInterface net, String brokerAddr) {
        super(name);
        this.network = net;
        this.brokerAddress = brokerAddr;
        this.heps = HEPS.getInstance(); // Ensure HEPS is init
        this.init(); // Init keys
        this.network.registerAgent(name, this);
    }

    public boolean attemptOnboard(FunctionProfile profile) {
        // 1. Install locally
        localRegistry.put(profile.getServiceName(), profile.getDockerImage());
        
        // 2. Capabilities
        Map<String, Object> offeredCaps = new HashMap<>(profile.getRequirements());
        offeredCaps.put("provider", this.getName());
        ServiceOffer offer = new ServiceOffer(profile.getServiceName(), offeredCaps);
        
        // 3. Crypto Subscription
        Subscription sub = this.generateSubscription(offer.getServiceName());
        
        // 4. Register
        RegisterMessage msg = new RegisterMessage(this.getName(), sub, offer.getCapabilities());
        network.send(brokerAddress, msg);
        
        System.out.println("[Provider " + getName() + "] Onboarded: " + profile.getServiceName());
        return true;
    }

    @Override
    public void onMessage(MarketplaceMessage message) {
        if (message instanceof ExecutionMessage) {
            ExecutionMessage execMsg = (ExecutionMessage) message;
            System.out.println("[Provider " + getName() + "] Executing Service Request for Client: " + execMsg.getOriginalClientId());
            // Logic to verify payload and run container would go here
        }
    }
    
    // Abstract methods from BlindingEntity requiring implementations
    public void publish() {}
    public void subscribe() {}
}