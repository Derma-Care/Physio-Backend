package com.clinicadmin.utils;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

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
	    
	    private Response buildFallbackResponse(String message) {
	        return Response.builder()
	                .success(false)
	                .status(503)
	                .message(message)
	                .build();
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "clinicLoginFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "clinicLoginFallback")
	    public Response clinicLogin(String userName) {
	        return adminServiceClient.clinicLogin(userName);
	    }

	    public Response clinicLoginFallback(String userName, Exception ex) {
	        return buildFallbackResponse("Admin Service unavailable");
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateClinicCredentialsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "updateClinicCredentialsFallback")
	    public Response updateClinicCredentials(String token,
	                                            UpdateClinicLoginCredentialsDTO dto,
	                                            String userName) {

	        return adminServiceClient.updateClinicCredentials(token, dto, userName);
	    }

	    public Response updateClinicCredentialsFallback(String token,
	                                                     UpdateClinicLoginCredentialsDTO dto,
	                                                     String userName,
	                                                     Exception ex) {

	        return buildFallbackResponse("Unable to update clinic credentials");
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getClinicByIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getClinicByIdFallback")
	    public Response getClinicById(String token, String clinicId) {
	        return adminServiceClient.getClinicById(token, clinicId).getBody();
	    }

	    public Response getClinicByIdFallback(String token,
	                                           String clinicId,
	                                           Exception ex) {

	        return buildFallbackResponse("Unable to fetch clinic");
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getAllClinicsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getAllClinicsFallback")
	    public Response getAllClinics(String token) {
	        return adminServiceClient.getAllClinics(token).getBody();
	    }

	    public Response getAllClinicsFallback(String token,
	                                           Exception ex) {

	        return buildFallbackResponse("Unable to fetch clinics");
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateClinicFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "updateClinicFallback")
	    public Response updateClinic(String token,
	                                 String clinicId,
	                                 ClinicDTO clinic) {

	        return adminServiceClient.updateClinic(token, clinicId, clinic);
	    }

	    public Response updateClinicFallback(String token,
	                                          String clinicId,
	                                          ClinicDTO clinic,
	                                          Exception ex) {

	        return buildFallbackResponse("Unable to update clinic");
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "deleteClinicFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "deleteClinicFallback")
	    public Response deleteClinic(String token,
	                                 String clinicId) {

	        return adminServiceClient.deleteClinic(token, clinicId);
	    }

	    public Response deleteClinicFallback(String token,
	                                          String clinicId,
	                                          Exception ex) {

	        return buildFallbackResponse("Unable to delete clinic");
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getHospitalUsingRecommendentaionFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getHospitalUsingRecommendentaionFallback")
	    public Response getHospitalUsingRecommendentaion(String token) {

	        return adminServiceClient
	                .getHospitalUsingRecommendentaion(token)
	                .getBody();
	    }

	    public Response getHospitalUsingRecommendentaionFallback(String token,
	                                                              Exception ex) {

	        return buildFallbackResponse("Unable to fetch recommended clinics");
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "firstRecommendedTureClincsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "firstRecommendedTureClincsFallback")
	    public Response firstRecommendedTureClincs() {

	        return adminServiceClient
	                .firstRecommendedTureClincs()
	                .getBody();
	    }

	    public Response firstRecommendedTureClincsFallback(Exception ex) {

	        return buildFallbackResponse("Unable to fetch recommended clinics");
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getBranchByClinicAndBranchIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getBranchByClinicAndBranchIdFallback")
	    public Response getBranchByClinicAndBranchId(String token,
	                                                 String clinicId,
	                                                 String branchId) {

	        return adminServiceClient
	                .getBranchByClinicAndBranchId(token, clinicId, branchId)
	                .getBody();
	    }

	    public Response getBranchByClinicAndBranchIdFallback(String token,
	                                                          String clinicId,
	                                                          String branchId,
	                                                          Exception ex) {

	        return buildFallbackResponse("Unable to fetch branch");
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getBranchByIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getBranchByIdFallback")
	    public Response getBranchById(String token,
	                                  String branchId) {

	        return adminServiceClient
	                .getBranchById(token, branchId)
	                .getBody();
	    }

	    public Response getBranchByIdFallback(String token,
	                                           String branchId,
	                                           Exception ex) {

	        return buildFallbackResponse("Unable to fetch branch");
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getAllBranchesFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getAllBranchesFallback")
	    public Response getAllBranches(String token) {

	        return adminServiceClient
	                .getAllBranches(token)
	                .getBody();
	    }

	    public Response getAllBranchesFallback(String token,
	                                            Exception ex) {

	        return buildFallbackResponse("Unable to fetch branches");
	    }

	    // Non-Response return types => throw exception

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getBranchByClinicIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getBranchByClinicIdFallback")
	    public ResponseEntity<?> getBranchByClinicId(String token,
	                                                 String clinicId) {

	        return adminServiceClient.getBranchByClinicId(token, clinicId);
	    }

	    public ResponseEntity<?> getBranchByClinicIdFallback(String token,
	                                                          String clinicId,
	                                                          Exception ex) {

	        throw new RuntimeException("Admin Service unavailable", ex);
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getDefaultAdminPermissionsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getDefaultAdminPermissionsFallback")
	    public Map<String, List<String>> getDefaultAdminPermissions(
	            String token) {

	        return adminServiceClient
	                .getDefaultAdminPermissions(token)
	                .getBody();
	    }

	    public Map<String, List<String>> getDefaultAdminPermissionsFallback(
	            String token,
	            Exception ex) {

	        throw new RuntimeException("Admin Service unavailable", ex);
	    }
	    
	    /// BOOKING SERVICE
	    
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getBookedServiceFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getBookedServiceFallback")
	    public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(
	            String token,
	            String id) {

	        return bookingFeign.getBookedService(token, id);
	    }

	    private ResponseEntity<ResponseStructure<BookingResponse>> getBookedServiceFallback(
	            String token,
	            String id,
	            Exception ex) {

	        log.error("getBookedService failed", ex);

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(ResponseStructure.buildResponse(
	                        null,
	                        "Booking Service Unavailable",
	                        HttpStatus.SERVICE_UNAVAILABLE,
	                        HttpStatus.SERVICE_UNAVAILABLE.value()));
	    }

	    // ==========================================================
	    // APPOINTMENTS BY PATIENT
	    // ==========================================================

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getAppointmentsByPatientIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getAppointmentsByPatientIdFallback")
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

	        log.error("getAppointmentsByPatientId failed", ex);

	        throw new ServiceUnavailableException(
	                "Booking Service is currently unavailable");
	    }

	    // ==========================================================
	    // CONSENT FORM
	    // ==========================================================

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getPatientDetailsForConsentFormFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getPatientDetailsForConsentFormFallback")
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

	        log.error("getPatientDetailsForConsentForm failed", ex);

	        Response response = Response.builder()
	                .success(false)
	                .message("Booking Service Unavailable")
	                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
	                .build();

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }

	    // ==========================================================
	    // UPDATE APPOINTMENT
	    // ==========================================================

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateAppointmentFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "updateAppointmentFallback")
	    public ResponseEntity<?> updateAppointment(
	            String token,
	            BookingResponse bookingResponse) {

	        return bookingFeign.updateAppointment(token, bookingResponse);
	    }

	    private ResponseEntity<?> updateAppointmentFallback(
	            String token,
	            BookingResponse bookingResponse,
	            Exception ex) {

	        log.error("updateAppointment failed", ex);

	        throw new ServiceUnavailableException(
	                "Booking Service is currently unavailable");
	    }

	    // ==========================================================
	    // DELETE BOOKING
	    // ==========================================================

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "deleteBookedServiceFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "deleteBookedServiceFallback")
	    public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedService(
	            String id) {

	        return bookingFeign.deleteBookedService(id);
	    }

	    private ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedServiceFallback(
	            String id,
	            Exception ex) {

	        log.error("deleteBookedService failed", ex);

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(ResponseStructure.buildResponse(
	                        null,
	                        "Unable to delete booking",
	                        HttpStatus.SERVICE_UNAVAILABLE,
	                        HttpStatus.SERVICE_UNAVAILABLE.value()));
	    }

	    // ==========================================================
	    // BOOK SERVICE
	    // ==========================================================

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "bookServiceFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "bookServiceFallback")
	    public ResponseEntity<ResponseStructure<BookingResponse>> bookService(
	            String token,
	            BookingResponse req) {

	        return bookingFeign.bookService(token, req);
	    }

	    private ResponseEntity<ResponseStructure<BookingResponse>> bookServiceFallback(
	            String token,
	            BookingResponse req,
	            Exception ex) {

	        log.error("bookService failed", ex);

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(ResponseStructure.buildResponse(
	                        null,
	                        "Unable to create booking",
	                        HttpStatus.SERVICE_UNAVAILABLE,
	                        HttpStatus.SERVICE_UNAVAILABLE.value()));
	    }

	    // ==========================================================
	    // REPORT API
	    // ==========================================================

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getReportFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getReportFallback")
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

	        log.error("getReport failed", ex);

	        Response response = Response.builder()
	                .success(false)
	                .message("Booking Service Unavailable")
	                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
	                .build();

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }

	    // ==========================================================
	    // BOOK PHYSIO APPOINTMENT
	    // ==========================================================

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "bookPhysioAppointmentFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "bookPhysioAppointmentFallback")
	    public ResponseEntity<Response> bookPhysioAppointment(
	            String token,
	            BookingRequset req) {

	        return bookingFeign.bookPhysioAppointment(token, req);
	    }

	    private ResponseEntity<Response> bookPhysioAppointmentFallback(
	            String token,
	            BookingRequset req,
	            Exception ex) {

	        log.error("bookPhysioAppointment failed", ex);

	        Response response = Response.builder()
	                .success(false)
	                .message("Booking Service Unavailable")
	                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
	                .build();

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }
	    
	    
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getTodayPhysioBookingsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getTodayPhysioBookingsFallback")
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

	        log.error("Fallback executed for getTodayPhysioBookings : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Booking Service is temporarily unavailable");
	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= UPCOMING BOOKINGS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getUpcomingBookingsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getUpcomingBookingsFallback")
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

	        log.error("Fallback executed for getUpcomingBookings : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Booking Service is temporarily unavailable");
	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= DATE BASED BOOKINGS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getPhysioBookingBasedOnDateFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getPhysioBookingBasedOnDateFallback")
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

	        log.error("Fallback executed for getPhysioBookingBasedOnDate : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Booking Service is temporarily unavailable");
	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= CUSTOM RANGE BOOKINGS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getPhysioBookingsByCustomeRangeFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getPhysioBookingsByCustomeRangeFallback")
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

	        log.error("Fallback executed for getPhysioBookingsByCustomeRange : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Booking Service is temporarily unavailable");
	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= BOOKING BY ID ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getBookingByIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getBookingByIdFallback")
	    public ResponseEntity<Response> getBookingById(
	            String token,
	            String bookingId) {

	        return bookingFeign.getBookingById(token, bookingId);
	    }

	    public ResponseEntity<Response> getBookingByIdFallback(
	            String token,
	            String bookingId,
	            Exception ex) {

	        log.error("Fallback executed for getBookingById : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Booking Service is temporarily unavailable");
	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= PAGINATED TODAY BOOKINGS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getTodayBookingsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getTodayBookingsFallback")
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

	        log.error("Fallback executed for getTodayBookings : {}", ex.getMessage());
	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }

	    /* ================= IN-PROGRESS APPOINTMENT ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getInProgressAppointmentFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getInProgressAppointmentFallback")
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

	        log.error("Fallback executed for getInProgressAppointment : {}", ex.getMessage());
	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	       
	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }

	    /* ================= REPORTS BY PATIENT ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getReportsByPatientIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getReportsByPatientIdFallback")
	    public ResponseEntity<Response> getReportsByPatientId(
	            String token,
	            String patientId) {

	        return bookingFeign.getReportsByPatientId(
	                token,
	                patientId);
	    }

	    public ResponseEntity<Response> getReportsByPatientIdFallback(
	            String token,
	            String patientId,
	            Exception ex) {

	        log.error("Fallback executed for getReportsByPatientId : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Booking Service is temporarily unavailable");
	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= DELETE REPORT ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "deleteReportFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "deleteReportFallback")
	    public void deleteReport(
	            String token,
	            String bookingId,
	            String index) {

	        bookingFeign.deleteReport(
	                token,
	                bookingId,
	                index);
	    }

	    public void deleteReportFallback(
	            String token,
	            String bookingId,
	            String index,
	            Exception ex) {

	        throw new RuntimeException(ex);
	    }
	    
	    
	    ///  CUSTOMER SERVICE FEIGN
	    
	    
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getByKeyFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getByKeyFallback")
	    public ResponseEntity<QuestionsByPartEntity> getByKey(
	            String token,
	            String key) {

	        return customerServiceFeignClient.getByKey(token, key);
	    }

	    public ResponseEntity<QuestionsByPartEntity> getByKeyFallback(
	            String token,
	            String key,
	            Exception ex) {

	       throw new RuntimeException(ex);
	    }
	    
	    
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "sendNotificationToClinicFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "sendNotificationToClinicFallback")
	    public ResponseEntity<?> sendNotificationToClinic( String token,String clinicId) {
	        return notificationFeign.sendNotificationToClinic(token,clinicId);
	    }

	    public ResponseEntity<?> sendNotificationToClinicFallback( String token,
	            String clinicId,
	            Exception ex) {

	        log.error("Fallback executed for sendNotificationToClinic : {}", ex.getMessage());
	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }

	    /* ================= PRICE DROP CREATE ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "pricedropFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "pricedropFallback")
	    public ResponseEntity<?> pricedrop( String token,PriceDropAlertDto priceDropAlertDto) {

	        return notificationFeign.pricedrop(token,priceDropAlertDto);
	    }

	    public ResponseEntity<?> pricedropFallback( String token,
	            PriceDropAlertDto priceDropAlertDto,
	            Exception ex) {

	        log.error("Fallback executed for pricedrop : {}", ex.getMessage());
	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }

	    /* ================= GET PRICE DROP NOTIFICATIONS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "priceDropNotificationFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "priceDropNotificationFallback")
	    public ResponseEntity<?> priceDropNotification( String token,
	            String clinicId,
	            String branchId) {

	        return notificationFeign.priceDropNotification(token,
	                clinicId,
	                branchId);
	    }

	    public ResponseEntity<?> priceDropNotificationFallback( String token,
	            String clinicId,
	            String branchId,
	            Exception ex) {

	        log.error("Fallback executed for priceDropNotification : {}", ex.getMessage());
	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }

	    /* ================= UPDATE PRICE DROP NOTIFICATION ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updatePriceDropNotificationFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "updatePriceDropNotificationFallback")
	    public ResponseEntity<?> updatePriceDropNotification( String token,
	            String clinicId,
	            String branchId,
	            String id,
	            PriceDropAlertDto dto) {

	        return notificationFeign.updatePriceDropNotification(token,
	                clinicId,
	                branchId,
	                id,
	                dto);
	    }

	    public ResponseEntity<?> updatePriceDropNotificationFallback( String token,
	            String clinicId,
	            String branchId,
	            String id,
	            PriceDropAlertDto dto,
	            Exception ex) {

	        log.error("Fallback executed for updatePriceDropNotification : {}", ex.getMessage());
	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }

	    /* ================= DELETE PRICE DROP NOTIFICATION ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "deletePriceDropNotificationFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "deletePriceDropNotificationFallback")
	    public ResponseEntity<?> deletePriceDropNotification( String token,
	            String clinicId,
	            String branchId,
	            String id) {

	        return notificationFeign.deletePriceDropNotification(token,
	                clinicId,
	                branchId,
	                id);
	    }

	    public ResponseEntity<?> deletePriceDropNotificationFallback( String token,
	            String clinicId,
	            String branchId,
	            String id,
	            Exception ex) {

	        log.error("Fallback executed for deletePriceDropNotification : {}", ex.getMessage());
	       
	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Physiotherapy Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }
	    
	    
	    /* ================= UPDATE SESSION STATUS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateSessionStatusFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "updateSessionStatusFallback")
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

	        log.error("Fallback executed for updateSessionStatus : {}", ex.getMessage());
	    }


	    /* ================= GET PAYMENT ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getPaymentFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getPaymentFallback")
	    public Response getPayment(
	            String token,
	            String bookingId) {

	        return physiotherapyFeign.getPayment(
	                token,
	                bookingId);
	    }

	    public Response getPaymentFallback(
	            String token,
	            String bookingId,
	            Exception ex) {

	        log.error("Fallback executed for getPayment : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Physiotherapy Service is temporarily unavailable");

	        return response;
	    }


	    /* ================= GET RECORD ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getRecordFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getRecordFallback")
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

	        log.error("Fallback executed for getRecord : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Physiotherapy Service is temporarily unavailable");

	        return response;
	    }


	    /* ================= GET PAYMENTS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getPaymentsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getPaymentsFallback")
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

	        log.error("Fallback executed for getPayments : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Physiotherapy Service is temporarily unavailable");

	        return response;
	    }


	    /* ================= TODAY SESSION COUNT ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getTodaySessionCountFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getTodaySessionCountFallback")
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
	    	throw new RuntimeException(ex.getMessage(), ex);
	       
	    }
	    
	    
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "blockingSlotFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "blockingSlotFallback")
	    public BookingResponse blockingSlot(
	            String token,
	            TempBlockingSlot temp) {

	        return bookingFeign.blockingSlot(token, temp);
	    }

	    public BookingResponse blockingSlotFallback(
	            String token,
	            TempBlockingSlot temp,
	            Exception ex) {

	    	throw new RuntimeException(ex.getMessage(), ex);
	    }
	    
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "bookingByBranchIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "bookingByBranchIdFallback")
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

	        log.error("Fallback executed for bookingByBranchId : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	   
	    }
	    
	    /* ================= CLINIC BOOKINGS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getBookedServicesByClinicIdWithBranchIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getBookedServicesByClinicIdWithBranchIdFallback")
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

	        log.error("Fallback executed for getBookedServicesByClinicIdWithBranchId : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }


	    /* ================= ONE WEEK APPOINTMENTS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "retrieveOneWeekAppointmentsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "retrieveOneWeekAppointmentsFallback")
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

	        log.error("Fallback executed for retrieveOneWeekAppointments : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }


	    /* ================= APPOINTMENTS BY DATE ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "retrieveAppointnmentsByServiceDateFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "retrieveAppointnmentsByServiceDateFallback")
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

	        log.error("Fallback executed for retrieveAppointnmentsByServiceDate : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }


	    /* ================= UPDATE APPOINTMENT ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateAppointmentBasedOnBookingIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "updateAppointmentBasedOnBookingIdFallback")
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

	        log.error("Fallback executed for updateAppointmentBasedOnBookingId : {}", ex.getMessage());

	        ResponseStructure<BookingResponse> rs = new ResponseStructure<>();
	        rs.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
	        rs.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(rs);
	    }
	    
	    
	    @CircuitBreaker(
	            name = "notificationService",
	            fallbackMethod = "sendDoctorRatingNotificationFallback")
	    @Retry(
	            name = "notificationService",
	            fallbackMethod = "sendDoctorRatingNotificationFallback")
	    public ResponseEntity<?> sendDoctorRatingNotification( String token,
	            DoctorRatingNotificationDTO dto) {

	        return notificationFeign.sendDoctorRatingNotification(token,dto);
	    }

	    /**
	     * Fallback Method
	     */
	    public ResponseEntity<?> sendDoctorRatingNotificationFallback( String token,
	            DoctorRatingNotificationDTO dto,
	            Exception ex) {

	        return ResponseEntity
	                .status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body("Notification Service is currently unavailable. Doctor rating notification could not be sent.");
	    }
	    
	    
	    @CircuitBreaker(
	            name = "notificationService",
	            fallbackMethod = "therapistOverallFeedbackFallback")
	    @Retry(
	            name = "notificationService",
	            fallbackMethod = "therapistOverallFeedbackFallback")
	    public void therapistOverallFeedback( String token,Map<String, String> data) {

	        notificationFeign.therapistOverallFeedback(token,data);
	    }

	    public void therapistOverallFeedbackFallback( String token,
	            Map<String, String> data,
	            Exception ex) {

	        log.error("Failed to send therapist overall feedback notification. Reason: {}",
	                ex.getMessage());
	    }

	    /**
	     * Therapist Session Feedback Notification
	     */
	    @CircuitBreaker(
	            name = "notificationService",
	            fallbackMethod = "therapistSessionFeedbackFallback")
	    @Retry(
	            name = "notificationService",
	            fallbackMethod = "therapistSessionFeedbackFallback")
	    public void therapistSessionFeedback( String token,Map<String, String> data) {

	        notificationFeign.therapistSessionFeedback(token,data);
	    }

	    public void therapistSessionFeedbackFallback( String token,
	            Map<String, String> data,
	            Exception ex) {

	        log.error("Failed to send therapist session feedback notification. Reason: {}",
	                ex.getMessage());
	    }
	}


