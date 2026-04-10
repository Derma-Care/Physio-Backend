package com.clinicadmin.feignclient;

import java.time.LocalDate;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.clinicadmin.dto.AreaDTO;
import com.clinicadmin.dto.CityDTO;
import com.clinicadmin.dto.MedicineDetailDTO;
import com.clinicadmin.dto.MedicineInventoryDto;
import com.clinicadmin.dto.OpSalesRequest;
import com.clinicadmin.dto.PurchaseBillDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.SalesReturnCreateResponse;
import com.clinicadmin.dto.SalesReturnRequest;
import com.clinicadmin.dto.SalesReturnResponse;
import com.clinicadmin.dto.SalesReturnSummaryResponse;
import com.clinicadmin.dto.SalesReturnUpdateRequest;
import com.clinicadmin.dto.StockDTO;
import com.clinicadmin.dto.SupplierDTO;
import com.clinicadmin.utils.ApiResponse;
import jakarta.validation.Valid;


@FeignClient(name = "pharmacy-management")
public interface PharmacyManagementFeignClient {

    @PostMapping("/api/pharmacy/area/save")
    Response saveArea(@RequestBody AreaDTO dto);

    @PutMapping("/api/pharmacy/area/update/{id}")
    Response updateArea(@PathVariable String id, @RequestBody AreaDTO dto);

    @GetMapping("/api/pharmacy/area/getById/{id}")
    Response getAreaById(@PathVariable String id);

    @GetMapping("/api/pharmacy/area/all")
    Response getAllAreas();

    @DeleteMapping("/api/pharmacy/area/delete/{id}")
    Response deleteArea(@PathVariable String id);

    @GetMapping("/api/pharmacy/area/city/{cityId}")
    Response getAreasByCity(@PathVariable String cityId);
    
    //// ========== CITY APIs ========== ////    

    // SAVE CITY
    @PostMapping("/api/pharmacy/city/save")
    Response saveCity(@RequestBody CityDTO dto);

    // UPDATE CITY
    @PutMapping("/api/pharmacy/city/update/{id}")
    Response updateCity(@PathVariable String id, @RequestBody CityDTO dto);

    // GET CITY BY ID
    @GetMapping("/api/pharmacy/city/getById/{id}")
    Response getCityById(@PathVariable String id);

    // GET ALL CITIES
    @GetMapping("/api/pharmacy/city/all")
    Response getAllCities();

    // DELETE CITY
    @DeleteMapping("/api/pharmacy/city/delete/{id}")
    Response deleteCity(@PathVariable String id);
    
    
    
 // ==================== PURCHASE BILL APIs ====================

 // SAVE
 @PostMapping("/api/pharmacy/purchase/save")
 Response savePurchase(@RequestBody PurchaseBillDTO dto);

 // UPDATE
 @PutMapping("/api/pharmacy/purchase/update/{id}")
 Response updatePurchase(@PathVariable String id, @RequestBody PurchaseBillDTO dto);

 // GET BY ID
 @GetMapping("/api/pharmacy/purchase/getById/{id}")
 Response getPurchaseById(@PathVariable String id);

 // GET BY BILL NO
 @GetMapping("/api/pharmacy/purchase/getByBillNo/{billNo}")
 Response getPurchaseByBillNo(@PathVariable String billNo);

 // GET ALL
 @GetMapping("/api/pharmacy/purchase/all")
 Response getAllPurchases();

 // DELETE
 @DeleteMapping("/api/pharmacy/purchase/delete/{id}")
 Response deletePurchase(@PathVariable String id);

 // SEARCH BY DATE RANGE
 @GetMapping("/api/pharmacy/purchase/search/date/{fromDate}/{toDate}")
 Response getByDateRange(@PathVariable String fromDate, @PathVariable String toDate);



//==================== SUPPLIER APIs ====================

@PostMapping("/api/pharmacy/supplier/add")
Response addSupplier(@RequestBody SupplierDTO dto);

@PutMapping("/api/pharmacy/supplier/update/{supplierId}")
Response updateSupplier(@PathVariable String supplierId, @RequestBody SupplierDTO dto);

@GetMapping("/api/pharmacy/supplier/get/{supplierId}")
Response getSupplierById(@PathVariable String supplierId);

@GetMapping("/api/pharmacy/supplier/getAll")
Response getAllSuppliers();

@DeleteMapping("/api/pharmacy/supplier/delete/{supplierId}")
Response deleteSupplier(@PathVariable String supplierId);


////////////////MedicanDetails/////////////////
@PostMapping("/api/pharmacy/medicines/addMedicine")
Response addMedicine(@RequestBody MedicineDetailDTO dto);

@GetMapping("/api/pharmacy/medicines/getMedicineById/{id}")
Response getMedicineById(@PathVariable String id);

@GetMapping("/api/pharmacy/medicines/getAllMedicines")
Response getAllMedicines();

@PutMapping("/api/pharmacy/medicines/updateMedicineById/{id}")
Response updateMedicine(@PathVariable String id, @RequestBody MedicineDetailDTO dto);

@DeleteMapping("/api/pharmacy/medicines/deleteMedicineById/{id}")
Response deleteMedicine(@PathVariable String id);

