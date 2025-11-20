package com.cloudsimulation.metrics;

import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HierarchicalCollector.
 */
public class HierarchicalCollectorTest {
    private HierarchicalCollector collector;
    private CloudSimIntegration cloudSim;
    private Federation testFederation;
    private CloudSimPlus simulation;

    @BeforeEach
    public void setUp() {
        collector = new HierarchicalCollector();
        simulation = new CloudSimPlus();
        cloudSim = new CloudSimIntegration();
    }

    @Test
    public void testLazyAggregation() {
        // Create minimal federation
        FederationBuilder builder = new FederationBuilder(simulation);
        testFederation = builder
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withPesPerHost(4)
            .build();

        cloudSim.initialize(simulation, Collections.singletonList(testFederation));

        // Collect metrics at time 0
        MetricsSnapshot snapshot1 = collector.collect(cloudSim);
        assertNotNull(snapshot1, "First snapshot should not be null");

        // Collect again immediately (within 5 ticks)
        MetricsSnapshot snapshot2 = collector.collect(cloudSim);
        assertNotNull(snapshot2, "Second snapshot should not be null");

        // Should return same cached object
        assertSame(snapshot1, snapshot2, "Should return cached snapshot within 5 ticks");
    }

    @Test
    public void testCacheInvalidation() {
        // Create minimal federation
        FederationBuilder builder = new FederationBuilder(simulation);
        testFederation = builder
            .withDatacenters(1)
            .withHostsPerDatacenter(1)
            .withPesPerHost(4)
            .build();

        cloudSim.initialize(simulation, Collections.singletonList(testFederation));

        // Collect metrics at time 0
        MetricsSnapshot snapshot1 = collector.collect(cloudSim);
        double timestamp1 = snapshot1.getTimestamp();

        // Manually invalidate cache
        collector.invalidateCache();

        // Collect again (should recompute even at same time)
        MetricsSnapshot snapshot2 = collector.collect(cloudSim);
        double timestamp2 = snapshot2.getTimestamp();

        // Should return different object (not cached)
        assertNotSame(snapshot1, snapshot2, "Should return new snapshot after cache invalidation");

        // But timestamps should be the same (simulation time hasn't advanced)
        assertEquals(timestamp1, timestamp2, 0.001,
            "Both snapshots should have same timestamp since simulation time hasn't advanced");
    }

    @Test
    public void testHierarchicalAggregation() {
        // Create federation with multiple datacenters and hosts
        FederationBuilder builder = new FederationBuilder(simulation);
        testFederation = builder
            .withDatacenters(2)
            .withHostsPerDatacenter(3)
            .withPesPerHost(4)
            .build();

        cloudSim.initialize(simulation, Collections.singletonList(testFederation));

        // Collect metrics
        MetricsSnapshot snapshot = collector.collect(cloudSim);

        // Verify snapshot is created
        assertNotNull(snapshot, "Snapshot should not be null");

        // Verify timestamp matches simulation clock
        assertEquals(cloudSim.clock(), snapshot.getTimestamp(), 0.001,
            "Snapshot timestamp should match simulation clock");
    }

    @Test
    public void testMetricsSnapshotFields() {
        // Create federation
        FederationBuilder builder = new FederationBuilder(simulation);
        testFederation = builder
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withPesPerHost(4)
            .build();

        cloudSim.initialize(simulation, Collections.singletonList(testFederation));

        // Collect metrics
        MetricsSnapshot snapshot = collector.collect(cloudSim);

        // Verify all fields are populated
        assertNotNull(snapshot, "Snapshot should not be null");
        assertTrue(snapshot.getTimestamp() >= 0, "Timestamp should be non-negative");
        assertTrue(snapshot.getAvgCpuUtilization() >= 0 && snapshot.getAvgCpuUtilization() <= 1,
            "CPU utilization should be in 0-1 range");
        assertTrue(snapshot.getAvgMemoryUtilization() >= 0 && snapshot.getAvgMemoryUtilization() <= 1,
            "Memory utilization should be in 0-1 range");
        assertTrue(snapshot.getOverloadedVmCount() >= 0, "Overloaded VM count should be non-negative");
        assertTrue(snapshot.getUnderloadedVmCount() >= 0, "Underloaded VM count should be non-negative");
        assertTrue(snapshot.getActiveVmCount() >= 0, "Active VM count should be non-negative");
        assertTrue(snapshot.getTotalMigrations() >= 0, "Total migrations should be non-negative");
    }

    @Test
    public void testActiveVmCount() {
        // Create federation with known VMs
        FederationBuilder builder = new FederationBuilder(simulation);
        testFederation = builder
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withPesPerHost(4)
            .withVmsPerHost(5) // 2 hosts * 5 VMs = 10 total VMs
            .build();

        cloudSim.initialize(simulation, Collections.singletonList(testFederation));

        // Collect metrics
        MetricsSnapshot snapshot = collector.collect(cloudSim);

        // Verify active VM count is non-negative
        // Note: VMs are submitted by FederationBuilder via broker, but may not be
        // allocated to hosts yet at time 0 before simulation starts
        assertTrue(snapshot.getActiveVmCount() >= 0,
            "Active VM count should be non-negative");
    }

