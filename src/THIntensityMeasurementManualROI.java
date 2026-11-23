/**
 * -----------------------------------------------------------------------------
 *  THIntensityMeasurementManualROI.java
 *  Author: Zhuoxi (Derrick) Chen
 *  Affiliation: Department of Neurobiology, University of California San Diego
 *  Date: 2025-04-30
 * -----------------------------------------------------------------------------
 *  Description:
 *  This ImageJ/Java plugin automates tyrosine hydroxylase (TH) intensity
 *  quantification in MitoPark mouse brain slices. It provides a
 *  semi-automated workflow that standardizes ROI placement, scale
 *  calibration, and CSV export for dopaminergic neuron integrity analysis.
 *
 *  Workflow Summary:
 *   - Waits for user to open a TIFF brain slice image.
 *   - User draws a horizontal line to set a 8000 µm spatial scale.
 *   - Automatically places four fixed-size ROIs:
 *       1. Right Striatum
 *       2. Right Cortex
 *       3. Left Striatum
 *       4. Left Cortex
 *   - Allows manual ROI repositioning before measurement.
 *   - Exports integrated density, mean, and area measurements to CSV.
 *
 *  Dependencies:
 *   - ImageJ/Fiji API
 *   - Java SE 8+ environment
 *
 *  License:
 *   MIT License © 2025 Zhuoxi (Derrick) Chen
 * -----------------------------------------------------------------------------
 */
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.ImageWindow;
import ij.measure.ResultsTable;
import ij.measure.Calibration;
import ij.WindowManager;

import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * THIntensityMeasurementManualROI
 * 
 * This ImageJ plugin automates the measurement of tyrosine hydroxylase
 * (TH) intensity in MitoPark mouse brain slices using manually placed
 * ROIs.
 */
public class THIntensityMeasurementManualROI {

    // Configuration constants - modify these values to adjust ROI
    // placement and sizing
    private static final String OUTPUT_CSV_FILENAME =
        "TH_Intensity_Results.csv";
    private static final String CSV_HEADER =
        "ImageName,Region,Area,Mean,IntegratedDensity,RawIntegratedDensity,Timestamp";
    private static final int WAIT_INTERVAL_MS = 500;
    
    // Scale calibration (in micrometers)
    private static final double SCALE_CALIBRATION_UM = 8000.0;
    
    // ROI dimensions (in micrometers)
    private static final double STRIATUM_ROI_SIZE_UM = 1500.0;
    private static final double CORTEX_ROI_SIZE_UM = 1000.0;
    
    // ROI position offsets from center (in micrometers)
    private static final double RIGHT_REGION_X_OFFSET_UM = 500.0;
    private static final double LEFT_REGION_X_OFFSET_UM = 1500.0;
    private static final double STRIATUM_Y_OFFSET_UM = 300.0;
    private static final double CORTEX_Y_OFFSET_UM = 400.0;

