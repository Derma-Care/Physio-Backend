package com.pharmacyManagement.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an OP Sales bill is submitted with no medicines.
 *
 * HTTP Status: 400 BAD REQUEST
 *
 * Example:
 *   throw new EmptyMedicineListException();
 *   throw new EmptyMedicineListException("729621");
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmptyMedicineListException extends RuntimeException {

    private final String billNo;

    // Constructor 1 — no args, generic message
    public EmptyMedicineListException() {
        super("Medicines list must not be empty. At least one medicine is required.");
        this.billNo = null;
    }

    // Constructor 2 — with billNo
    // Produces: "Bill '729621' must contain at least one medicine."
    public EmptyMedicineListException(String billNo) {
        super(String.format("Bill '%s' must contain at least one medicine.", billNo));
        this.billNo = billNo;
    }

    public String getBillNo() { return billNo; }
}

