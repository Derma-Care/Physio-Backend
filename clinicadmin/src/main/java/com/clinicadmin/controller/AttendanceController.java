package com.clinicadmin.controller;

import com.clinicadmin.dto.AttendanceDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.AttendanceService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clinic-admin")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;

    // ✅ SAVE
    @PostMapping("/saveUserAttendence")
    public ResponseEntity<Response> save(@RequestBody AttendanceDTO dto) {

        Response response = service.save(dto);

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }

    @PutMapping("/updateUserAttendence")
    public ResponseEntity<Response> updateActivity(@RequestBody AttendanceDTO dto) {

        Response response = service.updateActivity(dto);

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }
    // ✅ DAILY 
    @GetMapping("/getUserDailyAttendence/{userId}/{date}")
    public ResponseEntity<Response> getDaily(
            @PathVariable String userId,
            @PathVariable String date) {

        Response response = service.getDaily(userId, date);

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }

    @GetMapping("/getUserMonthlyAttendence/{userId}/{month}")
    public ResponseEntity<Response> getMonthlyReport(
            @PathVariable String userId,
            @PathVariable String month) {

        Response response = service.getMonthlyReport(userId, month);

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }

//    // ✅ CLINIC + BRANCH
//    @GetMapping("/getClinic/{clinicId}/{branchId}/{date}")
//    public ResponseEntity<Response> getByClinicBranch(
//            @PathVariable String clinicId,
//            @PathVariable String branchId,
//            @PathVariable String date) {
//
//        Response response = service.getByClinicBranch(clinicId, branchId, date);
//
//        return ResponseEntity
//                .status(response.getStatus())
//                .body(response);
//    }
    
    @GetMapping("/getAllUsersDailyByClinicAndBranch/{clinicId}/{branchId}/{date}")
    public ResponseEntity<Response> getDailyByClinicAndBranch(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String date) {

        Response response = service.getDailyByClinicAndBranch(
                clinicId,
                branchId,
                date
        );

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }
}