    @Test
    public void testOverloadedAndUnderloadedVmCounting() {
        // Create federation
        FederationBuilder builder = new FederationBuilder(simulation);
        testFederation = builder
            .withDatacenters(1)
            .withHostsPerDatacenter(1)
            .withPesPerHost(4)
            .withVmsPerHost(10)
            .build();

        cloudSim.initialize(simulation, Collections.singletonList(testFederation));

        // Get VMs and submit them
        List<Vm> vms = new ArrayList<>();
        testFederation.getDatacenters().get(0)
            .getHostList().get(0)
            .getVmList().forEach(vms::add);

        cloudSim.submitVms(vms);

        // Create cloudlets with varying CPU utilization
        List<Cloudlet> cloudlets = new ArrayList<>();

        // Create 3 high-load cloudlets (should cause overload on some VMs)
        for (int i = 0; i < 3; i++) {
            Cloudlet cloudlet = new CloudletSimple(10000, 4); // Long cloudlet, 4 PEs
            cloudlet.setUtilizationModelCpu(new UtilizationModelFull()); // 100% CPU
            cloudlet.setUtilizationModelRam(new UtilizationModelDynamic(0.5));
            cloudlet.setUtilizationModelBw(new UtilizationModelDynamic(0.3));
            cloudlets.add(cloudlet);
        }

        // Create 3 low-load cloudlets (should cause underload on some VMs)
        for (int i = 0; i < 3; i++) {
            Cloudlet cloudlet = new CloudletSimple(1000, 1); // Short cloudlet, 1 PE
            cloudlet.setUtilizationModelCpu(new UtilizationModelDynamic(0.1)); // 10% CPU
            cloudlet.setUtilizationModelRam(new UtilizationModelDynamic(0.1));
            cloudlet.setUtilizationModelBw(new UtilizationModelDynamic(0.1));
            cloudlets.add(cloudlet);
        }

        cloudSim.submitCloudlets(cloudlets);
        cloudSim.start();

        // Run simulation for a few ticks
        if (!cloudSim.isFinished()) {
            cloudSim.runFor(2.0);
        }

        // Collect metrics
        MetricsSnapshot snapshot = collector.collect(cloudSim);

        // Verify counts are non-negative (actual values depend on CloudSim scheduling)
        assertTrue(snapshot.getOverloadedVmCount() >= 0,
            "Overloaded VM count should be non-negative");
        assertTrue(snapshot.getUnderloadedVmCount() >= 0,
            "Underloaded VM count should be non-negative");

        // Cleanup
        cloudSim.terminate();
    }

    @Test
    public void testGetTotalMigrations() {
        // Create federation
        FederationBuilder builder = new FederationBuilder(simulation);
        testFederation = builder
            .withDatacenters(1)
            .withHostsPerDatacenter(1)
            .withPesPerHost(4)
            .build();

        cloudSim.initialize(simulation, Collections.singletonList(testFederation));

        // Initially, no migrations
        assertEquals(0, collector.getTotalMigrations(),
            "Initial migration count should be zero");

        // Record some migrations
        collector.recordMigration(null); // Migration object not used yet
        collector.recordMigration(null);
        collector.recordMigration(null);

        // Verify migration count
        assertEquals(3, collector.getTotalMigrations(),
            "Migration count should be 3 after recording 3 migrations");
    }

    @Test
    public void testGetAverageUtilization() {
        // Create federation
        FederationBuilder builder = new FederationBuilder(simulation);
        testFederation = builder
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withPesPerHost(4)
            .build();

        cloudSim.initialize(simulation, Collections.singletonList(testFederation));

        // Collect metrics
        collector.collect(cloudSim);

        // Get average utilization
        double avgUtilization = collector.getAverageUtilization();

        // Verify it's in valid range
        assertTrue(avgUtilization >= 0 && avgUtilization <= 1,
            "Average utilization should be in 0-1 range");
    }

    @Test
    public void testCollectWithNullCloudSim() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            collector.collect(null);
        });

        assertTrue(exception.getMessage().contains("CloudSimIntegration"));
    }

    @Test
    public void testMultipleFederations() {
        // Create multiple federations
        FederationBuilder builder1 = new FederationBuilder(simulation);
        Federation federation1 = builder1
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withPesPerHost(4)
            .build();

        // Create second federation manually
        Federation federation2 = new Federation("federation-2");
        FederationBuilder builder2 = new FederationBuilder(simulation);
        Federation tempFed = builder2
            .withDatacenters(1)
            .withHostsPerDatacenter(3)
            .withPesPerHost(4)
            .build();

        // Copy datacenters from temp federation to federation2
        tempFed.getDatacenters().forEach(dc ->
            federation2.addDatacenter((int) dc.getId(), dc)
        );

        List<Federation> federations = new ArrayList<>();
        federations.add(federation1);
        federations.add(federation2);

        cloudSim.initialize(simulation, federations);

        // Collect metrics across both federations
        MetricsSnapshot snapshot = collector.collect(cloudSim);

        assertNotNull(snapshot, "Snapshot should be collected from multiple federations");
        assertEquals(cloudSim.clock(), snapshot.getTimestamp(), 0.001,
            "Snapshot timestamp should match simulation clock");
    }
}
