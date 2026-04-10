package com.pharmacyManagement.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when trying to delete an OP Sales record that was already deleted.
 *
 * HTTP Status: 410 GONE
 *
 * Example:
 *   throw new OpSalesAlreadyDeletedException("501");
 */
@ResponseStatus(HttpStatus.GONE)
public class OpSalesAlreadyDeletedException extends RuntimeException {

    private final String recordId;

    // Constructor 1 — simple message
    public OpSalesAlreadyDeletedException(String message) {
        super(message);
        this.recordId = null;
    }

    // Constructor 2 — structured with recordId
    // Produces: "OP Sales record '501' has already been deleted."
    public OpSalesAlreadyDeletedException(String recordId, boolean structured) {
        super(String.format("OP Sales record '%s' has already been deleted.", recordId));
        this.recordId = recordId;
    }

    public String getRecordId() { return recordId; }
}

