package com.AdminService.service;

import java.util.List;

import com.AdminService.dto.BookingRequset;
import com.AdminService.dto.BookingResponse;
import com.AdminService.dto.BookingResponseDTO;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;

public interface BookingService {

    ResponseStructure<List<BookingResponse>> getAllBookedServices();

    Response deleteBookedService(String id);

    Response getBookingByDoctorId(String doctorId);

    Response getBookedServiceById(String bookingId);

    Response getAppointmentsByPatientId(String patientId);

    Response updateAppointment(BookingResponseDTO bookingResponseDTO);

    Response getPatientDetailsForConsent(String bookingId, String patientId, String mobileNumber);

    Response getInProgressAppointments(String mobileNumber);

	Response bookService(BookingRequset req);
}