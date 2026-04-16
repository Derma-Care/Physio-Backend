package com.clinicadmin.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import com.clinicadmin.dto.Response;

@FeignClient(name = "physiotherapydoctor-service")
public interface PhysiotherapyFeignClient {

    @PutMapping("/api/physiotherapy-doctor/updateSessionFromTherapist/{therapistRecordId}/{sessionId}")
    void updateSessionStatus(
            @PathVariable String therapistRecordId,
            @PathVariable String sessionId);
    
    
    @GetMapping("/api/physiotherapy-doctor/get-record/{clinicId}/{branchId}/{patientId}/{bookingId}/{therapistRecordId}")
    Response getRecord(
            @PathVariable("clinicId") String clinicId,
            @PathVariable("branchId") String branchId,
            @PathVariable("patientId") String patientId,
            @PathVariable("bookingId") String bookingId,
            @PathVariable("therapistRecordId") String therapistRecordId
    );
    
    @GetMapping("/api/physiotherapy-doctor/payment/{bookingId}")
    Response getPayment(@PathVariable("bookingId") String bookingId);


}
