package com.clinicadmin.utils;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.clinicadmin.dto.BookingRequset;
import com.clinicadmin.dto.BookingResponse;
import com.clinicadmin.dto.ClinicDTO;
import com.clinicadmin.dto.DoctorRatingNotificationDTO;
import com.clinicadmin.dto.PriceDropAlertDto;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.TempBlockingSlot;
import com.clinicadmin.dto.UpdateClinicLoginCredentialsDTO;
import com.clinicadmin.entity.QuestionsByPartEntity;
import com.clinicadmin.feignclient.AdminServiceClient;
import com.clinicadmin.feignclient.BookingFeign;
import com.clinicadmin.feignclient.CustomerServiceFeignClient;
import com.clinicadmin.feignclient.NotificationFeign;
import com.clinicadmin.feignclient.PhysiotherapyFeignClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.ws.rs.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeignImpl {
	
	    private final AdminServiceClient adminServiceClient;
	    private final BookingFeign bookingFeign;
	    private final CustomerServiceFeignClient customerServiceFeignClient;
	    private final NotificationFeign notificationFeign;
	    private final PhysiotherapyFeignClient physiotherapyFeign;
	    
	    private RuntimeException getFallbackException(Exception ex) {

	        if (ex instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
	            return new ResponseStatusException(
	                    HttpStatus.TOO_MANY_REQUESTS,
	                    "Too many requests. Please try again after some time."
	                    );      
	        }else if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
	            return new ResponseStatusException(
	                    HttpStatus.SERVICE_UNAVAILABLE,
	                    "Booking Service is temporarily unavailable"); 
	    }else{ return new ResponseStatusException(
	                HttpStatus.SERVICE_UNAVAILABLE,
	                "Booking Service is temporarily unavailable");}
	    }
	  
	    @CircuitBreaker(name = "adminService", fallbackMethod = "clinicLoginFallback")
	    @Retry(name = "adminService", fallbackMethod = "clinicLoginFallback")
	    public Response clinicLogin(String userName) {
	        return adminServiceClient.clinicLogin(userName);
	    }
	    public Response clinicLoginFallback(String userName, Exception ex) {
	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "updateClinicCredentialsFallback")
	    @Retry(name = "adminService", fallbackMethod = "updateClinicCredentialsFallback")
	    public Response updateClinicCredentials(
	            String token,
	            UpdateClinicLoginCredentialsDTO dto,
	            String userName) {

	        return adminServiceClient.updateClinicCredentials(token, dto, userName);
	    }
	    public Response updateClinicCredentialsFallback(
	            String token,
	            UpdateClinicLoginCredentialsDTO dto,
	            String userName,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "getClinicByIdFallback")
	    @Retry(name = "adminService", fallbackMethod = "getClinicByIdFallback")
	    public Response getClinicById(String token, String clinicId) {
	        return adminServiceClient.getClinicById(token, clinicId).getBody();
	    }
	    public Response getClinicByIdFallback(
	            String token,
	            String clinicId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "getAllClinicsFallback")
	    @Retry(name = "adminService", fallbackMethod = "getAllClinicsFallback")
	    public Response getAllClinics(String token) {
	        return adminServiceClient.getAllClinics(token).getBody();
	    }
	    public Response getAllClinicsFallback(
	            String token,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "updateClinicFallback")
	    @Retry(name = "adminService", fallbackMethod = "updateClinicFallback")
	    public Response updateClinic(
	            String token,
	            String clinicId,
	            ClinicDTO clinic) {

	        return adminServiceClient.updateClinic(token, clinicId, clinic);
	    }
	    public Response updateClinicFallback(
	            String token,
	            String clinicId,
	            ClinicDTO clinic,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "deleteClinicFallback")
	    @Retry(name = "adminService", fallbackMethod = "deleteClinicFallback")
	    public Response deleteClinic(
	            String token,
	            String clinicId) {

	        return adminServiceClient.deleteClinic(token, clinicId);
	    }
	    public Response deleteClinicFallback(
	            String token,
	            String clinicId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "getHospitalUsingRecommendentaionFallback")
	    @Retry(name = "adminService", fallbackMethod = "getHospitalUsingRecommendentaionFallback")
	    public Response getHospitalUsingRecommendentaion(String token) {
	        return adminServiceClient
	                .getHospitalUsingRecommendentaion(token)
	                .getBody();
	    }
	    public Response getHospitalUsingRecommendentaionFallback(
	            String token,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "firstRecommendedTureClincsFallback")
	    @Retry(name = "adminService", fallbackMethod = "firstRecommendedTureClincsFallback")
	    public Response firstRecommendedTureClincs() {
	        return adminServiceClient
	                .firstRecommendedTureClincs()
	                .getBody();
	    }
	    public Response firstRecommendedTureClincsFallback(Exception ex) {
	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "getBranchByClinicAndBranchIdFallback")
	    @Retry(name = "adminService", fallbackMethod = "getBranchByClinicAndBranchIdFallback")
	    public Response getBranchByClinicAndBranchId(
	            String token,
	            String clinicId,
	            String branchId) {

	        return adminServiceClient
	                .getBranchByClinicAndBranchId(token, clinicId, branchId)
	                .getBody();
	    }
	    public Response getBranchByClinicAndBranchIdFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "getBranchByIdFallback")
	    @Retry(name = "adminService", fallbackMethod = "getBranchByIdFallback")
	    public Response getBranchById(
	            String token,
	            String branchId) {

	        return adminServiceClient
	                .getBranchById(token, branchId)
	                .getBody();
	    }
	    public Response getBranchByIdFallback(
	            String token,
	            String branchId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "getAllBranchesFallback")
	    @Retry(name = "adminService", fallbackMethod = "getAllBranchesFallback")
	    public Response getAllBranches(String token) {
	        return adminServiceClient
	                .getAllBranches(token)
	                .getBody();
	    }
	    public Response getAllBranchesFallback(
	            String token,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "getBranchByClinicIdFallback")
	    @Retry(name = "adminService", fallbackMethod = "getBranchByClinicIdFallback")
	    public ResponseEntity<?> getBranchByClinicId(
	            String token,
	            String clinicId) {

	        return adminServiceClient.getBranchByClinicId(token, clinicId);
	    }
	    public ResponseEntity<?> getBranchByClinicIdFallback(
	            String token,
	            String clinicId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "adminService", fallbackMethod = "getDefaultAdminPermissionsFallback")
	    @Retry(name = "adminService", fallbackMethod = "getDefaultAdminPermissionsFallback")
	    public Map<String, List<String>> getDefaultAdminPermissions(
	            String token) {

	        return adminServiceClient
	                .getDefaultAdminPermissions(token)
	                .getBody();
	    }
	    public Map<String, List<String>> getDefaultAdminPermissionsFallback(
	            String token,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }
	    /// BOOKING SERVICE
	    
	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
	    public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(
	            String token,
	            String id) {

	        return bookingFeign.getBookedService(token, id);
	    }

	    private ResponseEntity<ResponseStructure<BookingResponse>> getBookedServiceFallback(
	            String token,
	            String id,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= APPOINTMENTS BY PATIENT ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getAppointmentsByPatientIdFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getAppointmentsByPatientIdFallback")
	    public ResponseEntity<?> getAppointmentsByPatientId(
	            String token,
	            String patientId,
	            int page,
	            int size) {

	        return bookingFeign.getAppointmentsByPatientId(
	                token,
	                patientId,
	                page,
	                size);
	    }

	    private ResponseEntity<?> getAppointmentsByPatientIdFallback(
	            String token,
	            String patientId,
	            int page,
	            int size,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= CONSENT FORM ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getPatientDetailsForConsentFormFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getPatientDetailsForConsentFormFallback")
	    public ResponseEntity<Response> getPatientDetailsForConsentForm(
	            String token,
	            String bookingId,
	            String patientId,
	            String mobileNumber) {

	        return bookingFeign.getPatientDetailsForConsentForm(
	                token,
	                bookingId,
	                patientId,
	                mobileNumber);
	    }

	    private ResponseEntity<Response> getPatientDetailsForConsentFormFallback(
	            String token,
	            String bookingId,
	            String patientId,
	            String mobileNumber,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= UPDATE APPOINTMENT ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "updateAppointmentFallback")
	    @Retry(name = "bookingService", fallbackMethod = "updateAppointmentFallback")
	    public ResponseEntity<?> updateAppointment(
	            String token,
	            BookingResponse bookingResponse) {

	        return bookingFeign.updateAppointment(token, bookingResponse);
	    }

	    private ResponseEntity<?> updateAppointmentFallback(
	            String token,
	            BookingResponse bookingResponse,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= DELETE BOOKING ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "deleteBookedServiceFallback")
	    @Retry(name = "bookingService", fallbackMethod = "deleteBookedServiceFallback")
	    public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedService(
	            String id) {

	        return bookingFeign.deleteBookedService(id);
	    }

	    private ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedServiceFallback(
	            String id,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= BOOK SERVICE ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookServiceFallback")
	    @Retry(name = "bookingService", fallbackMethod = "bookServiceFallback")
	    public ResponseEntity<ResponseStructure<BookingResponse>> bookService(
	            String token,
	            BookingResponse req) {

	        return bookingFeign.bookService(token, req);
	    }

	    private ResponseEntity<ResponseStructure<BookingResponse>> bookServiceFallback(
	            String token,
	            BookingResponse req,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= REPORT API ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getReportFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getReportFallback")
	    public ResponseEntity<Response> getReport(
	            String token,
	            String clinicId,
	            String branchId,
	            Integer number,
	            String startDate,
	            String endDate) {

	        return bookingFeign.getReport(
	                token,
	                clinicId,
	                branchId,
	                number,
	                startDate,
	                endDate);
	    }

	    private ResponseEntity<Response> getReportFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            Integer number,
	            String startDate,
	            String endDate,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= BOOK PHYSIO APPOINTMENT ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookPhysioAppointmentFallback")
	    @Retry(name = "bookingService", fallbackMethod = "bookPhysioAppointmentFallback")
	    public ResponseEntity<Response> bookPhysioAppointment(
	            String token,
	            BookingRequset req) {

	        return bookingFeign.bookPhysioAppointment(token, req);
	    }

	    private ResponseEntity<Response> bookPhysioAppointmentFallback(
	            String token,
	            BookingRequset req,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= TODAY PHYSIO BOOKINGS ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getTodayPhysioBookingsFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getTodayPhysioBookingsFallback")
	    public ResponseEntity<Response> getTodayPhysioBookings(
	            String token,
	            String clinicId,
	            String branchId) {

	        return bookingFeign.getTodayPhysioBookings(
	                token,
	                clinicId,
	                branchId);
	    }

	    public ResponseEntity<Response> getTodayPhysioBookingsFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= UPCOMING BOOKINGS ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getUpcomingBookingsFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getUpcomingBookingsFallback")
	    public ResponseEntity<Response> getUpcomingBookings(
	            String token,
	            String clinicId,
	            String branchId,
	            int option) {

	        return bookingFeign.getUpcomingBookings(
	                token,
	                clinicId,
	                branchId,
	                option);
	    }

	    public ResponseEntity<Response> getUpcomingBookingsFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            int option,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= DATE BASED BOOKINGS ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getPhysioBookingBasedOnDateFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getPhysioBookingBasedOnDateFallback")
	    public ResponseEntity<Response> getPhysioBookingBasedOnDate(
	            String token,
	            String clinicId,
	            String branchId,
	            String date) {

	        return bookingFeign.getPhysioBookingBasedOnDate(
	                token,
	                clinicId,
	                branchId,
	                date);
	    }

	    public ResponseEntity<Response> getPhysioBookingBasedOnDateFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            String date,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= CUSTOM RANGE BOOKINGS ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getPhysioBookingsByCustomeRangeFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getPhysioBookingsByCustomeRangeFallback")
	    public ResponseEntity<Response> getPhysioBookingsByCustomeRange(
	            String token,
	            String clinicId,
	            String branchId,
	            String start,
	            String end) {

	        return bookingFeign.getPhysioBookingsByCustomeRange(
	                token,
	                clinicId,
	                branchId,
	                start,
	                end);
	    }

	    public ResponseEntity<Response> getPhysioBookingsByCustomeRangeFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            String start,
	            String end,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= BOOKING BY ID ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookingByIdFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getBookingByIdFallback")
	    public ResponseEntity<Response> getBookingById(
	            String token,
	            String bookingId) {

	        return bookingFeign.getBookingById(token, bookingId);
	    }

	    public ResponseEntity<Response> getBookingByIdFallback(
	            String token,
	            String bookingId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= PAGINATED TODAY BOOKINGS ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getTodayBookingsFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getTodayBookingsFallback")
	    public ResponseEntity<?> getTodayBookings(
	            String token,
	            String clinicId,
	            String branchId,
	            int page,
	            int size) {

	        return bookingFeign.getTodayBookings(
	                token,
	                clinicId,
	                branchId,
	                page,
	                size);
	    }

	    public ResponseEntity<?> getTodayBookingsFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            int page,
	            int size,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= IN-PROGRESS APPOINTMENT ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getInProgressAppointmentFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getInProgressAppointmentFallback")
	    public ResponseEntity<?> getInProgressAppointmentByPatientIdAndBookingId(
	            String token,
	            String patientId,
	            String bookingId) {

	        return bookingFeign.getInProgressAppointmentByPatientIdAndBookingId(
	                token,
	                patientId,
	                bookingId);
	    }

	    public ResponseEntity<?> getInProgressAppointmentFallback(
	            String token,
	            String patientId,
	            String bookingId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= REPORTS BY PATIENT ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getReportsByPatientIdFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getReportsByPatientIdFallback")
	    public ResponseEntity<Response> getReportsByPatientId(
	            String token,
	            String patientId) {

	        return bookingFeign.getReportsByPatientId(token, patientId);
	    }

	    public ResponseEntity<Response> getReportsByPatientIdFallback(
	            String token,
	            String patientId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= DELETE REPORT ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "deleteReportFallback")
	    @Retry(name = "bookingService", fallbackMethod = "deleteReportFallback")
	    public void deleteReport(
	            String token,
	            String bookingId,
	            String index) {

	        bookingFeign.deleteReport(token, bookingId, index);
	    }

	    public void deleteReportFallback(
	            String token,
	            String bookingId,
	            String index,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    
	    ///  CUSTOMER SERVICE FEIGN
	    
	    
	    @CircuitBreaker(name = "customerService", fallbackMethod = "getByKeyFallback")
	    @Retry(name = "customerService", fallbackMethod = "getByKeyFallback")
	    public ResponseEntity<QuestionsByPartEntity> getByKey(
	            String token,
	            String key) {

	        return customerServiceFeignClient.getByKey(token, key);
	    }

	    public ResponseEntity<QuestionsByPartEntity> getByKeyFallback(
	            String token,
	            String key,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= NOTIFICATION SERVICE ================= */

	    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationToClinicFallback")
	    @Retry(name = "notificationService", fallbackMethod = "sendNotificationToClinicFallback")
	    public ResponseEntity<?> sendNotificationToClinic(
	            String token,
	            String clinicId) {

	        return notificationFeign.sendNotificationToClinic(token, clinicId);
	    }

	    public ResponseEntity<?> sendNotificationToClinicFallback(
	            String token,
	            String clinicId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "notificationService", fallbackMethod = "pricedropFallback")
	    @Retry(name = "notificationService", fallbackMethod = "pricedropFallback")
	    public ResponseEntity<?> pricedrop(
	            String token,
	            PriceDropAlertDto priceDropAlertDto) {

	        return notificationFeign.pricedrop(token, priceDropAlertDto);
	    }

	    public ResponseEntity<?> pricedropFallback(
	            String token,
	            PriceDropAlertDto priceDropAlertDto,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "notificationService", fallbackMethod = "priceDropNotificationFallback")
	    @Retry(name = "notificationService", fallbackMethod = "priceDropNotificationFallback")
	    public ResponseEntity<?> priceDropNotification(
	            String token,
	            String clinicId,
	            String branchId) {

	        return notificationFeign.priceDropNotification(
	                token,
	                clinicId,
	                branchId);
	    }

	    public ResponseEntity<?> priceDropNotificationFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "notificationService", fallbackMethod = "updatePriceDropNotificationFallback")
	    @Retry(name = "notificationService", fallbackMethod = "updatePriceDropNotificationFallback")
	    public ResponseEntity<?> updatePriceDropNotification(
	            String token,
	            String clinicId,
	            String branchId,
	            String id,
	            PriceDropAlertDto dto) {

	        return notificationFeign.updatePriceDropNotification(
	                token,
	                clinicId,
	                branchId,
	                id,
	                dto);
	    }

	    public ResponseEntity<?> updatePriceDropNotificationFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            String id,
	            PriceDropAlertDto dto,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "notificationService", fallbackMethod = "deletePriceDropNotificationFallback")
	    @Retry(name = "notificationService", fallbackMethod = "deletePriceDropNotificationFallback")
	    public ResponseEntity<?> deletePriceDropNotification(
	            String token,
	            String clinicId,
	            String branchId,
	            String id) {

	        return notificationFeign.deletePriceDropNotification(
	                token,
	                clinicId,
	                branchId,
	                id);
	    }

	    public ResponseEntity<?> deletePriceDropNotificationFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            String id,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= PHYSIOTHERAPY SERVICE ================= */

	    @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "updateSessionStatusFallback")
	    @Retry(name = "physioDoctorService", fallbackMethod = "updateSessionStatusFallback")
	    public void updateSessionStatus(
	            String token,
	            String therapistRecordId,
	            String sessionId) {

	        physiotherapyFeign.updateSessionStatus(
	                token,
	                therapistRecordId,
	                sessionId);
	    }

	    public void updateSessionStatusFallback(
	            String token,
	            String therapistRecordId,
	            String sessionId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getPaymentFallback")
	    @Retry(name = "physioDoctorService", fallbackMethod = "getPaymentFallback")
	    public Response getPayment(
	            String token,
	            String bookingId) {

	        return physiotherapyFeign.getPayment(token, bookingId);
	    }

	    public Response getPaymentFallback(
	            String token,
	            String bookingId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getRecordFallback")
	    @Retry(name = "physioDoctorService", fallbackMethod = "getRecordFallback")
	    public Response getRecord(
	            String token,
	            String clinicId,
	            String branchId,
	            String patientId,
	            String bookingId,
	            String therapistRecordId) {

	        return physiotherapyFeign.getRecord(
	                token,
	                clinicId,
	                branchId,
	                patientId,
	                bookingId,
	                therapistRecordId);
	    }

	    public Response getRecordFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            String patientId,
	            String bookingId,
	            String therapistRecordId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getPaymentsFallback")
	    @Retry(name = "physioDoctorService", fallbackMethod = "getPaymentsFallback")
	    public Response getPayments(
	            String token,
	            String clinicId,
	            String branchId) {

	        return physiotherapyFeign.getPayments(
	                token,
	                clinicId,
	                branchId);
	    }

	    public Response getPaymentsFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getTodaySessionCountFallback")
	    @Retry(name = "physioDoctorService", fallbackMethod = "getTodaySessionCountFallback")
	    public int getTodaySessionCount(
	            String clinicId,
	            String branchId,
	            String therapistId) {

	        return physiotherapyFeign.getTodaySessionCount(
	                clinicId,
	                branchId,
	                therapistId);
	    }

	    public int getTodaySessionCountFallback(
	            String clinicId,
	            String branchId,
	            String therapistId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= BOOKING SERVICE ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "blockingSlotFallback")
	    @Retry(name = "bookingService", fallbackMethod = "blockingSlotFallback")
	    public BookingResponse blockingSlot(
	            String token,
	            TempBlockingSlot temp) {

	        return bookingFeign.blockingSlot(token, temp);
	    }

	    public BookingResponse blockingSlotFallback(
	            String token,
	            TempBlockingSlot temp,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookingByBranchIdFallback")
	    @Retry(name = "bookingService", fallbackMethod = "bookingByBranchIdFallback")
	    public ResponseEntity<?> bookingByBranchId(
	            String token,
	            String branchId,
	            int page,
	            int size) {

	        return bookingFeign.bookingByBranchId(
	                token,
	                branchId,
	                page,
	                size);
	    }

	    public ResponseEntity<?> bookingByBranchIdFallback(
	            String token,
	            String branchId,
	            int page,
	            int size,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }
	    
	    /* ================= CLINIC BOOKINGS ================= */

	    /* ================= BOOKED SERVICES BY CLINIC & BRANCH ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookedServicesByClinicIdWithBranchIdFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getBookedServicesByClinicIdWithBranchIdFallback")
	    public ResponseEntity<?> getBookedServicesByClinicIdWithBranchId(
	            String token,
	            String clinicId,
	            String branchId,
	            int page,
	            int size) {

	        return bookingFeign.getBookedServicesByClinicIdWithBranchId(
	                token, clinicId, branchId, page, size);
	    }

	    public ResponseEntity<?> getBookedServicesByClinicIdWithBranchIdFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            int page,
	            int size,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= ONE WEEK APPOINTMENTS ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "retrieveOneWeekAppointmentsFallback")
	    @Retry(name = "bookingService", fallbackMethod = "retrieveOneWeekAppointmentsFallback")
	    public ResponseEntity<?> retrieveOneWeekAppointments(
	            String token,
	            String clinicId,
	            String branchId,
	            int page,
	            int size) {

	        return bookingFeign.retrieveOneWeekAppointments(
	                token, clinicId, branchId, page, size);
	    }

	    public ResponseEntity<?> retrieveOneWeekAppointmentsFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            int page,
	            int size,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= APPOINTMENTS BY DATE ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "retrieveAppointnmentsByServiceDateFallback")
	    @Retry(name = "bookingService", fallbackMethod = "retrieveAppointnmentsByServiceDateFallback")
	    public ResponseEntity<?> retrieveAppointnmentsByServiceDate(
	            String token,
	            String clinicId,
	            String branchId,
	            String date) {

	        return bookingFeign.retrieveAppointnmentsByServiceDate(
	                token, clinicId, branchId, date);
	    }

	    public ResponseEntity<?> retrieveAppointnmentsByServiceDateFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            String date,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= UPDATE APPOINTMENT ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "updateAppointmentBasedOnBookingIdFallback")
	    @Retry(name = "bookingService", fallbackMethod = "updateAppointmentBasedOnBookingIdFallback")
	    public ResponseEntity<ResponseStructure<BookingResponse>> updateAppointmentBasedOnBookingId(
	            String token,
	            BookingResponse bookingResponse) {

	        return bookingFeign.updateAppointmentBasedOnBookingId(
	                token, bookingResponse);
	    }

	    public ResponseEntity<ResponseStructure<BookingResponse>> updateAppointmentBasedOnBookingIdFallback(
	            String token,
	            BookingResponse bookingResponse,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= DOCTOR RATING NOTIFICATION ================= */

	    @CircuitBreaker(
	            name = "notificationService",
	            fallbackMethod = "sendDoctorRatingNotificationFallback")
	    @Retry(
	            name = "notificationService",
	            fallbackMethod = "sendDoctorRatingNotificationFallback")
	    public ResponseEntity<?> sendDoctorRatingNotification(
	            String token,
	            DoctorRatingNotificationDTO dto) {

	        return notificationFeign.sendDoctorRatingNotification(token, dto);
	    }

	    public ResponseEntity<?> sendDoctorRatingNotificationFallback(
	            String token,
	            DoctorRatingNotificationDTO dto,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= THERAPIST OVERALL FEEDBACK ================= */

	    @CircuitBreaker(
	            name = "notificationService",
	            fallbackMethod = "therapistOverallFeedbackFallback")
	    @Retry(
	            name = "notificationService",
	            fallbackMethod = "therapistOverallFeedbackFallback")
	    public void therapistOverallFeedback(
	            String token,
	            Map<String, String> data) {

	        notificationFeign.therapistOverallFeedback(token, data);
	    }

	    public void therapistOverallFeedbackFallback(
	            String token,
	            Map<String, String> data,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    /* ================= THERAPIST SESSION FEEDBACK ================= */

	    @CircuitBreaker(
	            name = "notificationService",
	            fallbackMethod = "therapistSessionFeedbackFallback")
	    @Retry(
	            name = "notificationService",
	            fallbackMethod = "therapistSessionFeedbackFallback")
	    public void therapistSessionFeedback(
	            String token,
	            Map<String, String> data) {

	        notificationFeign.therapistSessionFeedback(token, data);
	    }

	    public void therapistSessionFeedbackFallback(
	            String token,
	            Map<String, String> data,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }
	}


