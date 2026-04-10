package com.dermacare.doctorservice.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.dermacare.doctorservice.dto.Response;

@FeignClient(name = "adminservice")

public interface AdminFeignClient {

	
	    @GetMapping("/admin/getClinicById/{clinicId}")
	    ResponseEntity<Response> getClinicById(@PathVariable("clinicId") String clinicId);
}
