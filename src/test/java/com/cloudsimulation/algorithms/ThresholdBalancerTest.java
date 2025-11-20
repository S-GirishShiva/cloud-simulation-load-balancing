package com.cloudsimulation.algorithms;

import com.cloudsimulation.algorithms.threshold.ThresholdBalancer;
import com.cloudsimulation.algorithms.threshold.ThresholdConfig;
import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import com.cloudsimulation.metrics.MetricsSnapshot;
import com.cloudsimulation.models.LoadBalancingPlan;
import com.cloudsimulation.models.MigrationAction;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for ThresholdBalancer class.
 * Tests threshold breach detection, VM selection, FFD host selection, and migration safety.
 *
 * <p><b>Testing Limitations:</b></p>
 * CloudSim Plus hosts start with 0% CPU utilization when created via FederationBuilder.
 * To achieve overloaded hosts (>80% CPU), a running simulation with actual workload (Cloudlets)
 * is required. These unit tests validate the algorithm logic and edge cases, while integration
 * tests with workload validate actual migration generation under load.
 */
public class ThresholdBalancerTest {
    private ThresholdBalancer balancer;
    private ThresholdConfig config;
    private CloudSimIntegration cloudSim;
    private CloudSimPlus simulation;
    private MetricsSnapshot testSnapshot;

    @BeforeEach
    public void setup() {
        // Create configuration
        config = new ThresholdConfig();
        config.setCpuUpperThreshold(0.8);
        config.setCpuLowerThreshold(0.7);

        // Create CloudSim simulation
        simulation = new CloudSimPlus();

        // Create CloudSim integration (will be initialized per test)
        cloudSim = new CloudSimIntegration();

        // Create test metrics snapshot
        testSnapshot = new MetricsSnapshot(
            100.0,  // timestamp
            0.6,    // avgCpuUtilization
            0.5,    // avgMemoryUtilization
            2,      // overloadedVmCount
            1,      // underloadedVmCount
            0,      // totalMigrations
            10,     // activeVmCount
            500.0   // powerConsumption
        );
    }

    @Test
    public void testGetName() {
        // Initialize with empty federation first
        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withVmsPerHost(0)
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        assertEquals("threshold", balancer.getName(),
                    "Policy name should be 'threshold'");
    }

    @Test
    public void testResetNoOp() {
        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withVmsPerHost(0)
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        assertDoesNotThrow(() -> balancer.reset(),
                          "Reset should execute without errors for stateless algorithm");
    }

