package simulator.visualisation;

import simulator.regions.Region;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A JPanel component that renders a world map and overlays
 * geographic regions from the simulation.
 *
 * (Version 3: Fixes wrap-around label logic and color palette)
 */
public class SimWorldMap extends JPanel {

    private BufferedImage worldMapImage;
    // Use ConcurrentHashMap for thread-safe updates from the simulation
    private final Map<String, Region> regionsToDraw = new ConcurrentHashMap<>();
    
    // --- Distinct color palette ---
    private final Color[] colors = {
        new Color(230, 25, 75, 100),   // Red
        new Color(60, 180, 75, 100),   // Green
        new Color(255, 225, 25, 100),  // Yellow
        new Color(245, 130, 48, 100),  // Orange
        new Color(145, 30, 180, 100),  // Purple
        new Color(0, 130, 200, 100),   // Blue (was Magenta)
        new Color(70, 240, 240, 100)   // Cyan
    };

    public SimWorldMap() {
        try {
            // Load a standard Equirectangular projection map
            URL url = new URL("file:resources/world/world_map.jpg");
            worldMapImage = ImageIO.read(url);
        } catch (IOException e) {
            System.err.println("Could not load map image: " + e.getMessage());
            // Create a fallback placeholder
            worldMapImage = new BufferedImage(1024, 512, BufferedImage.TYPE_INT_RGB);
            Graphics g = worldMapImage.getGraphics();
            g.setColor(new Color(220, 220, 220));
            g.fillRect(0, 0, 1024, 512);
            g.setColor(Color.DARK_GRAY);
            g.drawString("Map Image Not Loaded", 450, 256);
            g.dispose();
        }
        
        if (worldMapImage != null) {
             this.setPreferredSize(new Dimension(worldMapImage.getWidth(), worldMapImage.getHeight()));
        } else {
             this.setPreferredSize(new Dimension(1024, 512));
        }
    }

    /**
     * Adds or updates a region to be visualized. This method is thread-safe.
     */
    public void updateRegion(String name, Region region) {
        if (name != null && region != null) {
            regionsToDraw.put(name, region);
            repaint(); // Trigger UI refresh
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // Enable anti-aliasing for smoother lines
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Draw Background Map
        if (worldMapImage != null) {
            g2d.drawImage(worldMapImage, 0, 0, getWidth(), getHeight(), this);
        }

        // 2. Draw Regions
        int colorIndex = 0;
        // Sort by name to ensure colors are consistent between runs
        java.util.List<String> sortedKeys = new java.util.ArrayList<>(regionsToDraw.keySet());
        java.util.Collections.sort(sortedKeys);
        
        for (String name : sortedKeys) {
            Region r = regionsToDraw.get(name);
            g2d.setColor(colors[colorIndex % colors.length]);
            g2d.setStroke(new BasicStroke(2));

            drawGeoRegion(g2d, r, name);
            colorIndex++;
        }
    }

    /**
     * Handles the logic to convert Geo-coordinates to Screen coordinates
     * and splits the rectangle if it crosses the date line.
     */
    private void drawGeoRegion(Graphics2D g2d, Region r, String name) {
        if (r == null || r.getBottomLeft() == null || r.getTopRight() == null) return;

        double minLon = r.getBottomLeft().getX(); // X = Lon
        double maxLon = r.getTopRight().getX(); // X = Lon
        double minLat = r.getBottomLeft().getY(); // Y = Lat
        double maxLat = r.getTopRight().getY(); // Y = Lat

        // Check for Antimeridian crossing logic
        if (minLon <= maxLon) {
            // Case A: Normal Region
            drawScreenRect(g2d, minLon, maxLon, minLat, maxLat, name);
        } else {
            // Case B: Region Wraps around 180/-180
            
            // --- FIX 1 & 2: Calculate widths to find the larger part ---
            double width1 = 180.0 - minLon; // Width of the right-side box
            double width2 = maxLon - (-180.0); // Width of the left-side box

            if (width1 >= width2) {
                // Part 1 is the main box (e.g., Europe, Oceania)
                drawScreenRect(g2d, minLon, 180.0, minLat, maxLat, name); // Main (with label)
                drawScreenRect(g2d, -180.0, maxLon, minLat, maxLat, ""); // Sliver (no label)
            } else {
                // Part 2 is the main box (e.g., North America)
                drawScreenRect(g2d, minLon, 180.0, minLat, maxLat, ""); // Sliver (no label)
                drawScreenRect(g2d, -180.0, maxLon, minLat, maxLat, name); // Main (with label)
            }
        }
    }

    private void drawScreenRect(Graphics2D g2d, double minLon, double maxLon, double minLat, double maxLat, String label) {
        
        // Save the original (semi-transparent) fill color
        Color originalFillColor = g2d.getColor();

        // Convert to screen coordinates
        int x1 = lonToX(minLon);
        int x2 = lonToX(maxLon);
        int y1 = latToY(maxLat); // Top Y (Screen Y is 0 at top)
        int y2 = latToY(minLat); // Bottom Y

        int width = x2 - x1;
        int height = y2 - y1;

        // 1. Draw the filled rectangle (uses originalFillColor)
        g2d.fillRect(x1, y1, width, height);
        
        // 2. Draw Border (use opaque black for high contrast)
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x1, y1, width, height);
        
        // 3. Draw Label
        if (!label.isEmpty()) {
            g2d.setColor(Color.BLACK);
            // Draw a slight shadow for better readability
            g2d.drawString(label, x1 + 6, y1 + 16); 
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, x1 + 5, y1 + 15); // Main text
        }

        // 4. --- RESTORE THE ORIGINAL FILL COLOR ---
        // This is critical so the next fillRect() call
        // uses the correct semi-transparent color from the loop.
        g2d.setColor(originalFillColor);
    }

    // --- Coordinate Helpers ---
    private int lonToX(double lon) {
        // Map Lon: -180..180 to Screen X: 0..width
        return (int) ((lon + 180.0) * (getWidth() / 360.0));
    }

    private int latToY(double lat) {
        // Map Lat: 90..-90 to Screen Y: 0..height
        return (int) ((90.0 - lat) * (getHeight() / 180.0));
    }
}