package marketplace.agents;

import encryption.BlindingPublisher;
import encryption.HEPS;
import publishing.Publication;
import marketplace.protocol.*;
import marketplace.network.NetworkInterface;
import java.util.HashMap;
import java.util.Map;

public class ClientAgent extends BlindingPublisher implements NetworkInterface.MessageReceiver {
    
    private final NetworkInterface network;
    private final String brokerAddress;
    
    // Cache for Fast Path tokens: ServiceName -> Token
    private final Map<String, byte[]> tokenCache = new HashMap<>();
    
    // Track the last request to map the async token back to the service
    private String lastRequestedService = null;

    public ClientAgent(String name, NetworkInterface net, String brokerAddr) {
        super(name);
        this.network = net;
        this.brokerAddress = brokerAddr;
        this.heps = HEPS.getInstance();
        this.init();
        this.network.registerAgent(name, this);
    }

    public void requestService(String serviceName, Map<String, Object> reqs) {
        this.lastRequestedService = serviceName; // Remember what we are asking for

        // 1. Check Fast Path Cache
        if (tokenCache.containsKey(serviceName)) {
            System.out.println("[Client " + getName() + "] [Fast Path] Cache Hit! Using Fast Path for: " + serviceName);
            byte[] token = tokenCache.get(serviceName);
            
            // Send Fast Execution Message
            FastExecutionMessage fastMsg = new FastExecutionMessage(this.getName(), token, "Some Input Data");
            network.send(brokerAddress, fastMsg);
            return;
        }

        // 2. Cold Path (Full Resolution)
        System.out.println("[Client " + getName() + "] [Cold Path] Cache Miss. Performing Full Resolution for: " + serviceName);
        Publication pub = this.generatePublication(serviceName);
        
        // Send Request
        RequestMessage msg = new RequestMessage(this.getName(), pub, reqs);
        network.send(brokerAddress, msg);
    }

    @Override
    public void onMessage(MarketplaceMessage message) {
        if (message instanceof TokenIssueMessage) {
            TokenIssueMessage tokenMsg = (TokenIssueMessage) message;
            
            // Fix: Map the token to the service we actually requested
            if (lastRequestedService != null) {
                System.out.println("[Client " + getName() + "] Received Fast Path Token. Mapping to: " + lastRequestedService);
                tokenCache.put(lastRequestedService, tokenMsg.getRoutingToken());
            }
        }
    }
    
    public void publish() {}
    public void subscribe() {}
}