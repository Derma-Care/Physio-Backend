package com.clinicadmin.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) 
@Builder
public class Response {
	private boolean success;
	private Object data;
	private String message;
	private int status;
	private String hospitalName;
	private String hospitalId;
	private String branchId;
    private String branchName;
	private String role;
	private Map<String, List<String>> permissions;

}