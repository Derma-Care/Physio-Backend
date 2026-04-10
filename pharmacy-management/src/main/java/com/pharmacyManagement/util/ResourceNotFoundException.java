package com.pharmacyManagement.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (OP Sale, Bill, OpNo) is not found in DB.
 *
 * HTTP Status: 404 NOT FOUND
 *
 * Example:
 *   throw new ResourceNotFoundException("OP Sales not found with id: " + id);
 *   throw new ResourceNotFoundException("OP Sales", "billNo", "729621");
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    // Constructor 1 — simple message
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = null;
        this.fieldName    = null;
        this.fieldValue   = null;
    }

    // Constructor 2 — structured: resource + field + value
    // Produces: "OP Sales not found with billNo : '729621'"
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName    = fieldName;
        this.fieldValue   = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName()    { return fieldName; }
    public Object getFieldValue()   { return fieldValue; }
}

