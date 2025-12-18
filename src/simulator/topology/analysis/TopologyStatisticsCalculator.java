package simulator.topology.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import simulator.regions.Region;
import simulator.topology.geonames.builder.GeoNamesBuilderNode;
import utils.CustomLogger;

/**
 * centralized utility for all offline topology analysis:
 * 1. Structural Geometry (Overlaps, Areas)
 * 2. Political Provenance (Continent Extremities)
 * 3. Population Distribution (Leaf Quality, Node Types)
 */
public class TopologyStatisticsCalculator {

    private static final Logger logger = CustomLogger.getLogger(TopologyStatisticsCalculator.class.getName());

    // Data holder for a single level's stats
    private static class LevelStats {
        int level;
        int nodeCount;
        double avgFanOut;
        
        long overlappingPairs = 0;
        long totalPairs = 0;
        double overlapPct = 0.0;
        
        // Detailed metrics (only for top levels)
        double totalIntersectionArea = 0.0;
        double totalNodesArea = 0.0;
        double redundancyPct = 0.0;
        boolean detailedGeometryCalculated = false;
        boolean fastGeometryCalculated = false;
    }

    // ============================================================================================
    // SECTION 1: GEOMETRIC & STRUCTURAL ANALYSIS
    // ============================================================================================

    public static void logTopologyStats(GeoNamesBuilderNode root) {
        logger.info("");
        logger.info("=== TOPOLOGY STRUCTURAL ANALYSIS (OFFLINE CALCULATION) ===");
        
        // 1. Collect Stats for all levels
        List<LevelStats> allStats = new ArrayList<>();
        int currentLevel = 0;
        
        while (true) {
            List<GeoNamesBuilderNode> nodesAtLevel = new ArrayList<>();
            collectNodesAtRelativeDepth(root, 0, currentLevel, nodesAtLevel);
            
            if (nodesAtLevel.isEmpty()) break;
            
            LevelStats stats = new LevelStats();
            stats.level = currentLevel;
            stats.nodeCount = nodesAtLevel.size();
            stats.avgFanOut = nodesAtLevel.stream()
                    .mapToInt(curr -> (curr.children == null) ? 0 : curr.children.size())
                    .average().orElse(0.0);
            
            if (stats.nodeCount > 1) {
                // STRATEGY A: Detailed Area Analysis (Continents & Countries)
                if (currentLevel >= 1 && currentLevel <= 2) {
                    calculateDetailedGeometry(nodesAtLevel, stats);
                } 
                // STRATEGY B: Fast Pair-Only Analysis (Provinces & Cities)
                else {
                    calculateFastOverlaps(nodesAtLevel, stats);
                }
            }
            allStats.add(stats);
            currentLevel++;
        }

        // 2. Print SPECIFIC / DETAILED Stats First
        logger.info("--- 1. DETAILED GEOMETRY & OVERLAP ANALYSIS ---");
        boolean anyIssues = false;
        
        for (LevelStats stats : allStats) {
            if (stats.nodeCount <= 1) continue;

            if (stats.detailedGeometryCalculated && stats.overlappingPairs > 0) {
                anyIssues = true;
                logger.info(String.format("Level %d (Continents/Countries):", stats.level));
                logger.info(String.format("   -> Overlapping Pairs:      %d / %d (%.4f%%)", 
                    stats.overlappingPairs, stats.totalPairs, stats.overlapPct));
                logger.info(String.format("   -> Intersection Area:      %.6f sq deg", stats.totalIntersectionArea));
                logger.info(String.format("   -> Geographic Redundancy:  %.4f%% (Shared Area / Total Area)", stats.redundancyPct));
            } 
            else if (stats.fastGeometryCalculated && stats.overlappingPairs > 0) {
                anyIssues = true;
                logger.info(String.format("Level %d (Sub-Regions):", stats.level));
                logger.info(String.format("   -> Overlapping Pairs:      %d / %d (%.4f%%)", 
                    stats.overlappingPairs, stats.totalPairs, stats.overlapPct));
            }
        }
        
        if (!anyIssues) {
            logger.info("   -> No significant geometric overlaps detected.");
        }

        // 3. Print GENERAL Stats Last
        logger.info("");
        logger.info("--- 2. GENERAL STRUCTURE SUMMARY ---");
        logger.info(String.format("%-6s | %-10s | %-12s", "LEVEL", "NODES", "AVG FAN-OUT"));
        logger.info("------------------------------------");
        for (LevelStats stats : allStats) {
            logger.info(String.format("%-6d | %-10d | %-12.2f", 
                stats.level, stats.nodeCount, stats.avgFanOut));
        }
        logger.info("==========================================================");
    }

