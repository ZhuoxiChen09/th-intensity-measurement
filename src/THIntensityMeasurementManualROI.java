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

public class THIntensityMeasurementManualROI {

    public static void main(String[] args) {
        new ij.ImageJ();

        try {
            PrintWriter writer = new PrintWriter(new FileWriter("TH_Intensity_Results.csv", true));
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.println("Timestamp: " + timeStamp);
            writer.println("ImageName,Region,Area,Mean,IntegratedDensity,RawIntegratedDensity");

            IJ.log("Please open the desired TIFF image in ImageJ first.");

            ImagePlus imp = null;
            while (imp == null) {
                IJ.showStatus("Waiting for image... Please open your TIFF image in ImageJ.");
                imp = WindowManager.getCurrentImage();
                IJ.wait(500);
            }
            IJ.log("Image detected: " + imp.getTitle());
            imp.show();

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

            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Roi line = imp.getRoi();
            if (line == null || !line.isLine()) {
                IJ.error("You must draw a horizontal line ROI before pressing ENTER.");
                System.exit(1);
            }

            double x1 = line.getFloatPolygon().xpoints[0];
            double y1 = line.getFloatPolygon().ypoints[0];
            double x2 = line.getFloatPolygon().xpoints[1];
            double y2 = line.getFloatPolygon().ypoints[1];
            double xCenter = (x1 + x2) / 2;
            double yCenter = (y1 + y2) / 2;

            IJ.run("Set Scale...", "known=8000 unit=\u00b5m pixel=1");
            imp.killRoi();

            Calibration cal = imp.getCalibration();
            double pixelPerMicron = cal.pixelWidth;

            int widthStriatum = (int) Math.round(1500 / pixelPerMicron);
            int heightStriatum = widthStriatum;
            int widthCortex = (int) Math.round(1000 / pixelPerMicron);
            int heightCortex = widthCortex;

            IJ.run("Set Measurements...", "area mean integrated raw redirect=None decimal=3");

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

    private static void waitForManualMeasure() {
        int initialRows = ResultsTable.getResultsTable().size();
        while (ResultsTable.getResultsTable().size() <= initialRows) {
            IJ.wait(500);
        }
    }
}
