package marketplace.network;

import marketplace.protocol.MarketplaceMessage;
import java.util.HashMap;
import java.util.Map;

public class InMemoryNetwork implements NetworkInterface {
    private final Map<String, MessageReceiver> routes = new HashMap<>();

    @Override
    public void registerAgent(String address, MessageReceiver agent) {
        routes.put(address, agent);
    }

    @Override
    public void send(String targetAddress, MarketplaceMessage message) {
        if (routes.containsKey(targetAddress)) {
            // In a real network, verify serialization here
            routes.get(targetAddress).onMessage(message);
        } else {
            System.err.println("Network Error: Unreachable destination " + targetAddress);
        }
    }
}