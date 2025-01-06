package experiments.cache;

import encryption.HEPS;
import broker.tree.binarybalanced.cache.BrokerWithBinaryBalancedTreeAndCache;
import experiments.inputdata.DBFactory;
import experiments.inputdata.DomainsDB;
import java.util.logging.Logger;
import utils.CustomLogger;
import broker.CachingBroker;
import experiments.RunSequentialTasksExecutor;
import experiments.SynchronousTask;
import java.util.List;
import java.util.logging.Level;
import publishing.Publication;
import publishing.Publisher;
import subscribing.Subscriber;
import utils.ExecutionTimeLogger;

/**
 *
 * @author f.tusa
 */
public final class DNSWithCacheRun extends RunSequentialTasksExecutor {
        
    private static final Logger logger = CustomLogger.getLogger(DNSWithCacheRun.class.getName());
    //private Subscriber subscriber;
    //private Publisher publisher;
    private CachingBroker broker;
    private DomainsDB domainsDB;

    private int numberOfPublications;
    private int numberOfSubscriptions;
    
    
    public DNSWithCacheRun(DomainsDB db, int numberOfPublications, int numberOfSubscriptions) {
        this(db, null, numberOfPublications, numberOfSubscriptions);
    }

    public DNSWithCacheRun(String fileName, int numberOfPublications, int numberOfSubscriptions) {
        this(null, fileName, numberOfPublications, numberOfSubscriptions);
    }

    private DNSWithCacheRun(DomainsDB db, String fileName, int nPublications, int nSubscriptions) {
        super(DNSWithCacheRun.class.getSimpleName());
        broker = new BrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
        domainsDB = (db != null) ? db : DBFactory.getDomainsDB(fileName);
        numberOfPublications = nPublications;
        numberOfSubscriptions = nSubscriptions;
    }
        
       
    @Override
    public void setUp() {
        //publisher.init();
        //subscriber.init();
    }
    
    
    @Override
    public void cleanUp() {
        logger.log(Level.INFO, "Cleaning Up Run");
        //publisher.publish(new PoisonPillPublication());
        //subscriber.subscribe(new PoisonPillSubscription());
    }
    
    
    
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
    
//    public boolean generateSubscriptions(String service, Publication p) 
//    {
//        double timing = matchTimings.get(service);
//        
//        long t = System.nanoTime();
//        boolean matchResult = broker.cacheLookUp(p);
//        timing += (System.nanoTime() - t);
//        
//        matchTimings.put(service, timing);
//        return matchResult;
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
//        DNSWithCacheRun dnsPlus = new DNSWithCacheRun(home + "/websites.txt");
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
//                System.out.println(service + ": " + dnsPlus.match(service, publication));
//            }
//        }
//        
//        System.out.println();
//        System.out.println("Total Subscriptions table generation (ms) [includes subscriptions generation]: " + subscriptions / 1000000);
//        System.out.println("Subscriptions table generation (ms) [actual time to add subscriptions to the table]: " + dnsPlus.cachePublications / 1000000);
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
    
    
    
    final class SubscriberTask extends SynchronousTask {
        
        private static final Logger logger = CustomLogger.getLogger(SubscriberTask.class.getName());
        
        private Subscriber subscriber;
        
        public SubscriberTask(String subscriberName) {
            subscriber = new Subscriber(subscriberName, broker);
            subscriber.init();
            setName(this.getClass().getSimpleName());
        }
        
        @Override
        public void run() {
            List<String> randomEntries = domainsDB.getRandomEntries(numberOfSubscriptions);
            ExecutionTimeLogger.ExecutionResult<Void> result = ExecutionTimeLogger.measureExecutionTime(()
                    -> {
                subscriber.generateAndSubscribeToAll(randomEntries);
                return null;
            });
            setDuration(result.getDuration());
            logger.log(Level.INFO, "Subscriber {0} completed their request task", subscriber.getName());
            //subscriber.subscribe(new PoisonPillSubscription());
        }
    }
    
    
    final class PublisherTask extends SynchronousTask {
        
        private static final Logger logger = CustomLogger.getLogger(PublisherTask.class.getName());
        
        private Publisher publisher;
        
        public PublisherTask(String publisherName) {
            publisher = new Publisher(publisherName, broker);
            publisher.init();
            setName(this.getClass().getSimpleName());
        }
        
        
        @Override
        public void run() {
            List<String> randomEntries = domainsDB.getRandomEntries(numberOfPublications);
            List<Publication> generatedPublications = publisher.generatePublications(randomEntries);
            
            //TODO: if multiple measurements are requested then they should be added to a collection
            // and average should be calculated over the runs for all of them
            ExecutionTimeLogger.ExecutionResult<Void> result = ExecutionTimeLogger.measureExecutionTime(()
                    -> {
                publisher.publishAll(generatedPublications);
                return null;
            });
            setDuration(result.getDuration());
            logger.log(Level.INFO, "Publisher task request completed");
            //publisher.publish(new PoisonPillPublication());
        }
    }


}