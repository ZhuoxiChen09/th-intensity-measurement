import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.measure.Calibration;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class THIntensityMeasurementManualROI {

    // ===== CONFIG =====
    static final String IMAGE_PATH = "tifOG/90 Sham S2.vsi - 10x.tif"; // <-- Set your image path here

    public static void main(String[] args) {
        new ij.ImageJ(); // Launch ImageJ GUI

        try {
            // ===== CSV Preparation =====
            PrintWriter writer = new PrintWriter(new FileWriter("TH_Intensity_Results.csv", true));

            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.println("Timestamp: " + timeStamp);
            writer.println("ImageName,Region,Area,Mean,IntegratedDensity,RawIntegratedDensity");

            // ===== Open Image =====
            ImagePlus imp = IJ.openImage(IMAGE_PATH);
            if (imp == null) {
                System.err.println("Failed to open image: " + IMAGE_PATH);
                System.exit(1);
            }
            imp.show();

            // ===== Set Scale =====
            IJ.log("STEP 1: Draw a horizontal line across the brain. Then press 'M' to measure.");
            waitForManualMeasure();

            // Get the Line ROI info
            Roi line = imp.getRoi();
            double x1 = line.getFloatPolygon().xpoints[0];
            double y1 = line.getFloatPolygon().ypoints[0];
            double x2 = line.getFloatPolygon().xpoints[1];
            double y2 = line.getFloatPolygon().ypoints[1];

            // Find the center of the line
            double xCenter = (x1 + x2) / 2;
            double yCenter = (y1 + y2) / 2;

            // Set the scale using your line
            IJ.run("Set Scale...", "known=8000 unit=µm pixel=1");
            imp.killRoi(); // Clear the line

            // ===== Get Calibration (pixels/µm) =====
            Calibration cal = imp.getCalibration();
            double pixelPerMicron = cal.pixelWidth;

            int widthStriatum = (int) Math.round(1500 / pixelPerMicron);
            int heightStriatum = widthStriatum;
            int widthCortex = (int) Math.round(1000 / pixelPerMicron);
            int heightCortex = widthCortex;

            // ===== Set Measurements =====
            IJ.run("Set Measurements...", "area mean integrated raw redirect=None decimal=3");

            // ===== STRIATUM Rectangle =====
            IJ.log("STEP 2: Move the 1500×1500 µm STRIATUM box to correct position if needed, then press 'M'.");

            int xStriatum = (int) Math.round(xCenter - widthStriatum / 2);
            int yStriatum = (int) Math.round(yCenter + 300 / pixelPerMicron); // +300 µm below the horizontal line
            imp.setRoi(new Roi(xStriatum, yStriatum, widthStriatum, heightStriatum));
            waitForManualMeasure();

            ResultsTable rt = ResultsTable.getResultsTable();
            int lastRow = rt.size() - 1;
            double areaStriatum = rt.getValue("Area", lastRow);
            double meanStriatum = rt.getValue("Mean", lastRow);
            double intDenStriatum = rt.getValue("IntDen", lastRow);
            double rawIntDenStriatum = rt.getValue("RawIntDen", lastRow);

            writer.printf("%s,%s,%.3f,%.3f,%.3f,%.3f\n",
                imp.getTitle(), "Striatum",
                areaStriatum, meanStriatum, intDenStriatum, rawIntDenStriatum);

            // ===== CORTEX Rectangle =====
            IJ.log("STEP 3: Move the 1000×1000 µm CORTEX box to correct position if needed, then press 'M'.");

            int xCortex = (int) Math.round(xCenter - widthCortex / 2);
            int yCortex = (int) Math.round(yCenter - 400 / pixelPerMicron); // -400 µm above the horizontal line
            imp.setRoi(new Roi(xCortex, yCortex, widthCortex, heightCortex));
            waitForManualMeasure();

            rt = ResultsTable.getResultsTable();
            lastRow = rt.size() - 1;
            double areaCortex = rt.getValue("Area", lastRow);
            double meanCortex = rt.getValue("Mean", lastRow);
            double intDenCortex = rt.getValue("IntDen", lastRow);
            double rawIntDenCortex = rt.getValue("RawIntDen", lastRow);

            writer.printf("%s,%s,%.3f,%.3f,%.3f,%.3f\n",
                imp.getTitle(), "Cortex",
                areaCortex, meanCortex, intDenCortex, rawIntDenCortex);

            // ===== Finish =====
            writer.println(); // Blank line after this image
            writer.close();

            IJ.showMessage("Done!", "Measurements complete. Exiting ImageJ.");
            IJ.run("Quit");
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Waits for the user to manually measure (press "M")
    private static void waitForManualMeasure() {
        int initialRows = ResultsTable.getResultsTable().size();
        while (ResultsTable.getResultsTable().size() <= initialRows) {
            IJ.wait(500);
        }
    }
}
