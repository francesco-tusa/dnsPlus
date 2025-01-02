package experiments.outputdata;

import experiments.Task;
import java.util.List;

public class RunOutput<T extends Task> {
    private String name;
    private List<T> tasks;

    public RunOutput(String name, List<T> tasks) {
        this.name = name;
        this.tasks = tasks;
    }

    public String getName() {
        return name;
    }

    public List<T> getTasks() {
        return tasks;
    }
}