                     ////////////Stock/////////////

@PostMapping("/api/pharmacy/stockMaster/add")
Response addStock(@RequestBody StockDTO stock);

@PutMapping("/api/pharmacy/stockMaster/update/{id}")
Response updateStock(@PathVariable("id") String id, @RequestBody StockDTO stock);

@GetMapping("/api/pharmacy/stockMaster/{id}")
Response getStockById(@PathVariable("id") String id);

@GetMapping("/api/pharmacy/stockMaster/product//{productId}")
Response getStockByProductId(@PathVariable("productId") String productId);

@GetMapping("/api/pharmacy/stockMaster/all")
Response getAllStock();

@DeleteMapping("/api/pharmacy/stockMaster/delete/{id}")
Response deleteStock(@PathVariable("id") String id);

@PutMapping("/api/pharmacy/stockMaster/status/{id}")
Response changeStatus(@PathVariable("id") String id, @RequestParam("status") String status);



                     // STOCK LEDGER — PURCHASE STOCK////
    @PostMapping("/stock/purchase/{purchaseBillNo}")
    Response addPurchase(
            @PathVariable("purchaseBillNo") String purchaseBillNo,
            @RequestBody StockDTO dto
    );

    // ------------------------------------------------------------------------------
    // STOCK LEDGER — SALE STOCK
    // ------------------------------------------------------------------------------
    @PostMapping("/stock/sale/{productId}/{batchNo}/{qty}/{saleId}")
    Response addSale(
            @PathVariable("productId") String productId,
            @PathVariable("batchNo") String batchNo,
            @PathVariable("qty") int qty,
            @PathVariable("saleId") String saleId
    );

    // ------------------------------------------------------------------------------
    // STOCK LEDGER — DAMAGE STOCK
    // ------------------------------------------------------------------------------
    @PostMapping("/stock/damage/{productId}/{batchNo}/{qty}/{reason}")
    Response addDamage(
            @PathVariable("productId") String productId,
            @PathVariable("batchNo") String batchNo,
            @PathVariable("qty") int qty,
            @PathVariable("reason") String reason
    );
    
    
    ///OP-SALES
    
    @PostMapping("/api/pharmacy/op-sales/createOpSales")
    public ResponseEntity<Response> createOpSales(
            @RequestBody OpSalesRequest request);
    
    @PutMapping("/api/pharmacy/op-sales/updateSale")
    public ResponseEntity<Response> updateOpSales(
            @RequestBody OpSalesRequest request);
    
    @GetMapping("/api/pharmacy/op-sales/getAllOpSales/{clinicId}/{branchId}")
    public ResponseEntity<Response> getAllOpSales(
            @PathVariable String clinicId,
            @PathVariable String branchId);
    
    @GetMapping("/api/pharmacy/op-sales/bill/{billNo}")
    public ResponseEntity<Response> getByBillNo(
            @PathVariable String billNo);
    
    @GetMapping("/api/pharmacy/op-sales/id/{id}")
    public ResponseEntity<Response> getById(
            @PathVariable String id);
    
    @GetMapping("/api/pharmacy/op-sales/{clinicId}/{branchId}/opno/{opNo}")
    public ResponseEntity<Response> getByOpNo(
    		 @PathVariable String clinicId,
    		 @PathVariable String branchId,
    		 @PathVariable String opNo) ;
    
    @DeleteMapping("/api/pharmacy/op-sales/{clinicId}/{branchId}/{id}")
    public ResponseEntity<Response> deleteOpSales(
            @PathVariable String id,
            @PathVariable String clinicId,
            @PathVariable String branchId);
    
    @GetMapping("/api/pharmacy/op-sales/filter")
    public ResponseEntity<Response> filterOpSales(
            @RequestParam(required = false) String clinicId,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String consultingDoctor,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) ;



//// SALES RETURN
      
    @PostMapping("/api/pharmacy/sales-return/create-sales-return")
    public ResponseEntity<ApiResponse<SalesReturnCreateResponse>> create(
            @Valid @RequestBody SalesReturnRequest request);
    
    @GetMapping("/api/pharmacy/sales-return/getAllSalesReturns")
    public ResponseEntity<Response> getAllSalesReturns();
    
    @GetMapping("/api/pharmacy/sales-return/get-by-returnNo/{returnNo}")
    public ResponseEntity<ApiResponse<SalesReturnResponse>> getByReturnNo(
            @PathVariable String returnNo);
    
    
    @PutMapping("/api/pharmacy/sales-return/update/{returnNo}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable String returnNo,
            @Valid @RequestBody SalesReturnUpdateRequest request);
    
    @DeleteMapping("/api/pharmacy/sales-return/cancel/{returnNo}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable String returnNo);
    
    @GetMapping("/api/pharmacy/sales-return/filter")
    public ResponseEntity<ApiResponse<List<SalesReturnSummaryResponse>>> filter(
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String mobileNo,
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) String returnType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String clinicId,
            @RequestParam(required = false) String branchId);


    
    
    ///MEDICINEINVENTORY
    
    @GetMapping("/api/pharmacy/medicines/medicineInventory/{medicineId}")
    public ResponseEntity<MedicineInventoryDto> getMedicineInventory(@PathVariable String medicineId);

    @GetMapping("/api/pharmacy/sales-return/getAllSales/hospitalId/{hospitalId}/branchId/{branchId}")
    public ResponseEntity<Response> getAllSalesByClinicAndBranchId(@PathVariable String hospitalId,
    		@PathVariable String branchId);
    
}




    

