package com.dermacare.doctorservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.dto.VitalsDTO;
import com.dermacare.doctorservice.service.DoctorVitalsService;

@RestController
@RequestMapping("/doctors")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DoctorVitalsController {

    @Autowired
    private DoctorVitalsService doctorVitalsService;

    /**
     * Add new vitals for a booking
     */
    @PostMapping("/addVitals/{bookingId}")
    public ResponseEntity<Response> addVitals(@PathVariable String bookingId,
                                              @RequestBody VitalsDTO dto) {
        return doctorVitalsService.addVitals(bookingId, dto);
    }

    /**
     * Get vitals by bookingId and patientId
     */
    @GetMapping("/getVitals/{bookingId}/{patientId}")
    public ResponseEntity<Response> getVitals(@PathVariable String bookingId,
                                              @PathVariable String patientId) {
        return doctorVitalsService.getVitals(bookingId, patientId);
    }

    /**
     * Delete vitals by bookingId and patientId
     */
    @DeleteMapping("/deleteVitals/{bookingId}/{patientId}")
    public ResponseEntity<Response> deleteVitals(@PathVariable String bookingId,
                                                 @PathVariable String patientId) {
        return doctorVitalsService.deleteVitals(bookingId, patientId);
    }

    /**
     * Update vitals by bookingId and patientId
     */
    @PutMapping("/updateVitals/{bookingId}/{patientId}")
    public ResponseEntity<Response> updateVitals(@PathVariable String bookingId,
                                                 @PathVariable String patientId,
                                                 @RequestBody VitalsDTO dto) {
        return doctorVitalsService.updateVitals(bookingId, patientId, dto);
    }
}
