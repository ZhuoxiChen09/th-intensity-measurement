import java.awt.Rectangle;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class RoiGeometryTest {
    public static void main(String[] args) throws IOException {
        Rectangle[] expected = {
            new Rectangle(1062, 547, 188, 188),
            new Rectangle(1062, 460, 125, 125),
            new Rectangle(812, 547, 188, 188),
            new Rectangle(812, 460, 125, 125),
        };
        Rectangle[] actual =
            THIntensityMeasurementManualROI.roiRectangles(500, 500, 1500, 520, 8);
        assert Arrays.equals(expected, actual) : Arrays.toString(actual);

        Path csv = Files.createTempFile("th-intensity-test", ".csv");
        try {
            try (PrintWriter writer = THIntensityMeasurementManualROI.openCsv(csv)) {
                writer.println("first synthetic row");
            }
            try (PrintWriter writer = THIntensityMeasurementManualROI.openCsv(csv)) {
                writer.println("second synthetic row");
            }
            assert Files.readAllLines(csv).equals(Arrays.asList(
                "ImageName,Region,Area,Mean,IntegratedDensity,RawIntegratedDensity,Timestamp",
                "first synthetic row", "second synthetic row"
            ));
            Files.write(csv, Arrays.asList("Timestamp: old format"));
            try (PrintWriter ignored = THIntensityMeasurementManualROI.openCsv(csv)) {
                throw new AssertionError("Appending to a legacy CSV should fail");
            } catch (IOException expectedError) {
                assert Files.readAllLines(csv).equals(Arrays.asList("Timestamp: old format"));
            }
        } finally {
            Files.delete(csv);
        }
        System.out.println("ROI geometry and CSV header checks passed.");
    }
}
