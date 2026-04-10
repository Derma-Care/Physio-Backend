package com.AdminService.controller;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.AdminService.dto.BookingRequset;
import com.AdminService.dto.BookingResponse;
import com.AdminService.dto.BookingResponseDTO;
import com.AdminService.service.BookingServiceImpl;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;
@RestController
@RequestMapping("/admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class BookingController {
    @Autowired
    private BookingServiceImpl serviceImpl;
    @PostMapping("/bookService")
    public ResponseEntity<?> bookService(@RequestBody BookingRequset req) {
        Response response = serviceImpl.bookService(req);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    @GetMapping("/getAllBookedServices")
    public ResponseEntity<ResponseStructure<List<BookingResponse>>> getAllBookedServices() {
        ResponseStructure<List<BookingResponse>> response = serviceImpl.getAllBookedServices();
        HttpStatus status = response.getHttpStatus() != null ? response.getHttpStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(response);
    }

    @DeleteMapping("/deleteServiceByBookingId/{id}")
    public ResponseEntity<Object> deleteBookedService(@PathVariable String id) {
        Response response = serviceImpl.deleteBookedService(id);
        return ResponseEntity.status(response.getStatus()).body(response.getData() != null ? response.getData() : response);
    }

    @GetMapping("/getBookingByDoctorId/{doctorId}")
    public ResponseEntity<Object> getBookingByDoctorId(@PathVariable String doctorId) {
        Response response = serviceImpl.getBookingByDoctorId(doctorId);
        return ResponseEntity.status(response.getStatus()).body(response.getData() != null ? response.getData() : response);
    }

    @GetMapping("/getBookedServiceById/{bookingId}")
    public ResponseEntity<Object> getBookedServiceById(@PathVariable String bookingId) {
        Response response = serviceImpl.getBookedServiceById(bookingId);
        return ResponseEntity.status(response.getStatus()).body(response.getData() != null ? response.getData() : response);
    }

    @GetMapping("/getAppointmentsByPatientId/{patientId}")
    public ResponseEntity<Object> getAppointmentsByPatientId(@PathVariable String patientId) {
        Response response = serviceImpl.getAppointmentsByPatientId(patientId);
        return ResponseEntity.status(response.getStatus()).body(response.getData() != null ? response.getData() : response);
    }

    @PutMapping("/updateAppointment")
    public ResponseEntity<Object> updateAppointment(@RequestBody BookingResponseDTO bookingResponseDTO) {
        Response response = serviceImpl.updateAppointment(bookingResponseDTO);
        return ResponseEntity.status(response.getStatus()).body(response.getData() != null ? response.getData() : response);
    }
    

    @GetMapping("/getPatientDetailsForConsent/{bookingId}/{patientId}/{mobileNumber}")
    public ResponseEntity<Object> getPatientDetailsForConsent(
            @PathVariable String bookingId,
            @PathVariable String patientId,
            @PathVariable String mobileNumber) {
        Response response = serviceImpl.getPatientDetailsForConsent(bookingId, patientId, mobileNumber);
        return ResponseEntity.status(response.getStatus()).body(response.getData() != null ? response.getData() : response);
}

    @GetMapping("/getInProgressAppointments/{mobileNumber}")
    public ResponseEntity<Object> getInProgressAppointments(@PathVariable String mobileNumber) {
        Response response = serviceImpl.getInProgressAppointments(mobileNumber);
        return ResponseEntity.status(response.getStatus()).body(response.getData() != null ? response.getData() : response);
}
}