# TH Intensity Measurement Tool

## Overview
Automated semi-manual tool for measuring TH signal intensities in brain slice images using ImageJ and Java.

## Features
- Manually set scale based on horizontal brain width (8000 µm)
- Auto-generate fixed-size ROIs for Striatum (1500×1500 µm) and Cortex (1000×1000 µm)
- Move ROIs freely; prevent resizing
- Save measurements automatically to structured CSV
- Auto-close ImageJ after processing

## Usage Instructions
1. Draw a horizontal line across brain width.
2. Press 'M' to set scale.
3. Move the Striatum box into place and press 'M'.
4. Move the Cortex box into place and press 'M'.
5. CSV file updated automatically.

## Requirements
- Java 8 or higher
- ImageJ (preferably Fiji distribution)

## Configurations
- Update `IMAGE_PATH` in the code to point to your input image.

## Output Example
```csv
Timestamp: 2025-04-29 22:30:01
ImageName,Region,Area,Mean,IntegratedDensity,RawIntegratedDensity
90 Sham S2.vsi - 10x.tif,Striatum,2284500.000,65.000,148500000.000,41000.000
90 Sham S2.vsi - 10x.tif,Cortex,1030000.000,37.000,38000000.000,10500.000

## Run
javac -cp "lib/*" src/THIntensityMeasurementManualROI.java -d bin
java -cp "bin;lib/*" THIntensityMeasurementManualROI
```