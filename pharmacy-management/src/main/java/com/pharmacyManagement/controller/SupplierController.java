package com.pharmacyManagement.controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.dto.SupplierDTO;
import com.pharmacyManagement.dto.SupplierLoginRequest;
import com.pharmacyManagement.service.SupplierService;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/supplier")
@RequiredArgsConstructor

public class SupplierController {

    private static final Logger log = LoggerFactory.getLogger(SupplierController.class);

    private final SupplierService supplierService;

    // ---------------------------------------------------------
    // CREATE SUPPLIER
    // ---------------------------------------------------------
    @PostMapping("/add")
    public ResponseEntity<Response> addSupplier(@RequestBody SupplierDTO dto) {
        log.info("API Call: Add Supplier");
        Response response = supplierService.addSupplier(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------------------------------------------
    // UPDATE SUPPLIER
    // ---------------------------------------------------------
    @PutMapping("/update/{supplierId}")
    public ResponseEntity<Response> updateSupplier(
            @PathVariable String supplierId,
            @RequestBody SupplierDTO dto) {

        log.info("API Call: Update Supplier with ID: {}", supplierId);
        Response response = supplierService.updateSupplier(supplierId, dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------------------------------------------
    // GET SUPPLIER BY ID
    // ---------------------------------------------------------
    @GetMapping("/get/{supplierId}")
    public ResponseEntity<Response> getSupplierById(@PathVariable String supplierId) {
        log.info("API Call: Get Supplier By ID: {}", supplierId);
        Response response = supplierService.getSupplierById(supplierId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------------------------------------------
    // GET ALL SUPPLIERS
    // ---------------------------------------------------------
    @GetMapping("/getAll")
    public ResponseEntity<Response> getAllSuppliers() {
        log.info("API Call: Get All Suppliers");
        Response response = supplierService.getAllSuppliers();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------------------------------------------
    // DELETE SUPPLIER
    // ---------------------------------------------------------
    @DeleteMapping("/delete/{supplierId}")
    public ResponseEntity<Response> deleteSupplier(@PathVariable String supplierId) {
        log.info("API Call: Delete Supplier: {}", supplierId);
        Response response = supplierService.deleteSupplier(supplierId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @PostMapping("/supplierLogin")
    public ResponseEntity<Response> supplierLogin(@RequestBody SupplierLoginRequest request) {

        log.info("API Call: Supplier Login: {}", request.getUserName());

        Response response = supplierService.supplierLogin(request);

        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @GetMapping("/getSuppliersByClinicIdAndBranchId/{clinicId}/{branchId}")
    public ResponseEntity<Response> getSuppliersByClinicAndBranch(
            @PathVariable String clinicId,
            @PathVariable String branchId) {

        log.info("API Call: Get Suppliers by clinicId {} and branchId {}", clinicId, branchId);

        Response response =
                supplierService.getSuppliersByClinicAndBranch(clinicId, branchId);

        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
