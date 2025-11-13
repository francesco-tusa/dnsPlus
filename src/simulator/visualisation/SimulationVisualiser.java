package simulator.visualisation;

import javax.swing.JFrame;
import simulator.regions.Region;
import utils.CustomLogger;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * Manages the Swing visualization window for the simulation.
 * This class is a singleton to provide global access to the UI frame
 * without polluting the simulation logic.
 */
public class SimulationVisualiser {

     private static final Logger logger = CustomLogger.getLogger(SimulationVisualiser.class.getName());

    private static SimulationVisualiser instance;
    private SimWorldMap worldMapPanel;
    private JFrame frame;

    /**
     * Private constructor for singleton pattern.
     */
    private SimulationVisualiser() {
        // This is intentionally left blank.
        // The UI components are created on demand by launch()
        // to ensure they run on the Swing Event Dispatch Thread.
    }

    /**
     * Gets the singleton instance of the visualizer.
     * @return The single SimulationVisualizer instance.
     */
    public static synchronized SimulationVisualiser getInstance() {
        if (instance == null) {
            instance = new SimulationVisualiser();
        }
        return instance;
    }

    /**
     * Creates and displays the visualization JFrame.
     * This should be called from the main thread of the simulation.
     */
    public void launch() {
        // Ensure UI operations run on the Event Dispatch Thread (EDT)
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (frame == null) {
                frame = new JFrame("Simulator World Map");
                worldMapPanel = new SimWorldMap();
                frame.add(worldMapPanel);
                frame.pack();
                
                // Set default close operation to DISPOSE_ON_CLOSE
                // so it doesn't terminate the whole simulation.
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setVisible(true);
            } else {
                // If already launched, just bring it to the front
                frame.setVisible(true);
                frame.toFront();
            }
        });
    }

    /**
     * Updates a region on the map. This method is thread-safe
     * and can be called from the simulation logic.
     *
     * @param name The name of the region (e.g., "Asia").
     * @param region The Region object with coordinate data.
     */
    public void updateRegion(String name, Region region) {
        // Forward the update to the map panel, ensuring it runs on the EDT.
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (worldMapPanel != null) {
                worldMapPanel.updateRegion(name, region);
            }
        });
    }

    /**
     * Saves a snapshot of the current map view to a PNG file.
     * @param simulationTimestamp The unique timestamp of the simulation for the filename.
     */
    public void saveMapImage(String simulationTimestamp) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (worldMapPanel == null) {
                logger.warning("Visualizer: Cannot save map image, panel is not initialized.");
                return;
            }

            int width = worldMapPanel.getWidth();
            int height = worldMapPanel.getHeight();
            if (width <= 0 || height <= 0) {
                 logger.warning("Visualizer: Cannot save map image, panel has zero size.");
                 return;
            }
            
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            
            // Paint the panel and its components onto the image's graphics
            worldMapPanel.paint(g2d);
            g2d.dispose();

            try {
                String dir = "output/maps/";
                String fileName = "topology_map_" + simulationTimestamp + ".png";
                File outputDir = new File(dir);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }
                File outputFile = new File(dir + fileName);
                ImageIO.write(image, "png", outputFile);
                logger.info("Visualiser: Map image saved to " + outputFile.getPath());
            } catch (IOException e) {
                logger.warning("Visualiser: Failed to save map image: " + e.getMessage());
            }
        });
    }

    /**
     * Closes the visualization window.
     */
    public void close() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (frame != null) {
                frame.dispose();
                frame = null;
                worldMapPanel = null;
            }
        });
    }
}