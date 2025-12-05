# TH Intensity Measurement Tool

A Java-based ImageJ plugin for semi-automated tyrosine hydroxylase (TH) signal intensity measurement from brain slice images. Enables consistent region-of-interest (ROI) placement and scale calibration.

## Requirements

- Java SE 8 or higher (JDK required for compilation)
- ImageJ library included in `lib/` directory
- ImageJ/Fiji (launched automatically by the plugin)

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

## Building and Running

### Compilation

**Windows:**
```bash
javac -cp "lib/*" src/THIntensityMeasurementManualROI.java -d bin
```

**Unix/Linux/macOS:**
```bash
javac -cp "lib/*" src/THIntensityMeasurementManualROI.java -d bin
```

### Execution

**Windows:**
```bash
java -cp "bin;lib/*" THIntensityMeasurementManualROI
```

**Unix/Linux/macOS:**
```bash
java -cp "bin:lib/*" THIntensityMeasurementManualROI
```

Note: Path separators differ between Windows (`;`) and Unix/Linux/macOS (`:`)

### Quick Start

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

- Waits for user to manually open the desired TIFF image
- Sets scale to 8000 µm based on a drawn horizontal line
- Detects Enter key to proceed after calibration
- Automatically places four fixed-size ROIs:
  - Right Striatum (1500×1500 µm)
  - Right Cortex (1000×1000 µm)
  - Left Striatum (1500×1500 µm)
  - Left Cortex (1000×1000 µm)
- Allows manual ROI adjustment before measurement
- Exports results to structured CSV format

---

## Measurement Workflow

1. Open the TIFF image manually in ImageJ
2. Draw a horizontal line across the full width of the brain
3. Press Enter to confirm and set the image scale to 8000 µm
4. The program automatically places ROIs for:
   - Right Striatum → press M to measure
   - Right Cortex → press M to measure
   - Left Striatum → press M to measure
   - Left Cortex → press M to measure
5. Results are appended to the CSV and ImageJ closes automatically

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

This project requires the ImageJ library (`ij.jar`), which is included in the `lib/` directory. The library provides:
- ImageJ API classes for image processing
- ROI (Region of Interest) handling
- Measurement and calibration utilities

To update the library, download from [ImageJ website](https://imagej.nih.gov/ij/download.html) and replace in `lib/` directory.

## Troubleshooting

### ClassNotFoundException or Missing ImageJ Classes

Ensure:
- `ij.jar` exists in the `lib/` directory
- Classpath includes `lib/*` or `lib/ij.jar` during compilation and execution
- Correct path separator is used (`;` for Windows, `:` for Unix/Linux/macOS)

### ImageJ Window Not Appearing

- Verify Java is properly installed
- Ensure sufficient system resources
- Check console output for error messages

---

## License

MIT License © 2025 Zhuoxi (Derrick) Chen