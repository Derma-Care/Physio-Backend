package com.pharmacyManagement.dto;

import lombok.Data;

@Data
public class Summary {
	private double totalQuantity;
	private double totalDiscountedAmount;
	private double totalGSTAmount;
	private double actualMedicineExcludedCost;
	private double totalCostPrice;
	private double grandTotal;
}
