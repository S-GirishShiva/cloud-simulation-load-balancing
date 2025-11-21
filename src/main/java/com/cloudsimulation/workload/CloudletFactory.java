package com.cloudsimulation.workload;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;

/**
 * CloudletFactory provides factory methods for creating standardized Cloudlet instances.
 *
 * This factory ensures consistent cloudlet configuration across the simulation,
 * using the Factory pattern for object creation.
 */
public class CloudletFactory {

    /**
     * Creates a cloudlet with specified parameters.
     *
     * @param id Unique cloudlet identifier
     * @param length Length in MIPS (Million Instructions Per Second)
     * @param fileSize Input data size in MB
     * @param outputSize Output data size in MB
     * @return Configured Cloudlet instance
     */
    public static Cloudlet createCloudlet(int id, long length, long fileSize, long outputSize) {
        // Create cloudlet with: id, length (MIPS), pesNumber (1 PE)
        // Removed ×2 multiplier for alignment with documented MI ranges
        Cloudlet cloudlet = new CloudletSimple(id, length, 1);

        // Configure file sizes
        cloudlet.setFileSize(fileSize);      // Input data size (MB)
        cloudlet.setOutputSize(outputSize);  // Output data size (MB)

        // Set utilization models to 100% (UtilizationModelFull)
        cloudlet.setUtilizationModelCpu(new UtilizationModelFull());   // 100% CPU usage
        cloudlet.setUtilizationModelRam(new UtilizationModelFull());   // 100% RAM usage
        cloudlet.setUtilizationModelBw(new UtilizationModelFull());    // 100% bandwidth usage

        return cloudlet;
    }
}
