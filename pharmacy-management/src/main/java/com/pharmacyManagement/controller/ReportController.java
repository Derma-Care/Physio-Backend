package com.pharmacyManagement.controller;

import org.springframework.web.bind.annotation.*;

import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/getExpiredMedicineReport/{clinicId}/{branchId}")
    public Response getExpiredMedicineReport(
            @PathVariable String clinicId,
            @PathVariable String branchId) {

        return reportService.getExpiredMedicineReport(clinicId, branchId);
    }
}