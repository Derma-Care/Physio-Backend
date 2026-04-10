package com.pharmacyManagement.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "inventory")
public class Inventory {

	@Id
	private String inventoryId;
	private String purchaseBillNo;
	private String medicineId;
	private String medicineName;

	private String brand;
	private String productType;
	private String pack;

	private String batchNo;
	private String mfgDate;
	private String expiryDate;
	private String hsnCode;
//    private String daysLeft;

	private double availableQty;
	private double minStock;

	private double purchaseRate;
	private double mrp;
	private double gstPercent;

	private String supplierId;

	private String supplier;
	private double totalQty;

	private String status;
	// ✅ Multi tenant fields
	private String clinicId;
	private String branchId;

}