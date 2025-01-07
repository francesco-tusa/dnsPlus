package experiments.measurement;

import java.time.Duration;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousPublicationMeasurementListener extends AsynchronousMeasurementListener {
    void publicationMeasurementPerformed(Duration duration);
}