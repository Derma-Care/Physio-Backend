//package com.pharmacyManagement.service.impl;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import org.springframework.stereotype.Service;
//import com.pharmacyManagement.dto.Response;
//import com.pharmacyManagement.entity.PurchaseItem;
//
//import lombok.RequiredArgsConstructor;
//
//@Service
//@RequiredArgsConstructor
//public class StockLedgerServiceImpl implements StockLedgerService {
//
//	private final StockRepository stockRepo;
//	private final StockLedgerRepository ledgerRepo;
//
//	// -------------------------------------------------------------------------
//	// ADD PURCHASE STOCK
//	// -------------------------------------------------------------------------
//	@Override
//	public synchronized Response addPurchaseStock(StockDTO dto, String purchaseBillNo) {
//		try {
//			int totalQty = dto.getQty() + dto.getFreeQty();
//
//			Optional<Stock> existingStockOpt = stockRepo.findByProductIdAndBatchNo(dto.getProductId(),
//					dto.getBatchNo());
//
//			Stock stock;
//
//			if (existingStockOpt.isPresent()) {
//
//				stock = existingStockOpt.get();
//
//				stock.setOpeningStock(stock.getClosingStock());
//
//				// ❌ WRONG:
//				// stock.setStockIn(stock.getStockIn() + totalQty);
//
//				// 🔥 FIXED: StockIn should be ONLY this transaction qty
//				stock.setStockIn(totalQty);
//
//			} else {
//
//				stock = new Stock();
//				stock.setProductId(dto.getProductId());
//				stock.setProductName(dto.getProductName());
//				stock.setBatchNo(dto.getBatchNo());
//				stock.setExpiryDate(dto.getExpiryDate());
//				stock.setCategory(dto.getCategory());
//				stock.setOpeningStock(0);
//				stock.setStockIn(totalQty);
//				stock.setStockOut(0);
//				stock.setDamageStock(0);
//			}
//
//			// common updates
//			stock.setLastPurchaseDate(dto.getPurchaseDate());
//			stock.setCostPrice(dto.getCostPrice());
//			stock.setMrp(dto.getMrp());
//			stock.setGstPercent(dto.getGstPercent());
////			stock.setSupplierId(dto.getSupplierId());
//			stock.setSupplierName(dto.getSupplierName());
//
//			// 🔥 FIXED closing stock remains correct
//			stock.setClosingStock(
//					stock.getOpeningStock() + stock.getStockIn() - stock.getStockOut() - stock.getDamageStock());
//
//			stockRepo.save(stock);
//
//			StockLedger ledger = saveLedger(stock, "PURCHASE", purchaseBillNo, totalQty, 0,
//					"Purchase - Bill: " + purchaseBillNo);
//
//			Map<String, Object> map = new HashMap<>();
//			map.put("stock", stock);
//			map.put("ledger", ledger);
//
//			return new Response(true, map, "Purchase stock added successfully", 200,null);
//
//		} catch (Exception e) {
//			return new Response(false, null, "Error: " + e.getMessage(), 500,null);
//		}
//	}
//
//	// -------------------------------------------------------------------------
//	// REVERSE PURCHASE FOR SINGLE ITEM
//	// -------------------------------------------------------------------------
//
//	@Override
//	public synchronized Response reversePurchaseItem(String productId, String batchNo, int qtyToReverse,
//			String purchaseBillNo) {
//		try {
//			Optional<Stock> stockOpt = stockRepo.findByProductIdAndBatchNo(productId, batchNo);
//
//			if (stockOpt.isEmpty()) {
//				return new Response(false, null, "Stock not found to reverse", 404,null);
//			}
//
//			Stock stock = stockOpt.get();
//			stock.setOpeningStock(stock.getClosingStock());
//
//			// 🔥 FIXED: StockIn should not be cumulative or subtracted
//			stock.setStockIn(0);
//
//			// reduce closing stock directly
//			stock.setClosingStock(stock.getOpeningStock() - qtyToReverse);
//
//			stockRepo.save(stock);
//
//			StockLedger ledger = saveLedger(stock, "PURCHASE_REVERSAL", purchaseBillNo, 0, qtyToReverse,
//					"Purchase Reversal - Bill: " + purchaseBillNo);
//
//			Map<String, Object> map = new HashMap<>();
//			map.put("stock", stock);
//			map.put("ledger", ledger);
//
//			return new Response(true, map, "Purchase reversed for item", 200,null);
//
//		} catch (Exception e) {
//			return new Response(false, null, "Error: " + e.getMessage(), 500,null);
//		}
//	}
//
//	// -------------------------------------------------------------------------
//	// REVERSE PURCHASE FOR FULL BILL
//	// -------------------------------------------------------------------------
//	@Override
//	public Response reversePurchaseByBill(String purchaseBillNo, List<PurchaseItem> items) {
//		if (items == null || items.isEmpty()) {
//			return new Response(false, null, "No items available to reverse", 400, null);
//		}
//
//		for (PurchaseItem item : items) {
//			int totalQty = item.getQuantity() + item.getFreeQty();
//			reversePurchaseItem(item.getProductId(), item.getBatchNo(), totalQty, purchaseBillNo);
//		}
//
//		return new Response(true, null, "Purchase reversed for entire bill", 200, null);
//	}
//
//	// -------------------------------------------------------------------------
//	// RECORD SALE
//	// -------------------------------------------------------------------------
//	@Override
//	public synchronized Response recordSale(String productId, String batchNo, int qty, String saleId) {
//		try {
//			Optional<Stock> stockOpt = stockRepo.findByProductIdAndBatchNo(productId, batchNo);
//
//			if (stockOpt.isEmpty()) {
//				return new Response(false, null, "Stock not found", 404, null);
//			}
//
//			Stock stock = stockOpt.get();
//
//			if (stock.getClosingStock() < qty) {
//				return new Response(false, null, "Insufficient stock for sale", 400, null);
//			}
//
//			stock.setOpeningStock(stock.getClosingStock());
//
//			// 🔥 FIXED
//			stock.setStockOut(qty);
//
//			stock.setClosingStock(
//					stock.getOpeningStock() + stock.getStockIn() - stock.getStockOut() - stock.getDamageStock());
//
//			stockRepo.save(stock);
//
//			StockLedger ledger = saveLedger(stock, "SALE", saleId, 0, qty, "Sale Entry");
//
//			Map<String, Object> map = new HashMap<>();
//			map.put("stock", stock);
//			map.put("ledger", ledger);
//
//			return new Response(true, map, "Sale recorded successfully", 200, null);
//
//		} catch (Exception e) {
//			return new Response(false, null, "Error: " + e.getMessage(), 500, null);
//		}
//	}
//
//	// -------------------------------------------------------------------------
//	// RECORD DAMAGE
//	// -------------------------------------------------------------------------
//	@Override
//	public synchronized Response recordDamage(String productId, String batchNo, int qty, String reason) {
//		try {
//			Optional<Stock> stockOpt = stockRepo.findByProductIdAndBatchNo(productId, batchNo);
//
//			if (stockOpt.isEmpty()) {
//				return new Response(false, null, "Stock not found", 404,null);
//			}
//
//			Stock stock = stockOpt.get();
//
//			if (stock.getClosingStock() < qty) {
//				return new Response(false, null, "Insufficient stock for damage", 400,null);
//			}
//
//			stock.setOpeningStock(stock.getClosingStock());
//
//			// 🔥 FIXED
//			stock.setDamageStock(qty);
//
//			stock.setClosingStock(
//					stock.getOpeningStock() + stock.getStockIn() - stock.getStockOut() - stock.getDamageStock());
//
//			stockRepo.save(stock);
//
//			StockLedger ledger = saveLedger(stock, "DAMAGE", null, 0, qty, "Damage: " + reason);
//
//			Map<String, Object> map = new HashMap<>();
//			map.put("stock", stock);
//			map.put("ledger", ledger);
//
//			return new Response(true, map, "Damage recorded successfully", 200,null);
//
//		} catch (Exception e) {
//			return new Response(false, null, "Error: " + e.getMessage(), 500,null);
//		}
//	}
//
//	// -------------------------------------------------------------------------
//	// LEDGER ENTRY CREATION (returns ledger)
//	// -------------------------------------------------------------------------
//	private StockLedger saveLedger(Stock stock, String type, String txnId, int qtyIn, int qtyOut, String remarks) {
//
//		StockLedger ledger = new StockLedger();
//
//		ledger.setProductId(stock.getProductId());
//		ledger.setProductName(stock.getProductName());
//		ledger.setBatchNo(stock.getBatchNo());
//		ledger.setExpiryDate(stock.getExpiryDate());
//		ledger.setCategory(stock.getCategory());
//		ledger.setTransactionType(type);
//		ledger.setTransactionId(txnId);
//		ledger.setQtyIn(qtyIn);
//		ledger.setQtyOut(qtyOut);
//
//		ledger.setClosingStock(stock.getClosingStock());
//		ledger.setCostPrice(stock.getCostPrice());
//		ledger.setMrp(stock.getMrp());
//
//		ledger.setDate(LocalDate.now().toString());
//		ledger.setTime(LocalTime.now().toString());
//		ledger.setRemarks(remarks);
//
//		return ledgerRepo.save(ledger);
//	}
//}
