package com.chiselon.bookingService.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.chiselon.bookingService.dto.BranchDTO;
import com.chiselon.bookingService.util.ResponseStructure;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "adminservice")
public interface AdminServiceClient {

	  @GetMapping("/admin/getBranchById/{branchId}")
	    ResponseEntity<ResponseStructure<BranchDTO>> getBranchById(@RequestHeader("Authorization") String token,
	            @PathVariable("branchId") String branchId);
}
