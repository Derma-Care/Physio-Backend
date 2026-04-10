package com.pharmacyManagement.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.pharmacyManagement.dto.PaymentDetails;
import com.pharmacyManagement.dto.PurchaseBillDTO;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.dto.Summary;
import com.pharmacyManagement.entity.Inventory;
import com.pharmacyManagement.entity.Medicine;
import com.pharmacyManagement.entity.PurchaseBill;
import com.pharmacyManagement.entity.PurchaseItem;
import com.pharmacyManagement.repository.InventoryRepository;
import com.pharmacyManagement.repository.MedicineRepository;
import com.pharmacyManagement.repository.PurchaseBillRepository;
import com.pharmacyManagement.service.PurchaseBillService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PurchaseBillServiceImpl implements PurchaseBillService {

	@Autowired
	private PurchaseBillRepository purchaseRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private MedicineRepository medicineRepository;

	@Override
	public Response createPurchase(PurchaseBillDTO dto) {

		log.info("Creating purchase for Bill No: {}", dto.getPurchaseBillNo());

		Response res = new Response();

		try {

			PurchaseBill purchase = new PurchaseBill();

			purchase.setPurchaseBillNo(dto.getPurchaseBillNo());
			purchase.setOrderId(dto.getOrderId());
			purchase.setInvoiceNo(dto.getInvoiceNo());
			purchase.setFinancialYear(dto.getFinancialYear());
			purchase.setDates(dto.getDates());
			purchase.setTaxDetails(dto.getTaxDetails());
			purchase.setSupplierDetails(dto.getSupplierDetails());
			purchase.setPaymentDetails(dto.getPaymentDetails());
			purchase.setItems(dto.getItems());

			purchase.setClinicId(dto.getClinicId());
			purchase.setBranchId(dto.getBranchId());

			purchase.setStatus("CREATED");
			purchase.setCreatedAt(LocalDateTime.now().toString());

			calculateAmounts(purchase);

			updateInventory(purchase);

			PurchaseBill savedPurchase = purchaseRepository.save(purchase);

			res.setSuccess(true);
			res.setData(savedPurchase);
			res.setMessage("Purchase created successfully");
			res.setStatus(HttpStatus.OK.value());

		} catch (Exception e) {

			log.error("Error while creating purchase", e);

			res.setSuccess(false);
			res.setMessage("Exception occurred while creating purchase : " + e.getMessage());
			res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		return res;
	}

	private void calculateAmounts(PurchaseBill purchase) {

		log.info("Calculating purchase amounts...");

		double totalQty = 0;
		double totalGST = 0;
		double grandTotal = 0;
		double totalDiscountAmount = 0;
		double totalCostPrice = 0;
		double totalActualMedicineCost=0;

		for (PurchaseItem item : purchase.getItems()) {

			double baseAmount = item.getQuantity() * item.getCostPrice(); // T

			item.setTotalLineCostAmount(baseAmount);

			double discountAmount = baseAmount * item.getDiscountPercent() / 100;

			item.setDiscountAmount(discountAmount);

			double taxableAmount = baseAmount - discountAmount; // after discount

			double gstAmount = baseAmount * item.getGstPercent() / 100;

			item.setGstAmount(gstAmount);
			item.setCgstAmount(gstAmount / 2);
			item.setSgstAmount(gstAmount / 2);

			double acutalMedicineCost = baseAmount - gstAmount;
			item.setActualMedicineCostExculdeGst(acutalMedicineCost);

			double netAmount = taxableAmount;

			item.setNetAmount(netAmount);
			double totalQtyforLine = item.getQuantity() + item.getFreeQuantity();
			item.setTotalQty(totalQtyforLine);
			totalQty += item.getQuantity() + item.getFreeQuantity();
			totalGST += gstAmount;
			grandTotal += netAmount;
			totalDiscountAmount += discountAmount;
			totalCostPrice += baseAmount;
			totalActualMedicineCost+=acutalMedicineCost;
		}

		Summary summary = new Summary();
		summary.setTotalQuantity(totalQty);
		summary.setActualMedicineExcludedCost(totalActualMedicineCost);
		summary.setTotalDiscountedAmount(totalDiscountAmount);
		summary.setTotalGSTAmount(totalGST);
		summary.setTotalCostPrice(totalCostPrice);
		summary.setGrandTotal(grandTotal);

		purchase.setSummary(summary);

		PaymentDetails payment = purchase.getPaymentDetails();

		double dueAmount = grandTotal - payment.getPaidAmount();

		payment.setDueAmount(dueAmount);

		log.info("Purchase calculation completed. Grand Total: {}", grandTotal);
	}

	private void updateInventory(PurchaseBill purchase) {

		log.info("Updating inventory for purchase: {}", purchase.getPurchaseBillNo());

		for (PurchaseItem item : purchase.getItems()) {

			Medicine medicine = medicineRepository.findById(item.getProductId())
					.orElseThrow(() -> new RuntimeException("Medicine not found with id: " + item.getProductId()));

			// Validate clinic
			if (!medicine.getClinicId().equals(purchase.getClinicId())) {
				throw new RuntimeException("ClinicId mismatch between Medicine and PurchaseBill");
			}

			// Validate branch
			if (!medicine.getBranchId().equals(purchase.getBranchId())) {
				throw new RuntimeException("BranchId mismatch between Medicine and PurchaseBill");
			}
			   // Updated search condition
	        Inventory inventory = inventoryRepository
	                .findByMedicineIdAndBatchNoAndExpiryDateAndClinicIdAndBranchId(
	                        item.getProductId(),
	                        item.getBatchNo(),
	                        item.getExpiryDate(),
	                        purchase.getClinicId(),
	                        purchase.getBranchId());

			double totalQty = item.getQuantity() + item.getFreeQuantity();

			if (inventory != null) {

				log.info("Existing inventory found for medicine {} batch {}", item.getProductId(), item.getBatchNo());

				inventory.setAvailableQty(inventory.getAvailableQty() + totalQty);
				inventory.setPurchaseRate(item.getCostPrice());
				inventory.setMrp(medicine.getMrp());
				inventory.setExpiryDate(item.getExpiryDate());

			} else {

				log.info("Creating new inventory entry for medicine {}", item.getProductId());

				inventory = new Inventory();

				inventory.setMedicineId(medicine.getId());
				inventory.setPurchaseBillNo(item.getPurchaseBillNo());
				inventory.setMedicineName(medicine.getProductName());
				inventory.setBrand(medicine.getBrandName());
				inventory.setHsnCode(medicine.getHsnCode());
				inventory.setProductType(medicine.getCategory());
				inventory.setPack(medicine.getPackSize());
				inventory.setGstPercent(medicine.getGstPercent());
				inventory.setHsnCode(medicine.getHsnCode());
				inventory.setMinStock(medicine.getMinStock());

				inventory.setBatchNo(item.getBatchNo());
				inventory.setMfgDate(item.getMfgDate());
				inventory.setExpiryDate(item.getExpiryDate());

				inventory.setAvailableQty(totalQty);
				inventory.setTotalQty(totalQty);

				inventory.setPurchaseRate(item.getCostPrice());
				inventory.setMrp(medicine.getMrp());

				inventory.setSupplierId(purchase.getSupplierDetails().getSupplierId());
				inventory.setSupplier(purchase.getSupplierDetails().getSupplierName());

				inventory.setClinicId(medicine.getClinicId());
				inventory.setBranchId(medicine.getBranchId());

				inventory.setStatus("ACTIVE");
			}

			inventoryRepository.save(inventory);
		}
	}

	@Override
	public Response getPuchaseByPurchasedId(String purchaseId) {

		Response res = new Response();

		Optional<PurchaseBill> purchaseBill = purchaseRepository.findById(purchaseId);

		if (purchaseBill.isPresent()) {

			res.setSuccess(true);
			res.setData(purchaseBill.get());
			res.setMessage("Purchased data fetched successfully");
			res.setStatus(HttpStatus.OK.value());

		} else {

			res.setSuccess(false);
			res.setMessage("Purchased data not found with id: " + purchaseId);
			res.setStatus(HttpStatus.NOT_FOUND.value());
		}

		return res;
	}

	@Override
	public Response getAll() {

		log.info("Fetching all purchases");

		List<PurchaseBill> purchasedData = purchaseRepository.findAll();

		Response res = new Response();

		res.setSuccess(true);
		res.setData(purchasedData);
		res.setMessage("Fetched purchase data");
		res.setStatus(HttpStatus.OK.value());

		return res;
	}

	@Override
	public Response updatePurchase(String purchaseId, PurchaseBillDTO dto) {

		log.info("Updating purchase with ID: {}", purchaseId);

		Response res = new Response();

		Optional<PurchaseBill> optional = purchaseRepository.findById(purchaseId);

		if (optional.isEmpty()) {

			res.setSuccess(false);
			res.setMessage("Purchase not found with id: " + purchaseId);
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return res;
		}

		PurchaseBill purchase = optional.get();

		purchase.setPurchaseBillNo(dto.getPurchaseBillNo());
		purchase.setOrderId(dto.getOrderId());
		purchase.setInvoiceNo(dto.getInvoiceNo());
		purchase.setFinancialYear(dto.getFinancialYear());
		purchase.setDates(dto.getDates());
		purchase.setTaxDetails(dto.getTaxDetails());
		purchase.setSupplierDetails(dto.getSupplierDetails());
		purchase.setPaymentDetails(dto.getPaymentDetails());
		purchase.setItems(dto.getItems());

		purchase.setClinicId(dto.getClinicId());
		purchase.setBranchId(dto.getBranchId());

		purchase.setUpdatedAt(LocalDateTime.now().toString());

		calculateAmounts(purchase);

		updateInventory(purchase);

		PurchaseBill updated = purchaseRepository.save(purchase);

		res.setSuccess(true);
		res.setData(updated);
		res.setMessage("Purchase updated successfully");
		res.setStatus(HttpStatus.OK.value());

		return res;
	}

	@Override
	public Response deletePurchase(String purchaseId) {

		log.info("Deleting purchase with ID: {}", purchaseId);

		Response res = new Response();

		Optional<PurchaseBill> optional = purchaseRepository.findById(purchaseId);

		if (optional.isEmpty()) {

			res.setSuccess(false);
			res.setMessage("Purchase not found with id: " + purchaseId);
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return res;
		}

		purchaseRepository.deleteById(purchaseId);

		res.setSuccess(true);
		res.setMessage("Purchase deleted successfully");
		res.setStatus(HttpStatus.OK.value());

		return res;
	}
	@Override
	public Response getPurchaseByClinicIdAndBranchId(String clinicId, String branchId) {

	    Response res = new Response();

	    List<PurchaseBill> bills =
	            purchaseRepository.findByClinicIdAndBranchId(clinicId, branchId);

	    if (bills != null && !bills.isEmpty()) {

	        for (PurchaseBill bill : bills) {

	            if (bill.getItems() != null) {

	                for (PurchaseItem item : bill.getItems()) {

	                   
	                    List<Inventory> inventoryList =
	                            inventoryRepository.findByMedicineIdAndClinicIdAndBranchId(
	                                    item.getProductId(),
	                                    clinicId,
	                                    branchId);

	                    if (inventoryList != null && !inventoryList.isEmpty()) {

	                        // ✅ Sum all available quantities (best approach)
	                        double totalQty = inventoryList.stream()
	                                .mapToDouble(Inventory::getAvailableQty)
	                                .sum();

	                        item.setAvailableQty(totalQty);

	                    } else {
	                        item.setAvailableQty(0);
	                    }
	                }
	            }
	        }

	        res.setSuccess(true);
	        res.setData(bills);
	        res.setMessage("Purchase bills fetched successfully");
	        res.setStatus(HttpStatus.OK.value());

	    } else {

	        res.setSuccess(false);
	        res.setMessage("No purchase bills found");
	        res.setStatus(HttpStatus.NOT_FOUND.value());
	    }

	    return res;
	}
	@Override
	public Response getPurchaseByClinicBranchAndBillNo(
	        String clinicId,
	        String branchId,
	        String purchaseBillNo) {

	    Response res = new Response();

	    Optional<PurchaseBill> purchaseBill =
	            purchaseRepository.findByClinicIdAndBranchIdAndPurchaseBillNo(
	                    clinicId, branchId, purchaseBillNo);

	    if (purchaseBill.isPresent()) {

	        PurchaseBill bill = purchaseBill.get();

	        // 🔹 Fetch availableQty from Inventory
	        if (bill.getItems() != null) {

	            for (PurchaseItem item : bill.getItems()) {

	                // ✅ Changed Optional → List
	                List<Inventory> inventoryList =
	                        inventoryRepository.findByMedicineIdAndClinicIdAndBranchId(
	                                item.getProductId(),
	                                clinicId,
	                                branchId);

	                if (inventoryList != null && !inventoryList.isEmpty()) {

	                    // 👉 Best: Sum all quantities
	                	int totalQty = inventoryList.stream()
	                	        .mapToInt(inv -> (int) inv.getAvailableQty())
	                	        .sum();

	                	item.setAvailableQty(totalQty);

	                } else {
	                    item.setAvailableQty(0);
	                }
	            }
	        }

	        res.setSuccess(true);
	        res.setData(bill);
	        res.setMessage("Purchase bill fetched successfully");
	        res.setStatus(HttpStatus.OK.value());

	    } else {

	        res.setSuccess(false);
	        res.setMessage("Purchase bill not found");
	        res.setStatus(HttpStatus.NOT_FOUND.value());
	    }

	    return res;
	}
}