package experiments.cache.asynchronous;

import experiments.outputdata.asynchronous.AsynchronousRunOutput;

/**
 *
 * @author uceeftu
 */
public final class DNSWithCacheAsynchronousExperiment extends AsynchronousExperiment {

    public DNSWithCacheAsynchronousExperiment(String inputFileName, int numberOfRuns) {
        super(DNSWithCacheAsynchronousExperiment.class.getSimpleName(), inputFileName, numberOfRuns);
    }
    
    @Override
    protected void executeRun() {
        DNSWithCacheAsynchronousRun experimentRun = new DNSWithCacheAsynchronousRun(getDomainsDB());
        experimentRun.setUp();
        
        DNSWithCacheAsynchronousRun.PublisherTask publisherTask = experimentRun.new PublisherTask();
        DNSWithCacheAsynchronousRun.SubscriberTask subscriberTask = experimentRun.new SubscriberTask();
        experimentRun.addTask(publisherTask);
        experimentRun.addTask(subscriberTask);
        
        experimentRun.start();
        experimentRun.waitForRequestsCompletion();
        experimentRun.cleanUp();
        experimentRun.waitForRepliesCompletion();
        
        AsynchronousRunOutput currentRunOutput = new AsynchronousRunOutput(experimentRun.getName(), experimentRun.getTasks());
        getExperimentRunsOutput().add(currentRunOutput);
    }
    
    
}
