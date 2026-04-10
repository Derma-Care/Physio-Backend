package com.dermacare.doctorservice.service;

import org.springframework.http.ResponseEntity;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.dto.VitalsDTO;

public interface DoctorVitalsService {

    /**
     * Add new vitals for a booking
     */
    ResponseEntity<Response> addVitals(String bookingId, VitalsDTO dto);

    /**
     * Get vitals by bookingId and patientId
     */
    ResponseEntity<Response> getVitals(String bookingId, String patientId);

    /**
     * Delete vitals by bookingId and patientId
     */
    ResponseEntity<Response> deleteVitals(String bookingId, String patientId);

    /**
     * Update vitals by bookingId and patientId
     */
    ResponseEntity<Response> updateVitals(String bookingId, String patientId, VitalsDTO dto);
}
