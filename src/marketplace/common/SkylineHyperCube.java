package marketplace.common;

import simulator.regions.Region;
import simulator.regions.SpatialRegion;
import simulator.core.Location;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SkylineHyperCube extends Region {

    private final List<double[]> paretoFrontier;
    private final boolean[] minimizeFlags;

    public SkylineHyperCube(double[] initialMetrics, boolean[] minimizeFlags, Location physicalLoc) {
        super(physicalLoc, physicalLoc);
        this.minimizeFlags = Arrays.copyOf(minimizeFlags, minimizeFlags.length);
        this.paretoFrontier = new ArrayList<>();
        this.paretoFrontier.add(Arrays.copyOf(initialMetrics, initialMetrics.length));
    }

    public SkylineHyperCube(double[] initialMetrics, boolean[] minimizeFlags, SpatialRegion physicalScope) {
        super(physicalScope);
        this.minimizeFlags = Arrays.copyOf(minimizeFlags, minimizeFlags.length);
        this.paretoFrontier = new ArrayList<>();
        this.paretoFrontier.add(Arrays.copyOf(initialMetrics, initialMetrics.length));
    }

    public SkylineHyperCube(SkylineHyperCube other) {
        super(other);
        this.minimizeFlags = Arrays.copyOf(other.minimizeFlags, other.minimizeFlags.length);
        this.paretoFrontier = new ArrayList<>();
        for (double[] pt : other.paretoFrontier) {
            this.paretoFrontier.add(Arrays.copyOf(pt, pt.length));
        }
    }

    public List<double[]> getParetoFrontier() {
        return paretoFrontier;
    }

    public boolean[] getMinimizeFlags() {
        return minimizeFlags;
    }

    @Override
    public Region copy() {
        return new SkylineHyperCube(this);
    }

    @Override
    public boolean contains(Location location) {
        // 1. Spatial Pruning (O(1) fast check)
        if (!super.contains(location)) return false;

        if (!(location instanceof MetricLocation req)) {
            throw new IllegalArgumentException(
                "Strict Type Enforcement: SkylineHyperCube requires a MetricLocation for QoS evaluation. " +
                "Received plain Location type: " + location.getClass().getName()
            );
        }

        if (req.getDimensions() != minimizeFlags.length) return false;

        // 2. Logical Skyline Check: Does ANY point in the frontier satisfy the strict constraints?
        for (double[] pt : paretoFrontier) {
            if (satisfies(pt, req)) return true;
        }
        return false;
    }

    private boolean satisfies(double[] providerMetrics, MetricLocation request) {
        for (int i = 0; i < minimizeFlags.length; i++) {
            double reqValue = request.getMetric(i);
            if (minimizeFlags[i]) {
                if (providerMetrics[i] > reqValue) return false;
            } else {
                if (providerMetrics[i] < reqValue) return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(SpatialRegion r) {
        if (r == null || r.getBottomLeft() == null) return false;

        // 1. Physical Containment
        boolean physicalContains = false;
        if (this.getWidth() >= 360.0 - 1e-5) {
            physicalContains = true;
        } else {
            physicalContains = super.contains(r.getBottomLeft()) && super.contains(r.getTopRight());
        }
        if (!physicalContains) return false;

        // 2. Logical Containment (Does this skyline cover everything in the other skyline?)
        if (!(r instanceof SkylineHyperCube other)) return false;

        for (double[] otherPt : other.paretoFrontier) {
            boolean covered = false;
            for (double[] thisPt : this.paretoFrontier) {
                if (dominatesOrEquals(thisPt, otherPt)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) return false;
        }
        return true;
    }

    @Override
    public boolean expand(SpatialRegion r) {
        boolean physicalChanged = super.expand(r);
        if (!(r instanceof SkylineHyperCube other)) return physicalChanged;

        boolean skylineChanged = false;
        for (double[] newPt : other.paretoFrontier) {
            if (addPointToFrontier(newPt)) {
                skylineChanged = true;
            }
        }
        return physicalChanged || skylineChanged;
    }

    private boolean addPointToFrontier(double[] newPt) {
        for (double[] existingPt : paretoFrontier) {
            if (dominatesOrEquals(existingPt, newPt)) {
                return false; // Dominated by existing point
            }
        }

        Iterator<double[]> it = paretoFrontier.iterator();
        while (it.hasNext()) {
            double[] existingPt = it.next();
            // We use strict domination here to remove inferior points
            if (dominates(newPt, existingPt)) {
                it.remove();
            }
        }

        paretoFrontier.add(Arrays.copyOf(newPt, newPt.length));
        return true;
    }

    private boolean dominatesOrEquals(double[] a, double[] b) {
        for (int i = 0; i < minimizeFlags.length; i++) {
            if (minimizeFlags[i] && a[i] > b[i] + 1e-9) return false;
            if (!minimizeFlags[i] && a[i] < b[i] - 1e-9) return false;
        }
        return true;
    }

    private boolean dominates(double[] a, double[] b) {
        boolean strictlyBetter = false;
        for (int i = 0; i < a.length; i++) {
            if (minimizeFlags[i]) {
                if (a[i] > b[i] + 1e-9) return false;
                if (a[i] < b[i] - 1e-9) strictlyBetter = true;
            } else {
                if (a[i] < b[i] - 1e-9) return false;
                if (a[i] > b[i] + 1e-9) strictlyBetter = true;
            }
        }
        return strictlyBetter;
    }

    @Override
    public String toShortString() {
        return super.toShortString() + " | Skyline Points: " + paretoFrontier.size();
    }
}