package com.pharmacyManagement.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.pharmacyManagement.dto.ApiResponse;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.dto.SalesReturnCreateResponse;
import com.pharmacyManagement.dto.SalesReturnFilterRequest;
import com.pharmacyManagement.dto.SalesReturnRequest;
import com.pharmacyManagement.dto.SalesReturnResponse;
import com.pharmacyManagement.dto.SalesReturnSummaryResponse;
import com.pharmacyManagement.dto.SalesReturnUpdateRequest;
import com.pharmacyManagement.enums.ReturnType;
import com.pharmacyManagement.service.SalesReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sales-return")
public class SalesReturnController {

    private final SalesReturnService service;

    /**
     * POST /api/pharmacy/sales-return
     * Create a new sales return
     */
    @PostMapping("/create-sales-return")
    public ResponseEntity<ApiResponse<SalesReturnCreateResponse>> create(
            @Valid @RequestBody SalesReturnRequest request) {

        SalesReturnCreateResponse data = service.createReturn(request);
        if(data != null) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sales return created successfully", data));
        }else {
        	 return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                     .body(ApiResponse.success("sales returns unprocessed", data));
           	
        }}

    /**
     * GET /api/op-sales-return/{returnNo}
     * Fetch full details by return number
     */
    @GetMapping("/get-by-returnNo/{returnNo}")
    public ResponseEntity<ApiResponse<SalesReturnResponse>> getByReturnNo(
            @PathVariable String returnNo) {

        SalesReturnResponse data = service.getByReturnNo(returnNo);
        return ResponseEntity.ok(ApiResponse.success(null, data));
    }

    
    @GetMapping("/getAllSalesReturns")
    public ResponseEntity<Response> getAllSalesReturns() {

     return service.getAllSalesReturns();
    }

    
    @GetMapping("/getAllSales/hospitalId/{hospitalId}/branchId/{branchId}")
    public ResponseEntity<Response> getAllSalesByClinicAndBranchId(@PathVariable String hospitalId,
    		@PathVariable String branchId) {

     return service.getAllSalesReturnsByClinicAndBranchId(hospitalId, branchId);
    }
    /**
     * PUT /api/op-sales-return/{returnNo}
     * Modify an existing return
     */
    @PutMapping("/update/{returnNo}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable String returnNo,
            @Valid @RequestBody SalesReturnUpdateRequest request) {

        service.updateReturn(returnNo, request);
        return ResponseEntity.ok(ApiResponse.successMessage("Return updated successfully"));
    }

    /**
     * DELETE /api/op-sales-return/{returnNo}
     * Cancel (soft-delete) a return
     */
    @DeleteMapping("/cancel/{returnNo}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable String returnNo) {

        service.cancelReturn(returnNo);
        return ResponseEntity.ok(ApiResponse.successMessage("Return cancelled successfully"));
    }

    /**
     * GET /api/op-sales-return/filter
     * Filter returns by patient name, mobile, bill no, type, date range
     */
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<SalesReturnSummaryResponse>>> filter(
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String mobileNo,
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) String returnType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String clinicId,
            @RequestParam(required = false) String branchId) {

        SalesReturnFilterRequest filter = SalesReturnFilterRequest.builder()
                .patientName(patientName)
                .mobileNo(mobileNo)
                .billNo(billNo)
                .returnType(returnType != null ? ReturnType.valueOf(returnType.toUpperCase()) : null)
                .fromDate(fromDate)
                .toDate(toDate)
                .clinicId(clinicId)
                .branchId(branchId)
                .build();

        List<SalesReturnSummaryResponse> data = service.filterReturns(filter);
        return ResponseEntity.ok(ApiResponse.successList(data, data.size()));
    }
}
