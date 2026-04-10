package com.clinicadmin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.clinicadmin.dto.SupplierDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.SupplierService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clinic-admin")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    // ADD SUPPLIER
    @PostMapping("/api/pharmacy/supplier/add")
    public ResponseEntity<Response> addSupplier(@RequestBody SupplierDTO dto) {
        Response res = supplierService.addSupplier(dto);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // UPDATE SUPPLIER
    @PutMapping("/api/pharmacy/supplier/update/{supplierId}")
    public ResponseEntity<Response> updateSupplier(
            @PathVariable String supplierId,
            @RequestBody SupplierDTO dto) {

        Response res = supplierService.updateSupplier(supplierId, dto);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // GET SUPPLIER BY ID
    @GetMapping("/api/pharmacy/supplier/get/{supplierId}")
    public ResponseEntity<Response> getSupplierById(@PathVariable String supplierId) {
        Response res = supplierService.getSupplierById(supplierId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // GET ALL SUPPLIERS
    @GetMapping("/api/pharmacy/supplier/getAll")
    public ResponseEntity<Response> getAllSuppliers() {
        Response res = supplierService.getAllSuppliers();
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // DELETE SUPPLIER
    @DeleteMapping("/api/pharmacy/supplier/delete/{supplierId}")
    public ResponseEntity<Response> deleteSupplier(@PathVariable String supplierId) {
        Response res = supplierService.deleteSupplier(supplierId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
