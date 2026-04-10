//package com.pharmacyManagement.service.impl;
//
//import java.util.Optional;
//
//import org.springframework.stereotype.Service;
//
//import com.pharmacyManagement.dto.Response;
//import com.pharmacyManagement.entity.Stock;
//import com.pharmacyManagement.repository.StockRepository;
//import com.pharmacyManagement.service.StockService;
//
//import lombok.RequiredArgsConstructor;
//
//@Service
//@RequiredArgsConstructor
//public class StockServiceImpl implements StockService {
//
//	private final StockRepository stockRepo;
//
//	// ----------------------------------------------------------------------------
//	// ADD STOCK (MANUAL ENTRY)
//	// ----------------------------------------------------------------------------
//	@Override
//	public Response addStock(Stock stock) {
//
//		if (stock.getProductId() == null || stock.getBatchNo() == null) {
//			return new Response(false, null, "productId & batchNo required", 400,null);
//		}
//
//		Optional<Stock> exists = stockRepo.findByProductIdAndBatchNo(stock.getProductId(), stock.getBatchNo());
//
//		if (exists.isPresent()) {
//			return new Response(false, null, "Stock already exists for this batch", 409,null);
//		}
//
//		stock.setClosingStock(
//				stock.getOpeningStock() + stock.getStockIn() - stock.getStockOut() - stock.getDamageStock());
//
//		Stock saved = stockRepo.save(stock);
//		return new Response(true, saved, "Stock added", 200,null);
//	}
//
//	// ----------------------------------------------------------------------------
//	// UPDATE STOCK (MANUAL ADMIN EDIT)
//	// ----------------------------------------------------------------------------
//	@Override
//	public Response updateStock(String id, Stock updatedStock) {
//
//		Optional<Stock> stockOpt = stockRepo.findById(id);
//		if (stockOpt.isEmpty()) {
//			return new Response(false, null, "Stock not found", 404,null);
//		}
//
//		Stock stock = stockOpt.get();
//
//		stock.setProductName(updatedStock.getProductName());
//		stock.setExpiryDate(updatedStock.getExpiryDate());
//		stock.setCategory(updatedStock.getCategory());
//		stock.setCostPrice(updatedStock.getCostPrice());
//		stock.setMrp(updatedStock.getMrp());
//		stock.setGstPercent(updatedStock.getGstPercent());
////        stock.setSupplierId(updatedStock.getSupplierId());
//		stock.setSupplierName(updatedStock.getSupplierName());
//
//		stock.setOpeningStock(updatedStock.getOpeningStock());
//		stock.setStockIn(updatedStock.getStockIn());
//		stock.setStockOut(updatedStock.getStockOut());
//		stock.setDamageStock(updatedStock.getDamageStock());
//
//		stock.setClosingStock(
//				stock.getOpeningStock() + stock.getStockIn() - stock.getStockOut() - stock.getDamageStock());
//
//		stockRepo.save(stock);
//		return new Response(true, stock, "Stock updated", 200,null);
//	}
//
//	// ----------------------------------------------------------------------------
//	// GET STOCK BY ID
//	// ----------------------------------------------------------------------------
//	@Override
//	public Response getStockById(String id) {
//
//		Optional<Stock> stock = stockRepo.findById(id);
//		return stock.isPresent() ? new Response(true, stock.get(), "Stock found", 200,null)
//				: new Response(false, null, "Stock not found", 404,null);
//	}
//
//	// ----------------------------------------------------------------------------
//	// GET STOCK BY PRODUCT ID
//	// ----------------------------------------------------------------------------
//	@Override
//	public Response getStockByProductId(String productId) {
//
//		return new Response(true, stockRepo.findByProductId(productId), "Stock list", 200,null);
//	}
//
//	// ----------------------------------------------------------------------------
//	// GET ALL STOCK
//	// ----------------------------------------------------------------------------
//	@Override
//	public Response getAllStock() {
//		return new Response(true, stockRepo.findAll(), "All stock list", 200,null);
//	}
//
//	// ----------------------------------------------------------------------------
//	// DELETE STOCK (SOFT DELETE)
//	// ----------------------------------------------------------------------------
//	@Override
//	public Response deleteStock(String id) {
//		Optional<Stock> stockOpt = stockRepo.findById(id);
//
//		if (stockOpt.isEmpty()) {
//			return new Response(false, null, "Stock not found", 404,null);
//		}
//
//		Stock stock = stockOpt.get();
//		stock.setStatus("Inactive");
//		stockRepo.save(stock);
//
//		return new Response(true, null, "Stock deleted (status=Inactive)", 200,null);
//	}
//	@Override
//	public Response deleteStockById(String id) {
//		Optional<Stock> stock = stockRepo.findById(id);
//		Response res = new Response();
//		if(stock.isPresent()) {
//			stockRepo.deleteById(id);
//			res.setSuccess(true);
//			res.setMessage("Stock deleted successfully");
//			res.setStatus(200);
//			return res;
//		}
//		else {
//			res.setSuccess(false);
//			res.setMessage("Stock not found not found with is id"+id);
//			res.setStatus(404);
//			return res;
//		}
//		
//	}
//
//	// ----------------------------------------------------------------------------
//	// CHANGE STATUS (Active/Inactive)
//	// ----------------------------------------------------------------------------
//	@Override
//	public Response changeStatus(String id, String status) {
//
//		Optional<Stock> stockOpt = stockRepo.findById(id);
//
//		if (stockOpt.isEmpty()) {
//			return new Response(false, null, "Stock not found", 404,null);
//		}
//
//		Stock stock = stockOpt.get();
//		stock.setStatus(status);
//		stockRepo.save(stock);
//
//		return new Response(true, stock, "Status updated", 200,null);
//	}
//}
