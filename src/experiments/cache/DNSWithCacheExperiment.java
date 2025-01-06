package experiments.cache;

import experiments.RunTasksOutputManager;
import experiments.SynchronousExperiment;
import experiments.cache.DNSWithCacheRun.PublisherTask;
import experiments.cache.DNSWithCacheRun.SubscriberTask;
import java.util.List;

/**
 *
 * @author uceeftu
 */
public final class DNSWithCacheExperiment extends SynchronousExperiment {

    private int numberOfPublications;
    private int numberOfSubscriptions;

    public DNSWithCacheExperiment(String inputFileName, int numberOfRuns) {
        this(inputFileName, numberOfRuns, 100, 10);
    }

    public DNSWithCacheExperiment(String inputFileName, int numberOfRuns, int numberOfPublications, int numberOfSubscriptions) {
        super(DNSWithCacheExperiment.class.getSimpleName(), inputFileName, numberOfRuns);
        this.numberOfPublications = numberOfPublications;
        this.numberOfSubscriptions = numberOfSubscriptions;
    }

    @Override
    protected void executeRun() {
        DNSWithCacheRun experimentRun = new DNSWithCacheRun(getDomainsDB(), numberOfPublications, numberOfSubscriptions);
        experimentRun.setUp();
        
        DNSWithCacheRun.SubscriberTask subscriberTask = experimentRun.new SubscriberTask("Sub1");
        DNSWithCacheRun.PublisherTask publisherTask = experimentRun.new PublisherTask("Pub");
        
        experimentRun.addTask(subscriberTask);
        experimentRun.addTask(publisherTask);
        
        experimentRun.start();
       
        experimentRun.cleanUp();
        
        List<RunTasksOutputManager> allRunsTasksOutput = getAllRunsTasksOutput();
        allRunsTasksOutput.add(experimentRun);
    }
}
