package com.chiselon.physiotherapydoctor.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import com.chiselon.physiotherapydoctor.dto.BranchDTO;
import com.chiselon.physiotherapydoctor.dto.Response;
import com.chiselon.physiotherapydoctor.dto.ResponseStructure;

@FeignClient(name = "adminservice")

public interface AdminFeignClient {

	@GetMapping("/admin/getClinicById/{clinicId}")
	ResponseEntity<Response> getClinicById(@RequestHeader("Authorization") String token,@PathVariable("clinicId") String clinicId);

	 @GetMapping("/admin/getBranchById/{branchId}")
	    ResponseEntity<ResponseStructure<BranchDTO>> getBranchById(@RequestHeader("Authorization") String token,
	            @PathVariable("branchId") String branchId);

}
