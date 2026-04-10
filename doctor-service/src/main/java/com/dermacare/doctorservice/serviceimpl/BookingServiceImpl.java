package com.dermacare.doctorservice.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.dermacare.doctorservice.feignclient.BookingFeignClient;
import com.dermacare.doctorservice.service.BookingService;

import feign.FeignException;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingFeignClient bookingFeignClient;

    @Override
    public  ResponseEntity<?> getAppointmentsByPatientId(String patientId) {
        try {
            return bookingFeignClient.getAppointmentByPatientId(patientId);
        } catch (FeignException ex) {
            return ResponseEntity.status(ex.status()).body(ex.contentUTF8());
        }
    }

    @Override
    public  ResponseEntity<?> searchAppointmentsByInput(String input) {
        try {
            return bookingFeignClient.getAppointsByInput(input);
        } catch (FeignException ex) {
            return ResponseEntity.status(ex.status()).body(ex.contentUTF8());
        }
    }

    @Override
    public  ResponseEntity<?> getTodaysAppointments(String clinicId, String doctorId) {
        try {
            return bookingFeignClient.getTodayDoctorAppointmentsByDoctorId(clinicId, doctorId);
        } catch (FeignException ex) {
            return ResponseEntity.status(ex.status()).body(ex.contentUTF8());
        }
    }

    @Override
    public  ResponseEntity<?> getFilteredAppointments(String clinicId, String doctorId, String number) {
        try {
            return bookingFeignClient.filterDoctorAppointmentsByDoctorId(clinicId, doctorId, number);
        } catch (FeignException ex) {
            return ResponseEntity.status(ex.status()).body(ex.contentUTF8());
        }
    }

    @Override
    public  ResponseEntity<?> getCompletedAppointments(String clinicId, String doctorId) {
        try {
            return bookingFeignClient.filterDoctorAppointmentsByDoctorId(clinicId, doctorId);
        } catch (FeignException ex) {
            return ResponseEntity.status(ex.status()).body(ex.contentUTF8());
        }
    }

    @Override
    public  ResponseEntity<?> getConsultationTypeCounts(String clinicId, String doctorId) {
        try {
            return bookingFeignClient.getSizeOfConsultationTypesByDoctorId(clinicId, doctorId);
        } catch (FeignException ex) {
            return ResponseEntity.status(ex.status()).body(ex.contentUTF8());
        }
    }
    @Override
    public ResponseEntity<?> getInProgressAppointments(String mobileNumber) {
        try {
            return bookingFeignClient.inProgressAppointments(mobileNumber);
        } catch (FeignException ex) {
            return ResponseEntity.status(ex.status()).body(ex.contentUTF8());
        }
    }
    
    @Override
    public ResponseEntity<?> getAllBookedServicesByDoctorId(String doctorId) {
        try {
            return bookingFeignClient.getBookingByDoctorId(doctorId);
        } catch (FeignException ex) {
            return ResponseEntity.status(ex.status()).body(ex.contentUTF8());
        }
    }
    @Override
    public ResponseEntity<?> getDoctorFutureAppointments(String doctorId) {
        try {
            return bookingFeignClient.getDoctorFutureAppointments(doctorId);
        } catch (FeignException ex) {
            return ResponseEntity.status(ex.status()).body(ex.contentUTF8());
        }
    }

}

