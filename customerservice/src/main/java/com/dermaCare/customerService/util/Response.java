package com.dermaCare.customerService.util;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Only include non-null fields in JSON response

public class Response {

	    private String message;
	    private int status; // HTTP status code or custom status
	    private boolean success; // New field to indicate success
	    private Object data; // New field to hold form data

}