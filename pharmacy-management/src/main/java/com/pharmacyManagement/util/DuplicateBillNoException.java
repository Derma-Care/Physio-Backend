package com.pharmacyManagement.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a bill number already exists in the database for the same clinic/branch.
 *
 * HTTP Status: 409 CONFLICT
 *
 * Example:
 *   throw new DuplicateBillNoException("729621");
 *   throw new DuplicateBillNoException("729621", "CLN001", "BR001");
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateBillNoException extends RuntimeException {

    private final String billNo;
    private final String clinicId;
    private final String branchId;

    // Constructor 1 — simple message
    public DuplicateBillNoException(String message) {
        super(message);
        this.billNo    = null;
        this.clinicId  = null;
        this.branchId  = null;
    }

    // Constructor 2 — structured: billNo only
    // Produces: "Bill number '729621' already exists."
    public DuplicateBillNoException(String billNo, boolean structured) {
        super(String.format("Bill number '%s' already exists.", billNo));
        this.billNo   = billNo;
        this.clinicId = null;
        this.branchId = null;
    }

    // Constructor 3 — structured: billNo + clinic + branch
    // Produces: "Bill number '729621' already exists for clinicId 'CLN001' and branchId 'BR001'."
    public DuplicateBillNoException(String billNo, String clinicId, String branchId) {
        super(String.format(
            "Bill number '%s' already exists for clinicId '%s' and branchId '%s'.",
            billNo, clinicId, branchId));
        this.billNo   = billNo;
        this.clinicId = clinicId;
        this.branchId = branchId;
    }

    public String getBillNo()   { return billNo; }
    public String getClinicId() { return clinicId; }
    public String getBranchId() { return branchId; }
}
