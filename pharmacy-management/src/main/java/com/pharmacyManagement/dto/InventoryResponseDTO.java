package com.pharmacyManagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryResponseDTO {

	private String inventoryId;

	private String medicineId;
	private String medicineName;

	private String brand;
	private String productType;
	private String pack;

	private String batchNo;
	private String mfgDate;
	private String expiryDate;
	private String hsnCode;

	private double daysLeft;

	private double availableQty;
	private double minStock;

	private double purchaseRate;
	private double mrp;
	private double gstPercent;

	private String supplierId;
	private String supplier;

	private String status;

	// ✅ Multi tenant
	private String clinicId;
	private String branchId;

}