package com.clinicadmin.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.SalesReturnCreateResponse;
import com.clinicadmin.dto.SalesReturnRequest;
import com.clinicadmin.dto.SalesReturnResponse;
import com.clinicadmin.dto.SalesReturnSummaryResponse;
import com.clinicadmin.dto.SalesReturnUpdateRequest;
import com.clinicadmin.service.SalesReturn;
import com.clinicadmin.utils.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/clinic-admin/sales-return")
public class SalesReturnController {
	
	@Autowired
	private SalesReturn service;
	
	  @PostMapping("/create-sales-return")
	    public ResponseEntity<ApiResponse<SalesReturnCreateResponse>> create(
	            @Valid @RequestBody SalesReturnRequest request) {

	        return service.create(request);
	        }

	    /**
	     * GET /api/op-sales-return/{returnNo}
	     * Fetch full details by return number
	     */
	    @GetMapping("/get-by-returnNo/{returnNo}")
	    public ResponseEntity<ApiResponse<SalesReturnResponse>> getByReturnNo(
	            @PathVariable String returnNo) {

	        return service.getByReturnNo(returnNo);
	         }
	    
	    @GetMapping("/getAllSalesReturns")
	    public ResponseEntity<Response> getAllSalesReturns() {

	        return service.getAllSalesReturns();
	      }
	    
	    @GetMapping("/getAllSalesReturns/clinicId/{clinicId}/branchId/{branchId}")
	    public ResponseEntity<Response> getAllSalesReturnByclinicAndBranchId(@PathVariable String clinicId,@PathVariable String branchId ) {

	        return service.getAllSalesByClinicAndBranchId(clinicId, branchId);
	      }



	    /**
	     * PUT /api/op-sales-return/{returnNo}
	     * Modify an existing return
	     */
	    @PutMapping("/update/{returnNo}")
	    public ResponseEntity<ApiResponse<Void>> update(
	            @PathVariable String returnNo,
	            @Valid @RequestBody SalesReturnUpdateRequest request) {

	        return service.update(returnNo, request);
	         }

	    /**
	     * DELETE /api/op-sales-return/{returnNo}
	     * Cancel (soft-delete) a return
	     */
	    @DeleteMapping("/cancel/{returnNo}")
	    public ResponseEntity<ApiResponse<Void>> cancel(
	            @PathVariable String returnNo) {

	       return service.cancel(returnNo);
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

	        return service.filter(patientName, mobileNo, billNo, returnType, fromDate, toDate, clinicId, branchId);
	       
	    }

}
