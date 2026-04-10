package com.pharmacyManagement.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when medicine data inside the bill is invalid.
 * E.g. negative qty, rate, or mismatched computed totals.
 *
 * HTTP Status: 400 BAD REQUEST
 *
 * Example:
 *   throw new InvalidMedicineDataException("Paracetamol", "qty must be greater than 0");
 *   throw new InvalidMedicineDataException(0, "Paracetamol", "rate must be greater than 0");
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMedicineDataException extends RuntimeException {

    private final String medicineName;
    private final Integer medicineIndex;

    // Constructor 1 — simple message
    public InvalidMedicineDataException(String message) {
        super(message);
        this.medicineName  = null;
        this.medicineIndex = null;
    }

    // Constructor 2 — medicine name + reason
    // Produces: "Invalid medicine data for 'Paracetamol': qty must be greater than 0"
    public InvalidMedicineDataException(String medicineName, String reason) {
        super(String.format("Invalid medicine data for '%s': %s", medicineName, reason));
        this.medicineName  = medicineName;
        this.medicineIndex = null;
    }

    // Constructor 3 — index + medicine name + reason (for list position tracking)
    // Produces: "Invalid medicine data at index [0] for 'Paracetamol': rate must be greater than 0"
    public InvalidMedicineDataException(Integer index, String medicineName, String reason) {
        super(String.format(
            "Invalid medicine data at index [%d] for '%s': %s", index, medicineName, reason));
        this.medicineName  = medicineName;
        this.medicineIndex = index;
    }

    public String  getMedicineName()  { return medicineName; }
    public Integer getMedicineIndex() { return medicineIndex; }
}

