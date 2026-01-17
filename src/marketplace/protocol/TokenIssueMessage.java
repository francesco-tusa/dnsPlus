package marketplace.protocol;

public class TokenIssueMessage extends MarketplaceMessage {
    private final String serviceName; // Plaintext ID for client cache key (client knows what they asked for)
    private final byte[] routingToken;

    public TokenIssueMessage(String senderId, String serviceName, byte[] token) {
        super(senderId);
        this.serviceName = serviceName;
        this.routingToken = token;
    }
    public String getServiceName() { return serviceName; }
    public byte[] getRoutingToken() { return routingToken; }
}