package experiments.cache.asynchronous;

import experiments.outputdata.asynchronous.AsynchronousExperimentOutput;
import experiments.outputdata.asynchronous.AsynchronousRunOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public class AsynchronousDNSWithCacheExperiment {
    
    private static final Logger logger = CustomLogger.getLogger(AsynchronousDNSWithCacheExperiment.class.getName());
    private String name;
    private String inputFileName;
    private int numberOfRuns;
    
    private List<AsynchronousRunOutput> experimentRunsOutput;
    private AsynchronousExperimentOutput experimentOutput;
    

    public AsynchronousDNSWithCacheExperiment(String inputFileName, int numberOfRuns) {
        name = this.getClass().getSimpleName();
        this.inputFileName = inputFileName;
        this.numberOfRuns = numberOfRuns;
        experimentRunsOutput = new ArrayList<>(numberOfRuns);
    }
    
    private void executeRun() {
        String home = System.getProperty("user.home");
        AsynchronousDNSWithCacheRun currentRun = new AsynchronousDNSWithCacheRun(home + "/" + inputFileName);
        currentRun.setUp();
        
        AsynchronousDNSWithCacheRun.PublisherTask publisherTask = currentRun.new PublisherTask();
        AsynchronousDNSWithCacheRun.SubscriberTask subscriberTask = currentRun.new SubscriberTask();
        currentRun.addTask(publisherTask);
        currentRun.addTask(subscriberTask);
        
        currentRun.start();
        currentRun.waitForRequestsCompletion();
        currentRun.cleanUp();
        currentRun.waitForRepliesCompletion();
        
        AsynchronousRunOutput currentRunOutput = new AsynchronousRunOutput(currentRun.getName(), currentRun.getTasks());
        experimentRunsOutput.add(currentRunOutput);
    }
    
    
    public void executeExperiment() {
        for (int i=0; i<numberOfRuns; i++) {
            logger.log(Level.WARNING, "Executing run " + i + " of " + numberOfRuns);
            executeRun();
        }
    }
    
    
    public void calculateStats() {
        experimentOutput = new AsynchronousExperimentOutput(name, experimentRunsOutput);
        experimentOutput.calculateStats();
    }

    @Override
    public String toString() {
        return "{name=" + name + ", inputFileName=" + inputFileName + ", numberOfRuns=" + numberOfRuns + ", \n\texperimentOutput=" + experimentOutput + '}';
    }
}
