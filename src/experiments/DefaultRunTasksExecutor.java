package experiments;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author uceeftu
 */
public abstract class DefaultRunTasksExecutor implements RunTasksExecutor {
    
    protected final String name;
    protected final List<Task> tasks;

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

    @Override
    public abstract void setUp();

    
    @Override
    public void addTask(Task task) {
        tasks.add(task);
    }

    @Override
    public abstract void cleanUp();
    
}
