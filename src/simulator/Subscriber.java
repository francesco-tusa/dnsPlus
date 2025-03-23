package simulator;

public class Subscriber extends TreeNode {
    
    private String name;
    
    public Subscriber(String name) {
        this.name = name;
    }

    public SimulationBroker getBroker() {
        return (SimulationBroker) getParent();
    }
    
    public void send(SubscriptionWithLocation s) {
        SimulationBroker broker = getBroker();
        s.setSource(this);
        if (broker != null) {
            System.out.println(name + " is sending the subscription to " + broker.getName());
            broker.processSubscription(s);
        } else {
            System.out.println(name + " has no broker to send the subscription to");
        }
    }
    
}
