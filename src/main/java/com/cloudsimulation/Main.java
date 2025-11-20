package com.cloudsimulation;

import com.cloudsimulation.cli.CommandLineInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for the Cloud Simulation Load Balancing system.
 *
 * Delegates command-line argument parsing and execution to CommandLineInterface.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Cloud Simulation Load Balancing System - Starting");

        try {
            CommandLineInterface cli = new CommandLineInterface();
            int exitCode = cli.execute(args);

            logger.info("Application completed with exit code: {}", exitCode);
            System.exit(exitCode);

        } catch (Exception e) {
            logger.error("Application failed with error", e);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
