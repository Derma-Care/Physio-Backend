package com.AdminService.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.AdminService.dto.BookingRequset;
import com.AdminService.dto.BookingResponse;
import com.AdminService.dto.BookingResponseDTO;
import com.AdminService.feign.BookingFeign;
import com.AdminService.util.ExtractFeignMessage;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;

import feign.FeignException;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingFeign bookingFeign;
    @Override
    public Response bookService(BookingRequset req) {
        try {
            ResponseEntity<?> res = bookingFeign.bookService(req); // call POST API
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value()); // take status from Booking Service
            return response;
        } catch (FeignException e) {
            Response response = new Response();
            response.setStatus(e.status());
            response.setMessage(ExtractFeignMessage.clearMessage(e)); // get exception message from Booking Service
            response.setSuccess(false);
            return response;
        }
    }


    @Override
    public ResponseStructure<List<BookingResponse>> getAllBookedServices() {
        try {
            ResponseEntity<ResponseStructure<List<BookingResponse>>> res = bookingFeign.getAllBookedService();
            return res.getBody(); // return exactly what BookingService sends
        } catch (FeignException e) {
            throw e; // propagate exception to controller
        }
    }

    @Override
    public Response deleteBookedService(String id) {
        try {
            ResponseEntity<ResponseStructure<BookingResponse>> res = bookingFeign.deleteBookedService(id);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getBody() != null ? res.getBody().getStatusCode() : res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e; // propagate exception to controller
        }
    }

    @Override
    public Response getBookingByDoctorId(String doctorId) {
        try {
            ResponseEntity<ResponseStructure<List<BookingResponse>>> res = bookingFeign.getBookingByDoctorId(doctorId);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getBody() != null ? res.getBody().getStatusCode() : res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    public Response getBookedServiceById(String bookingId) {
        try {
            ResponseEntity<ResponseStructure<BookingResponseDTO>> res = bookingFeign.getBookedService(bookingId);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getBody() != null ? res.getBody().getStatusCode() : res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    public Response getAppointmentsByPatientId(String patientId) {
        try {
            ResponseEntity<?> res = bookingFeign.getAppointmentByPatientId(patientId);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    public Response updateAppointment(BookingResponseDTO bookingResponseDTO) {
        try {
            ResponseEntity<?> res = bookingFeign.updateAppointment(bookingResponseDTO);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    public Response getPatientDetailsForConsent(String bookingId, String patientId, String mobileNumber) {
        try {
            ResponseEntity<Response> res = bookingFeign.getPatientDetailsForConsentForm(bookingId, patientId, mobileNumber);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    public Response getInProgressAppointments(String mobileNumber) {
        try {
            ResponseEntity<?> res = bookingFeign.inProgressAppointments(mobileNumber);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());
            return response;
        } catch (FeignException e) {throw e;
        }
}
}
