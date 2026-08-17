package com.chiselon.clinicadmin.service;

import org.springframework.http.ResponseEntity;

import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.utils.RevenueResponse;

public interface RevenueService {
	
	
	public ResponseEntity<RevenueResponse> getRevenueManagement(
			 String clinicId,
			 String branchId,
			 String number);
	
	public ResponseEntity<RevenueResponse> getRevenueManagementByDateRange(
			 String clinicId,
			 String branchId,
			 String startDate,
			 String endDate);
	
	public ResponseEntity<Response> getRevenueSummary(
			 String clinicId,
			 String branchId);
		
	

}
