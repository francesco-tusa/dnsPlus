package simulator;

public class SubscriptionTableEntryWithRegion extends SubscriptionTableEntry {

    private Region region;

    public SubscriptionTableEntryWithRegion(SimulationSubscription s) {
        super(s);

        if (s instanceof SubscriptionWithRegion sWithRegion) {
            Location bottomLeft = sWithRegion.getRegion().getBottomLeft();
            Location topRight = sWithRegion.getRegion().getTopRight();

            region = new Region(
                    new Location(bottomLeft.getX(), bottomLeft.getY(), bottomLeft.getZ()),
                    new Location(topRight.getX(), topRight.getY(), topRight.getZ()));
        }
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public String toString() {
        return "SubscriptionTableEntryWithRegion [region=" + region + "]";
    }

}
