package com.chiselon.customerservice.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.chiselon.customerservice.dto.FirstVisitHistoryRequest;
import com.chiselon.customerservice.dto.VisitHistoryRequest;
import com.chiselon.customerservice.util.Response;


@FeignClient(name = "physiotherapydoctor-service")
public interface PhysioFeign {
	
	  @PostMapping("/api/physiotherapy-doctor/visit-history")
	    public ResponseEntity<Response> getVisitHistoryByDoctor(
	    		@RequestHeader("Authorization") String token,   @RequestBody VisitHistoryRequest request);
	  
	  @PostMapping("/api/physiotherapy-doctor/first-visit-history")
	    public ResponseEntity<Response> getFirstVisitHistory(
	    		@RequestHeader("Authorization") String token, @RequestBody FirstVisitHistoryRequest request);
		
	  @GetMapping("/api/physiotherapy-doctor/payment/getExerciseSessionsWithRecords/{clinicId}/{branchId}/{bookingId}/{patientId}/{therapistId}/{therapistRecordId}")
		public ResponseEntity<Response> getExerciseSessionsWithRecords(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
				@PathVariable String branchId, @PathVariable String bookingId, @PathVariable String patientId,
				@PathVariable String therapistId,@PathVariable String therapistRecordId);
	 
	  @GetMapping("/api/physiotherapy-doctor/getDoctorSaveDetailsByCustomerId/{customerId}")
	    public ResponseEntity<Response> getDoctorSaveDetailsByCustomerId(@PathVariable String customerId);
	     
}
