package com.pharmacyManagement.service;

import com.pharmacyManagement.dto.PurchaseBillDTO;
import com.pharmacyManagement.dto.Response;

public interface PurchaseBillService {

	Response createPurchase(PurchaseBillDTO dto);

	Response getAll();

	Response getPuchaseByPurchasedId(String purchaseId);

	Response updatePurchase(String purchaseId, PurchaseBillDTO dto);

	Response deletePurchase(String purchaseId);

	Response getPurchaseByClinicIdAndBranchId(String clinicId, String branchId);

	Response getPurchaseByClinicBranchAndBillNo(String clinicId, String branchId, String purchaseBillNo);
}