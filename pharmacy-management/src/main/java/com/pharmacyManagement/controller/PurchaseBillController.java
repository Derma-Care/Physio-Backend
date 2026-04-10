package com.pharmacyManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pharmacyManagement.dto.PurchaseBillDTO;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.service.PurchaseBillService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/purchases")
@Slf4j
public class PurchaseBillController {

	@Autowired
	private PurchaseBillService purchaseService;

	@PostMapping
	public ResponseEntity<Response> createPurchase(@RequestBody PurchaseBillDTO dto) {

		log.info("API Call: Create Purchase");
		Response res = purchaseService.createPurchase(dto);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@GetMapping
	public ResponseEntity<Response> getAllPurchases() {

		log.info("API Call: Get All Purchases");
		Response res = purchaseService.getAll();
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@GetMapping("/{purchaseId}")
	public ResponseEntity<Response> getPurchaseById(@PathVariable String purchaseId) {

		log.info("API Call: Get Purchase By Id");
		Response res = purchaseService.getPuchaseByPurchasedId(purchaseId);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@PutMapping("/{purchaseId}")
	public ResponseEntity<Response> updatePurchase(
			@PathVariable String purchaseId,
			@RequestBody PurchaseBillDTO dto) {

		log.info("API Call: Update Purchase");
		Response res = purchaseService.updatePurchase(purchaseId, dto);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@DeleteMapping("/{purchaseId}")
	public ResponseEntity<Response> deletePurchase(@PathVariable String purchaseId) {

		log.info("API Call: Delete Purchase");
		Response res = purchaseService.deletePurchase(purchaseId);
		return ResponseEntity.status(res.getStatus()).body(res);
	}
	
	@GetMapping("/getPurchaseByClinicIdAndBranchId/{clinicId}/{branchId}")
	public ResponseEntity<Response> getPurchaseByClinicAndBranch(
	        @PathVariable String clinicId,
	        @PathVariable String branchId) {

	    Response res = purchaseService.getPurchaseByClinicIdAndBranchId(clinicId, branchId);

	    return ResponseEntity.status(res.getStatus()).body(res);
	}
	
	@GetMapping("/getPurchaseByclinicIdBranchIdBillNo/{clinicId}/{branchId}/{purchaseBillNo}")
	public ResponseEntity<Response> getPurchaseByclinicIdBranchIdBillNo(
	        @PathVariable String clinicId,
	        @PathVariable String branchId,
	        @PathVariable String purchaseBillNo) {

	    Response res =
	            purchaseService.getPurchaseByClinicBranchAndBillNo(
	                    clinicId, branchId, purchaseBillNo);

	    return ResponseEntity.status(res.getStatus()).body(res);
	}
}