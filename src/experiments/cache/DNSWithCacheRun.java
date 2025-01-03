//package experiments.cache;
//
//import encryption.HEPS;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.util.Scanner;
//import broker.tree.binarybalanced.cache.BrokerWithBinaryBalancedTreeAndCache;
//import experiments.RunTasksExecutor;
//import experiments.Task;
//import experiments.inputdata.DBFactory;
//import experiments.inputdata.DomainsDB;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.logging.Logger;
//import java.util.logging.Level;
//import publishing.Publication;
//import publishing.Publisher;
//import subscribing.Subscriber;
//import utils.CustomLogger;
//
///**
// *
// * @author f.tusa
// */
//public final class DNSWithCacheRun implements RunTasksExecutor
//{
//        
//    private static final Logger logger = CustomLogger.getLogger(DNSWithCacheRun.class.getName());
//    private Subscriber subscriber;
//    private Publisher publisher;
//    private BrokerWithBinaryBalancedTreeAndCache broker;
//    private DomainsDB domainsDB;
//    
//    private List<Task> tasks;
//    private String name;
//
//    private int numberOfPublications;
//    private int numberOfSubscriptions;
//    
//    
//    public DNSWithCacheRun(DomainsDB db, int numberOfPublications, int numberOfSubscriptions) {
//        this(db, null, numberOfPublications, numberOfSubscriptions);
//    }
//
//    public DNSWithCacheRun(String fileName, int numberOfPublications, int numberOfSubscriptions) {
//        this(null, fileName, numberOfPublications, numberOfSubscriptions);
//    }
//
//    private DNSWithCacheRun(DomainsDB db, String fileName, int nPublications, int nSubscriptions) {
//        broker = new BrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
//        
//        subscriber = new Subscriber("Subscriber1");
//        publisher = new Publisher("Publisher1");
//        domainsDB = (db != null) ? db : DBFactory.getDomainsDB(fileName);
//        tasks = new ArrayList<>();
//        name = this.getClass().getSimpleName();
//        numberOfPublications = nPublications;
//        numberOfSubscriptions = nSubscriptions;
//    }
//        
//       
//    @Override
//    public void setUp() {
//        broker.startProcessing();
//        publisher.init();
//        subscriber.init();
//    }
//    
//    
//     @Override
//    public void addTask(Task task) {
//        tasks.add(task);
//    }
//
//    @Override
//    public void start() {
//        for (Task task : tasks) {
//            Thread t = new Thread(task, task.getName());
//            t.start();
//        }       
//    }
//
//    @Override
//    public void cleanUp() {
//        logger.log(Level.INFO, "Cleaning Up Run");
//        //publisher.publish(new PoisonPillPublication());
//        //subscriber.subscribe(new PoisonPillSubscription());
//    }
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    
//    public DNSWithCacheRun(String file)
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
//    
//    public void simpleTest() {
//        System.out.println("Publication: fonts");
//        broker.matchPublication(publisher.generatePublication("fonts.googleapis.com"));
//     
//        System.out.println("Publication: facebook");
//        broker.matchPublication(publisher.generatePublication("facebook.com"));
//        
//        System.out.println("Publication: facebook");
//        broker.matchPublication(publisher.generatePublication("facebook.com"));
//        
//        System.out.println("Subscription facebook");
//        broker.cacheLookUp(subscriber.generateSubscription("facebook.com"));
//        
//        System.out.println("Subscription facebook");
//        broker.cacheLookUp(subscriber.generateSubscription("facebook.com")); 
//        
//        System.out.println("Publication: facebook");
//        broker.matchPublication(publisher.generatePublication("facebook.com"));
//    }
//    
//    public void generatePublications() throws FileNotFoundException 
//    {
//        long t;
//        int i = 0;
//        
//        try (Scanner scanner = new Scanner(serviceNames)) {
//            while (scanner.hasNextLine())
//            {
//                String service = scanner.nextLine();
//                services[i++] = service;
//                System.out.println("Publication: " + service);
//                Publication p = publisher.generatePublication(service);
//                t = System.nanoTime();
//                broker.matchPublication(p);
//                cachePublications += (System.nanoTime() - t);
//            }
//        }
//    }
//    
////    public boolean generateSubscriptions(String service, Publication p) 
////    {
////        double timing = matchTimings.get(service);
////        
////        long t = System.nanoTime();
////        boolean matchResult = broker.cacheLookUp(p);
////        timing += (System.nanoTime() - t);
////        
////        matchTimings.put(service, timing);
////        return matchResult;
////    }
////    
////    
////    public static void main(String[] args) 
////    {    
////        int iterations = 1;
////        double subscriptions = 0;
////             
////        long t;
////        
////        String home = System.getProperty("user.home");
////        DNSWithCacheRun dnsPlus = new DNSWithCacheRun(home + "/websites.txt");
////
////        try {
////            System.out.println("*** Generating subscriptions table ***");
////            t = System.nanoTime();
////            dnsPlus.generateSubscriptions(); 
////            subscriptions = System.nanoTime() - t;
////            
////        } catch (FileNotFoundException ex) {
////            System.out.println("Subscription could not be loaded: " + ex.getMessage());
////            System.exit(-1);
////        }
////        
////        System.out.println();
////        System.out.println("*** Matching publications ***");
////
////        for (String service : dnsPlus.matchTimings.keySet())
////        {
////            Publication publication = dnsPlus.publisher.generatePublication(service);
////
////            for (int i=0; i<iterations; i++) 
////            {
////                System.out.println(service + ": " + dnsPlus.match(service, publication));
////            }
////        }
////        
////        System.out.println();
////        System.out.println("Total Subscriptions table generation (ms) [includes subscriptions generation]: " + subscriptions / 1000000);
////        System.out.println("Subscriptions table generation (ms) [actual time to add subscriptions to the table]: " + dnsPlus.cachePublications / 1000000);
////        System.out.println();
////        double timingSum = 0;
////        for (String service : dnsPlus.matchTimings.keySet()) 
////        {
////            double timing = (dnsPlus.matchTimings.get(service) / 1000000) / iterations;
////            System.out.println("Match [" + service + "] (ms): " + String.format("%.2f", timing));
////            timingSum += timing;
////        }
////        
////        System.out.println();
////        System.out.println("Average match time (ms): " + timingSum / dnsPlus.matchTimings.size());
////    } 
//    
//    
//    public static void main(String[] args) 
//    {
//        String home = System.getProperty("user.home");
//        DNSWithCacheRun dnsPlus = new DNSWithCacheRun(home + "/websites.txt");
//        
//        dnsPlus.simpleTest();
//        
//    }
//}