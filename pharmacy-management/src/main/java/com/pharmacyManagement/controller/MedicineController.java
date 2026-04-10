package com.pharmacyManagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pharmacyManagement.dto.MedicineDTO;
import com.pharmacyManagement.dto.MedicineInventoryDto;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.service.MedicineService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService service;

    // 1️ ADD
    @PostMapping("/addMedicine")
    public ResponseEntity<Response> addMedicine(@RequestBody MedicineDTO dto) {

        Response response = service.addMedicine(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // 2️ UPDATE
    @PutMapping("/updateMedicineById/{id}")
    public ResponseEntity<Response> updateMedicine(@PathVariable String id,
                                                   @RequestBody MedicineDTO dto) {

        Response response = service.updateMedicine(id, dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // 3️ GET ALL
    @GetMapping("/getAllMedicines")
    public ResponseEntity<Response> getAllMedicines() {

        Response response = service.getAllMedicines();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // 4️ GET BY ID
    @GetMapping("/getMedicineByMedicineId/{id}")
    public ResponseEntity<Response> getMedicineById(@PathVariable String id) {

        Response response = service.getMedicineById(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    

    // 5️ DELETE
    @DeleteMapping("deleteMedicineByMedicineId/{id}")
    public ResponseEntity<Response> deleteMedicine(@PathVariable String id) {

        Response response = service.deleteMedicine(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    @GetMapping("/getMedicineByBarcode/{barcode}")
    public ResponseEntity<Response> getMedicineByBarcode(
            @PathVariable String barcode) {

        Response response = service.getMedicineByBarcode(barcode);

        return ResponseEntity.status(response.getStatus())
                .body(response);
    }
    
    
    @GetMapping("/medicineInventory/{medicineId}")
    public ResponseEntity<MedicineInventoryDto> getMedicineInventory(@PathVariable String medicineId) {

        MedicineInventoryDto response =
        		service.getMedicineInventory(medicineId);

        return ResponseEntity.ok(response);
    }
}