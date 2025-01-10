package experiments.cache.asynchronous;

import experiments.RunTasksOutputManager;
import experiments.cache.asynchronous.tasks.PublisherTask;
import experiments.cache.asynchronous.tasks.SubscriberTask;
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
            PublisherTask publisherTask = new PublisherTask("Pub" + i, experimentRun, getInputFileName(), numberOfPublications);
            experimentRun.addTask(publisherTask);
        }
    }

    private void addSubscribers() {
        for (int i = 1; i <= nSubs; i++) {
            SubscriberTask subscriberTask = new SubscriberTask("Sub" + i, experimentRun, getInputFileName(), numberOfSubscriptions);
            experimentRun.addTask(subscriberTask);
        }
    }

    @Override
    protected void executeRun() {
        experimentRun = new DNSWithCacheAsynchronousRun();
        experimentRun.setUp();
        
        addPublishers();
        addSubscribers();
        
        experimentRun.executeRun();
        
        experimentRun.finalise();
        
        List<RunTasksOutputManager> allRunsTasksOutput = getAllRunsTasksOutput();
        allRunsTasksOutput.add(experimentRun);
    }
}