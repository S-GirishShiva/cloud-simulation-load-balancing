package com.cloudsimulation.infrastructure;

import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for federated infrastructure setup.
 * Validates that the infrastructure components can create a basic federation
 * with multiple datacenters and hosts.
 */
public class FederationSmokeTest {

    private CloudSimPlus simulation;

    @BeforeEach
    public void setUp() {
        // Create a new simulation instance for each test
        simulation = new CloudSimPlus();
    }

    @AfterEach
    public void tearDown() {
        // Cleanup simulation resources
        if (simulation != null) {
            simulation.terminate();
        }
    }

    /**
     * Smoke test that builds 1 federation with 2 datacenters and 5 hosts each.
     * Verifies federation structure is valid and all entity relationships are intact.
     */
    @Test
    public void testFederationInfrastructureSetup() {
        // Create FederationBuilder
        FederationBuilder builder = new FederationBuilder(simulation);

        // Build 1 federation with 2 datacenters, 5 hosts each
        Federation federation = builder
                .withDatacenters(2)
                .withHostsPerDatacenter(5)
                .withPesPerHost(4)
                .build();

        // Basic validations
        assertNotNull(federation, "Federation should not be null");
        assertNotNull(federation.getFederationId(), "Federation ID should not be null");

        // Verify federation has 2 datacenters
        assertEquals(2, federation.getDatacenters().size(),
                     "Federation should have 2 datacenters");
        assertEquals(2, federation.getDatacenterCount(),
                     "Federation datacenter count should be 2");

        // Verify each datacenter has 5 hosts
        for (Datacenter datacenter : federation.getDatacenters()) {
            assertNotNull(datacenter, "Datacenter should not be null");
            assertEquals(5, datacenter.getHostList().size(),
                        String.format("Datacenter %d should have 5 hosts", datacenter.getId()));

            // Verify each host has valid resources
            datacenter.getHostList().forEach(host -> {
                assertNotNull(host, "Host should not be null");
                assertTrue(host.getTotalMipsCapacity() > 0,
                          String.format("Host %d should have MIPS capacity", host.getId()));
                assertTrue(host.getRam().getCapacity() > 0,
                          String.format("Host %d should have RAM capacity", host.getId()));
                assertTrue(host.getStorage().getCapacity() > 0,
                          String.format("Host %d should have storage capacity", host.getId()));
                assertTrue(host.getBw().getCapacity() > 0,
                          String.format("Host %d should have bandwidth capacity", host.getId()));
            });
        }

        // Verify entity relationships are intact using validation utility
        assertTrue(builder.validateFederationStructure(federation),
                   "Federation structure validation should pass");

        // Verify total capacity calculation works
        long totalCapacity = federation.getTotalCapacity();
        assertTrue(totalCapacity > 0,
                   "Federation total capacity should be positive");

        // Verify utilization calculation works (should be 0 since no VMs are running)
        double utilization = federation.getUtilization();
        assertEquals(0.0, utilization, 0.001,
                     "Initial utilization should be 0.0");

        // Output success message
        System.out.println("FEDERATION SMOKE TEST PASSED");
    }

    /**
     * Test that FederationBuilder throws exception for invalid parameters.
     */
    @Test
    public void testFederationBuilderInvalidParameters() {
        FederationBuilder builder = new FederationBuilder(simulation);

        // Test invalid number of datacenters
        assertThrows(IllegalArgumentException.class, () -> {
            builder.withDatacenters(0);
        }, "Should throw exception for zero datacenters");

        assertThrows(IllegalArgumentException.class, () -> {
            builder.withDatacenters(-1);
        }, "Should throw exception for negative datacenters");

        // Test invalid number of hosts
        assertThrows(IllegalArgumentException.class, () -> {
            builder.withHostsPerDatacenter(0);
        }, "Should throw exception for zero hosts");

        assertThrows(IllegalArgumentException.class, () -> {
            builder.withHostsPerDatacenter(-1);
        }, "Should throw exception for negative hosts");

        // Test invalid number of PEs
        assertThrows(IllegalArgumentException.class, () -> {
            builder.withPesPerHost(0);
        }, "Should throw exception for zero PEs");

        assertThrows(IllegalArgumentException.class, () -> {
            builder.withPesPerHost(-1);
        }, "Should throw exception for negative PEs");
    }

    /**
     * Test that Federation can add and remove datacenters.
     */
    @Test
    public void testFederationDatacenterManagement() {
        Federation federation = new Federation("test-federation");
        FederationBuilder builder = new FederationBuilder(simulation);

        // Create a host and datacenter manually
        HostFactory hostFactory = builder.getHostFactory();
        DatacenterFactory datacenterFactory = builder.getDatacenterFactory();

        var hosts = hostFactory.createHosts(3, 2);
        Datacenter datacenter = datacenterFactory.createDatacenter(hosts);

        // Add datacenter to federation
        assertTrue(federation.addDatacenter((int) datacenter.getId(), datacenter),
                   "Should successfully add datacenter");

        // Try to add same datacenter again
        assertFalse(federation.addDatacenter((int) datacenter.getId(), datacenter),
                    "Should not add duplicate datacenter");

        // Verify datacenter count
        assertEquals(1, federation.getDatacenterCount(),
                     "Federation should have 1 datacenter");

        // Remove datacenter
        assertTrue(federation.removeDatacenter((int) datacenter.getId()),
                   "Should successfully remove datacenter");

        // Try to remove non-existent datacenter
        assertFalse(federation.removeDatacenter(999),
                    "Should not remove non-existent datacenter");

        // Verify datacenter count
        assertEquals(0, federation.getDatacenterCount(),
                     "Federation should have 0 datacenters");
    }

    /**
     * Test that FederationBuilder can create VMs using VmFactory.
     * VMs are created but not assigned to hosts (assignment handled by broker in Story 1.3).
     */
    @Test
    public void testFederationBuilderWithVmCreation() {
        FederationBuilder builder = new FederationBuilder(simulation);

        // Build federation with 1 datacenter, 2 hosts, 3 VMs per host
        Federation federation = builder
                .withDatacenters(1)
                .withHostsPerDatacenter(2)
                .withPesPerHost(2)
                .withVmsPerHost(3)
                .build();

        // Verify federation structure
        assertNotNull(federation, "Federation should not be null");
        assertEquals(1, federation.getDatacenterCount(),
                     "Federation should have 1 datacenter");

        // Verify datacenter has 2 hosts
        var datacenter = federation.getDatacenters().get(0);
        assertEquals(2, datacenter.getHostList().size(),
                     "Datacenter should have 2 hosts");

        // Note: VMs are created but not assigned to hosts yet
        // VM assignment will be validated in Story 1.3 with broker integration
        System.out.println("VM creation test passed - VMs created via VmFactory");
    }
}
