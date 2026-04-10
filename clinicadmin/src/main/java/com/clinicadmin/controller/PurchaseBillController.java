package com.clinicadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.clinicadmin.dto.PurchaseBillDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.PurchaseBillService;

@RestController
@RequestMapping("/clinic-admin")
public class PurchaseBillController {

    @Autowired
    private PurchaseBillService purchaseBillService;

    // SAVE PURCHASE BILL
    @PostMapping("/api/pharmacy/purchase/save")
    public ResponseEntity<Response> savePurchase(@RequestBody PurchaseBillDTO dto) {
        Response res = purchaseBillService.savePurchase(dto);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // UPDATE PURCHASE BILL
    @PutMapping("/api/pharmacy/purchase/update/{id}")
    public ResponseEntity<Response> updatePurchaseById(
            @PathVariable String id,
            @RequestBody PurchaseBillDTO dto) {

        Response res = purchaseBillService.updatePurchase(id, dto);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // GET BY ID
    @GetMapping("/api/pharmacy/purchase/getById/{id}")
    public ResponseEntity<Response> getPurchaseById(@PathVariable String id) {
        Response res = purchaseBillService.getPurchaseById(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // GET BY BILL NUMBER
    @GetMapping("/api/pharmacy/purchase/getByBillNo/{billNo}")
    public ResponseEntity<Response> getPurchaseByBillNo(@PathVariable String billNo) {
        Response res = purchaseBillService.getPurchaseByBillNo(billNo);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // GET ALL PURCHASES
    @GetMapping("/api/pharmacy/purchase/all")
    public ResponseEntity<Response> getAllPurchases() {
        Response res = purchaseBillService.getAllPurchases();
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // DELETE PURCHASE
    @DeleteMapping("/api/pharmacy/purchase/delete/{id}")
    public ResponseEntity<Response> deletePurchaseById(@PathVariable String id) {
        Response res = purchaseBillService.deletePurchase(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // SEARCH BY DATE RANGE
    @GetMapping("/api/pharmacy/purchase/search/{fromDate}/{toDate}")
    public ResponseEntity<Response> getByDateRange(
            @PathVariable String fromDate,
            @PathVariable String toDate) {

        Response res = purchaseBillService.getByDateRange(fromDate, toDate);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
