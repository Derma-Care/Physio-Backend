package com.pharmacyManagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pharmacyManagement.dto.AreaDTO;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.service.AreaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/area")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    @PostMapping("/save")
    public ResponseEntity<Response> saveArea(@RequestBody AreaDTO dto) {
        Response res = areaService.addArea(dto);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Response> updateArea(
            @PathVariable String id,
            @RequestBody AreaDTO dto) {

        Response res = areaService.updateArea(id, dto);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/getById/{id}")
    public ResponseEntity<Response> getAreaById(@PathVariable String id) {
        Response res = areaService.getAreaById(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/all")
    public ResponseEntity<Response> getAllAreas() {
        Response res = areaService.getAllAreas();
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Response> deleteArea(@PathVariable String id) {
        Response res = areaService.deleteArea(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    @GetMapping("/city/{cityId}")
    public ResponseEntity<Response> getAreasByCity(@PathVariable String cityId) {
        Response response = areaService.getAreasByCityId(cityId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

}
