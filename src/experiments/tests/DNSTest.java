//package experiments.tests;
//
//import encryption.HEPS;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Random;
//import java.util.Scanner;
//import encryption.BlindedMatchingBroker;
//import broker.tree.binarybalanced.BrokerWithBinaryBalancedTree;
//import publishing.Publication;
//import encryption.BlindingPublisher;
//import encryption.BlindingSubscriber;
//import subscribing.Subscription;
//
///**
// *
// * @author f.tusa
// */
//public final class DNSTest 
//{
//    // may use a list for each type of entity
//    HEPS heps = HEPS.getInstance();
//        
//    BlindingSubscriber subscriber = new BlindingSubscriber("Subscriber1");
//    BlindingPublisher publisher = new BlindingPublisher("Publisher1");
//    BlindedMatchingBroker broker = new BrokerWithBinaryBalancedTree("Broker1", heps);
//    //AbstractBroker broker = new BrokerWithBinaryTree("Broker1", heps);
//    
//    File serviceNames;
//    String[] services;
//    
//    double addSubscriptions = 0;
//    Map<String, Double> matchTimings = new HashMap<>();
//        
//       
//    
//    public DNSTest(String file)
//    {
//        serviceNames = new File(file);
//        services = new String[1000];
//        
//        subscriber.setHeps(heps);
//        publisher.setHeps(heps);
//        
//        subscriber.getSecurityParameters();
//        publisher.getSecurityParameters();
//    }
//    
//    private void setExperimentParameters() 
//    {
//        matchTimings.put("google.com", 0d);
//        matchTimings.put("doesnotexist.co.uk", 0d);
//        matchTimings.put("allmusic.com", 0d);
//        matchTimings.put("ticketmaster.com", 0d);
//        matchTimings.put("eventbrite.com", 0d);
//        matchTimings.put("facebook.com", 0d);
//        matchTimings.put("myfakedomain.com", 0d);
//    }
//    
//    private void setRandomExperimentParameters() 
//    {
//        Random n = new Random();
//        int randomIndex;
//        
//        for (int i = 0; i < 900; i += 99) {
//            randomIndex = n.nextInt(i, i+99);
//            System.out.println("Index: " + randomIndex + " service: " + services[randomIndex]);
//            matchTimings.put(services[randomIndex], 0d);
//        }
//        
//        matchTimings.put("doesnotexist", 0d);
//    }
//    
//    public void generateSubscriptions() throws FileNotFoundException 
//    {
//        long t;
//        int i = 0;
//        
//        try (Scanner scanner = new Scanner(serviceNames)) {
//            while (scanner.hasNextLine())
//            {
//                String service = scanner.nextLine();
//                services[i++] = service;
//                Subscription s = subscriber.generateSubscription(service);
//                t = System.nanoTime();
//                broker.addSubscription(s);
//                addSubscriptions += (System.nanoTime() - t);
//            }
//        }
//        
//        //setRandomExperimentParameters();
//        setExperimentParameters();
//    }
//    
//    public void match(String service, Publication p) 
//    {
//        double timing = matchTimings.get(service);
//        
//        long t = System.nanoTime();
//        Subscription matchResult = broker.matchPublication(p); // not used
//        timing += (System.nanoTime() - t);
//        
//        matchTimings.put(service, timing);
//    }
//    
//    
//    public static void main(String[] args) 
//    {    
//        int iterations = 1;
//        double subscriptions = 0;
//             
//        long t;
//        
//        String home = System.getProperty("user.home");
//        DNSTest dnsPlus = new DNSTest(home + "/websites.txt");
//
//        try {
//            System.out.println("*** Generating subscriptions table ***");
//            t = System.nanoTime();
//            dnsPlus.generateSubscriptions(); 
//            subscriptions = System.nanoTime() - t;
//            
//        } catch (FileNotFoundException ex) {
//            System.out.println("Subscription could not be loaded: " + ex.getMessage());
//            System.exit(-1);
//        }
//        
//        System.out.println();
//        System.out.println("*** Matching publications ***");
//
//        for (String service : dnsPlus.matchTimings.keySet())
//        {
//            Publication publication = dnsPlus.publisher.generatePublication(service);
//
//            for (int i=0; i<iterations; i++) 
//            {
//                dnsPlus.match(service, publication);
//            }
//        }
//        
//        System.out.println();
//        System.out.println("Total Subscriptions table generation (ms) [includes subscriptions generation]: " + subscriptions / 1000000);
//        System.out.println("Subscriptions table generation (ms) [actual time to add subscriptions to the table]: " + dnsPlus.addSubscriptions / 1000000);
//        System.out.println();
//        double timingSum = 0;
//        for (String service : dnsPlus.matchTimings.keySet()) 
//        {
//            double timing = (dnsPlus.matchTimings.get(service) / 1000000) / iterations;
//            System.out.println("Match [" + service + "] (ms): " + String.format("%.2f", timing));
//            timingSum += timing;
//        }
//        
//        System.out.println();
//        System.out.println("Average match time (ms): " + timingSum / dnsPlus.matchTimings.size());
//    } 
//}