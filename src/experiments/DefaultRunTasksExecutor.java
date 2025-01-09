package experiments;

import experiments.Task.TaskType;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author uceeftu
 */
public abstract class DefaultRunTasksExecutor implements RunTasksExecutor {
    
    private final String name;
    private List<Task> tasks;

    public DefaultRunTasksExecutor(String name) {
        this.name = name;
        tasks = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Task> getTasks() {
        return tasks;
    }

    protected void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public abstract void setUp();

    
    @Override
    public void addTask(Task task) {
        task.setTaskType(TaskType.TASK);
        tasks.add(task);
    }
    
}
