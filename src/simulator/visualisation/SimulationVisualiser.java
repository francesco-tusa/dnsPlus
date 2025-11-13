package simulator.visualisation;

import javax.swing.JFrame;
import simulator.regions.Region;

/**
 * Manages the Swing visualization window for the simulation.
 * This class is a singleton to provide global access to the UI frame
 * without polluting the simulation logic.
 */
public class SimulationVisualiser {

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