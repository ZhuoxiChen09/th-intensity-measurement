/**
 * -----------------------------------------------------------------------------
 *  THIntensityMeasurementManualROI.java
 *  Author: Zhuoxi (Derrick) Chen
 *  Affiliation: Department of Neurobiology, University of California San Diego
 *  Date: 2025-04-30
 * -----------------------------------------------------------------------------
 *  Description:
 *  This ImageJ/Java plugin automates tyrosine hydroxylase (TH) intensity
 *  quantification in MitoPark mouse brain slices. It provides a semi-automated
 *  workflow that standardizes ROI placement, scale calibration, and CSV export
 *  for dopaminergic neuron integrity analysis.
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
import ij.gui.Roi;
import ij.gui.ImageWindow;
import ij.measure.ResultsTable;
import ij.measure.Calibration;
import ij.WindowManager;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * THIntensityMeasurementManualROI
 * 
 * This ImageJ plugin automates the measurement of tyrosine hydroxylase (TH)
 * intensity in MitoPark mouse brain slices using manually placed ROIs.
 */
public class THIntensityMeasurementManualROI {

    public static void main(String[] args) {
        new ij.ImageJ();

        // Prepare CSV file for output
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("TH_Intensity_Results.csv", true));
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.println("Timestamp: " + timeStamp);
            writer.println("ImageName,Region,Area,Mean,IntegratedDensity,RawIntegratedDensity");

            // Wait for user to open an image
            IJ.log("Please open the desired TIFF image in ImageJ first.");

            ImagePlus imp = null;
            while (imp == null) {
                IJ.showStatus("Waiting for image... Please open your TIFF image in ImageJ.");
                imp = WindowManager.getCurrentImage();
                IJ.wait(500);
            }
            // Image is now open
            IJ.log("Image detected: " + imp.getTitle());
            imp.show();

            // Prompt user to draw horizontal line for scale
            IJ.log("STEP 1: Draw a horizontal line across the entire brain width (from left to right).");
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
            Roi line = imp.getRoi();
            if (line == null || !line.isLine()) {
                IJ.error("You must draw a horizontal line ROI before pressing ENTER.");
                System.exit(1);
            }

            // Calculate center point of the line
            double x1 = line.getFloatPolygon().xpoints[0];
            double y1 = line.getFloatPolygon().ypoints[0];
            double x2 = line.getFloatPolygon().xpoints[1];
            double y2 = line.getFloatPolygon().ypoints[1];
            double xCenter = (x1 + x2) / 2;
            double yCenter = (y1 + y2) / 2;

            // Set scale based on 8000 µm
            IJ.run("Set Scale...", "known=8000 unit=\u00b5m pixel=1");
            imp.killRoi();

            Calibration cal = imp.getCalibration();
            double pixelPerMicron = cal.pixelWidth;

            // Define ROI sizes in pixels
            int widthStriatum = (int) Math.round(1500 / pixelPerMicron);
            int heightStriatum = widthStriatum;
            int widthCortex = (int) Math.round(1000 / pixelPerMicron);
            int heightCortex = widthCortex;

            IJ.run("Set Measurements...", "area mean integrated raw redirect=None decimal=3");

            // Measure each region
            measureRegion(imp, writer, "Right Striatum", (int)(xCenter + 500 / pixelPerMicron), (int)(yCenter + 300 / pixelPerMicron), widthStriatum, heightStriatum);
            measureRegion(imp, writer, "Right Cortex", (int)(xCenter + 500 / pixelPerMicron), (int)(yCenter - 400 / pixelPerMicron), widthCortex, heightCortex);
            measureRegion(imp, writer, "Left Striatum", (int)(xCenter - 1500 / pixelPerMicron), (int)(yCenter + 300 / pixelPerMicron), widthStriatum, heightStriatum);
            measureRegion(imp, writer, "Left Cortex", (int)(xCenter - 1500 / pixelPerMicron), (int)(yCenter - 400 / pixelPerMicron), widthCortex, heightCortex);

            writer.println();
            writer.close();

            IJ.showMessage("Done!", "All four regions measured and saved to CSV. Exiting ImageJ.");
            IJ.run("Quit");
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * measureRegion
     * @param imp
     * @param writer
     * @param region
     * @param x
     * @param y
     * @param width
     * @param height
     */
    private static void measureRegion(ImagePlus imp, PrintWriter writer, String region, int x, int y, int width, int height) {
        IJ.log("STEP: A fixed-size box for " + region + " has been created. Please move the box to the correct location (do NOT resize it), then press 'M' to record the measurement.");
        imp.setRoi(new Roi(x, y, width, height));
        waitForManualMeasure();

        ResultsTable rt = ResultsTable.getResultsTable();
        int lastRow = rt.size() - 1;
        double area = rt.getValue("Area", lastRow);
        double mean = rt.getValue("Mean", lastRow);
        double intDen = rt.getValue("IntDen", lastRow);
        double rawIntDen = rt.getValue("RawIntDen", lastRow);

        writer.printf("%s,%s,%.3f,%.3f,%.3f,%.3f\n",
            imp.getTitle(), region, area, mean, intDen, rawIntDen);
    }
    
    /**
     * waitForManualMeasure
     * Waits for the user to press 'M' to record the measurement.
     */
    private static void waitForManualMeasure() {
        int initialRows = ResultsTable.getResultsTable().size();
        while (ResultsTable.getResultsTable().size() <= initialRows) {
            IJ.wait(500);
        }
    }
}
