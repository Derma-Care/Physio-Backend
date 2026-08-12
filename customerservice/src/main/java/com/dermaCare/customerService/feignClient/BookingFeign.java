package com.dermaCare.customerService.feignClient;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dermaCare.customerService.dto.BookingRequset;
import com.dermaCare.customerService.dto.BookingResponse;
import com.dermaCare.customerService.util.ResponseStructure;

@FeignClient(
        name = "bookingservice"
        // fallback = BookingFeignFallback.class
)
public interface BookingFeign {

    @GetMapping("/api/v1/getBookedServiceById/{id}")
    ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(
            @PathVariable("id") String id);

    @PutMapping("/api/v1/updateAppointment")
    ResponseEntity<?> updateAppointment(
            @RequestBody BookingResponse bookingResponse);

    @PostMapping("/api/v1/bookService")
    public  ResponseEntity<?> bookService(@RequestBody BookingResponse req);


    @DeleteMapping("/api/v1/deleteService/{id}")
    ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedService(
            @PathVariable("id") String id);

    @GetMapping("/api/v1/getBookedServicesByMobileNumber/{mobileNumber}")
    ResponseEntity<ResponseStructure<List<BookingResponse>>> getCustomerBookedServices(
            @PathVariable("mobileNumber") String mobileNumber);

    @GetMapping("/api/v1/getAllBookedServices")
    ResponseEntity<ResponseStructure<List<BookingResponse>>> getAllBookedService();

    @GetMapping("/api/v1/getAllBookedServices/{doctorId}")
    ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByDoctorId(
            @PathVariable("doctorId") String doctorId);

    @GetMapping("/api/v1/getBookedServicesByServiceId/{serviceId}")
    ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByServiceId(
            @PathVariable("serviceId") String serviceId);

    @GetMapping("/api/v1/getBookedServicesByClinicId/{clinicId}")
    ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByClinicId(
            @PathVariable("clinicId") String clinicId);

    @GetMapping("/api/v1/appointments/FilterbyRelation/{customerId}")
	public ResponseEntity<?> retrieveAppointnmentsByRelation(@PathVariable("customerId") String customerId);
    
    @GetMapping("/api/v1/getInProgressAppointments/{mobileNumber}")
    ResponseEntity<?> inProgressAppointments(
            @PathVariable("mobileNumber") String mobileNumber);

    @GetMapping("/api/v1/getDoctorFutureAppointments/{doctorId}")
    ResponseEntity<?> getDoctorFutureAppointments(
            @PathVariable("doctorId") String doctorId);

    @GetMapping("/api/v1/getAllBookedServicesByBranchId/{branchId}")
    ResponseEntity<ResponseStructure<List<BookingResponse>>> getAllBookedServicesByBranchId(
            @PathVariable("branchId") String branchId);

    @GetMapping("/api/v1/getBookedServicesByClinicIdWithBranchId/{clinicId}/{branchId}")
    ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookedServicesByClinicIdWithBranchId(
            @PathVariable("clinicId") String clinicId,
            @PathVariable("branchId") String branchId);

    @GetMapping("/api/v1/booking/customerId/{customerId}")
    ResponseEntity<ResponseStructure<List<Map<String, Object>>>> getBookingByCustomerId(
            @PathVariable("customerId") String customerId);

    @GetMapping("/api/v1/appointments/Inprogress/{customerId}")
    ResponseEntity<?> getInprogressAppointmentsByCustomerId(
            @PathVariable("customerId") String customerId);

    @GetMapping("/api/v1/appointments/FilterbyRelation/{customerId}")
    ResponseEntity<?> retrieveAppointmentsByRelation(
            @PathVariable("customerId") String customerId);

    @GetMapping("/api/v1/appointments/patientId/{patientId}")
    ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByPatientId(
            @PathVariable("patientId") String patientId);

    @GetMapping("/api/v1/appointments/Inprogress/patientId/{patientId}/{clinicId}")
    ResponseEntity<?> getInprogressAppointmentsByPatientId(
            @PathVariable("patientId") String patientId,
            @PathVariable("clinicId") String clinicId);

    @PostMapping("/api/v1/bookPhysioAppointment")
    ResponseEntity<?> bookPhysioAppointment(
            @RequestBody BookingRequset request);

    @GetMapping("/api/v1/booking/completed/customerId/{customerId}")
    ResponseEntity<ResponseStructure<List<Map<String, Object>>>> getCompletedBookingByCustomerId(
            @PathVariable("customerId") String customerId);
}