package com.pharmacyManagement.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.pharmacyManagement.dto.Response;


@FeignClient(value = "clinicadmin")
public interface ClinicAdminFeign {
	
	@GetMapping("/clinic-admin/customers/{customerId}")
    public ResponseEntity<Response> getCustomerById(@PathVariable String customerId);
   
	@GetMapping("/clinic-admin/customers/{mobileNumber}")
    public ResponseEntity<Response> getCustomerByMobileNumber(@PathVariable String mobileNumber);
    
}
