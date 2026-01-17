package marketplace.protocol;

import java.io.Serializable;

public abstract class MarketplaceMessage implements Serializable {
    private final String senderId;
    public MarketplaceMessage(String senderId) { this.senderId = senderId; }
    public String getSenderId() { return senderId; }
}