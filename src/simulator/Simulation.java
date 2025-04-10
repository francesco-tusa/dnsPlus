package simulator;

public class Simulation {

    public static void main(String[] args) {

        BrokerWithRegion root = new BrokerWithRegionProcessingRegion("root");

        BrokerWithRegion child1 = new BrokerWithRegionProcessingRegion("child1");
        BrokerWithRegion child2 = new BrokerWithRegionProcessingRegion("child2");
        BrokerWithRegion child3 = new BrokerWithRegionProcessingRegion("child3");

        // leaf brokers
        BrokerWithRegion grandchild1 = new LeafBrokerWithRegionProcessingRegion("grandchild1");
        BrokerWithRegion grandchild2 = new LeafBrokerWithRegionProcessingRegion("grandchild2");
        BrokerWithRegion grandchild3 = new LeafBrokerWithRegionProcessingRegion("grandchild3");
        BrokerWithRegion grandchild4 = new LeafBrokerWithRegionProcessingRegion("grandchild4");

        Subscriber s1 = new Subscriber("sub1", new Location(0, 0, 0));
        Subscriber s2 = new Subscriber("sub2", new Location(5, 2, 0));
        Subscriber s3 = new Subscriber("sub3", new Location(10, 1, 0));
        Subscriber s4 = new Subscriber("sub4", new Location(15, 1, 0));

        Subscriber s5 = new Subscriber("sub5", new Location(4, 3, 0));
        Subscriber s6 = new Subscriber("sub6", new Location(9, 5, 0));
        Subscriber s7 = new Subscriber("sub7", new Location(13, 3, 0));
        Subscriber s8 = new Subscriber("sub8", new Location(20, 5, 0));

        Publisher p1 = new Publisher("pub1", new Location(2, 2, 0));

        root.addChild(child1);
        root.addChild(child2);
        root.addChild(child3);

        child1.addChild(grandchild1);
        child2.addChild(grandchild2);
        child2.addChild(grandchild3);
        child3.addChild(grandchild4);

        /* 
        System.out.println(child1.getRegion());
        System.out.println(child2.getRegion());
        System.out.println(child3.getRegion());
        */

        // subscribing to leaf brokers
        grandchild1.addChild(s1);
        grandchild2.addChild(s2);
        grandchild3.addChild(s3);
        grandchild4.addChild(s4);

        /*
        System.out.println();
        System.out.println(grandchild1.getRegion());
        System.out.println(grandchild2.getRegion());
        System.out.println(grandchild3.getRegion());
        System.out.println(grandchild4.getRegion());
        */
        
        grandchild1.addChild(s5);
        grandchild2.addChild(s6);
        grandchild3.addChild(s7);
        grandchild4.addChild(s8);
        
        /*
        System.out.println();
        System.out.println(grandchild1.getRegion());
        System.out.println(grandchild2.getRegion());
        System.out.println(grandchild3.getRegion());
        System.out.println(grandchild4.getRegion());

        System.out.println();
        System.out.println(child1.getRegion());
        System.out.println(child2.getRegion());
        System.out.println(child3.getRegion());

        System.out.println();
        System.out.println(root.getRegion());
        
        System.out.println();
        Subscriber subTest = new Subscriber("subTest", new Location(2, 2, 0));
        grandchild1.addChild(subTest);
        */

        System.out.println();
        System.out.println(grandchild1.getRegion());
        System.out.println(grandchild2.getRegion());
        System.out.println(grandchild3.getRegion());
        System.out.println(grandchild4.getRegion());

        System.out.println();
        System.out.println(child1.getRegion());
        System.out.println(child2.getRegion());
        System.out.println(child3.getRegion());

        System.out.println();
        System.out.println(root.getRegion());
        System.out.println();

        
        Region subscription1Region = new Region(new Location(5, 2,0), new Location(8, 5, 0));
        Region subscription2Region = new Region(new Location(6, 3,0), new Location(7, 4, 0));
        Region subscription3Region = new Region(new Location(4, 2,0), new Location(7, 4, 0));

        grandchild2.printSubscriptionsTable();
        child2.printSubscriptionsTable();
        root.printSubscriptionsTable();

        SubscriptionWithRegion subscription1 = new SubscriptionWithRegion(subscription1Region);
        s2.send(subscription1);

        grandchild2.printSubscriptionsTable();
        child2.printSubscriptionsTable();
        root.printSubscriptionsTable();
        
        SubscriptionWithRegion subscription2 = new SubscriptionWithRegion(subscription2Region);
        s2.send(subscription2);

        grandchild2.printSubscriptionsTable();
        child2.printSubscriptionsTable();
        root.printSubscriptionsTable();

        SubscriptionWithRegion subscription3 = new SubscriptionWithRegion(subscription3Region);
        s2.send(subscription3);

        root.printSubscriptionsTable();
        child1.printSubscriptionsTable();
        child2.printSubscriptionsTable();
        child3.printSubscriptionsTable();
        grandchild1.printSubscriptionsTable();
        grandchild2.printSubscriptionsTable();
        grandchild3.printSubscriptionsTable();
        grandchild4.printSubscriptionsTable();
        
        /* 

        // TODO: check whether adding a publisher changes the region
        grandchild1.addChild(p1);

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

        SubscriptionWithLocation subscription6 = new SubscriptionWithLocation(new Location(1, 1, 1));
        s5.send(subscription6);

        root.printSubscriptionsTable();
        child1.printSubscriptionsTable();
        child2.printSubscriptionsTable();
        child3.printSubscriptionsTable();
        grandchild1.printSubscriptionsTable();
        grandchild2.printSubscriptionsTable();
        grandchild3.printSubscriptionsTable();
        grandchild4.printSubscriptionsTable();

        PublicationWithLocation publication1 = new PublicationWithLocation(new Location(20, 20, 20));
        p1.send(publication1);

        PublicationWithLocation publication2 = new PublicationWithLocation(new Location(2, 2, 2));
        p1.send(publication2);
        */
        
    }

}
