package com.cloudsimulation.core;

import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CloudSimIntegration class.
 * Tests individual methods in isolation.
 */
class CloudSimIntegrationTest {

    private CloudSimIntegration cloudSimIntegration;
    private Federation testFederation;
    private CloudSimPlus testSimulation;

    @BeforeEach
    void setUp() {
        cloudSimIntegration = new CloudSimIntegration();

        // Create minimal test federation
        testSimulation = new CloudSimPlus();
        FederationBuilder builder = new FederationBuilder(testSimulation);
        testFederation = builder
                .withDatacenters(1)
                .withHostsPerDatacenter(2)
                .withPesPerHost(4)
                .build();
    }

    @Test
    void testSimulationInitializationWithDefaultTickRate() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        assertNotNull(cloudSimIntegration.getSimulation(), "Simulation should be initialized");
        assertNotNull(cloudSimIntegration.getBroker(), "Broker should be created");
        assertEquals("NOT_STARTED", cloudSimIntegration.getState(), "Initial state should be NOT_STARTED");
    }

    @Test
    void testSimulationInitializationWithCustomTickRate() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation), 0.5);

        assertNotNull(cloudSimIntegration.getSimulation(), "Simulation should be initialized");
        assertNotNull(cloudSimIntegration.getBroker(), "Broker should be created");
    }

    @Test
    void testInitializeWithNullSimulation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cloudSimIntegration.initialize(null, Collections.singletonList(testFederation));
        });

        assertTrue(exception.getMessage().contains("Simulation"));
    }

    @Test
    void testInitializeWithNullFederations() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cloudSimIntegration.initialize(testSimulation, null);
        });

        assertTrue(exception.getMessage().contains("federation"));
    }

    @Test
    void testInitializeWithEmptyFederations() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cloudSimIntegration.initialize(testSimulation, Collections.emptyList());
        });

        assertTrue(exception.getMessage().contains("federation"));
    }

    @Test
    void testInitializeWithInvalidTickRate() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation), -1.0);
        });

        assertTrue(exception.getMessage().contains("tick rate"));
    }

    @Test
    void testBrokerCreationAndLinking() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        assertNotNull(cloudSimIntegration.getBroker(), "Broker should be created");
        assertTrue(cloudSimIntegration.getBroker().getId() >= 0, "Broker should have valid ID");
    }

    @Test
    void testVmSubmissionWithValidVms() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        CloudSimPlus simulation = new CloudSimPlus();
        FederationBuilder builder = new FederationBuilder(simulation);
        List<Vm> vms = builder.getVmFactory().createVms(5);

        List<Vm> submittedVms = cloudSimIntegration.submitVms(vms);

        assertEquals(5, submittedVms.size(), "Should submit all VMs");
    }

    @Test
    void testVmSubmissionWithEmptyList() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        List<Vm> emptyList = Collections.emptyList();
        List<Vm> result = cloudSimIntegration.submitVms(emptyList);

        assertTrue(result.isEmpty(), "Should return empty list for empty input");
    }

    @Test
    void testVmSubmissionWithoutInitialization() {
        CloudSimPlus simulation = new CloudSimPlus();
        FederationBuilder builder = new FederationBuilder(simulation);
        List<Vm> vms = builder.getVmFactory().createVms(5);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            cloudSimIntegration.submitVms(vms);
        });

        assertTrue(exception.getMessage().contains("not initialized"));
    }

    @Test
    void testCloudletSubmissionWithValidCloudlets() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        List<Cloudlet> cloudlets = createTestCloudlets(10);

        assertDoesNotThrow(() -> {
            cloudSimIntegration.submitCloudlets(cloudlets);
        });
    }

    @Test
    void testCloudletSubmissionWithEmptyList() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        List<Cloudlet> emptyList = Collections.emptyList();

        assertDoesNotThrow(() -> {
            cloudSimIntegration.submitCloudlets(emptyList);
        });
    }

    @Test
    void testCloudletSubmissionWithoutInitialization() {
        List<Cloudlet> cloudlets = createTestCloudlets(5);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            cloudSimIntegration.submitCloudlets(cloudlets);
        });

        assertTrue(exception.getMessage().contains("not initialized"));
    }

    @Test
    void testSimulationLifecycle() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        // Initial state
        assertEquals("NOT_STARTED", cloudSimIntegration.getState());
        assertEquals(0.0, cloudSimIntegration.clock(), 0.001);

        // Start simulation
        cloudSimIntegration.start();
        assertEquals("RUNNING", cloudSimIntegration.getState());

        // Run for duration
        cloudSimIntegration.runFor(1.0);
        assertTrue(cloudSimIntegration.clock() >= 0.0, "Clock should advance");

        // Terminate
        cloudSimIntegration.terminate();
    }

    @Test
    void testStartWithoutInitialization() {
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            cloudSimIntegration.start();
        });

        assertTrue(exception.getMessage().contains("not initialized"));
    }

    @Test
    void testRunForWithoutStart() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            cloudSimIntegration.runFor(1.0);
        });

        assertTrue(exception.getMessage().contains("not running"));
    }

    @Test
    void testRunForWithInvalidDuration() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));
        cloudSimIntegration.start();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cloudSimIntegration.runFor(-1.0);
        });

        assertTrue(exception.getMessage().contains("positive"));
    }

    @Test
    void testIsFinishedBeforeStart() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        assertFalse(cloudSimIntegration.isFinished(), "Should not be finished before start");
    }

    @Test
    void testClockBeforeInitialization() {
        assertEquals(0.0, cloudSimIntegration.clock(), 0.001, "Clock should be 0 before initialization");
    }

    @Test
    void testGetSimulationBeforeInitialization() {
        assertNull(cloudSimIntegration.getSimulation(), "Simulation should be null before initialization");
    }

    @Test
    void testGetBrokerBeforeInitialization() {
        assertNull(cloudSimIntegration.getBroker(), "Broker should be null before initialization");
    }

    /**
     * Creates simple test cloudlets.
     *
     * @param count Number of cloudlets to create
     * @return List of test cloudlets
     */
    private List<Cloudlet> createTestCloudlets(int count) {
        List<Cloudlet> cloudlets = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            CloudletSimple cloudlet = new CloudletSimple(i, 10000, 1);
            cloudlet.setUtilizationModelCpu(new UtilizationModelFull());
            cloudlets.add(cloudlet);
        }

        return cloudlets;
    }

    @Test
    void testGetElapsedTimeBeforeStart() {
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        // Before start(), elapsed time should be 0.0
        assertEquals(0.0, cloudSimIntegration.getElapsedTime(), 0.001,
            "Elapsed time should be 0.0 before simulation starts");
    }

    @Test
    void testGetElapsedTimeAfterStart() {
        // Initialize and start simulation
        cloudSimIntegration.initialize(testSimulation, Collections.singletonList(testFederation));

        // Create and submit minimal workload using VmFactory
        FederationBuilder builder = new FederationBuilder(testSimulation);
        List<Vm> vms = builder.getVmFactory().createVms(2);
        cloudSimIntegration.submitVms(vms);

        List<Cloudlet> cloudlets = createTestCloudlets(2);
        cloudSimIntegration.submitCloudlets(cloudlets);

        cloudSimIntegration.start();

        // After start(), elapsed time should be 0.0 at the exact start moment
        double elapsedAtStart = cloudSimIntegration.getElapsedTime();
        assertEquals(0.0, elapsedAtStart, 0.001,
            "Elapsed time should be 0.0 immediately after start");

        // The absolute clock should be a very large number (CloudSim Plus characteristic)
        double absoluteClock = cloudSimIntegration.clock();
        assertTrue(absoluteClock > 1_000_000,
            "CloudSim Plus absolute clock should be very large (> 1 million)");

        // But elapsed time should remain near 0.0
        assertTrue(elapsedAtStart < 1.0,
            "Elapsed time should be < 1.0 at start, but was: " + elapsedAtStart);
    }
}
