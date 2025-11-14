# TH Intensity Measurement Tool (Manual ROI via ImageJ + Java)

This Java-based ImageJ plugin allows semi-automated TH signal intensity measurement from brain slice images.  
It enables consistent region-of-interest (ROI) placement and scale calibration based on user input.

---

## Features

- Waits for user to manually open the desired TIFF image.
- Sets scale to **8000 µm** based on a drawn horizontal line (no dummy measurement).
- Detects **Enter key** to proceed after the line is drawn.
- Automatically places four fixed-size ROIs:
  - Right Striatum (1500×1500 µm)
  - Right Cortex (1000×1000 µm)
  - Left Striatum (1500×1500 µm)
  - Left Cortex (1000×1000 µm)
- Allows manual adjustment (movement only) of ROIs before measuring.
- Exports all data to a structured CSV file: `TH_Intensity_Results.csv`.

---

## Measurement Workflow

1. **Open the TIFF image** manually in ImageJ.
2. **Draw a horizontal line** across the full width of the brain.
3. **Press the Enter key** (↵) to confirm and set the image scale to 8000 µm.
4. The program places a fixed-size ROI for:
   - Right Striatum → press `M` to measure
   - Right Cortex → press `M` to measure
   - Left Striatum → press `M` to measure
   - Left Cortex → press `M` to measure
5. All results are appended to the CSV and ImageJ closes automatically.

---

## Output Format

```csv
Timestamp: 2025-04-30 15:32:10
ImageName,Region,Area,Mean,IntegratedDensity,RawIntegratedDensity
example_image.tif,Right Striatum,2284500.000,65.000,148500000.000,41000.000
example_image.tif,Right Cortex,1030000.000,37.000,38000000.000,10500.000
example_image.tif,Left Striatum,2272235.000,66.796,149505855.000,41123.000
example_image.tif,Left Cortex,1050681.000,39.339,41332880.000,11369.000
```

*** Command
```
javac -cp "lib/*" src/THIntensityMeasurementManualROI.java -d bin
java -cp "bin;lib/*" THIntensityMeasurementManualROI
```