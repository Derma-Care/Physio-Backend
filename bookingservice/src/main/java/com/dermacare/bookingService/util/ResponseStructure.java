package com.dermacare.bookingService.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseStructure<T> {
	
	private T data;
	private String message;
	private HttpStatus httpStatus;
	private int statusCode;

	 public static <T> ResponseStructure<T> buildResponse(T data, String message, HttpStatus httpStatus, int statusCode) {
	        return new ResponseStructure<>(data, message, httpStatus, statusCode);
	    }
}
