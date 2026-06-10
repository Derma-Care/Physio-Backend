package com.dermaCare.customerService.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.dermaCare.customerService.dto.FirstVisitHistoryRequest;
import com.dermaCare.customerService.dto.VisitHistoryRequest;
import com.dermaCare.customerService.util.Response;


@FeignClient(name = "physiotherapydoctor-service")
public interface PhysioFeign {
	
	  @PostMapping("/api/physiotherapy-doctor/visit-history")
	    public ResponseEntity<Response> getVisitHistoryByDoctor(
	            @RequestBody VisitHistoryRequest request);
	  
	  @PostMapping("/api/physiotherapy-doctor/first-visit-history")
	    public ResponseEntity<Response> getFirstVisitHistory(
	            @RequestBody FirstVisitHistoryRequest request);
		
	  @GetMapping("/api/physiotherapy-doctor/payment/getExerciseSessionsWithRecords/{clinicId}/{branchId}/{bookingId}/{patientId}/{therapistId}/{therapistRecordId}")
		public ResponseEntity<Response> getExerciseSessionsWithRecords(@PathVariable String clinicId,
				@PathVariable String branchId, @PathVariable String bookingId, @PathVariable String patientId,
				@PathVariable String therapistId,@PathVariable String therapistRecordId);



}
