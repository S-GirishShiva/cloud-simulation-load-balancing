package com.cloudsimulation.models;

import java.util.Objects;

/**
 * Immutable descriptor for cloudlet specification before instantiation.
 * Used by PatternLibrary to define cloudlet characteristics.
 */
public class CloudletDescriptor {
    private final double arrivalTime;    // Simulation time when cloudlet arrives (seconds)
    private final long length;           // Cloudlet length in MIPS
    private final long fileSize;         // Input file size in MB
    private final long outputSize;       // Output file size in MB

    public CloudletDescriptor(double arrivalTime, long length, long fileSize, long outputSize) {
        this.arrivalTime = arrivalTime;
        this.length = length;
        this.fileSize = fileSize;
        this.outputSize = outputSize;
    }

    public double getArrivalTime() { return arrivalTime; }
    public long getLength() { return length; }
    public long getFileSize() { return fileSize; }
    public long getOutputSize() { return outputSize; }

    @Override
    public String toString() {
        return String.format("CloudletDescriptor{arrivalTime=%.2f, length=%d, fileSize=%d, outputSize=%d}",
            arrivalTime, length, fileSize, outputSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloudletDescriptor that = (CloudletDescriptor) o;
        return Double.compare(that.arrivalTime, arrivalTime) == 0 &&
               length == that.length &&
               fileSize == that.fileSize &&
               outputSize == that.outputSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrivalTime, length, fileSize, outputSize);
    }
}
