package com.pharmacyManagement.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.pharmacyManagement.dto.Response;

import feign.FeignException.FeignClientException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
	Response res = new Response();
	
   @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Response> handleResourceNotFound(ResourceNotFoundException ex) {
    	res.setStatus(404);
    	res.setMessage(ex.getMessage());
    	res.setSuccess(false);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(res);
    }

    @ExceptionHandler(DuplicateBillNoException.class)
    public ResponseEntity<Response> handleDuplicateBillNo(DuplicateBillNoException ex) {
    	res.setStatus(404);
    	res.setMessage(ex.getMessage());
    	res.setSuccess(false);
    	return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(res);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });
        res.setStatus(400);
    	res.setMessage(ex.getMessage());
    	res.setData(errors);
    	res.setSuccess(false);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(res);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleGenericException(Exception ex) {
    	res.setStatus(500);
      	res.setMessage(ex.getMessage());
      	res.setSuccess(false);   
       return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(res);
    }
    
    @ExceptionHandler(FeignClientException.class)
    public ResponseEntity<Response> handleGenericException(FeignClientException ex) {
    	res.setStatus(ex.status());
      	res.setMessage(ex.getMessage());
      	res.setSuccess(false);   
       return ResponseEntity
                .status(ex.status())
                .body(res);
    }
}