    public static void main(String[] args) {
        new ij.ImageJ();

        Path csvPath = Paths.get(OUTPUT_CSV_FILENAME);
        try (PrintWriter writer = openCsv(csvPath)) {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                .format(new Date());

            // Wait for user to open an image
            IJ.log("Please open the desired TIFF image in ImageJ " +
                "first.");

            ImagePlus imp = null;
            while (imp == null) {
                IJ.showStatus(
                    "Waiting for image... Please open your TIFF image " +
                    "in ImageJ.");
                imp = WindowManager.getCurrentImage();
                IJ.wait(WAIT_INTERVAL_MS);
            }
            // Image is now open
            IJ.log("Image detected: " + imp.getTitle());
            imp.show();

            // Prompt user to draw horizontal line for scale
            IJ.log("STEP 1: Draw a horizontal line across the entire " +
                "brain width (from left to right).");
            IJ.log("Press ENTER when done.");

            final Object lock = new Object();
            ImageWindow win = imp.getWindow();
            win.getCanvas().addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                }
            });

            // Wait for user to press ENTER
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Retrieve the drawn line ROI
            Roi calibrationRoi = imp.getRoi();
            if (!(calibrationRoi instanceof Line)) {
                IJ.error("Draw a straight line ROI before pressing ENTER.");
                return;
            }
            Line line = (Line) calibrationRoi;
            if (line.getRawLength() == 0) {
                IJ.error("Draw a calibration line with distinct endpoints.");
                return;
            }

            // Read the line endpoints for ROI geometry and the angle warning.
            double x1 = line.getFloatPolygon().xpoints[0];
            double y1 = line.getFloatPolygon().ypoints[0];
            double x2 = line.getFloatPolygon().xpoints[1];
            double y2 = line.getFloatPolygon().ypoints[1];
            double lineAngle = Math.toDegrees(
                Math.atan2(Math.abs(y2 - y1), Math.abs(x2 - x1)));
            if (lineAngle > 5.0) {
                IJ.log("Warning: the calibration line is more than 5 degrees from horizontal.");
            }

            // Set scale based on calibration distance
            IJ.run("Set Scale...", scaleOptions(line));
            imp.killRoi();

            Calibration cal = imp.getCalibration();
            double micronsPerPixel = cal.pixelWidth;
            Rectangle[] rectangles = roiRectangles(x1, y1, x2, y2, micronsPerPixel);
            String[] regions = {"Right Striatum", "Right Cortex", "Left Striatum", "Left Cortex"};

            IJ.run("Set Measurements...",
                "area mean integrated raw redirect=None " +
                "decimal=3");

            for (int i = 0; i < regions.length; i++) {
                measureRegion(imp, writer, regions[i], rectangles[i], timeStamp);
            }
            if (writer.checkError()) {
                throw new IOException("Could not finish writing " + csvPath);
            }
        } catch (IOException e) {
            IJ.error(e.getMessage());
            return;
        }
        IJ.showMessage("Done!",
            "All four regions measured and saved to CSV. Exiting ImageJ.");
        IJ.run("Quit");
        System.exit(0);
    }

    static String scaleOptions(Line line) {
        // Set Scale expects a pixel distance even when the image is already calibrated.
        return "distance=" + line.getRawLength() + " known=" + SCALE_CALIBRATION_UM +
            " pixel=1 unit=\u00b5m";
    }

    static Rectangle[] roiRectangles(double x1, double y1, double x2, double y2,
            double micronsPerPixel) {
        double xCenter = (x1 + x2) / 2;
        double yCenter = (y1 + y2) / 2;
        int striatumSize = (int) Math.round(STRIATUM_ROI_SIZE_UM / micronsPerPixel);
        int cortexSize = (int) Math.round(CORTEX_ROI_SIZE_UM / micronsPerPixel);
        int rightX = (int) (xCenter + RIGHT_REGION_X_OFFSET_UM / micronsPerPixel);
        int leftX = (int) (xCenter - LEFT_REGION_X_OFFSET_UM / micronsPerPixel);
        int striatumY = (int) (yCenter + STRIATUM_Y_OFFSET_UM / micronsPerPixel);
        int cortexY = (int) (yCenter - CORTEX_Y_OFFSET_UM / micronsPerPixel);
        return new Rectangle[] {
            new Rectangle(rightX, striatumY, striatumSize, striatumSize),
            new Rectangle(rightX, cortexY, cortexSize, cortexSize),
            new Rectangle(leftX, striatumY, striatumSize, striatumSize),
            new Rectangle(leftX, cortexY, cortexSize, cortexSize),
        };
    }

    static PrintWriter openCsv(Path path) throws IOException {
        boolean needsHeader = !Files.exists(path) || Files.size(path) == 0;
        if (!needsHeader) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                if (!CSV_HEADER.equals(reader.readLine())) {
                    throw new IOException("Move the existing CSV to a different filename before appending: " + path);
                }
            }
        }
        PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND));
        if (needsHeader) {
            writer.println(CSV_HEADER);
        }
        return writer;
    }

    private static void measureRegion(ImagePlus imp, PrintWriter writer,
            String region, Rectangle bounds, String timeStamp) {
        IJ.log("STEP: A fixed-size box for " + region +
            " has been created. Please move the box to the correct " +
            "location (do NOT resize it), then press 'M' to record " +
            "the measurement.");
        imp.setRoi(new Roi(bounds.x, bounds.y, bounds.width, bounds.height));
        waitForManualMeasure();

        ResultsTable rt = ResultsTable.getResultsTable();
        int lastRow = rt.size() - 1;
        double area = rt.getValue("Area", lastRow);
        double mean = rt.getValue("Mean", lastRow);
        double intDen = rt.getValue("IntDen", lastRow);
        double rawIntDen = rt.getValue("RawIntDen", lastRow);

        String imageName = "\"" + imp.getTitle().replace("\"", "\"\"") + "\"";
        writer.printf(Locale.ROOT, "%s,%s,%.3f,%.3f,%.3f,%.3f,%s%n",
            imageName, region, area, mean, intDen, rawIntDen, timeStamp);
    }
    
    /**
     * waitForManualMeasure
     * Waits for the user to press 'M' to record the measurement.
     */
    private static void waitForManualMeasure() {
        int initialRows = ResultsTable.getResultsTable().size();
        while (ResultsTable.getResultsTable().size() <=
                initialRows) {
            IJ.wait(WAIT_INTERVAL_MS);
        }
    }
}
