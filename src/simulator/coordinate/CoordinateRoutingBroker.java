package simulator.coordinate;

import simulator.entities.SimulationBroker;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;

/* This Broker operates in a loosely-regionally-aware overlay, where each broker does not 
 * know the region it covers. It just sees, when running, the coordinates announced 
 * by the publications it receives
 */

public class CoordinateRoutingBroker extends SimulationBroker {

    public CoordinateRoutingBroker(String name) {
        super(name);
    }

    @Override
    public void addSubscription(SimulationSubscription s) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        // TODO Auto-generated method stub
        
    }

}
