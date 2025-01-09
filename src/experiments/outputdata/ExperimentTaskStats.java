package experiments.outputdata;

import experiments.Task.TaskType;

/**
 *
 * @author uceeftu
 */
public class ExperimentTaskStats {
    private final String name;
    private final TaskType taskType;
    private double averageDuration;
    private double durationStandardDeviation;

    public ExperimentTaskStats(String name) {
        this.name = name;
        this.taskType = TaskType.TASK;
    }
    
    public ExperimentTaskStats(String name, TaskType taskType) {
        this.name = name;
        this.taskType = taskType;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public double getAverageDuration() {
        return averageDuration;
    }

    public double getDurationStandardDeviation() {
        return durationStandardDeviation;
    }

    public String getName() {
        return name;
    }

    public void setAverageDuration(double averageDuration) {
        this.averageDuration = averageDuration;
    }

    public void setDurationStandardDeviation(double durationStandardDeviation) {
        this.durationStandardDeviation = durationStandardDeviation;
    }

    @Override
    public String toString() {
        return "\n\t\t{" + getTaskType() +
               ", name=" + getName() +
               ", averageDuration=" + getAverageDuration() + 
               ", durationStandardDeviation=" + getDurationStandardDeviation() + '}';
    }
}
