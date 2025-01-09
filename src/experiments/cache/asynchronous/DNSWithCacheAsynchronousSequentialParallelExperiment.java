package experiments.cache.asynchronous;

import encryption.BlindingEntity;
import experiments.RunTasksOutputManager;
import experiments.cache.asynchronous.tasks.MeasurementRequesterPublisherTask;
import experiments.cache.asynchronous.tasks.MeasurementRequesterSubscriberTask;
import experiments.cache.asynchronous.tasks.PublisherNopTask;
import experiments.cache.asynchronous.tasks.PublisherTask;
import experiments.cache.asynchronous.tasks.SubscriberTask;
import java.util.List;
import publishing.Publisher;
import subscribing.AsynchronousSubscriber;

/**
 *
 * @author uceeftu
 */
public final class DNSWithCacheAsynchronousSequentialParallelExperiment extends AsynchronousExperiment {

    DNSWithCacheAsynchronousSequentialParallelRun experimentRun;
    
    private int numberOfPublications;
    private int numberOfSubscriptions;
    
    private int nPubs;
    private int nSubs;

    public DNSWithCacheAsynchronousSequentialParallelExperiment(String inputFileName, int numberOfRuns) {
        this(inputFileName, numberOfRuns, 100, 10);
    }

    public DNSWithCacheAsynchronousSequentialParallelExperiment(String inputFileName, int numberOfRuns, int numberOfPublications, int numberOfSubscriptions) {
        this(inputFileName, numberOfRuns, numberOfPublications, numberOfSubscriptions, 1, 1);
    }
    
    public DNSWithCacheAsynchronousSequentialParallelExperiment(String inputFileName, 
                                              int numberOfRuns, 
                                              int numberOfPublications, 
                                              int numberOfSubscriptions,
                                              int numberOfPubs,
                                              int numberOfSubs) 
    {
        super(DNSWithCacheAsynchronousSequentialParallelExperiment.class.getSimpleName(), inputFileName, numberOfRuns);
        this.numberOfPublications = numberOfPublications;
        this.numberOfSubscriptions = numberOfSubscriptions;
        nPubs = numberOfPubs;
        nSubs = numberOfSubs;
    }
    
    
//    private void addPublishers() {
//        for (int i = 1; i <= nPubs; i++) {
//            DNSWithCacheAsynchronousSequentialParallelRun.PublisherTask publisherTask = experimentRun.new PublisherTask("Pub" + i);
//            experimentRun.addTask(publisherTask);
//        }
//    }
//
//    private void addSubscribers() {
//        for (int i = 1; i <= nSubs; i++) {
//            DNSWithCacheAsynchronousSequentialParallelRun.SubscriberTask subscriberTask = experimentRun.new SubscriberTask("Sub" + i);
//            experimentRun.addTask(subscriberTask);
//        }
//    }

    @Override
    protected void executeRun() {
        experimentRun = new DNSWithCacheAsynchronousSequentialParallelRun(numberOfPublications, numberOfSubscriptions);
        experimentRun.setUp();
        
        //addPublishers();
        //addSubscribers();
        
        Publisher pub1 = new Publisher("pub1");
        Publisher pub2 = new Publisher("pub2");
        AsynchronousSubscriber sub1 = new AsynchronousSubscriber("sub1");
        AsynchronousSubscriber sub2 = new AsynchronousSubscriber("sub2");
        
        PublisherTask preTask = new MeasurementRequesterPublisherTask(pub1, experimentRun, getInputFileName());
        experimentRun.addPreTask(preTask);
        
        SubscriberTask task1 = new MeasurementRequesterSubscriberTask(sub1, experimentRun, getInputFileName(), 10);
        SubscriberTask task2 = new MeasurementRequesterSubscriberTask(sub2, experimentRun, getInputFileName(), 10);
        PublisherTask task3 = new MeasurementRequesterPublisherTask(pub1, experimentRun, getInputFileName(), 100);
        PublisherTask task4 = new MeasurementRequesterPublisherTask(pub2, experimentRun, getInputFileName(), 200);
        experimentRun.addTask(task1);
        experimentRun.addTask(task2);
        experimentRun.addTask(task3);
        experimentRun.addTask(task4);
        
        SubscriberTask postTask = new SubscriberTask(sub1, experimentRun, getInputFileName(), 20);
        experimentRun.addPostTask(postTask);
        // needed to properly shutdown the broker
        experimentRun.addPostTask(new PublisherNopTask(pub1, experimentRun));
        
        
        experimentRun.executeRun();
        
        experimentRun.finalise();
        
        List<RunTasksOutputManager> allRunsTasksOutput = getAllRunsTasksOutput();
        allRunsTasksOutput.add(experimentRun);
    }
    
    
    private class TaskGenerator {
        public void addPreTask (BlindingEntity entity) {
            if (entity instanceof Publisher p) {
                
            }
            
        }
    }
}
