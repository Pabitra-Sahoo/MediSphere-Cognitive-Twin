package com.medisphere.twin.model;

import java.time.Instant;

/**
 * Nested sub-document representing FHIR synchronization metadata in the HealthTwin.
 * Counts as 1 logical completeness field when populated with a non-null syncStatus.
 */
public class TwinFhirSyncStatus {

    private Instant lastSyncTime;
    private int resourceCount;
    private String syncStatus; // SYNCED, PENDING, ERROR

    public TwinFhirSyncStatus() {
        this.syncStatus = "PENDING";
    }

    public TwinFhirSyncStatus(Instant lastSyncTime, int resourceCount, String syncStatus) {
        this.lastSyncTime = lastSyncTime;
        this.resourceCount = resourceCount;
        this.syncStatus = syncStatus;
    }

    public Instant getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(Instant lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public int getResourceCount() {
        return resourceCount;
    }

    public void setResourceCount(int resourceCount) {
        this.resourceCount = resourceCount;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }
}
