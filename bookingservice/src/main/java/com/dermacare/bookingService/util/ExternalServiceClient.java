package com.dermacare.bookingService.util;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.dermacare.bookingService.dto.BookingResponse;
import com.dermacare.bookingService.dto.BranchDTO;
import com.dermacare.bookingService.dto.CustomerOnbordingDTO;
import com.dermacare.bookingService.dto.NotificationDTO;
import com.dermacare.bookingService.dto.SessionForBooking;
import com.dermacare.bookingService.feign.AdminServiceClient;
import com.dermacare.bookingService.feign.ClinicAdminFeign;
import com.dermacare.bookingService.feign.NotificationFeign;
import com.dermacare.bookingService.feign.PhysioDoctorFeign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ExternalServiceClient {

    @Autowired
    private ClinicAdminFeign clinicAdminFeign;

    @Autowired
    private NotificationFeign notificationFeign;

    @Autowired
    private PhysioDoctorFeign physioDoctorFeign;
    
    @Autowired
    private AdminServiceClient adminServiceClient;

    // ============================================================
    // CLINIC ADMIN SERVICE
    // ============================================================

    
    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getBranchByIdFallback")
    @Retry(name = "clinicAdminService", fallbackMethod = "getBranchByIdFallback")
    public ResponseEntity<ResponseStructure<BranchDTO>> getBranchById(String branchId) {

        
    	return adminServiceClient.getBranchById(branchId);

    }
    
    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getCustomerByPatientIdFallback")
    @Retry(name = "clinicAdminService", fallbackMethod = "getCustomerByPatientIdFallback")
    public Response getCustomerByPatientId(String token, String patientId, String clinicId) {
        return clinicAdminFeign.getCustomerByPatientId(token, patientId, clinicId).getBody();
    }

    public Response getCustomerByPatientIdFallback(String token,
                                                    String patientId,
                                                    String clinicId,
                                                    Exception ex) {
        Response response = new Response();
        response.setStatus(429);
        response.setMessage("Unable to fetch customer details.");
        return response;
    }

    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getTodayExpensesFallback")
    @Retry(name = "clinicAdminService", fallbackMethod = "getTodayExpensesFallback")
    public Double getTodayExpenses(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getTodayExpenses(token, clinicId, branchId);
    }

     Double getTodayExpensesFallback(String token,
                                            String clinicId,
                                            String branchId,
                                            Exception ex) {
    	 throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getWeeklyExpensesFallback")
    @Retry(name = "clinicAdminService", fallbackMethod = "getWeeklyExpensesFallback")
    public Double getWeeklyExpenses(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getWeeklyExpenses(token, clinicId, branchId);
    }

    public Double getWeeklyExpensesFallback(String token,
                                             String clinicId,
                                             String branchId,
                                             Exception ex) {
    	throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getMonthlyExpensesFallback")
    @Retry(name = "clinicAdminService", fallbackMethod = "getMonthlyExpensesFallback")
    public Double getMonthlyExpenses(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getMonthlyExpenses(token, clinicId, branchId);
    }

    public Double getMonthlyExpensesFallback(String token,
                                              String clinicId,
                                              String branchId,
                                              Exception ex) {
    	throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "customFilterFallback")
    @Retry(name = "clinicAdminService", fallbackMethod = "customFilterFallback")
    public Double customFilter(String token, String startDate, String endDate) {
        return clinicAdminFeign.customFilter(token, startDate, endDate);
    }

    public Double customFilterFallback(String token,
                                        String startDate,
                                        String endDate,
                                        Exception ex) {
    	throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getCustomerByMobilenumberAndNameFallback")
    @Retry(name = "clinicAdminService", fallbackMethod = "getCustomerByMobilenumberAndNameFallback")
    public Map<String, String> getCustomerByMobilenumberAndName(String token,
                                                                String mobileNumber,
                                                                String name) {
        return clinicAdminFeign.getCustomerByMobilenumberAndName(token, mobileNumber, name);
    }

    public Map<String, String> getCustomerByMobilenumberAndNameFallback(String token,
                                                                         String mobileNumber,
                                                                         String name,
                                                                         Exception ex) {
    	throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getCustomerByMobileNumberAndClinicIdFallback")
    @Retry(name = "clinicAdminService", fallbackMethod = "getCustomerByMobileNumberAndClinicIdFallback")
    public CustomerOnbordingDTO getCustomerByMobileNumberAndClinicId(String token,
                                                                     String mobileNumber,
                                                                     String clinicId) {
        return clinicAdminFeign.getCustomerByMobileNumberAndClinicId(token, mobileNumber, clinicId);
    }

    public CustomerOnbordingDTO getCustomerByMobileNumberAndClinicIdFallback(String token,
                                                                              String mobileNumber,
                                                                              String clinicId,
                                                                              Exception ex) {
    	throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getCustomerByNameAndClinicIdFallback")
    @Retry(name = "clinicAdminService", fallbackMethod = "getCustomerByNameAndClinicIdFallback")
    public List<CustomerOnbordingDTO> getCustomerByNameAndClinicId(String token,
                                                                   String name,
                                                                   String clinicId) {
        return clinicAdminFeign.getCustomerByNameAndClinicId(token, name, clinicId);
    }

    public List<CustomerOnbordingDTO> getCustomerByNameAndClinicIdFallback(String token,
                                                                            String name,
                                                                            String clinicId,
                                                                            Exception ex) {
    	throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getSignedUrlFallback")
    @Retry(name = "clinicAdminService", fallbackMethod = "getSignedUrlFallback")
    public String getSignedUrl(String token, String fileKey) {
        return clinicAdminFeign.getSignedUrl(token, fileKey);
    }

    public String getSignedUrlFallback(String token,
                                        String fileKey,
                                        Exception ex) {
    	throw new RuntimeException(ex);
    }

    // ============================================================
    // NOTIFICATION SERVICE
    // ============================================================

    @CircuitBreaker(name = "notificationService", fallbackMethod = "getNotificationByBookingIdFallback")
    @Retry(name = "notificationService", fallbackMethod = "getNotificationByBookingIdFallback")
    public NotificationDTO getNotificationByBookingId(String bookingId) {
        return notificationFeign.getNotificationByBookingId(bookingId);
    }

    public NotificationDTO getNotificationByBookingIdFallback(String bookingId,
                                                               Exception ex) {
    	throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "updateNotificationFallback")
    @Retry(name = "notificationService", fallbackMethod = "updateNotificationFallback")
    public NotificationDTO updateNotification(NotificationDTO notificationDTO) {
        return notificationFeign.updateNotification(notificationDTO);
    }

    public NotificationDTO updateNotificationFallback(NotificationDTO notificationDTO,
                                                       Exception ex) {
    	throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "createNotificationFallback")
    @Retry(name = "notificationService", fallbackMethod = "createNotificationFallback")
    public Response createNotification(String token,
                                       BookingResponse booking) {
        return notificationFeign.createNotification(token, booking).getBody();
    }

    public Response createNotificationFallback(String token,
                                                BookingResponse booking,
                                                Exception ex) {
        Response response = new Response();
        response.setStatus(429);
        response.setMessage("Notification service unavailable.");
        return response;
    }

    // ============================================================
    // PHYSIO SERVICE
    // ============================================================

    @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getPhysioByBookingIdFallback")
    @Retry(name = "physioDoctorService", fallbackMethod = "getPhysioByBookingIdFallback")
    public List<SessionForBooking> getPhysioByBookingId(String token,
                                                        String bookingId,
                                                        String date) {
        return physioDoctorFeign.getPhysioByBookingId(token, bookingId, date).getBody();
    }

    public List<SessionForBooking> getPhysioByBookingIdFallback(String token,
                                                                 String bookingId,
                                                                 String date,
                                                                 Exception ex) {
    	throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getTodayFollowUpBookingIdsFallback")
    @Retry(name = "physioDoctorService", fallbackMethod = "getTodayFollowUpBookingIdsFallback")
    public List<String> getTodayFollowUpBookingIds(String token) {
        return physioDoctorFeign.getTodayFollowUpBookingIds(token);
    }

    public List<String> getTodayFollowUpBookingIdsFallback(String token,
                                                            Exception ex) {
    	throw new RuntimeException(ex);
    }

    @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getByBookingIdFallback")
    @Retry(name = "physioDoctorService", fallbackMethod = "getByBookingIdFallback")
    public String getByBookingId(String token, String bookingId) {
        return physioDoctorFeign.getByBookingId(token, bookingId);
    }

    public String getByBookingIdFallback(String token,
                                          String bookingId,
                                          Exception ex) {
        throw new RuntimeException(ex);
    }
}