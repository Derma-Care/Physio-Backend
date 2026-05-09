package com.dermaCare.customerService.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
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


}
