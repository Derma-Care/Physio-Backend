package com.clinicadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.clinicadmin.dto.Response;
import com.clinicadmin.service.AnalyticsService;

@RestController
@RequestMapping("/clinic-admin")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/getDoctorReferralAnalytics/{clinicId}/{branchId}/{type}")
    public ResponseEntity<Response> getDoctorReferralAnalytics(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable Integer type) {

        return ResponseEntity.ok(
                analyticsService.getDoctorReferralAnalytics(
                        clinicId,
                        branchId,
                        type,
                        null,
                        null));
    }

    @GetMapping("/getDoctorReferralAnalyticsCustom/{clinicId}/{branchId}/{startDate}/{endDate}")
    public ResponseEntity<Response> getDoctorReferralAnalyticsCustom(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String startDate,
            @PathVariable String endDate) {

        return ResponseEntity.ok(
                analyticsService.getDoctorReferralAnalytics(
                        clinicId,
                        branchId,
                        5,
                        startDate,
                        endDate));
    }
}