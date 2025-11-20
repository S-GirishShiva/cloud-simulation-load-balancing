package com.cloudsimulation.models;

/**
 * Describes a single VM migration action.
 * Immutable data class representing a planned VM migration from one host to another.
 * Used by load balancing algorithms to specify migration decisions.
 */
public class MigrationAction {
    private final int vmId;
    private final int sourceHostId;
    private final int targetHostId;
    private final double estimatedMigrationTime;
    private final String migrationReason;

    /**
     * Constructs a new MigrationAction.
     *
     * @param vmId Virtual machine identifier
     * @param sourceHostId Current host identifier
     * @param targetHostId Destination host identifier
     * @param estimatedMigrationTime Expected duration in seconds
     * @param migrationReason Why migration was triggered (e.g., "overload", "underload", "optimization")
     */
    public MigrationAction(int vmId, int sourceHostId, int targetHostId,
                          double estimatedMigrationTime, String migrationReason) {
        this.vmId = vmId;
        this.sourceHostId = sourceHostId;
        this.targetHostId = targetHostId;
        this.estimatedMigrationTime = estimatedMigrationTime;
        this.migrationReason = migrationReason;
    }

    /**
     * Gets the VM identifier.
     *
     * @return Virtual machine ID
     */
    public int getVmId() {
        return vmId;
    }

    /**
     * Gets the source host identifier.
     *
     * @return Current host ID
     */
    public int getSourceHostId() {
        return sourceHostId;
    }

    /**
     * Gets the target host identifier.
     *
     * @return Destination host ID
     */
    public int getTargetHostId() {
        return targetHostId;
    }

    /**
     * Gets the estimated migration time.
     *
     * @return Expected duration in seconds
     */
    public double getEstimatedMigrationTime() {
        return estimatedMigrationTime;
    }

    /**
     * Gets the migration reason.
     *
     * @return Reason string (e.g., "overload", "underload", "optimization")
     */
    public String getMigrationReason() {
        return migrationReason;
    }

    @Override
    public String toString() {
        return String.format(
            "MigrationAction{vmId=%d, source=%d, target=%d, estTime=%.2fs, reason='%s'}",
            vmId, sourceHostId, targetHostId, estimatedMigrationTime, migrationReason
        );
    }
}
