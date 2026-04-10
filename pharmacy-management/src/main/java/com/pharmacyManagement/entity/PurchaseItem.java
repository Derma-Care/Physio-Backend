package com.pharmacyManagement.entity;

import lombok.Data;

@Data
public class PurchaseItem {
	private String productId;
	private String purchaseBillNo;
	private String productName;
	private String batchNo;
	private String expiryDate;
	private String hsnCode;
	private String category;
	private double quantity;
	private String packSize;
	private String mfgDate;

	private double freeQuantity;
	private double gstPercent;
	private double costPrice;
	private double mrp;

	private double discountPercent;
	private double discountAmount;

//	private double baseAmount;

	private double gstAmount;
	private double cgstAmount;
	private double sgstAmount;
	private double totalLineCostAmount;
	private double netAmount;
	private double actualMedicineCostExculdeGst;
	private double totalQty; // qunatity+freeQuanttiy
	private double availableQty;

}
