package com.cloudsimulation.benchmarks.benchmarks;

import com.cloudsimulation.benchmarks.core.BenchmarkConfig;
import com.cloudsimulation.benchmarks.core.BenchmarkResult;
import com.cloudsimulation.benchmarks.core.BenchmarkRunner;
import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.core.MemoryOptimizedCloudSim;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import org.cloudsimplus.core.CloudSimPlus;

import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark measuring CloudSim initialization and startup time.
 * Measures the time required to initialize CloudSim, create federations,
 * and prepare the simulation environment.
 */
public class StartupBenchmark implements BenchmarkRunner {

    private static final int FEDERATIONS = 2;
    private static final int DCS_PER_FED = 2;
    private static final int HOSTS_PER_DC = 5;

    private CloudSimPlus simulation;
    private List<Federation> federations;
    private CloudSimIntegration cloudSim;

    @Override
    public String getName() {
        return "startup";
    }

    @Override
    public String getDescription() {
        return "Measures CloudSim initialization and federation setup time";
    }

    @Override
    public void setup() throws Exception {
        // Minimal setup - most work happens in run()
        federations = new ArrayList<>();
    }

    /**
     * Warmup phase is skipped for CloudSim benchmarks.
     * CloudSim Plus simulations can only be started once per instance and cannot be reset or rerun.
     * This benchmark measures cold initialization time, making warmup inappropriate.
     * Sufficient measurement accuracy is achieved through timing multiple initialization phases.
     */
    @Override
    public void warmup(BenchmarkConfig config) {
        // Warmup not applicable - simulation cannot be rerun
    }

    @Override
    public BenchmarkResult run(BenchmarkConfig config) throws Exception {
        long startTime = System.nanoTime();

        // Phase 1: CloudSim initialization
        long phase1Start = System.nanoTime();
        simulation = new CloudSimPlus();
        long phase1End = System.nanoTime();
        double phase1Ms = (phase1End - phase1Start) / 1_000_000.0;

        // Phase 2: Federation creation
        long phase2Start = System.nanoTime();
        for (int i = 0; i < FEDERATIONS; i++) {
            FederationBuilder builder = new FederationBuilder(simulation);
            Federation federation = builder
                    .withDatacenters(DCS_PER_FED)
                    .withHostsPerDatacenter(HOSTS_PER_DC)
                    .withPesPerHost(4)
                    .withVmsPerHost(0)  // Don't create VMs for startup benchmark
                    .build();
            federations.add(federation);
        }
        long phase2End = System.nanoTime();
        double phase2Ms = (phase2End - phase2Start) / 1_000_000.0;

        // Phase 3: CloudSimIntegration initialization
        long phase3Start = System.nanoTime();
        cloudSim = new CloudSimIntegration();
        MemoryOptimizedCloudSim memoryOptimizedCloudSim = new MemoryOptimizedCloudSim();
        memoryOptimizedCloudSim.setCleanupEnabled(true);

        cloudSim.initialize(simulation, federations);
        long phase3End = System.nanoTime();
        double phase3Ms = (phase3End - phase3Start) / 1_000_000.0;

        long endTime = System.nanoTime();
        double totalMs = (endTime - startTime) / 1_000_000.0;

        // Build result
        BenchmarkResult.Builder resultBuilder = new BenchmarkResult.Builder(getName())
                .profile(config.getProfile())
                .addMetric("initialization_time_ms", totalMs)
                .addMetric("cloudsim_creation_ms", phase1Ms)
                .addMetric("federation_creation_ms", phase2Ms)
                .addMetric("integration_init_ms", phase3Ms)
                .addMetadata("federations", String.valueOf(FEDERATIONS))
                .addMetadata("datacenters_per_federation", String.valueOf(DCS_PER_FED))
                .addMetadata("hosts_per_datacenter", String.valueOf(HOSTS_PER_DC));

        // Check if startup time is within acceptable range (< 1 second for small setup)
        if (totalMs > 1000) {
            resultBuilder.failureReason("Startup time " + totalMs + "ms exceeds 1000ms threshold");
        }

        return resultBuilder.build();
    }

    @Override
    public void teardown() throws Exception {
        // Clean up resources
        if (cloudSim != null) {
            cloudSim.terminate();
        }
        federations.clear();
        simulation = null;
        cloudSim = null;
    }
}
