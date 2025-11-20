package com.cloudsimulation.algorithms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PolicyManager class.
 * Tests policy registration, retrieval, switching, and thread-safety.
 */
public class PolicyManagerTest {
    private PolicyManager manager;
    private NoOpPolicy noopPolicy;

    @BeforeEach
    public void setup() {
        manager = new PolicyManager();
        noopPolicy = new NoOpPolicy();
    }

    @Test
    public void testAddAndRetrievePolicy() {
        manager.addPolicy("noop", noopPolicy);

        LoadBalancingPolicy retrieved = manager.getPolicy("noop");
        assertNotNull(retrieved, "Retrieved policy should not be null");
        assertEquals("noop", retrieved.getName(), "Policy name should match");
        assertSame(noopPolicy, retrieved, "Should return same policy instance");
    }

    @Test
    public void testAddPolicyWithNullName() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.addPolicy(null, noopPolicy);
        }, "Adding policy with null name should throw IllegalArgumentException");
    }

    @Test
    public void testAddPolicyWithEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.addPolicy("", noopPolicy);
        }, "Adding policy with empty name should throw IllegalArgumentException");
    }

    @Test
    public void testAddPolicyWithNullPolicy() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.addPolicy("test", null);
        }, "Adding null policy should throw IllegalArgumentException");
    }

    @Test
    public void testGetPolicyWithNullName() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.getPolicy(null);
        }, "Getting policy with null name should throw IllegalArgumentException");
    }

    @Test
    public void testGetNonExistentPolicy() {
        LoadBalancingPolicy result = manager.getPolicy("nonexistent");
        assertNull(result, "Getting non-existent policy should return null");
    }

    @Test
    public void testSetActivePolicy() {
        manager.addPolicy("noop", noopPolicy);
        manager.setActivePolicy("noop");

        LoadBalancingPolicy active = manager.getActivePolicy();
        assertNotNull(active, "Active policy should not be null");
        assertEquals("noop", active.getName(), "Active policy name should match");
    }

    @Test
    public void testSetActivePolicyNotRegistered() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.setActivePolicy("nonexistent");
        }, "Setting non-existent policy as active should throw IllegalArgumentException");
    }

    @Test
    public void testGetActivePolicyWhenNoneSet() {
        assertThrows(IllegalStateException.class, () -> {
            manager.getActivePolicy();
        }, "Getting active policy when none set should throw IllegalStateException");
    }

    @Test
    public void testGetActivePolicyName() {
        assertNull(manager.getActivePolicyName(), "Active policy name should be null initially");

        manager.addPolicy("noop", noopPolicy);
        manager.setActivePolicy("noop");

        assertEquals("noop", manager.getActivePolicyName(), "Active policy name should match");
    }

    @Test
    public void testSwitchActivePolicy() {
        NoOpPolicy policy1 = new NoOpPolicy();
        NoOpPolicy policy2 = new NoOpPolicy();

        manager.addPolicy("policy1", policy1);
        manager.addPolicy("policy2", policy2);

        manager.setActivePolicy("policy1");
        assertEquals("policy1", manager.getActivePolicyName(), "First policy name should be active");
        assertSame(policy1, manager.getActivePolicy(), "First policy instance should be active");

        manager.setActivePolicy("policy2");
        assertEquals("policy2", manager.getActivePolicyName(), "Second policy name should be active");
        assertSame(policy2, manager.getActivePolicy(), "Second policy instance should be active");
    }

    @Test
    public void testRemovePolicy() {
        manager.addPolicy("noop", noopPolicy);
        assertTrue(manager.hasPolicy("noop"), "Policy should exist");

        manager.removePolicy("noop");
        assertFalse(manager.hasPolicy("noop"), "Policy should be removed");
        assertNull(manager.getPolicy("noop"), "Removed policy should return null");
    }

    @Test
    public void testRemoveActivePolicy() {
        manager.addPolicy("noop", noopPolicy);
        manager.setActivePolicy("noop");

        assertThrows(IllegalStateException.class, () -> {
            manager.removePolicy("noop");
        }, "Removing active policy should throw IllegalStateException");
    }

    @Test
    public void testGetAllPolicyNames() {
        Set<String> names = manager.getAllPolicyNames();
        assertTrue(names.isEmpty(), "Initially should have no policies");

        manager.addPolicy("policy1", new NoOpPolicy());
        manager.addPolicy("policy2", new NoOpPolicy());

        names = manager.getAllPolicyNames();
        assertEquals(2, names.size(), "Should have 2 policies");
        assertTrue(names.contains("policy1"), "Should contain policy1");
        assertTrue(names.contains("policy2"), "Should contain policy2");
    }

    @Test
    public void testGetPolicyCount() {
        assertEquals(0, manager.getPolicyCount(), "Initially should have 0 policies");

        manager.addPolicy("policy1", new NoOpPolicy());
        assertEquals(1, manager.getPolicyCount(), "Should have 1 policy");

        manager.addPolicy("policy2", new NoOpPolicy());
        assertEquals(2, manager.getPolicyCount(), "Should have 2 policies");
    }

    @Test
    public void testHasPolicy() {
        assertFalse(manager.hasPolicy("noop"), "Policy should not exist initially");

        manager.addPolicy("noop", noopPolicy);
        assertTrue(manager.hasPolicy("noop"), "Policy should exist after adding");

        assertFalse(manager.hasPolicy(null), "hasPolicy with null should return false");
    }

    @Test
    public void testClear() {
        manager.addPolicy("policy1", new NoOpPolicy());
        manager.addPolicy("policy2", new NoOpPolicy());
        manager.setActivePolicy("policy1");

        manager.clear();

        assertEquals(0, manager.getPolicyCount(), "Policy count should be 0 after clear");
        assertNull(manager.getActivePolicyName(), "Active policy should be null after clear");
    }

    @Test
    public void testReplacePolicyWithSameName() {
        NoOpPolicy policy1 = new NoOpPolicy();
        NoOpPolicy policy2 = new NoOpPolicy();

        manager.addPolicy("test", policy1);
        assertSame(policy1, manager.getPolicy("test"), "Should return first policy");

        manager.addPolicy("test", policy2);
        assertSame(policy2, manager.getPolicy("test"), "Should return replaced policy");
    }

    @Test
    public void testThreadSafeConcurrentAdd() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    manager.addPolicy("policy" + index, new NoOpPolicy());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected - some may fail
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertEquals(threadCount, successCount.get(), "All threads should successfully add policies");
        assertEquals(threadCount, manager.getPolicyCount(), "All policies should be registered");
    }

    @Test
    public void testThreadSafePolicySwitching() throws InterruptedException {
        manager.addPolicy("policy1", new NoOpPolicy());
        manager.addPolicy("policy2", new NoOpPolicy());
        manager.setActivePolicy("policy1");

        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String policyName = (i % 2 == 0) ? "policy1" : "policy2";
            new Thread(() -> {
                try {
                    startLatch.await();
                    manager.setActivePolicy(policyName);
                } catch (Exception e) {
                    fail("Setting active policy should not throw exception: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        String activePolicyName = manager.getActivePolicyName();
        assertNotNull(activePolicyName, "Active policy name should not be null after concurrent switching");
        assertTrue(
            activePolicyName.equals("policy1") || activePolicyName.equals("policy2"),
            "Active policy should be one of the two policies"
        );
    }
}
