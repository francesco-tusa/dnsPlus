package marketplace.network;

import marketplace.protocol.MarketplaceMessage;

public interface NetworkInterface {
    void registerAgent(String address, MessageReceiver agent);
    void send(String targetAddress, MarketplaceMessage message);
    
    interface MessageReceiver {
        void onMessage(MarketplaceMessage message);
    }
}