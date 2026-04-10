package com.dermacare.doctorservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dermacare.doctorservice.dto.MedicineTypeDTO;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.service.MedicineTypeService;

@RestController
@RequestMapping("/doctors")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class MedicineTypeController {

    @Autowired
    private MedicineTypeService service; 

    @PostMapping("/search-or-add")
    public ResponseEntity<Response> searchOrAddMedicineType(@RequestBody MedicineTypeDTO dto) {
        Response response = service.searchOrAddMedicineType(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/getMedicineTypes/{id}")
    public ResponseEntity<Response> getMedicineTypes(@PathVariable String id) {

        Response response = service.getMedicineTypesById(id);

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }
    
    @GetMapping("/getAllMedicineTypes")
    public ResponseEntity<Response> getAllMedicineTypes() {

        Response response = service.getAllMedicineTypes();

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }
}
