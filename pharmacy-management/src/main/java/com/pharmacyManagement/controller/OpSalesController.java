package com.pharmacyManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.pharmacyManagement.dto.OpSalesRequest;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.service.Opservice;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/op-sales")
public class OpSalesController {

	@Autowired
    private Opservice opSalesService;
    
    // ─────────────────────────────────────────────────────────────
    // 1. CREATE
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/createOpSales")
    public ResponseEntity<Response> createOpSales(
            @RequestBody OpSalesRequest request) {

        log.info("REST request to create OP Sales");
        return opSalesService.createOpSales(request);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. UPDATE
    // ─────────────────────────────────────────────────────────────
    @PutMapping("/updateSale")
    public ResponseEntity<Response> updateOpSales(
            @RequestBody OpSalesRequest request) {

        log.info("REST request to update OP Sales id: {}", request.getBillNo());
        return opSalesService.updateOpSales(request);
    }

    // ─────────────────────────────────────────────────────────────
    // 3. GET ALL
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/getAllOpSales/{clinicId}/{branchId}")
    public ResponseEntity<Response> getAllOpSales(
            @PathVariable String clinicId,
            @PathVariable String branchId) {

        log.info("REST request to fetch all OP Sales");
        return opSalesService.getAllOpSales(clinicId, branchId);
    }

    // ─────────────────────────────────────────────────────────────
    // 4a. GET BY BILL NO
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/bill/{billNo}")
    public ResponseEntity<Response> getByBillNo(
            @PathVariable String billNo) {

        log.info("REST request to fetch OP Sales by billNo: {}", billNo);
        return opSalesService.getByBillNo(billNo);
    }

    // ─────────────────────────────────────────────────────────────
    // 4b. GET BY ID
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/id/{id}")
    public ResponseEntity<Response> getById(
            @PathVariable String id) {

        log.info("REST request to fetch OP Sales by id: {}", id);
        return opSalesService.getById(id);
    }

    // ─────────────────────────────────────────────────────────────
    // 5. GET BY OPNO
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/{clinicId}/{branchId}/opno/{opNo}/{mobileNumber}")
    public ResponseEntity<Response> getByOpNo(
    		 @PathVariable String clinicId,
    		 @PathVariable String branchId,
    		 @PathVariable String opNo,
    		 @PathVariable String mobileNumber) {

        log.info("REST request to fetch OP Sales by opNo");
        System.out.println(opNo); 
        System.out.println(mobileNumber);
        return opSalesService.getByOpNo(clinicId, branchId, opNo,mobileNumber);
    }

    // ─────────────────────────────────────────────────────────────
    // 6. DELETE
    // ─────────────────────────────────────────────────────────────
    @DeleteMapping("/{clinicId}/{branchId}/{id}")
    public ResponseEntity<Response> deleteOpSales(
            @PathVariable String id,
            @PathVariable String clinicId,
            @PathVariable String branchId) {

        log.info("REST request to delete OP Sales id: {}", id);
        return opSalesService.deleteOpSales(clinicId, branchId, id);
    }

    // ─────────────────────────────────────────────────────────────
    // 7. FILTER
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/filter")
    public ResponseEntity<Response> filterOpSales(
            @RequestParam(required = false) String clinicId,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String consultingDoctor,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        log.info("REST request to filter OP Sales");

        return opSalesService.filterOpSales(
                clinicId,
                branchId,
                billNo,
                patientName,
                mobile,
                consultingDoctor,
                fromDate,
                toDate
        );
    }
}