package com.pharmacyManagement.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pharmacyManagement.dto.CityDTO;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.service.CityService;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/city")
@RequiredArgsConstructor
public class CityController {
    private final CityService cityService;

    // =============================
    //   SAVE CITY
    // =============================
    @PostMapping("/save")
    public ResponseEntity<Response> saveCity(@RequestBody CityDTO dto) {
        Response response = cityService.addCity(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // =============================
    //   UPDATE CITY
    // =============================
    @PutMapping("/update/{id}")
    public ResponseEntity<Response> updateCity(
            @PathVariable String id,
            @RequestBody CityDTO dto) {

        Response response = cityService.updateCity(id, dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // =============================
    //   GET BY ID
    // =============================
    @GetMapping("/getById/{id}")
    public ResponseEntity<Response> getCityById(@PathVariable String id) {
        Response response = cityService.getCityById(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // =============================
    //   GET ALL CITIES
    // =============================
    @GetMapping("/all")
    public ResponseEntity<Response> getAllCities() {
        Response response = cityService.getAllCities();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // =============================
    //   DELETE CITY
    // =============================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Response> deleteCity(@PathVariable String id) {
        Response response = cityService.deleteCity(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
