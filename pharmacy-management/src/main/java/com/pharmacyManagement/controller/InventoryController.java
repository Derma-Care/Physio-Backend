package com.pharmacyManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pharmacyManagement.dto.InventoryResponseDTO;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.service.InventoryService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/inventory")
@Slf4j
public class InventoryController {

	@Autowired
	private InventoryService inventoryService;

	// Get inventory by clinic & branch
	@GetMapping("/clinic/{clinicId}/branch/{branchId}")
	public ResponseEntity<Response> getAllInventory(@PathVariable String clinicId, @PathVariable String branchId) {

		Response res = inventoryService.getAllInventory(clinicId, branchId);

		return ResponseEntity.status(res.getStatus()).body(res);
	}

	// Get inventory by id
	@GetMapping("/{id}")
	public ResponseEntity<Response> getInventoryById(@PathVariable String id) {

		Response res = inventoryService.getInventoryById(id);

		return ResponseEntity.status(res.getStatus()).body(res);
	}

	// Create inventory
	@PostMapping
	public ResponseEntity<Response> createInventory(@RequestBody InventoryResponseDTO dto) {

		Response res = inventoryService.createInventory(dto);

		return ResponseEntity.status(res.getStatus()).body(res);
	}

	// Update inventory
	@PutMapping("/{id}")
	public ResponseEntity<Response> updateInventory(@PathVariable String id, @RequestBody InventoryResponseDTO dto) {

		Response res = inventoryService.updateInventory(id, dto);

		return ResponseEntity.status(res.getStatus()).body(res);
	}

	// Delete inventory by id
	@DeleteMapping("/{id}")
	public ResponseEntity<Response> deleteInventory(@PathVariable String id) {

		Response res = inventoryService.deleteInventory(id);

		return ResponseEntity.status(res.getStatus()).body(res);
	}

	// Delete inventory by medicineId + batchNo
	@DeleteMapping("/medicine/{medicineId}/batch/{batchNo}")
	public ResponseEntity<Response> deleteInventoryByBatch(@PathVariable String medicineId,
			@PathVariable String batchNo) {

		Response res = inventoryService.deleteInventory(medicineId, batchNo);

		return ResponseEntity.status(res.getStatus()).body(res);
	}
//
//	@GetMapping("/clinic/{clinicId}/branch/{branchId}")
//	public ResponseEntity<Response> getInventoryByClinicAndBranch(@PathVariable String clinicId,
//			@PathVariable String branchId) {
//
//		Response res = inventoryService.getInventoryByClinicAndBranch(clinicId, branchId);
//
//		return ResponseEntity.status(res.getStatus()).body(res);
//	}
}
