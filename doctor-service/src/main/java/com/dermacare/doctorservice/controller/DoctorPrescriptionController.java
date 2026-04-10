package com.dermacare.doctorservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dermacare.doctorservice.dto.DoctorPrescriptionDTO;
import com.dermacare.doctorservice.dto.MedicineDTO;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.service.DoctorPrescriptionService;

@RestController
@RequestMapping("/doctors")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DoctorPrescriptionController {

    @Autowired
    private DoctorPrescriptionService service;

    @PostMapping("/createPrescription")
    public ResponseEntity<Response> createPrescription(@RequestBody DoctorPrescriptionDTO dto) {
        Response res = service.createPrescription(dto);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/getAllPrescriptions")
    public ResponseEntity<Response> getAllPrescriptions() {
        Response res = service.getAllPrescriptions();
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/getPrescriptionById/{id}")
    public ResponseEntity<Response> getPrescriptionById(@PathVariable String id) {
        Response res = service.getPrescriptionById(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/getMedicineById/{medicineId}")
    public ResponseEntity<Response> getMedicineById(@PathVariable String medicineId) {
        Response res = service.getMedicineById(medicineId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    
 // DoctorPrescriptionController.java
    
    @GetMapping("/searchMedicines/{keyword}")
    public ResponseEntity<Response> searchMedicines(@PathVariable String keyword) {
        Response response = service.searchMedicinesByName(keyword);
        return ResponseEntity.status(response.getStatus()).body(response);
    }



    @DeleteMapping("/deletePrescription/{id}")
    public ResponseEntity<Response> deletePrescription(@PathVariable String id) {
        Response res = service.deletePrescription(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    
    @DeleteMapping("/deleteMedicine/{medicineId}")
    public ResponseEntity<Response> deleteMedicine(@PathVariable String medicineId) {
        Response res = service.deleteMedicineById(medicineId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    @GetMapping("/getPrescriptionsByClinicId/{clinicId}")
    public ResponseEntity<Response> getPrescriptionsByClinicId(@PathVariable String clinicId) {
        Response response = service.getPrescriptionsByClinicId(clinicId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @PutMapping("/updatePrescription/{id}")
    public ResponseEntity<Response> updatePrescription(@PathVariable String id,
                                                       @RequestBody DoctorPrescriptionDTO dto) {
        Response response = service.updatePrescription(id, dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    
    @PutMapping("/updateMedicine/{medicineId}")
    public ResponseEntity<Response> updateMedicine(@PathVariable String medicineId,
                                                   @RequestBody MedicineDTO dto) {
        Response response = service.updateMedicineById(medicineId, dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

//    
//    @GetMapping("/medicineTypes")
//    public List<MedicineType> getAllMedicineTypes() {
//        
//        return Arrays.asList(MedicineType.values());
//    }


}
