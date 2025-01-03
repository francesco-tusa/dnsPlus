package experiments.outputdata;

import experiments.RunTasksOutputManager;
import experiments.Task;
import java.util.List;

public class AsynchronousExperimentStatsCalculator extends ExperimentStatsCalculator {

    public AsynchronousExperimentStatsCalculator(String name) {
        super(name);
    }

    @Override
    public void calculateStats() {

        // getting number of tasks in a run (from first run element)
        List<RunTasksOutputManager> allRunsOutput = getAllRunsOutput();
        int numberOfTasks = allRunsOutput.get(0).getTasks().size();
        
        AsynchronousExperimentTaskStats taskStats = null;

        for (int i = 0; i < numberOfTasks; i++) {

            long taskDurationSum = 0;
            long taskDurationSumSquares = 0;

            long taskReplyDurationSum = 0;
            long taskReplyDurationSumSquares = 0;
            
            for (RunTasksOutputManager r : allRunsOutput) {
                List<Task> tasks = r.getTasks();
                Task currentTask = tasks.get(i);

                long taskDuration = currentTask.getDuration();
                taskDurationSum += taskDuration;
                taskDurationSumSquares += taskDuration * taskDuration;

                long taskReplyDuration = currentTask.getReplyDuration();
                taskReplyDurationSum += taskReplyDuration;
                taskReplyDurationSumSquares += taskReplyDuration * taskReplyDuration;

                taskStats = new AsynchronousExperimentTaskStats(currentTask.getName());
            }

            int numberOfRuns = allRunsOutput.size();

            double average = (double) taskDurationSum / numberOfRuns;
            double variance = ((double) taskDurationSumSquares / numberOfRuns) - (average * average);
            double stdDeviation = Math.sqrt(variance);

            double replyAverage = (double) taskReplyDurationSum / numberOfRuns;
            double replyVariance = ((double) taskReplyDurationSumSquares / numberOfRuns) - (replyAverage * replyAverage);
            double replyStdDeviation = Math.sqrt(replyVariance);

            if (taskStats != null) {
                taskStats.setAverageDuration(average);
                taskStats.setDurationStandardDeviation(stdDeviation);
                taskStats.setAverageReplyDuration(replyAverage);
                taskStats.setReplyDurationStandardDeviation(replyStdDeviation);
            }

            List<ExperimentTaskStats> experimentTasksStats = getExperimentTasksStats();
            experimentTasksStats.add(taskStats);
        }
    }
}
