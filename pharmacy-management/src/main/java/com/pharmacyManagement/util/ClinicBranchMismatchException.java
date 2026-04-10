package com.pharmacyManagement.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the clinicId or branchId in the request
 * does not match the record stored in the database.
 *
 * HTTP Status: 403 FORBIDDEN
 *
 * Example:
 *   throw new ClinicBranchMismatchException("CLN001", "BR001");
 *   throw new ClinicBranchMismatchException("CLN001", "BR001", "501");
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ClinicBranchMismatchException extends RuntimeException {

    private final String clinicId;
    private final String branchId;
    private final String recordId;

    // Constructor 1 — simple message
    public ClinicBranchMismatchException(String message) {
        super(message);
        this.clinicId = null;
        this.branchId = null;
        this.recordId = null;
    }

    // Constructor 2 — clinicId + branchId
    // Produces: "Access denied: record does not belong to clinicId 'CLN001' and branchId 'BR001'."
    public ClinicBranchMismatchException(String clinicId, String branchId) {
        super(String.format(
            "Access denied: record does not belong to clinicId '%s' and branchId '%s'.",
            clinicId, branchId));
        this.clinicId = clinicId;
        this.branchId = branchId;
        this.recordId = null;
    }

    // Constructor 3 — clinicId + branchId + recordId
    // Produces: "Access denied: record '501' does not belong to clinicId 'CLN001' and branchId 'BR001'."
    public ClinicBranchMismatchException(String clinicId, String branchId, String recordId) {
        super(String.format(
            "Access denied: record '%s' does not belong to clinicId '%s' and branchId '%s'.",
            recordId, clinicId, branchId));
        this.clinicId = clinicId;
        this.branchId = branchId;
        this.recordId = recordId;
    }

    public String getClinicId() { return clinicId; }
    public String getBranchId() { return branchId; }
    public String getRecordId() { return recordId; }
}

