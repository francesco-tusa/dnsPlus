package experiments.outputdata;

import experiments.Task.TaskType;

/**
 *
 * @author uceeftu
 */
public class AsynchronousExperimentTaskStats extends ExperimentTaskStats {

    private double asynchronousProcessingAverageDuration;
    private double asynchronousProcessingDurationStandardDeviation;
    
    public AsynchronousExperimentTaskStats(String name) {
        super(name);
    }
    
    public AsynchronousExperimentTaskStats(String name, TaskType taskType) {
        super(name, taskType);
    }

    public double getAsynchronousProcessingAverageDuration() {
        return asynchronousProcessingAverageDuration;
    }

    public void setAsynchronousProcessingAverageDuration(double asynchronousProcessingAverageDuration) {
        this.asynchronousProcessingAverageDuration = asynchronousProcessingAverageDuration;
    }

    public double getAsynchronousProcessingDurationStandardDeviation() {
        return asynchronousProcessingDurationStandardDeviation;
    }

    public void setAsynchronousProcessingDurationStandardDeviation(double asynchronousProcessingDurationStandardDeviation) {
        this.asynchronousProcessingDurationStandardDeviation = asynchronousProcessingDurationStandardDeviation;
    }

    @Override
    public String toString() {
        return "\n\t\t{" + getTaskType() +
               ", name=" + getName() + 
               ", averageDuration=" + getAverageDuration() + 
               ", durationStandardDeviation=" + getDurationStandardDeviation() +
               ", asynchronousProcessingAverageDuration=" + asynchronousProcessingAverageDuration + 
               ", asynchronousProcessingDurationStandardDeviation=" + asynchronousProcessingDurationStandardDeviation + '}';
    }
}
