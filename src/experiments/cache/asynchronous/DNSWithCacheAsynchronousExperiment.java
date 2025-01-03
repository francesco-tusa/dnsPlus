package experiments.cache.asynchronous;

import experiments.RunTasksOutputManager;
import java.util.List;

/**
 *
 * @author uceeftu
 */
public final class DNSWithCacheAsynchronousExperiment extends AsynchronousExperiment {

    private int numberOfPublications;
    private int numberOfSubscriptions;

    public DNSWithCacheAsynchronousExperiment(String inputFileName, int numberOfRuns) {
        this(inputFileName, numberOfRuns, 100, 10);
    }

    public DNSWithCacheAsynchronousExperiment(String inputFileName, int numberOfRuns, int numberOfPublications, int numberOfSubscriptions) {
        super(DNSWithCacheAsynchronousExperiment.class.getSimpleName(), inputFileName, numberOfRuns);
        this.numberOfPublications = numberOfPublications;
        this.numberOfSubscriptions = numberOfSubscriptions;
    }

    @Override
    protected void executeRun() {
        DNSWithCacheAsynchronousRun experimentRun = new DNSWithCacheAsynchronousRun(getDomainsDB(), numberOfPublications, numberOfSubscriptions);
        experimentRun.setUp();
        
        DNSWithCacheAsynchronousRun.PublisherTask publisherTask = experimentRun.new PublisherTask();
        DNSWithCacheAsynchronousRun.SubscriberTask subscriberTask = experimentRun.new SubscriberTask();
        experimentRun.addTask(publisherTask);
        experimentRun.addTask(subscriberTask);
        
        experimentRun.start();
        experimentRun.waitForRequestsCompletion();
        experimentRun.cleanUp();
        experimentRun.waitForRepliesCompletion();
        
        List<RunTasksOutputManager> allRunsTasksOutput = getAllRunsTasksOutput();
        allRunsTasksOutput.add(experimentRun);
    }
    
    
}
