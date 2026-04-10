package com.dermacare.doctorservice.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.dto.VitalsDTO;
import com.dermacare.doctorservice.feignclient.ClinicAdminServiceClient;
import com.dermacare.doctorservice.service.DoctorVitalsService;

@Service
public class DoctorVitalsServiceImpl implements DoctorVitalsService {

    @Autowired
    private ClinicAdminServiceClient clinicAdminServiceClient;

    /**
     * Add Vitals for a booking
     */
    @Override
    public ResponseEntity<Response> addVitals(String bookingId, VitalsDTO dto) {
        // Directly forward Clinic Admin response
        return clinicAdminServiceClient.addVitals(bookingId, dto);
    }

    /**
     * Get Vitals by bookingId and patientId
     */
    @Override
    public ResponseEntity<Response> getVitals(String bookingId, String patientId) {
        return clinicAdminServiceClient.getVitals(bookingId, patientId);
    }

    /**
     * Delete Vitals by bookingId and patientId
     */
    @Override
    public ResponseEntity<Response> deleteVitals(String bookingId, String patientId) {
        return clinicAdminServiceClient.delVitals(bookingId, patientId);
    }

    /**
     * Update Vitals by bookingId and patientId
     */
    @Override
    public ResponseEntity<Response> updateVitals(String bookingId, String patientId, VitalsDTO dto) {
        return clinicAdminServiceClient.updateVitals(bookingId, patientId, dto);
    }
}
