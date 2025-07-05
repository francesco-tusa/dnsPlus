package simulator;

import simulator.simulations.performance.GridServiceSimulation;
import simulator.simulations.validation.FixedTopologyValidationSimulation;
import simulator.simulations.validation.RandomTopologyValidationSimulation;


/**
 * Main entry point for running simulations.
 * Delegates execution to a specific SimulationRunner implementation.
 */
public class SimulationMain {

    public static void main(String[] args) {
        GridServiceSimulation.main(args);
        //RegionFixedSimulation.main(args);
        //RegionRandomSimulation.main(args);
    }
}