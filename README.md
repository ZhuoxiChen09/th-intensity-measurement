# TH Intensity Measurement Tool (Manual ROI via ImageJ + Java)

This Java-based ImageJ plugin allows semi-automated TH signal intensity
measurement from brain slice images. It enables consistent
region-of-interest (ROI) placement and scale calibration based on user
input.

---

## Requirements

- **Java SE 8 or higher** (JDK required for compilation)
- **ImageJ library** (`ij.jar`) - included in the `lib/` directory
- **ImageJ/Fiji** - The plugin launches ImageJ automatically when run

---

## Project Structure

```
leutgeb_lab/
├── src/                          # Source code
│   └── THIntensityMeasurementManualROI.java
├── lib/                          # Dependencies
│   └── ij.jar                    # ImageJ library (required)
├── bin/                          # Compiled classes (generated)
└── tifOG/                        # Sample TIFF images
```

---

## Building and Running

### Compilation

Compile the Java source file with the ImageJ library in the classpath:

**Windows:**
```bash
javac -cp "lib/*" src/THIntensityMeasurementManualROI.java -d bin
```

**Unix/Linux/macOS:**
```bash
javac -cp "lib/*" src/THIntensityMeasurementManualROI.java -d bin
```

### Execution

Run the compiled program with the ImageJ library in the classpath:

**Windows:**
```bash
java -cp "bin;lib/*" THIntensityMeasurementManualROI
```

**Unix/Linux/macOS:**
```bash
java -cp "bin:lib/*" THIntensityMeasurementManualROI
```

**Note:** The difference between Windows and Unix-like systems is the
path separator:
- Windows uses semicolon (`;`) to separate paths
- Unix/Linux/macOS uses colon (`:`) to separate paths

### Quick Start (One-liner)

**Windows:**
```bash
javac -cp "lib/*" src/THIntensityMeasurementManualROI.java -d bin && java -cp "bin;lib/*" THIntensityMeasurementManualROI
```

**Unix/Linux/macOS:**
```bash
javac -cp "lib/*" src/THIntensityMeasurementManualROI.java -d bin && java -cp "bin:lib/*" THIntensityMeasurementManualROI
```

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

---

## Dependencies

This project requires the **ImageJ library** (`ij.jar`) which is included
in the `lib/` directory. The `ij.jar` file provides the ImageJ API classes
used by the plugin:

- `ij.IJ` - ImageJ main class for running commands
- `ij.ImagePlus` - Image representation
- `ij.gui.Roi` - Region of Interest handling
- `ij.measure.ResultsTable` - Measurement results
- `ij.measure.Calibration` - Image calibration/scaling

If you need to update or replace `ij.jar`, download it from the
[ImageJ website](https://imagej.nih.gov/ij/download.html) and place it in
the `lib/` directory.

---

## Troubleshooting

### ClassNotFoundException or NoClassDefFoundError

If you encounter errors about missing ImageJ classes, ensure:
1. The `ij.jar` file exists in the `lib/` directory
2. The classpath includes `lib/*` or `lib/ij.jar` when compiling and
   running
3. You're using the correct path separator (`;` for Windows, `:` for
   Unix/Linux/macOS)

### ImageJ Window Not Appearing

The plugin automatically launches ImageJ. If the window doesn't appear:
1. Check that Java is properly installed (`java -version`)
2. Ensure you have sufficient system resources
3. Check console output for error messages

---

## License

MIT License © 2025 Zhuoxi (Derrick) Chen