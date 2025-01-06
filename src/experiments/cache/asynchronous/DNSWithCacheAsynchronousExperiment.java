package experiments.cache.asynchronous;

import experiments.RunTasksOutputManager;
import java.util.List;

/**
 *
 * @author uceeftu
 */
public final class DNSWithCacheAsynchronousExperiment extends AsynchronousExperiment {

    DNSWithCacheAsynchronousRun experimentRun;
    
    private int numberOfPublications;
    private int numberOfSubscriptions;
    
    private int nPubs;
    private int nSubs;

    public DNSWithCacheAsynchronousExperiment(String inputFileName, int numberOfRuns) {
        this(inputFileName, numberOfRuns, 100, 10);
    }

    public DNSWithCacheAsynchronousExperiment(String inputFileName, int numberOfRuns, int numberOfPublications, int numberOfSubscriptions) {
        this(inputFileName, numberOfRuns, numberOfPublications, numberOfSubscriptions, 1, 1);
    }
    
    public DNSWithCacheAsynchronousExperiment(String inputFileName, 
                                              int numberOfRuns, 
                                              int numberOfPublications, 
                                              int numberOfSubscriptions,
                                              int numberOfPubs,
                                              int numberOfSubs) 
    {
        super(DNSWithCacheAsynchronousExperiment.class.getSimpleName(), inputFileName, numberOfRuns);
        this.numberOfPublications = numberOfPublications;
        this.numberOfSubscriptions = numberOfSubscriptions;
        nPubs = numberOfPubs;
        nSubs = numberOfSubs;
    }
    
    
    private void addPublishers() {
        for (int i = 1; i <= nPubs; i++) {
            DNSWithCacheAsynchronousRun.PublisherTask publisherTask = experimentRun.new PublisherTask("Pub" + i);
            experimentRun.addTask(publisherTask);
        }
    }

    private void addSubscribers() {
        for (int i = 1; i <= nSubs; i++) {
            DNSWithCacheAsynchronousRun.SubscriberTask subscriberTask = experimentRun.new SubscriberTask("Sub" + i);
            experimentRun.addTask(subscriberTask);
        }
    }

    @Override
    protected void executeRun() {
        experimentRun = new DNSWithCacheAsynchronousRun(getDomainsDB(), numberOfPublications, numberOfSubscriptions);
        experimentRun.setUp();
        
        addPublishers();
        addSubscribers();
        
        experimentRun.start();
        experimentRun.waitForRequestsCompletion();
        experimentRun.cleanUp();
        experimentRun.waitForRepliesCompletion();
        
        List<RunTasksOutputManager> allRunsTasksOutput = getAllRunsTasksOutput();
        allRunsTasksOutput.add(experimentRun);
    }
}
