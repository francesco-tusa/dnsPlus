package marketplace.protocol;

public class FastExecutionMessage extends MarketplaceMessage {
    private final byte[] routingToken; // Opaque encrypted blob
    private final Object payload;      // Actual input data

    public FastExecutionMessage(String senderId, byte[] token, Object payload) {
        super(senderId);
        this.routingToken = token;
        this.payload = payload;
    }
    public byte[] getRoutingToken() { return routingToken; }
    public Object getPayload() { return payload; }
}