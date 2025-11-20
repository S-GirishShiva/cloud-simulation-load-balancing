package com.cloudsimulation.utils;

import org.cloudsimplus.util.Log;
import ch.qos.logback.classic.Level;

/**
 * Utility class for configuring simulation runtime settings.
 * Provides programmatic control over logging levels for performance optimization.
 */
public class SimulationConfig {

    /**
     * Configures logging levels for CloudSim Plus and application loggers.
     *
     * @param verbose If true, enables INFO level logging for operational visibility.
     *                If false, disables CloudSim Plus logging (Level.OFF) for maximum performance.
     */
    public static void configureLogging(boolean verbose) {
        if (verbose) {
            // Verbose mode: INFO level for operational visibility
            Log.setLevel(Level.INFO);
        } else {
            // Performance mode: Disable all CloudSim Plus logging
            // This provides 5-10x performance improvement in large simulations
            Log.setLevel(Level.OFF);
        }
    }

    /**
     * Configures logging to a specific level for fine-grained control.
     * This overloaded method allows explicit control over CloudSim Plus logging levels,
     * useful for troubleshooting or custom logging requirements.
     *
     * Available levels (from least to most verbose):
     * - Level.OFF: Disables all logging (maximum performance, ~5-10x speedup)
     * - Level.ERROR: Only critical errors are logged
     * - Level.WARN: Warnings and errors are logged
     * - Level.INFO: Informational messages, warnings, and errors (default for verbose mode)
     * - Level.DEBUG: Detailed debugging information
     * - Level.TRACE: Most verbose, includes all internal CloudSim Plus operations
     *
     * Performance impact increases with verbosity level. For large-scale simulations
     * (100+ VMs), consider using Level.OFF or Level.ERROR.
     *
     * @param level The Logback Level to set (OFF, ERROR, WARN, INFO, DEBUG, TRACE)
     * @throws NullPointerException if level is null (handled internally by Log.setLevel)
     */
    public static void configureLogging(Level level) {
        Log.setLevel(level);
    }
}
