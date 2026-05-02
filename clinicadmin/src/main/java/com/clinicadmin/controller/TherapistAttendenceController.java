package com.clinicadmin.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicadmin.dto.Response;
import com.clinicadmin.service.TherapistAttendenceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clinic-admin")
@RequiredArgsConstructor
public class TherapistAttendenceController {

    private final TherapistAttendenceService service;

    // ✅ DAILY GET
    @GetMapping("/getDaily/{therapistId}/{date}")
    public ResponseEntity<Response> getDaily(
            @PathVariable String therapistId,
            @PathVariable String date) {

        Response response = service.getDailyReport(therapistId, date);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/updateAttendance/{therapistId}")
    public ResponseEntity<Response> updateAttendance(
            @PathVariable String therapistId,
            @RequestBody Map<String, String> body) {

        Response response = service.updateAttendance(therapistId, body);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    // ✅ MONTHLY GET
    @GetMapping("/getMonthly/{therapistId}/{month}")
    public ResponseEntity<Response> getMonthly(
            @PathVariable String therapistId,
            @PathVariable String month) {

        Response response = service.getMonthlyReport(therapistId, month);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @DeleteMapping("/deleteSession/{therapistId}/{date}/{sessionId}")
    public ResponseEntity<Response> deleteSession(
            @PathVariable String therapistId,
            @PathVariable String date,
            @PathVariable String sessionId) {

        Response response = service.deleteSession(therapistId, date, sessionId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
     @PostMapping("/attendance/manual-session/{therapistId}")
    public ResponseEntity<Response> addManualSession(
            @PathVariable String therapistId,
            @RequestBody Map<String, String> body) {

        Response response = service.addManualSession(therapistId, body);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
   
    
    
//    @GetMapping("/getLocation/{latitude}/{longitude}")
//    public ResponseEntity<Response> getLocation(
//            @PathVariable String latitude,
//            @PathVariable String longitude) {
//
//        Response response = new Response();
//
//        try {
//
//            String city = service.getCityFromLatLong(latitude, longitude);
//
//            Map<String, String> data = new HashMap<>();
//            data.put("latitude", latitude);
//            data.put("longitude", longitude);
//            data.put("city", city);
//
//            response.setSuccess(true);
//            response.setMessage("Location fetched successfully");
//            response.setData(data);
//            response.setStatus(200);
//
//        } catch (Exception e) {
//            response.setSuccess(false);
//            response.setMessage(e.getMessage());
//            response.setStatus(500);
//        }
//
//        return ResponseEntity.status(response.getStatus()).body(response);
//    }
}