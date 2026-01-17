package marketplace.agents;

import broker.tree.binarybalanced.BrokerWithBinaryBalancedTree;
import encryption.HEPS;
import marketplace.logic.ServiceEntry;
import marketplace.policy.*;
import marketplace.network.NetworkInterface;
import marketplace.protocol.*;
import publishing.Publication;
import subscribing.Subscription;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BrokerAgent extends BrokerWithBinaryBalancedTree implements NetworkInterface.MessageReceiver {

    private final NetworkInterface network;
    private final ServiceSelectionStrategy selectionStrategy;
    private final SecretKeySpec tokenKey;

    // Topology: If null, this node is a Root/Top-Level Broker
    private String parentBrokerAddress = null;

    // State
    private final Map<String, Map<String, Object>> providerCapabilities = new HashMap<>();
    private final Set<String> seenMessages = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Configuration
    private static final int DEFAULT_TTL = 10; // Max hops

    public BrokerAgent(String name, NetworkInterface net) {
        super(name, HEPS.getInstance()); 
        this.network = net;
        
        // Use Pareto strategy for selection
        this.selectionStrategy = new ParetoSelectionStrategy();
        
        // Initialize Token Key
        byte[] keyBytes = new byte[16]; 
        new java.security.SecureRandom().nextBytes(keyBytes);
        this.tokenKey = new SecretKeySpec(keyBytes, "AES");
        
        this.network.registerAgent(name, this);
    }

    public void setParent(String parentAddress) {
        this.parentBrokerAddress = parentAddress;
    }

    @Override
    public void onMessage(MarketplaceMessage message) {
        if (message instanceof RegisterMessage) {
            handleLocalRegistration((RegisterMessage) message);
        } else if (message instanceof PropagateSubscriptionMessage) {
            handleRemoteRegistration((PropagateSubscriptionMessage) message);
        } else if (message instanceof RequestMessage) {
            handleRequest((RequestMessage) message);
        } else if (message instanceof FastExecutionMessage) {
            handleFastExecution((FastExecutionMessage) message);
        }
    }

    // --- 1. LOCAL REGISTRATION (Entry Point) ---

    private void handleLocalRegistration(RegisterMessage msg) {
        System.out.println("[Broker " + name + "] Onboarding Local Provider: " + msg.getSenderId());
        
        // Enrich Subscription
        Subscription s = msg.getSubscription();
        if (!s.getSubscribers().contains(msg.getSenderId())) {
            s.addSubscriber(msg.getSenderId());
        }

        // Add to Local Storage
        super.addSubscription(s);
        providerCapabilities.put(msg.getSenderId(), msg.getCapabilities());

        // Determine Scope and Propagate Upstream
        PropagateSubscriptionMessage.Scope scope = determineScope(msg.getCapabilities());
        
        if (scope != PropagateSubscriptionMessage.Scope.LOCAL && parentBrokerAddress != null) {
            System.out.println("[Broker " + name + "] Propagating Upstream to Parent: " + parentBrokerAddress);
            
            PropagateSubscriptionMessage prop = new PropagateSubscriptionMessage(
                this.name, msg.getSenderId(), s, msg.getCapabilities(), 
                DEFAULT_TTL, scope
            );
            
            seenMessages.add(prop.getMessageId()); // Don't process my own message if it loops back
            network.send(parentBrokerAddress, prop);
        }
    }

    // --- 2. REMOTE PROPAGATION (Router Logic) ---

    private void handleRemoteRegistration(PropagateSubscriptionMessage msg) {
        // A. Loop Detection
        if (seenMessages.contains(msg.getMessageId())) {
            return; // Duplicate message, ignore
        }
        seenMessages.add(msg.getMessageId());

        System.out.println("[Broker " + name + "] Received Upstream Propagation from " + msg.getSenderId());

        // B. Add to Local Storage (Federated View)
        Subscription s = msg.getSubscription();
        String originalProvider = msg.getOriginalProviderId();
        
        if (!s.getSubscribers().contains(originalProvider)) {
            s.addSubscriber(originalProvider);
        }
        super.addSubscription(s);
        providerCapabilities.put(originalProvider, msg.getCapabilities());

        // C. Forwarding Logic
        if (msg.getTtl() <= 0) {
            System.out.println("[Broker " + name + "] TTL Expired for message " + msg.getMessageId());
            return;
        }

        if (parentBrokerAddress == null) {
            System.out.println("[Broker " + name + "] I am ROOT. Propagation ends here.");
            return;
        }

        if (msg.getScope() == PropagateSubscriptionMessage.Scope.REGIONAL) {
            // Assume "Regional" stops at the first aggregator (Core). 
            // In a real system, you might check "Am I a Region Core?" logic here.
            System.out.println("[Broker " + name + "] Regional Scope Limit Reached.");
            return;
        }

        // Forward Upwards
        System.out.println("[Broker " + name + "] Forwarding to Parent: " + parentBrokerAddress);
        PropagateSubscriptionMessage forwardMsg = new PropagateSubscriptionMessage(this.name, msg);
        network.send(parentBrokerAddress, forwardMsg);
    }

    // --- 3. REQUEST HANDLING (Routing) ---

    private void handleRequest(RequestMessage msg) {
        System.out.println("---------------------------------------------------------------");
        System.out.println("[Broker " + name + "] Processing Request from " + msg.getSenderId());
        
        Subscription match = super.matchPublication(msg.getPublication());
        
        if (match == null) {
            System.out.println("[Broker " + name + "] [Fail] No matching services found.");
            // In a hierarchical system, if not found locally, we might query DOWN or UP.
            // For this design, we assume the Root has visibility of everything propagated to it.
            return;
        }

        List<ServiceEntry> candidates = new ArrayList<>();
        for (String pid : match.getSubscribers()) {
            Map<String, Object> caps = providerCapabilities.get(pid);
            if (caps != null) {
                candidates.add(new ServiceEntry(pid, match, caps));
            }
        }

        String bestProvider = selectionStrategy.selectBestProvider(candidates, msg.getRequirements());
        
        if (bestProvider != null) {
            System.out.println("[Broker " + name + "] [Selected] Routing to: " + bestProvider);
            network.send(bestProvider, new ExecutionMessage(this.name, msg.getSenderId(), msg.getPublication()));
            
            try {
                byte[] token = generateToken(bestProvider);
                network.send(msg.getSenderId(), new TokenIssueMessage(this.name, "SERVICE-TOKEN", token));
            } catch (Exception e) { e.printStackTrace(); }

        } else {
            System.out.println("[Broker " + name + "] [Fail] Candidates exist but failed policy check.");
        }
        System.out.println("---------------------------------------------------------------");
    }

    private void handleFastExecution(FastExecutionMessage msg) {
        try {
            String providerId = decryptToken(msg.getRoutingToken());
            System.out.println("[Broker " + name + "] [Fast Path] Forwarding to " + providerId);
            network.send(providerId, new ExecutionMessage(this.name, msg.getSenderId(), msg.getPayload()));
        } catch (Exception e) {
            System.err.println("[Broker] Invalid Token.");
        }
    }

    // --- HELPERS ---

    private PropagateSubscriptionMessage.Scope determineScope(Map<String, Object> caps) {
        String s = (String) caps.getOrDefault("scope", "GLOBAL");
        try {
            return PropagateSubscriptionMessage.Scope.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PropagateSubscriptionMessage.Scope.GLOBAL;
        }
    }

    private byte[] generateToken(String providerId) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, tokenKey);
        return cipher.doFinal(providerId.getBytes());
    }
    
    private String decryptToken(byte[] token) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, tokenKey);
        return new String(cipher.doFinal(token));
    }

    @Override public void processSubscription(Subscription s) { addSubscription(s); }
    @Override public void processPublication(Publication p) { matchPublication(p); }
}