package com.dermacare.bookingService.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.dermacare.bookingService.dto.BranchDTO;
import com.dermacare.bookingService.util.ResponseStructure;

@FeignClient(name = "adminservice")
public interface AdminServiceClient {

	  @GetMapping("/admin/getBranchById/{branchId}")
	    ResponseEntity<ResponseStructure<BranchDTO>> getBranchById(
	            @PathVariable("branchId") String branchId);

	@GetMapping("/admin/freeFollowUps/{id}")
	public int getFreeFollowUps(@PathVariable("id") String hospitalId) ;

	}
