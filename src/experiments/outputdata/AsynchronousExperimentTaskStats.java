package experiments.outputdata;

import experiments.Task.TaskType;

/**
 *
 * @author uceeftu
 */
public class AsynchronousExperimentTaskStats extends ExperimentTaskStats {

    private double averageReplyDuration;
    private double replyDurationStandardDeviation;
    
    public AsynchronousExperimentTaskStats(String name) {
        super(name);
    }
    
    public AsynchronousExperimentTaskStats(String name, TaskType taskType) {
        super(name, taskType);
    }

    public double getAverageReplyDuration() {
        return averageReplyDuration;
    }

    public void setAverageReplyDuration(double averageReplyDuration) {
        this.averageReplyDuration = averageReplyDuration;
    }

    public double getReplyDurationStandardDeviation() {
        return replyDurationStandardDeviation;
    }

    public void setReplyDurationStandardDeviation(double replyDurationStandardDeviation) {
        this.replyDurationStandardDeviation = replyDurationStandardDeviation;
    }

    @Override
    public String toString() {
        return "\n\t\t{" + getTaskType() +
               ", name=" + getName() + 
               ", averageDuration=" + getAverageDuration() + 
               ", durationStandardDeviation=" + getDurationStandardDeviation() +
               ", averageReplyDuration=" + averageReplyDuration + 
               ", replyDurationStandardDeviation=" + replyDurationStandardDeviation + '}';
    }
}
