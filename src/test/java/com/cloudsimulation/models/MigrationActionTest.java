package com.cloudsimulation.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MigrationAction class.
 * Tests construction, immutability, and getter methods.
 */
public class MigrationActionTest {

    @Test
    public void testConstructor() {
        MigrationAction action = new MigrationAction(
            10,      // vmId
            5,       // sourceHostId
            7,       // targetHostId
            2.5,     // estimatedMigrationTime
            "overload"  // migrationReason
        );

        assertNotNull(action, "MigrationAction should not be null");
        assertEquals(10, action.getVmId(), "VM ID should match");
        assertEquals(5, action.getSourceHostId(), "Source host ID should match");
        assertEquals(7, action.getTargetHostId(), "Target host ID should match");
        assertEquals(2.5, action.getEstimatedMigrationTime(), 0.001, "Migration time should match");
        assertEquals("overload", action.getMigrationReason(), "Migration reason should match");
    }

    @Test
    public void testImmutability() {
        MigrationAction action = new MigrationAction(1, 2, 3, 1.0, "test");

        // Verify getters return same values
        int vmId1 = action.getVmId();
        int vmId2 = action.getVmId();
        assertEquals(vmId1, vmId2, "Multiple calls to getVmId should return same value");

        int sourceId1 = action.getSourceHostId();
        int sourceId2 = action.getSourceHostId();
        assertEquals(sourceId1, sourceId2, "Multiple calls to getSourceHostId should return same value");

        int targetId1 = action.getTargetHostId();
        int targetId2 = action.getTargetHostId();
        assertEquals(targetId1, targetId2, "Multiple calls to getTargetHostId should return same value");
    }

    @Test
    public void testToString() {
        MigrationAction action = new MigrationAction(15, 3, 8, 1.5, "underload");

        String result = action.toString();

        assertNotNull(result, "toString should not return null");
        assertTrue(result.contains("15"), "toString should contain VM ID");
        assertTrue(result.contains("3"), "toString should contain source host ID");
        assertTrue(result.contains("8"), "toString should contain target host ID");
        assertTrue(result.contains("1.5") || result.contains("1.50"), "toString should contain migration time");
        assertTrue(result.contains("underload"), "toString should contain reason");
    }

    @Test
    public void testZeroMigrationTime() {
        MigrationAction action = new MigrationAction(1, 2, 3, 0.0, "instant");

        assertEquals(0.0, action.getEstimatedMigrationTime(), 0.001, "Zero migration time should be allowed");
    }

    @Test
    public void testNegativeIds() {
        // CloudSim Plus IDs start at 0, but negative IDs might be used for special cases
        MigrationAction action = new MigrationAction(-1, 0, 1, 1.0, "special");

        assertEquals(-1, action.getVmId(), "Negative VM ID should be stored");
    }

    @Test
    public void testLongMigrationReason() {
        String longReason = "This is a very long migration reason that explains in detail " +
                           "why this VM needs to be migrated from its current host to another host " +
                           "due to various resource constraints and optimization goals.";

        MigrationAction action = new MigrationAction(1, 2, 3, 1.0, longReason);

        assertEquals(longReason, action.getMigrationReason(), "Long reason should be preserved");
    }

    @Test
    public void testNullMigrationReason() {
        MigrationAction action = new MigrationAction(1, 2, 3, 1.0, null);

        assertNull(action.getMigrationReason(), "Null reason should be allowed");
    }

    @Test
    public void testEmptyMigrationReason() {
        MigrationAction action = new MigrationAction(1, 2, 3, 1.0, "");

        assertEquals("", action.getMigrationReason(), "Empty reason should be allowed");
    }

    @Test
    public void testSameSourceAndTarget() {
        // Edge case: source and target are the same (no-op migration)
        MigrationAction action = new MigrationAction(1, 5, 5, 0.0, "noop");

        assertEquals(5, action.getSourceHostId(), "Source should be 5");
        assertEquals(5, action.getTargetHostId(), "Target should be 5");
    }

    @Test
    public void testMultipleInstancesIndependent() {
        MigrationAction action1 = new MigrationAction(1, 2, 3, 1.0, "first");
        MigrationAction action2 = new MigrationAction(4, 5, 6, 2.0, "second");

        assertNotEquals(action1.getVmId(), action2.getVmId(), "Different instances should have different VM IDs");
        assertNotEquals(action1.getMigrationReason(), action2.getMigrationReason(), "Different instances should have different reasons");
    }
}
