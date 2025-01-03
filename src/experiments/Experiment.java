package experiments;

import experiments.inputdata.DBFactory;
import experiments.inputdata.DomainsDB;
import experiments.outputdata.ExperimentStatsCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public abstract class Experiment {
    
    private static final Logger logger = CustomLogger.getLogger(Experiment.class.getName());
    private final String name;
    private final String inputFileName;
    private final int numberOfRuns;
    private final DomainsDB domainsDB;
    
    private final List<RunTasksOutputManager> allRunsTasksOutput;
    private final ExperimentStatsCalculator experimentStatsCalculator;

    public Experiment(String name, String inputFileName, int numberOfRuns, ExperimentStatsCalculator experimentStatsCalculator) {
        this.name = name;
        this.inputFileName = inputFileName;
        this.numberOfRuns = numberOfRuns;
        this.domainsDB = DBFactory.getDomainsDB(System.getProperty("user.home") + "/" + inputFileName);
        allRunsTasksOutput = new ArrayList<>(numberOfRuns);
        
        this.experimentStatsCalculator = experimentStatsCalculator;
        experimentStatsCalculator.setAllRunsOutput(allRunsTasksOutput);
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

    protected List<RunTasksOutputManager> getAllRunsTasksOutput() {
        return allRunsTasksOutput;
    }

    protected ExperimentStatsCalculator getExperimentStatsCalculator() {
        return experimentStatsCalculator;
    }
    
    public void executeExperiment() {
        for (int i = 0; i < numberOfRuns; i++) {
            logger.log(Level.INFO, "Executing run {0} of {1}", new Object[]{i+1, numberOfRuns});
            executeRun();
        }
    }
    
    public void calculateStats() {
        experimentStatsCalculator.calculateStats();
    }

    @Override
    public String toString() {
        return "{name=" + getName() + ", inputFileName=" + getInputFileName() + ", numberOfRuns=" + getNumberOfRuns() + ", \n\texperimentOutput=" + experimentStatsCalculator + '}';
    }
    
    
    protected abstract void executeRun();
    
}
