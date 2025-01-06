package experiments;

/**
 *
 * @author uceeftu
 */
public abstract class RunParallelTasksExecutor extends DefaultRunTasksExecutor  {
    

    public RunParallelTasksExecutor(String name) {
        super(name);
    }
    
    
    @Override
    public void start() {
        for (Task task : tasks) {
            Thread t = new Thread(task, task.getName());
            t.start();
        }       
    }   
    
}
