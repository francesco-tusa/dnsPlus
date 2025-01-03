package experiments.outputdata;

import java.util.ArrayList;
import java.util.List;
import experiments.RunTasksOutputManager;

/**
 *
 * @author uceeftu
 */
public abstract class ExperimentStatsCalculator {

    private final String name;
    private List<RunTasksOutputManager> allRunsOutput;
    private List<ExperimentTaskStats> experimentTasksStats;

    public ExperimentStatsCalculator(String name) {
        this.name = name;
        this.experimentTasksStats = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<RunTasksOutputManager> getAllRunsOutput() {
        return allRunsOutput;
    }

    public List<ExperimentTaskStats> getExperimentTasksStats() {
        return experimentTasksStats;
    }
    
    public void setAllRunsOutput(List<RunTasksOutputManager> allRunsOutput) {
        this.allRunsOutput = allRunsOutput;
    }

    
    @Override
    public String toString() {
        return "{" + "name=" + name + ", tasksStats=" + experimentTasksStats + '}';
    }
    
    public abstract void calculateStats();
}
