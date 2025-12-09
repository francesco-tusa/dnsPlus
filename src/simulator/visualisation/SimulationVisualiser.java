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

public class SimulationVisualiser {

    private static final Logger logger = CustomLogger.getLogger(SimulationVisualiser.class.getName());
    private static SimulationVisualiser instance;
    private SimWorldMap worldMapPanel;
    private JFrame frame;

    private SimulationVisualiser() {}

    public static synchronized SimulationVisualiser getInstance() {
        if (instance == null) {
            instance = new SimulationVisualiser();
        }
        return instance;
    }

    public boolean isInitialized() {
        return worldMapPanel != null;
    }

    public void launch() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (frame == null) {
                frame = new JFrame("Simulator World Map");
                worldMapPanel = new SimWorldMap();
                frame.add(worldMapPanel);
                frame.pack();
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setVisible(true);
            } else {
                frame.setVisible(true);
                frame.toFront();
            }
        });
    }

    public void updateRegion(String name, Region region) {
        if (worldMapPanel == null) return;
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (worldMapPanel != null) worldMapPanel.updateRegion(name, region);
        });
    }

    /**
     * Saves the current map state to the specified file.
     * @param destinationFile The full path and filename for the output image.
     */
    public void saveMapImage(File destinationFile) {
        if (worldMapPanel == null) return;

        javax.swing.SwingUtilities.invokeLater(() -> {
            if (worldMapPanel == null) return;

            int width = worldMapPanel.getWidth();
            int height = worldMapPanel.getHeight();
            if (width <= 0 || height <= 0) return;
            
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            worldMapPanel.paint(g2d);
            g2d.dispose();

            try {
                // Ensure parent directory exists
                File parent = destinationFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                
                ImageIO.write(image, "png", destinationFile);
                logger.info("Visualiser: Map image saved to " + destinationFile.getAbsolutePath());
            } catch (IOException e) {
                logger.warning("Visualiser: Failed to save map image: " + e.getMessage());
            }
        });
    }

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