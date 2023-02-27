/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
        
        Publication p1 = pub1.generatePublication("www.google.com");
        b1.equalityMatch(p1, s1);   
        
        Publication p2 = pub1.generatePublication("www.google.con");
        b1.equalityMatch(p2, s1);   
        
        Publication p3 = pub1.generatePublication("www.google.col");
        b1.equalityMatch(p3, s1);   
    }
    
}
