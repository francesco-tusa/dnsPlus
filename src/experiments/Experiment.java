package experiments;

import experiments.cache.asynchronous.DNSWithCacheAsynchronousExperiment;
import experiments.inputdata.DBFactory;
import experiments.inputdata.DomainsDB;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public abstract class Experiment {
    
    private static final Logger logger = CustomLogger.getLogger(DNSWithCacheAsynchronousExperiment.class.getName());
    private String name;
    private String inputFileName;
    private int numberOfRuns;
    private DomainsDB domainsDB;

    public Experiment(String name, String inputFileName, int numberOfRuns) {
        this.name = name;
        this.inputFileName = inputFileName;
        this.numberOfRuns = numberOfRuns;
        this.domainsDB = DBFactory.getDomainsDB(System.getProperty("user.home") + "/" + inputFileName);
    }

    public String getName() {
        return name;
    }

    public DomainsDB getDomainsDB() {
        return domainsDB;
    }
    
    public String getInputFileName() {
        return inputFileName;
    }

    public int getNumberOfRuns() {
        return numberOfRuns;
    }

    
    public void executeExperiment() {
        for (int i = 0; i < numberOfRuns; i++) {
            logger.log(Level.WARNING, "Executing run {0} of {1}", new Object[]{i+1, numberOfRuns});
            executeRun();
        }
    }

    public abstract void calculateStats();
    
    protected abstract void executeRun();
    
}
