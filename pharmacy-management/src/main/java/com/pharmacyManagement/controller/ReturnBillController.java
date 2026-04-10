package com.pharmacyManagement.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.dto.ReturnBillDTO;
import com.pharmacyManagement.service.ReturnBillService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/return-bill")
@RequiredArgsConstructor
public class ReturnBillController {

	@Autowired
    private  ReturnBillService service;

    // =========================
    //  CREATE RETURN BILL
    // =========================
    @PostMapping("/createReturnBill")
    public ResponseEntity<Response> createReturnBill(@RequestBody ReturnBillDTO dto) {

        Response res = service.createReturnBill(dto);

        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================
    //  GET BY RECEIPT NO
    // =========================
    @GetMapping("getByreceiptNo/{receiptNo}")
    public ResponseEntity<Response> getByreceiptNo(@PathVariable String receiptNo) {

        Response res = service.getByreceiptNo(receiptNo);

        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================
    //  GET BY CLINIC + BRANCH
    // =========================
    @GetMapping("/getReturnByClinicIdAndBranchId/{clinicId}/{branchId}")
    public ResponseEntity<Response> getByClinicIdAndBranchId(
            @PathVariable String clinicId,
            @PathVariable String branchId) {

        Response res = service.getByClinicIdAndBranchId(clinicId, branchId);

        return ResponseEntity.status(res.getStatus()).body(res);
    }
    // =========================
    //  UPDATE BY RECEIPT NO 
    // =========================
    @PutMapping("updateByReceiptNo/{receiptNo}")
    public ResponseEntity<Response> updateByReceiptNo(
            @PathVariable String receiptNo,
            @RequestBody ReturnBillDTO dto) {

        Response res = service.updateByReceiptNo(receiptNo, dto);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    // =========================
    //  GET BY CLINICID  BRANCHID AND RECEIPT NO
    // =========================
    @GetMapping("/getByClinicIdBranchIdAndReceiptNo/{clinicId}/{branchId}/{receiptNo}")
    public ResponseEntity<Response> getByClinicIdBranchIdAndReceiptNo(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String receiptNo) {

        Response res = service.getByClinicBranchAndReceiptNo(clinicId, branchId, receiptNo);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================
    //  DELETE BY RECEIPT NO
    // =========================
    @DeleteMapping("/deleteByreceiptNo/{receiptNo}")
    public ResponseEntity<Response> deleteByreceiptNo(@PathVariable String receiptNo) {

        Response res = service.deleteByreceiptNo(receiptNo);

        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
