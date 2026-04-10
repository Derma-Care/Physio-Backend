package com.clinicadmin.controller;

import org.springframework.web.bind.annotation.*;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.StockDTO;
import com.clinicadmin.service.StockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clinic-admin")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;   // ✅ DO NOT ASSIGN NULL

    @PostMapping("/api/pharmacy/stockMaster/add")
    public Response addStock(@RequestBody StockDTO dto) {
        return stockService.addStock(dto);
    }

    @PutMapping("/api/pharmacy/stockMaster/update/{id}")
    public Response updateStock(@PathVariable String id, @RequestBody StockDTO dto) {
        return stockService.updateStock(id, dto);
    }

    @GetMapping("/api/pharmacy/stockMaster/{id}")
    public Response getStockById(@PathVariable String id) {
        return stockService.getStockById(id);
    }

    @GetMapping("/api/pharmacy/stockMaster/product//{productId}")
    public Response getStockByProductId(@PathVariable String productId) {
        return stockService.getStockByProductId(productId);
    }

    @GetMapping("/api/pharmacy/stockMaster/all")
    public Response getAllStock() {
        return stockService.getAllStock();
    }

    @DeleteMapping("/api/pharmacy/stockMaster/delete/{id}")
    public Response deleteStock(@PathVariable String id) {
        return stockService.deleteStock(id);
    }
}
