package com.clinicadmin.controller;
import org.springframework.web.bind.annotation.*;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.StockDTO;
import com.clinicadmin.service.StockLedgerService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clinic-admin")
@RequiredArgsConstructor
public class StockLedgerController {

    private final StockLedgerService stockLedgerService;

    // --------------------------------------------------------------------------
    // PURCHASE STOCK
    // --------------------------------------------------------------------------
    @PostMapping("/api/pharmacy/purchase/{purchaseBillNo}")
    public Response addPurchase(@PathVariable String purchaseBillNo,
                                @RequestBody StockDTO dto) {
        return stockLedgerService.addPurchase(purchaseBillNo, dto);
    }

    // --------------------------------------------------------------------------
    // SALE STOCK
    // --------------------------------------------------------------------------
    @PostMapping("/api/pharmacy/sale/{productId}/{batchNo}/{qty}/{saleId}")
    public Response addSale(@PathVariable String productId,
                            @PathVariable String batchNo,
                            @PathVariable int qty,
                            @PathVariable String saleId) {
        return stockLedgerService.addSale(productId, batchNo, qty, saleId);
    }

    // --------------------------------------------------------------------------
    // DAMAGE STOCK
    // --------------------------------------------------------------------------
    @PostMapping("/api/pharmacy/damage/{productId}/{batchNo}/{qty}/{reason}")
    public Response addDamage(@PathVariable String productId,
                              @PathVariable String batchNo,
                              @PathVariable int qty,
                              @PathVariable String reason) {
        return stockLedgerService.addDamage(productId, batchNo, qty, reason);
    }
}
