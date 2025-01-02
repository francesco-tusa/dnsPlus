package experiments.outputdata.asynchronous;

import experiments.cache.asynchronous.AsynchronousTask;
import java.util.ArrayList;
import java.util.List;

public class AsynchronousExperimentOutput {

    private final String name;
    private final List<AsynchronousRunOutput> runsOutput;
    private List<AsynchronousTaskOutput> tasksStats;

    public AsynchronousExperimentOutput(String name, List<AsynchronousRunOutput> runs) {
        this.name = name;
        this.runsOutput = runs;
        this.tasksStats = new ArrayList<>(runs.size());
    }

    public String getName() {
        return name;
    }

    public List<AsynchronousTaskOutput> getTasksStats() {
        return tasksStats;
    }

    public void calculateStats() {

        // getting number of tasks in a run (from first run element)
        int numberOfTasks = runsOutput.get(0).getTasks().size();
        
        AsynchronousTaskOutput taskStats = null;

        for (int i = 0; i < numberOfTasks; i++) {

            long taskDurationSum = 0;
            long taskDurationSumSquares = 0;

            long taskReplyDurationSum = 0;
            long taskReplyDurationSumSquares = 0;
            
            for (AsynchronousRunOutput r : runsOutput) {
                List<AsynchronousTask> tasks = r.getTasks();
                AsynchronousTask currentTask = tasks.get(i);

                long taskDuration = currentTask.getDuration();
                taskDurationSum += taskDuration;
                taskDurationSumSquares += taskDuration * taskDuration;

                long taskReplyDuration = currentTask.getReplyDuration();
                taskReplyDurationSum += taskReplyDuration;
                taskReplyDurationSumSquares += taskReplyDuration * taskReplyDuration;

                taskStats = new AsynchronousTaskOutput(currentTask.getName());
            }

            int numberOfRuns = runsOutput.size();

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

            tasksStats.add(taskStats);

        }
    }

    @Override
    public String toString() {
        return "{" + "name=" + name + ", tasksStats=" + tasksStats + '}';
    }
}
