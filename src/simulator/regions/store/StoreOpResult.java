package simulator.regions.store;

public enum StoreOpResult {
    NO_CHANGE, // The new subscription was fully covered by existing state (Filter)
    EXPANDED,  // An existing subscription was merged/expanded with the new one
    ADDED      // The subscription was added as a new, disjoint entry
}