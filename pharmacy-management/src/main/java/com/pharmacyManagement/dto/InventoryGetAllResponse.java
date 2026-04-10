package com.pharmacyManagement.dto;

import java.util.List;

import com.pharmacyManagement.entity.Inventory;

import lombok.Data;

@Data
public class InventoryGetAllResponse {

	private String medicineId;
	private String medicineName;
	private String brand;
	private String productType;
	private String hsnCode;
	private double availableQty;
	private double minStock;
	private double gstPercent;

	private String status; // priority status

	private List<Inventory> inventory;
}