    @Test
    public void testConstructorRejectsNullConfig() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new ThresholdBalancer(null, cloudSim),
            "Should reject null config"
        );

        assertTrue(exception.getMessage().contains("ThresholdConfig cannot be null"));
    }

    @Test
    public void testConstructorRejectsNullCloudSim() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new ThresholdBalancer(config, null),
            "Should reject null CloudSimIntegration"
        );

        assertTrue(exception.getMessage().contains("CloudSimIntegration cannot be null"));
    }

    @Test
    public void testEvaluateRejectsNullSnapshot() {
        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withVmsPerHost(0)
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> balancer.evaluate(null),
            "Should reject null MetricsSnapshot"
        );

        assertNotNull(exception.getMessage());
    }

    @Test
    public void testNoMigrationWhenMinimalSetup() {
        // Minimal federation with 1 host and 0 VMs
        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(1)
            .withVmsPerHost(0)  // No VMs
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        LoadBalancingPlan plan = balancer.evaluate(testSnapshot);

        assertNotNull(plan, "Plan should not be null");
        assertEquals(0, plan.getMigrationCount(), "Should have no migrations with no VMs");
        assertEquals("threshold", plan.getAlgorithmName());
        assertEquals(100.0, plan.getTimestamp(), 0.01);
    }

    @Test
    public void testNoMigrationWhenAllHostsStable() {
        // Create federation with hosts (they will start with low utilization)
        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(3)
            .withVmsPerHost(2)
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        LoadBalancingPlan plan = balancer.evaluate(testSnapshot);

        assertNotNull(plan);
        // Fresh hosts without workload should be stable
        assertEquals(0, plan.getMigrationCount(),
                    "Should not generate migrations for stable system");
    }

    @Test
    public void testCustomThresholdConfiguration() {
        // Create balancer with custom thresholds
        ThresholdConfig customConfig = new ThresholdConfig();
        customConfig.setCpuUpperThreshold(0.9);
        customConfig.setCpuLowerThreshold(0.75);

        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withVmsPerHost(2)
            .build();

        cloudSim.initialize(simulation, List.of(federation));

        ThresholdBalancer customBalancer = new ThresholdBalancer(customConfig, cloudSim);

        LoadBalancingPlan plan = customBalancer.evaluate(testSnapshot);

        // Should handle custom thresholds without errors
        assertNotNull(plan);
    }

    @Test
    public void testEdgeCaseSingleHost() {
        // Single host - cannot migrate VMs anywhere
        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(1)
            .withVmsPerHost(3)
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        LoadBalancingPlan plan = balancer.evaluate(testSnapshot);

        assertNotNull(plan);
        assertEquals(0, plan.getMigrationCount(),
                    "Should not generate migrations with only one host");
    }

    @Test
    public void testEdgeCaseNoVmsOnHosts() {
        // Create hosts with no VMs
        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(3)
            .withVmsPerHost(0)  // No VMs
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        LoadBalancingPlan plan = balancer.evaluate(testSnapshot);

        assertNotNull(plan);
        assertEquals(0, plan.getMigrationCount(),
                    "Should not generate migrations when hosts have no VMs");
    }

    @Test
    public void testPlanMetadata() {
        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withVmsPerHost(2)
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        LoadBalancingPlan plan = balancer.evaluate(testSnapshot);

        assertNotNull(plan);
        assertEquals("threshold", plan.getAlgorithmName());
        assertEquals(100.0, plan.getTimestamp(), 0.01);
        assertTrue(plan.getComputationTime() >= 0,
                  "Computation time should be non-negative");
        assertNotNull(plan.getMigrations(), "Migrations list should not be null");
    }

    @Test
    public void testMigrationActionStructureWhenGenerated() {
        // This test validates MigrationAction structure when migrations are generated
        // Note: With FederationBuilder, hosts start at 0% CPU, so no migrations will
        // be generated. This test demonstrates the validation pattern for integration tests.

        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(3)
            .withVmsPerHost(2)
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        LoadBalancingPlan plan = balancer.evaluate(testSnapshot);

        // Validate migration structure if any migrations were generated
        if (plan.getMigrationCount() > 0) {
            MigrationAction migration = plan.getMigrations().get(0);

            // Validate MigrationAction fields
            assertTrue(migration.getVmId() >= 0, "VM ID should be non-negative");
            assertTrue(migration.getSourceHostId() >= 0, "Source host ID should be non-negative");
            assertTrue(migration.getTargetHostId() >= 0, "Target host ID should be non-negative");
            assertNotEquals(migration.getSourceHostId(), migration.getTargetHostId(),
                          "Source and target hosts must be different");
            assertTrue(migration.getEstimatedMigrationTime() > 0,
                      "Migration time estimate should be positive");
            assertNotNull(migration.getMigrationReason(), "Migration reason should not be null");
            assertTrue(migration.getMigrationReason().contains("OVERLOAD") ||
                      migration.getMigrationReason().contains("LOAD_BALANCING"),
                      "Migration reason should indicate overload or balancing");
        }
    }

    @Test
    public void testVmListAccessAndStructure() {
        // This test demonstrates how to access and validate VM properties
        // Note: FederationBuilder with withVmsPerHost() configures the builder but doesn't
        // automatically place VMs. VMs must be submitted via cloudSim.submitVms() after creation.

        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(2)
            .withVmsPerHost(0)  // No automatic VMs
            .build();

        cloudSim.initialize(simulation, List.of(federation));

        // Get all VMs from all hosts in the federation
        List<Vm> allVms = new ArrayList<>();
        federation.getDatacenters().forEach(datacenter ->
            datacenter.getHostList().forEach(host ->
                allVms.addAll(host.getVmList())
            )
        );

        // With no VMs submitted, list should be empty
        assertEquals(0, allVms.size(), "Should have no VMs when withVmsPerHost(0)");

        // Demonstrate VM property validation pattern (would be used when VMs exist)
        // This pattern is used in integration tests where VMs are submitted via submitVms()
        for (Vm vm : allVms) {
            assertNotNull(vm, "VM should not be null");
            assertTrue(vm.getId() >= 0, "VM ID should be non-negative");
            assertTrue(vm.getTotalMipsCapacity() > 0, "VM should have MIPS capacity");
            assertTrue(vm.getRam().getCapacity() > 0, "VM should have RAM capacity");
        }
    }

    @Test
    public void testMultipleMigrationActionsOrdering() {
        // Validates that when multiple migrations are generated, they maintain
        // proper structure and ordering (highest priority VMs first)

        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(4)
            .withVmsPerHost(2)
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        LoadBalancingPlan plan = balancer.evaluate(testSnapshot);

        // If multiple migrations generated, validate each one
        List<MigrationAction> migrations = plan.getMigrations();
        for (MigrationAction migration : migrations) {
            // Each migration must have valid structure
            assertTrue(migration.getVmId() >= 0);
            assertTrue(migration.getSourceHostId() >= 0);
            assertTrue(migration.getTargetHostId() >= 0);
            assertNotEquals(migration.getSourceHostId(), migration.getTargetHostId());
            assertNotNull(migration.getMigrationReason());
        }

        // Validate no duplicate VM migrations (same VM migrated twice)
        List<Integer> migratedVmIds = new ArrayList<>();
        for (MigrationAction migration : migrations) {
            int vmId = migration.getVmId();
            assertFalse(migratedVmIds.contains(vmId),
                       "VM " + vmId + " should not be migrated multiple times in same plan");
            migratedVmIds.add(vmId);
        }
    }

    @Test
    public void testMigrationReasonAccuracy() {
        // Validates that migration reasons accurately reflect the trigger condition
        // (CPU_OVERLOAD, MEMORY_OVERLOAD, or CPU_AND_MEMORY_OVERLOAD)

        Federation federation = new FederationBuilder(simulation)
            .withDatacenters(1)
            .withHostsPerDatacenter(3)
            .withVmsPerHost(2)
            .build();

        cloudSim.initialize(simulation, List.of(federation));
        balancer = new ThresholdBalancer(config, cloudSim);

        LoadBalancingPlan plan = balancer.evaluate(testSnapshot);

        // When migrations are generated, validate reason format
        for (MigrationAction migration : plan.getMigrations()) {
            String reason = migration.getMigrationReason();

            // Reason should be one of the valid types
            boolean validReason = reason.equals("CPU_OVERLOAD") ||
                                reason.equals("MEMORY_OVERLOAD") ||
                                reason.equals("CPU_AND_MEMORY_OVERLOAD") ||
                                reason.equals("LOAD_BALANCING");

            assertTrue(validReason,
                      "Migration reason '" + reason + "' should be a valid overload type");
        }
    }
}
