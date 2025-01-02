package experiments.tests;

import encryption.HEPS;
import broker.AbstractBroker;
import broker.tree.binary.BrokerWithBinaryTree;
import publishing.Publication;
import publishing.Publisher;
import subscribing.Subscriber;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class SimpleTest {
    public static void main(String[] args) {
        HEPS heps = HEPS.getInstance();

        Subscriber subscriber = new Subscriber("Subscriber1");
        Publisher publisher = new Publisher("Publisher1");
        AbstractBroker broker = new BrokerWithBinaryTree("Broker1", heps);

        subscriber.setHeps(heps);
        publisher.setHeps(heps);

        subscriber.getSecurityParameters();
        publisher.getSecurityParameters();

//        for (int i = 0; i < 1; i++) {
//            java.util.Random rand = new java.util.Random();
//            BigInteger bi1 = new BigInteger(1024, rand);
//            BigInteger bi2 = new BigInteger(1024, rand);
//
//            Publication p = publisher.generatePublication(bi1);
//            Subscription s = subscriber.generateSubscription(bi1);
//
//            Integer match = broker.match(p, s);
//
//            System.out.println("match?" + match);
//        }

        String name1 = "twitter.com";
        String name2 = "fonts.it";
        String name3 = "fonts.org";

        Subscription s1 = subscriber.generateSubscription(name1);
        Subscription s2 = subscriber.generateSubscription(name2);
        Subscription s3 = subscriber.generateSubscription(name3);

        Publication p1 = publisher.generatePublication(name2);

//        int name1 = 1000;
//        int name2 = 20;
//        int name3 = 15;
//        
//        Subscription s1 = subscriber.generateSubscription(BigInteger.valueOf(name1));
//        Subscription s2 = subscriber.generateSubscription(BigInteger.valueOf(name2));
//        Subscription s3 = subscriber.generateSubscription(BigInteger.valueOf(name3));
//        Publication p1 = publisher.generatePublication(BigInteger.valueOf(name2));

        System.out.println(name2 + " " + name1 + " : " + broker.match(s2, s1));
        System.out.println(name2 + " " + name3 + " : " + broker.match(s2, s3));
        System.out.println(name3 + " " + name3 + " : " + broker.match(s3, s3));
        System.out.println(name1 + " " + name3 + " : " + broker.match(s1, s3));
        System.out.println(name3 + " " + name2 + " : " + broker.match(s3, s2));

        System.out.println(name2 + " matching " + name1 + " : " + broker.match(p1, s1));
        System.out.println(name2 + " matching " + name2 + " : " + broker.match(p1, s2));
        System.out.println(name2 + " matching " + name3 + " : " + broker.match(p1, s3));
        
        System.out.println("*** Publication Match ***");
        Publication p2 = publisher.generatePublication(name1);
        
        Publication p3 = publisher.generatePublication(name2);
        
        System.out.println(name2 + " " + name1 + " : " + broker.match(p1, p2));
        System.out.println(name1 + " " + name2 + " : " + broker.match(p2, p1));
        
        System.out.println(name2 + " " + name2 + " : " + broker.match(p1, p3));
        System.out.println(name2 + " " + name2 + " : " + broker.match(p3, p1));
        
    }
}
