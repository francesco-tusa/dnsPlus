package experiments.outputdata;

import experiments.Task;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author uceeftu
 */
public class ExperimentOutput {
    private final String name;
    private final List<RunOutput> runsOutput;
    private List<TaskOutput> tasksOutput;

    public ExperimentOutput(String name, List<RunOutput> runs) {
        this.name = name;
        this.runsOutput = runs;
        this.tasksOutput = new ArrayList<>(runs.size());
    }

    public String getName() {
        return name;
    }

    public void calculateStats() {
        
        // getting number of tasks in a run (from first run element)
        int numberOfTasks = runsOutput.get(0).getTasks().size();
        
        TaskOutput taskStats = null;
        
        for (int i = 0; i < numberOfTasks; i++) {
            
            long taskDurationSum = 0;
            long taskDurationSumSquares = 0;
            
            for (RunOutput r : runsOutput) {
                List<Task> tasks = r.getTasks();
                Task currentTask = tasks.get(i);
                long taskDuration = currentTask.getDuration();
                taskDurationSum += taskDuration;
                taskDurationSumSquares += taskDuration * taskDuration;
                taskStats = new TaskOutput(currentTask.getName());
            }
            
            int numberOfRuns = runsOutput.size();
            double average = (double) taskDurationSum / numberOfRuns;
            double variance = ((double) taskDurationSumSquares / numberOfRuns) - (average * average);
            double stdDeviation = Math.sqrt(variance);
            
            if (taskStats != null) {
                taskStats.setAverageDuration(average);
                taskStats.setDurationStandardDeviation(stdDeviation);
            }
        }
    }
}
