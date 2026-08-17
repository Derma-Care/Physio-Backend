package com.chiselon.adminservice.service;

import com.chiselon.adminservice.dto.BookingRequset;
import com.chiselon.adminservice.dto.BookingResponse;
import com.chiselon.adminservice.dto.BookingResponseDTO;
import com.chiselon.adminservice.util.Response;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

public interface BookingService {


    ResponseEntity<Page<BookingResponse>>  getAllBookedServices(int page) ;

    Response deleteBookedService(String id);

    public ResponseEntity<?> getBookingByDoctorId(String doctorId,
                                                  int page,
                                                  int size);

    Response getBookedServiceById(String bookingId);

    Response getAppointmentsByPatientId(String clinicId,String patientId,int page);

    Response updateAppointment(BookingResponseDTO bookingResponseDTO);

    Response getPatientDetailsForConsent(String bookingId, String patientId, String mobileNumber);

    Response getInProgressAppointments(String mobileNumber);

    public ResponseEntity<?> physioAppointment(BookingRequset req);

    }