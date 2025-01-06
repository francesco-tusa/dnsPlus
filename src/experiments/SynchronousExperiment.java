package experiments;

import experiments.outputdata.SynchronousExperimentStatsCalculator;

/**
 *
 * @author uceeftu
 */
public abstract class SynchronousExperiment extends Experiment {

    public SynchronousExperiment(String name, String inputFileName, int numberOfRuns) {
        super(name, inputFileName, numberOfRuns, new SynchronousExperimentStatsCalculator(name));
    }
}
