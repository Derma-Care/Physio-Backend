package com.AdminService.service;

import java.util.List;

import com.AdminService.dto.BookingRequset;
import com.AdminService.dto.BookingResponse;
import com.AdminService.dto.BookingResponseDTO;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;
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