package com.pharmacyManagement.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the bill date format is invalid or the date is a future date.
 *
 * HTTP Status: 400 BAD REQUEST
 *
 * Example:
 *   throw new InvalidBillDateException("2026-13-45");
 *   throw new InvalidBillDateException("2026-13-45", "Bill date must be in format YYYY-MM-DD");
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidBillDateException extends RuntimeException {

    private final String billDate;

    // Constructor 1 — simple message
    public InvalidBillDateException(String message) {
        super(message);
        this.billDate = null;
    }

    // Constructor 2 — date + custom reason
    // Produces: "Invalid bill date '2026-13-45': Bill date must be in format YYYY-MM-DD"
    public InvalidBillDateException(String billDate, String reason) {
        super(String.format("Invalid bill date '%s': %s", billDate, reason));
        this.billDate = billDate;
    }

    public String getBillDate() { return billDate; }
}