package simulator;

public class Simulation {

    public static void main(String[] args) {
        BrokerWithRegion root = new BrokerWithRegion("root");
        BrokerWithRegion child1 = new BrokerWithRegion("child1");
        BrokerWithRegion child2 = new BrokerWithRegion("child2");

        Subscriber s1 = new Subscriber("sub1");
        Subscriber s2 = new Subscriber("sub2");
        Subscriber s3 = new Subscriber("sub3");
        Subscriber s4 = new Subscriber("sub4");

        root.addChild(child1);
        root.addChild(child2);

        child2.addChild(s1);
        child2.addChild(s2);
        child1.addChild(s3);
        child1.addChild(s4);

        SubscriptionWithLocation subscription1 = new SubscriptionWithLocation(new Location(5, 5, 5));
        s1.send(subscription1);
        
        SubscriptionWithLocation subscription2 = new SubscriptionWithLocation(new Location(5, 5, 5));
        s2.send(subscription2);
        
        SubscriptionWithLocation subscription3 = new SubscriptionWithLocation(new Location(10, 5, 5));
        s3.send(subscription3);
        
        SubscriptionWithLocation subscription4 = new SubscriptionWithLocation(new Location(15, 5, 5));
        s4.send(subscription4);

        SubscriptionWithLocation subscription5 = new SubscriptionWithLocation(new Location(3, 3, 3));
        s4.send(subscription5);
    }

}
