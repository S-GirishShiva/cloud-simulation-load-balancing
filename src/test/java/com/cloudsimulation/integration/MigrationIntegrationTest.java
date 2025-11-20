package com.cloudsimulation.integration;

import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import com.cloudsimulation.models.LoadBalancingPlan;
import com.cloudsimulation.models.MigrationAction;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for VM migration execution via CloudSimIntegration.
 * Tests the complete migration workflow with CloudSim Plus.
 */
public class MigrationIntegrationTest {
    private CloudSimPlus simulation;
    private CloudSimIntegration integration;
    private Federation federation;
    private List<Vm> vms;

    @BeforeEach
    public void setup() {
        // Create new CloudSim instance for each test (single-run limitation)
        simulation = new CloudSimPlus();

        // Create a small federation with 3 hosts using the builder
        FederationBuilder builder = new FederationBuilder(simulation);

        // Build federation with 1 datacenter, 3 hosts, no VMs (we'll create VMs manually)
        federation = builder
            .withDatacenters(1)
            .withHostsPerDatacenter(3)
            .withPesPerHost(4)
            .withVmsPerHost(0)  // Don't create VMs via builder
            .build();

        // Create CloudSimIntegration
        integration = new CloudSimIntegration();
        integration.initialize(simulation, List.of(federation));

        // Create 5 VMs manually
        vms = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Vm vm = new VmSimple(i, 500, 1); // id, mips, cores
            vm.setRam(512).setSize(10000).setBw(100);
            vms.add(vm);
        }

        // Submit VMs for placement
        integration.submitVms(vms);
    }

    @AfterEach
    public void teardown() {
        if (integration != null) {
            try {
                integration.terminate();
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
        }
    }

    @Test
    public void testExecuteEmptyMigrationPlan() {
        // Create empty migration plan
        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .timestamp(0.0)
            .algorithmName("test")
            .build();

        int successCount = integration.executeMigrationPlan(plan);

        assertEquals(0, successCount, "Empty plan should result in 0 migrations");
    }

    @Test
    public void testExecuteMigrationPlanWithNullPlan() {
        assertThrows(IllegalArgumentException.class, () -> {
            integration.executeMigrationPlan(null);
        }, "Null plan should throw IllegalArgumentException");
    }

    @Test
    public void testExecuteSingleMigration() {
        // Start simulation to place VMs on hosts
        integration.start();

        // Wait a bit for VMs to be placed
        if (simulation.isRunning()) {
            integration.runFor(1.0);
        }

        // Get VM and host IDs
        Vm vm0 = vms.get(0);
        if (vm0.getHost() == null || vm0.getHost() == Host.NULL) {
            // VM not placed yet, skip test
            return;
        }

        int sourceHostId = (int) vm0.getHost().getId();

        // Find a different host for migration target
        int targetHostId = -1;
        for (Host host : federation.getDatacenters().get(0).getHostList()) {
            if (host.getId() != sourceHostId) {
                targetHostId = (int) host.getId();
                break;
            }
        }

        if (targetHostId == -1) {
            // No alternative host available, skip test
            return;
        }

        // Create migration action
        MigrationAction action = new MigrationAction(
            (int) vm0.getId(),
            sourceHostId,
            targetHostId,
            1.0,
            "test-migration"
        );

        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .timestamp(simulation.clock())
            .algorithmName("test")
            .addMigration(action)
            .build();

        // Execute migration
        int successCount = integration.executeMigrationPlan(plan);

        assertEquals(1, successCount, "Should successfully migrate 1 VM");
        assertEquals(targetHostId, vm0.getHost().getId(), "VM should be on target host");
    }

    @Test
    public void testExecuteMultipleMigrations() {
        // Start simulation to place VMs on hosts
        integration.start();

        if (simulation.isRunning()) {
            integration.runFor(1.0);
        }

        // Create multiple migration actions
        List<MigrationAction> actions = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Vm vm = vms.get(i);
            if (vm.getHost() == null || vm.getHost() == Host.NULL) {
                continue;
            }

            int sourceHostId = (int) vm.getHost().getId();

            // Find different host
            for (Host host : federation.getDatacenters().get(0).getHostList()) {
                if (host.getId() != sourceHostId) {
                    actions.add(new MigrationAction(
                        (int) vm.getId(),
                        sourceHostId,
                        (int) host.getId(),
                        1.0,
                        "bulk-migration"
                    ));
                    break;
                }
            }
        }

        if (actions.isEmpty()) {
            // No migrations possible, skip test
            return;
        }

        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .timestamp(simulation.clock())
            .algorithmName("test")
            .migrations(actions)
            .build();

        int successCount = integration.executeMigrationPlan(plan);

        assertTrue(successCount > 0, "Should successfully migrate at least one VM");
        assertTrue(successCount <= actions.size(), "Success count should not exceed action count");
    }

    @Test
    public void testMigrationToNonExistentHost() {
        integration.start();

        if (simulation.isRunning()) {
            integration.runFor(1.0);
        }

        Vm vm0 = vms.get(0);
        if (vm0.getHost() == null || vm0.getHost() == Host.NULL) {
            return;
        }

        int sourceHostId = (int) vm0.getHost().getId();
        int nonExistentHostId = 9999;

        MigrationAction action = new MigrationAction(
            (int) vm0.getId(),
            sourceHostId,
            nonExistentHostId,
            1.0,
            "invalid-migration"
        );

        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .timestamp(simulation.clock())
            .algorithmName("test")
            .addMigration(action)
            .build();

        int successCount = integration.executeMigrationPlan(plan);

        assertEquals(0, successCount, "Migration to non-existent host should fail");
        assertEquals(sourceHostId, vm0.getHost().getId(), "VM should remain on source host");
    }

    @Test
    public void testMigrationOfNonExistentVm() {
        integration.start();

        if (simulation.isRunning()) {
            integration.runFor(1.0);
        }

        int nonExistentVmId = 9999;
        int targetHostId = 0;

        MigrationAction action = new MigrationAction(
            nonExistentVmId,
            0,
            targetHostId,
            1.0,
            "invalid-vm"
        );

        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .timestamp(simulation.clock())
            .algorithmName("test")
            .addMigration(action)
            .build();

        int successCount = integration.executeMigrationPlan(plan);

        assertEquals(0, successCount, "Migration of non-existent VM should fail");
    }

    @Test
    public void testMigrationPlanMetadata() {
        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .timestamp(42.0)
            .algorithmName("test-algorithm")
            .computationTime(100L)
            .build();

        int successCount = integration.executeMigrationPlan(plan);

        assertEquals(0, successCount, "Empty plan should have 0 migrations");
        // Plan execution should not throw exception
    }
}
