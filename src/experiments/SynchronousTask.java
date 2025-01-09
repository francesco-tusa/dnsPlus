package experiments;

import java.time.Duration;

/**
 *
 * @author uceeftu
 */
public abstract class SynchronousTask implements Task {
    
    private String name;
    private Duration duration;
    private TaskType taskType;

    protected SynchronousTask() {
        duration = Duration.ZERO;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getDuration() {
        return duration.toMillis();
    }
    
    @Override
    public void setDuration(Duration d) {
        duration = d;
    }

    @Override
    public TaskType getTaskType() {
        return taskType;
    }

    @Override
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    @Override
    public long getReplyDuration() {
        throw new UnsupportedOperationException("No reply duration in a Synchronous Task");
    }

    @Override
    public void setReplyDuration(Duration duration) {
        throw new UnsupportedOperationException("No reply duration in a Synchronous Task");
    }

    @Override
    public abstract void run();
}


