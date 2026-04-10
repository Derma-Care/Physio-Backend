package com.clinicadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.clinicadmin.dto.CustomerLoginDTO;
import com.clinicadmin.dto.CustomerOnbordingDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.CustomerOnboardingService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/clinic-admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CustomerOnboardingController {

	@Autowired
	private CustomerOnboardingService customerOnboardingService;
	// ✅ Create / Onboard Customer
    @PostMapping("/customers/onboard")
    public ResponseEntity<Response> onboardCustomer(@Valid @RequestBody CustomerOnbordingDTO dto) {
        Response response = customerOnboardingService.onboardCustomer(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ✅ Get All Customers
    @GetMapping("/customers/getAllCustomers")
    public ResponseEntity<Response> getAllCustomers() {
        Response response = customerOnboardingService.getAllCustomers();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ✅ Get Customers by HospitalId
    @GetMapping("/customers/hospital/{hospitalId}")
    public ResponseEntity<Response> getCustomersByHospitalId(@PathVariable String hospitalId) {
        Response response = customerOnboardingService.getCustomersByHospitalId(hospitalId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ✅ Get Customers by BranchId
    @GetMapping("/customers/branch/{branchId}")
    public ResponseEntity<Response> getCustomersByBranchId(@PathVariable String branchId) {
        Response response = customerOnboardingService.getCustomersByBranchId(branchId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ✅ Get Customers by HospitalId & BranchId
    @GetMapping("/customers/hospital/{hospitalId}/branch/{branchId}")
    public ResponseEntity<Response> getCustomersByHospitalIdAndBranchId(@PathVariable String hospitalId,
                                                                        @PathVariable String branchId) {
        Response response = customerOnboardingService.getCustomersByHospitalIdAndBranchId(hospitalId, branchId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ✅ Get Customer By ID   
    @GetMapping("/customers/id/{customerId}")
    public ResponseEntity<Response> getCustomerById(@PathVariable String customerId) {
        Response response = customerOnboardingService.getCustomerById(customerId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @GetMapping("/customers/mobileNumber/{mobileNumber}")
    public ResponseEntity<Response> getCustomerByMobileNumber(@PathVariable String mobileNumber) {
        Response response = customerOnboardingService.getCustomerByMobiileNumber(mobileNumber);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @GetMapping("/customer/patientId/{patientId}/{clinicId}")
    public ResponseEntity<Response> getCustomerByPatientId(@PathVariable String patientId,@PathVariable String clinicId) {
        Response response = customerOnboardingService.getCustomersByPatientId(patientId,clinicId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ✅ Update Customer
    @PutMapping("/customers/updatecustomer/{customerId}")
    public ResponseEntity<Response> updateCustomer(@PathVariable String customerId,
                                                   @RequestBody CustomerOnbordingDTO dto) {
        Response response = customerOnboardingService.updateCustomer(customerId, dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ✅ Delete Customer
    @DeleteMapping("/customers/deletecustomer/{customerId}")
    public ResponseEntity<Response> deleteCustomer(@PathVariable String customerId) {
        Response response = customerOnboardingService.deleteCustomer(customerId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ✅ Login
    @PostMapping("/customers/login")
    public ResponseEntity<Response> login(@RequestBody CustomerLoginDTO dto) {
        Response response = customerOnboardingService.login(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }


//	// ✅ Reset Password with PathVariable
//	@PostMapping("/customers/reset-password/{username}/{oldPassword}/{newPassword}")
//	public ResponseEntity<Response> resetPassword(@PathVariable String username, @PathVariable String oldPassword,
//			@PathVariable String newPassword) {
//		Response response = customerOnboardingService.resetPassword(username, oldPassword, newPassword);
//		return ResponseEntity.status(response.getStatus()).body(response);
//	}
    
    
    @GetMapping("/gcmToken/{token}")
    public CustomerOnbordingDTO getCustomerByToken(
 			 @PathVariable String token ){
 	   return customerOnboardingService.getCustomerByToken(token);
  }
}
