package experiments;

import java.time.Duration;

/**
 *
 * @author uceeftu
 */
public interface Task extends Runnable {
    String getName();
    void setName(String name);
    long getDuration();
    void setDuration(Duration duration);
    TaskType getTaskType();
    void setTaskType(TaskType taskType);
    long getReplyDuration();
    void setReplyDuration(Duration duration);
    
    enum TaskType {
        PRE_TASK, TASK, POST_TASK
    }
}
