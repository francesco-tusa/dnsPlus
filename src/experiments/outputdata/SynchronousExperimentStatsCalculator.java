package experiments.outputdata;

import experiments.RunTasksOutputManager;
import experiments.Task;
import java.util.List;

/**
 *
 * @author uceeftu
 */
public class SynchronousExperimentStatsCalculator extends ExperimentStatsCalculator {

    public SynchronousExperimentStatsCalculator(String name) {
        super(name);
    }

    @Override
    public void calculateStats() {
        
        // getting number of tasks in a run (from first run element)
        List<RunTasksOutputManager> allRunsOutput = getAllRunsOutput();
        int numberOfTasks = allRunsOutput.get(0).getTasks().size();
        
        ExperimentTaskStats taskStats = null;
        
        for (int i = 0; i < numberOfTasks; i++) {
            
            long taskDurationSum = 0;
            long taskDurationSumSquares = 0;
            
            for (RunTasksOutputManager r : allRunsOutput) {
                List<Task> tasks = r.getTasks();
                Task currentTask = tasks.get(i);
                long taskDuration = currentTask.getDuration();
                taskDurationSum += taskDuration;
                taskDurationSumSquares += taskDuration * taskDuration;
                taskStats = new ExperimentTaskStats(currentTask.getName(), currentTask.getTaskType());
            }
            
            int numberOfRuns = allRunsOutput.size();
            double average = (double) taskDurationSum / numberOfRuns;
            double variance = ((double) taskDurationSumSquares / numberOfRuns) - (average * average);
            double stdDeviation = Math.sqrt(variance);
            
            if (taskStats != null) {
                taskStats.setAverageDuration(average);
                taskStats.setDurationStandardDeviation(stdDeviation);
            }
            
            getExperimentTasksStats().add(taskStats);
        }
    }
}