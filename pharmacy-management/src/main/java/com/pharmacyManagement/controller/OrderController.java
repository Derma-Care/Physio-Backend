package com.pharmacyManagement.controller;

import com.pharmacyManagement.dto.OrderDTO;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService service;

 
    @PostMapping("/createOrder")
    public ResponseEntity<Response> createOrder(@RequestBody OrderDTO dto) {

        Response response = service.createOrder(dto);

        return ResponseEntity
                .status(response.getStatus()) 
                .body(response);
    }
    @GetMapping("/getByClinicIdAndBranchId/{clinicId}/{branchId}")
    public ResponseEntity<Response> getByClinicIdAndBranchId(
            @PathVariable String clinicId,
            @PathVariable String branchId) {

        Response res = service.getOrdersByClinicAndBranch(clinicId, branchId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/getByClinicIdBranchIdAndOrderId/{clinicId}/{branchId}/{orderId}")
    public ResponseEntity<Response> getByClinicIdBranchIdAndOrderId(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String orderId) {

        Response res = service.getOrderByClinicBranchAndOrderId(clinicId, branchId, orderId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PutMapping("/updateByOrderId/{orderId}")
    public ResponseEntity<Response> updateByOrderId(
            @PathVariable String orderId,
            @RequestBody OrderDTO dto) {

        Response res = service.updateOrder(orderId, dto);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    
    @GetMapping("/getByClinicIdBranchIdSupplierId/{clinicId}/{branchId}/{supplierId}")
    public ResponseEntity<Response> getByClinicBranchSupplier(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String supplierId) {

        Response res = service.getOrderByClinicBranchAndSupplierId(
                clinicId, branchId, supplierId);

        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @DeleteMapping("/deleteByOrderId/{orderId}")
    public ResponseEntity<Response> deleteByOrderId(@PathVariable String orderId) {

        Response res = service.deleteOrder(orderId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}