    private static void calculateDetailedGeometry(List<GeoNamesBuilderNode> nodes, LevelStats stats) {
        stats.detailedGeometryCalculated = true;
        long n = nodes.size();
        
        for (GeoNamesBuilderNode curr : nodes) {
            if (curr.bounds != null) stats.totalNodesArea += new Region(curr.bounds).getArea();
        }

        long checkedPairs = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                checkedPairs++;
                GeoNamesBuilderNode n1 = nodes.get(i);
                GeoNamesBuilderNode n2 = nodes.get(j);
                
                if (n1.bounds != null && n2.bounds != null) {
                    Region r1 = new Region(n1.bounds);
                    Region r2 = new Region(n2.bounds);
                    
                    if (r1.intersects(r2)) {
                        stats.overlappingPairs++;
                        stats.totalIntersectionArea += r1.getIntersectionArea(r2);
                    }
                }
            }
        }
        stats.totalPairs = checkedPairs;
        stats.overlapPct = (stats.totalPairs > 0) ? (100.0 * stats.overlappingPairs / stats.totalPairs) : 0.0;
        stats.redundancyPct = (stats.totalNodesArea > 0) ? (100.0 * stats.totalIntersectionArea / stats.totalNodesArea) : 0.0;
    }

    private static void calculateFastOverlaps(List<GeoNamesBuilderNode> nodes, LevelStats stats) {
        stats.fastGeometryCalculated = true;
        long n = nodes.size();
        stats.totalPairs = n * (n - 1) / 2;
        stats.overlappingPairs = calculateOverlapsSweep(nodes);
        stats.overlapPct = (stats.totalPairs > 0) ? (100.0 * stats.overlappingPairs / stats.totalPairs) : 0.0;
    }

    // ============================================================================================
    // SECTION 2: POLITICAL & PROVENANCE ANALYSIS (Moved from PoliticalTopologyStrategy)
    // ============================================================================================

    public static void analyzeContinentExtremities(GeoNamesBuilderNode root) {
        logger.info("\n=== CONTINENT BOUNDING BOX PROVENANCE ANALYSIS ===");
        double epsilon = 1e-5;
        if (root == null || root.children == null) return;

        for (GeoNamesBuilderNode continent : root.children) {
            if (continent.type != GeoNamesBuilderNode.NodeType.CONTINENT || continent.bounds == null) continue;
            Region cReg = continent.bounds;
            List<String> north = new ArrayList<>(); List<String> south = new ArrayList<>();
            List<String> east = new ArrayList<>(); List<String> west = new ArrayList<>();
            
            for (GeoNamesBuilderNode country : continent.children) {
                if (country.bounds == null || country.bounds.getBottomLeft() == null) continue;
                Region childReg = country.bounds;
                if (Math.abs(childReg.getTopRight().getY() - cReg.getTopRight().getY()) < epsilon) north.add(country.name);
                if (Math.abs(childReg.getBottomLeft().getY() - cReg.getBottomLeft().getY()) < epsilon) south.add(country.name);
                if (Math.abs(childReg.getTopRight().getX() - cReg.getTopRight().getX()) < epsilon) east.add(country.name);
                if (Math.abs(childReg.getBottomLeft().getX() - cReg.getBottomLeft().getX()) < epsilon) west.add(country.name);
            }
            logger.info(String.format("Continent: %-15s %s", continent.name, cReg.toShortString()));
            if (!north.isEmpty()) logger.info("  -> NORTH limit: " + String.join(", ", north));
            if (!south.isEmpty()) logger.info("  -> SOUTH limit: " + String.join(", ", south));
            if (!east.isEmpty())  logger.info("  -> EAST  limit: " + String.join(", ", east));
            if (!west.isEmpty())  logger.info("  -> WEST  limit: " + String.join(", ", west));
            logger.info("------------------------------------------------------------");
        }
        logger.info("=== END ANALYSIS ===\n");
    }

    public static void logLeafRegionStatistics(GeoNamesBuilderNode root) {
        long totalLeaves = 0; long zeroSizeIssues = 0; long emptyNodes = 0; long affectedPop = 0;
        Queue<GeoNamesBuilderNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);
        
        while (!queue.isEmpty()) {
            GeoNamesBuilderNode node = queue.poll();
            if (node.children == null || node.children.isEmpty()) {
                totalLeaves++;
                boolean isZeroSize = false;
                if (node.bounds == null || node.bounds.getBottomLeft() == null) isZeroSize = true;
                else if (node.bounds.getWidth() < 1e-6 || node.bounds.getHeight() < 1e-6) isZeroSize = true;
                
                if (isZeroSize) {
                    if (node.aggregatedPopulation > 0) { zeroSizeIssues++; affectedPop += node.aggregatedPopulation; }
                    else emptyNodes++;
                }
            } else { queue.addAll(node.children); }
        }
        double percentIssue = (totalLeaves > 0) ? (100.0 * zeroSizeIssues / totalLeaves) : 0.0;
        logger.info("\n=== LEAF REGION QUALITY ANALYSIS ===");
        logger.info(String.format("Total Leaf Brokers      : %d", totalLeaves));
        logger.info(String.format("Zero-Size Issues        : %d (%.2f%%) [Nodes with Pop > 0]", zeroSizeIssues, percentIssue));
        logger.info(String.format("Empty/Unused Nodes      : %d [Nodes with Pop = 0]", emptyNodes));
        logger.info(String.format("Population at Risk      : %d", affectedPop));
        if (zeroSizeIssues > 0) logger.warning("(!) High number of Point Regions with Population detected.");
        else logger.info("(OK) All populated leaf regions have valid spatial extent.");
        logger.info("====================================\n");
    }

    public static void printNodeTypeStatistics(GeoNamesBuilderNode root) {
        Map<GeoNamesBuilderNode.NodeType, Long> typeCounts = new HashMap<>();
        Map<GeoNamesBuilderNode.NodeType, Long> typePop = new HashMap<>();
        Map<GeoNamesBuilderNode.NodeType, Long> typeNetPop = new HashMap<>();
        Queue<GeoNamesBuilderNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);
        
        while (!queue.isEmpty()) {
            GeoNamesBuilderNode node = queue.poll();
            typeCounts.merge(node.type, 1L, Long::sum);
            typePop.merge(node.type, node.aggregatedPopulation, Long::sum);
            typeNetPop.merge(node.type, node.internetPopulation, Long::sum);
            if (node.children != null) queue.addAll(node.children);
        }
        logger.info("\n=== TOPOLOGY DIAGNOSTICS ===");
        logger.info(String.format("%-12s | %-10s | %-15s | %-15s", "TYPE", "COUNT", "TOTAL POP", "INTERNET POP"));
        logger.info("------------------------------------------------------------");
        for (GeoNamesBuilderNode.NodeType type : GeoNamesBuilderNode.NodeType.values()) {
            logger.info(String.format("%-12s | %-10d | %-15d | %-15d", type, typeCounts.getOrDefault(type, 0L), typePop.getOrDefault(type, 0L), typeNetPop.getOrDefault(type, 0L)));
        }
        logger.info("============================\n");
    }

    // ============================================================================================
    // INTERNAL HELPERS
    // ============================================================================================

    private static void collectNodesAtRelativeDepth(GeoNamesBuilderNode current, int currentDepth, int targetDepth, List<GeoNamesBuilderNode> result) {
        if (currentDepth == targetDepth) {
            result.add(current);
            return;
        }
        if (current.children != null) {
            for (GeoNamesBuilderNode child : current.children) {
                collectNodesAtRelativeDepth(child, currentDepth + 1, targetDepth, result);
            }
        }
    }

    private static class SweepEvent implements Comparable<SweepEvent> {
        enum EventType { START, END }
        final double x;
        final EventType type;
        final GeoNamesBuilderNode node;

        SweepEvent(double x, EventType type, GeoNamesBuilderNode node) {
            this.x = x; this.type = type; this.node = node;
        }
        @Override
        public int compareTo(SweepEvent other) {
            int xCompare = Double.compare(this.x, other.x);
            if (xCompare != 0) return xCompare;
            return this.type.compareTo(other.type);
        }
    }

    private static long calculateOverlapsSweep(List<GeoNamesBuilderNode> nodes) {
        List<SweepEvent> events = new ArrayList<>(nodes.size() * 2);
        for (GeoNamesBuilderNode node : nodes) {
            if (node.bounds == null || node.bounds.getBottomLeft() == null) continue;
            double minLon = node.bounds.getBottomLeft().getX();
            double maxLon = node.bounds.getTopRight().getX();
            
            if (minLon <= maxLon) {
                events.add(new SweepEvent(minLon, SweepEvent.EventType.START, node));
                events.add(new SweepEvent(maxLon, SweepEvent.EventType.END, node));
            } else {
                events.add(new SweepEvent(minLon, SweepEvent.EventType.START, node));
                events.add(new SweepEvent(180.0, SweepEvent.EventType.END, node));
                events.add(new SweepEvent(-180.0, SweepEvent.EventType.START, node));
                events.add(new SweepEvent(maxLon, SweepEvent.EventType.END, node));
            }
        }
        Collections.sort(events);
        
        long overlapCount = 0;
        Map<GeoNamesBuilderNode, Integer> activeSegments = new HashMap<>();
        Set<String> countedPairs = new HashSet<>(); 
        
        for (SweepEvent event : events) {
            GeoNamesBuilderNode eventNode = event.node;
            Region r1 = new Region(eventNode.bounds);
            
            if (event.type == SweepEvent.EventType.START) {
                for (GeoNamesBuilderNode activeNode : activeSegments.keySet()) {
                    if (activeNode == eventNode) continue;
                    String k1 = Integer.toHexString(System.identityHashCode(eventNode));
                    String k2 = Integer.toHexString(System.identityHashCode(activeNode));
                    String pairKey = (k1.compareTo(k2) < 0) ? k1 + "::" + k2 : k2 + "::" + k1;
                    
                    if (countedPairs.contains(pairKey)) continue;
                    Region r2 = new Region(activeNode.bounds);
                    if (r1.intersects(r2)) {
                        overlapCount++;
                        countedPairs.add(pairKey);
                    }
                }
                activeSegments.put(eventNode, activeSegments.getOrDefault(eventNode, 0) + 1);
            } else { 
                int count = activeSegments.getOrDefault(eventNode, 0);
                if (count <= 1) activeSegments.remove(eventNode);
                else activeSegments.put(eventNode, count - 1);
            }
        }
        return overlapCount;
    }
}