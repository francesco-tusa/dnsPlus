package naming;

/**
 *
 * @author f.tusa
 */
public class DNSPlus {
    
    public static void main(String[] args) {
        HEPS heps = new HEPS(2048, 2048/8);
        
        Subscriber sub1 = new Subscriber("Subscriber1");
        Publisher pub1 = new Publisher("Publisher1");
        Broker b1 = new Broker(heps);
        
        sub1.setHeps(heps);
        pub1.setHeps(heps);
        
        sub1.getSecurityParameters();
        pub1.getSecurityParameters();
        
        Subscription s1 = sub1.generateSubscription("www.google.com");
        
        // testing match
        Publication p1 = pub1.generatePublication("www.google.com");
        System.out.println(b1.equalityMatch(p1, s1));   
        
        Publication p2 = pub1.generatePublication("www.google.con");
        System.out.println(b1.equalityMatch(p2, s1)); 
        
        Publication p3 = pub1.generatePublication("www.google.col");
        System.out.println(b1.equalityMatch(p3, s1));   
        
        Publication p4 = pub1.generatePublication("www.google.cog");
        System.out.println(b1.equalityMatch(p4, s1));   
        
        
        // testing cover
        Subscription s2 = sub1.generateSubscription("www.google.com");
        Subscription s3 = sub1.generateSubscription("www.google.col");
        Subscription s4 = sub1.generateSubscription("www.google.con");
        Subscription s5 = sub1.generateSubscription("www.google.cog");
        
        
        b1.addSubscription(s1);
        b1.addSubscription(s2);
        b1.addSubscription(s3);
        b1.addSubscription(s4);
        b1.addSubscription(s5);
        
        b1.matchPublication(p4);
        
        
        System.out.println(b1.cover(s1, s2));
        System.out.println(b1.cover(s1, s3));
        System.out.println(b1.cover(s1, s4));
    } 
}