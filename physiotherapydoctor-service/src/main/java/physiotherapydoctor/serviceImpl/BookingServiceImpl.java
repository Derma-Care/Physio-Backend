package physiotherapydoctor.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import feign.FeignException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import org.springframework.web.bind.annotation.PathVariable;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.feign.BookingFeignClient;
import physiotherapydoctor.service.BookingService;
import physiotherapydoctor.util.ExtractFeignMessage;
import physiotherapydoctor.util.FeignImpl;
import physiotherapydoctor.util.KeyCloakTokenStore;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private FeignImpl bookingFeignClient;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
    

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAppointmentsByPatientIdFallback")
@Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getAppointmentsByPatientId(String clinicId,
                                                         String patientId,
                                                         int page
                                                       ) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.bookingByPatientId(clinicId,patientId,page,10);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "searchAppointmentsByInputFallback")
@Secured("ROLE_DOCTOR")
    public  ResponseEntity<?> searchAppointmentsByInput(String input) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.getAppointsByInput(input);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }
    
   

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getTodaysAppointmentsFallback")
@Secured("ROLE_DOCTOR")
    public  ResponseEntity<?> getTodaysAppointments(String clinicId,
                                                    String doctorId,
                                                    int page
                                                    ) {
       Response res = new Response();
    	try {
            return bookingFeignClient.getTodayDoctorAppointmentsByDoctorId(clinicId,
                    doctorId,page,10);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getFilteredAppointmentsFallback")
@Secured("ROLE_DOCTOR")
    public  ResponseEntity<?> getFilteredAppointments(String clinicId, String doctorId, String number) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.filterDoctorAppointmentsByDoctorId(clinicId, doctorId, number);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getCompletedAppointmentsFallback")
@Secured("ROLE_DOCTOR")
    public  ResponseEntity<?> getCompletedAppointments(String clinicId, String doctorId) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.filterDoctorAppointmentsByDoctorId(keyCloakTokenStore.getAccess_token(),clinicId, doctorId);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getConsultationTypeCountsFallback")
@Secured("ROLE_DOCTOR")
    public  ResponseEntity<?> getConsultationTypeCounts(String clinicId, String doctorId) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.getSizeOfConsultationTypesByDoctorId(clinicId, doctorId);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getInProgressAppointmentsFallback")
@Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getInProgressAppointments(String mobileNumber) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.inProgressAppointments(mobileNumber);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAllBookedServicesByDoctorIdFallback")
@Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getAllBookedServicesByDoctorId(String doctorId,
                                                            int page) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.bookingByDoctorId(doctorId,page,10);

        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorFutureAppointmentsFallback")
@Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getDoctorFutureAppointments(String doctorId,
                                                         int page) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.getDoctorFutureAppointments(doctorId,page,10);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getInProgressBookingsByIdsFallback")
@Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getInProgressBookingsByIds(String patientId,
    		String bookingId) {
    	Response response = new Response();
        try {
            return bookingFeignClient.getInProgressAppointmentByPatientIdAndBookingId(patientId, bookingId);
        } catch (FeignException e) {
        	response.setStatus(e.status());
    		response.setMessage(e.getMessage());
    		response.setSuccess(false);
            return ResponseEntity.status(response.getStatus()).body(response);
        }}

        @Override
        @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorAppointmentsonStatusFallback")
@Secured("ROLE_DOCTOR")
        public  ResponseEntity<?> getDoctorAppointmentsonStatus(String clinicId,
                                                                 String branchId,
                                                                 String doctorId,
                                                                 String status,
                                                                 int page) {
        	 Response res = new Response();
        	try {
                return bookingFeignClient.getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus(clinicId,branchId,doctorId,status,page,10);
            } catch (FeignException ex) {
            	res.setStatus(ex.status());
            	res.setMessage(ExtractFeignMessage.clearMessage(ex));
            	res.setSuccess(false);
                return ResponseEntity.status(ex.status()).body(res);
            }
        }

        
        @Override
        @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "searchPatientFallback")
        @Secured("ROLE_DOCTOR")
        public ResponseEntity<?> searchPatient(String clinicId, String input) {

            try {

                ResponseEntity<ResponseStructure<List<Map<String, Object>>>> response =
                        bookingFeignClient.searchBookings(clinicId, input);

                return ResponseEntity.status(response.getStatusCode())
                        .body(response.getBody());

            } catch (FeignException ex) {

                ResponseStructure<List<Map<String, Object>>> errorResponse =
                        ResponseStructure.buildResponse(
                                new ArrayList<>(),
                                ExtractFeignMessage.clearMessage(ex),
                                org.springframework.http.HttpStatus.valueOf(ex.status()),
                                ex.status());

                return ResponseEntity.status(ex.status())
                        .body(errorResponse);

            } catch (Exception e) {

                ResponseStructure<List<Map<String, Object>>> errorResponse =
                        ResponseStructure.buildResponse(
                                new ArrayList<>(),
                                "Internal Server Error : " + e.getMessage(),
                                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                                500);

                return ResponseEntity.internalServerError()
                        .body(errorResponse);
            }
        }

    private ResponseEntity<?> buildRateLimitResponse(Exception ex) {
        Response response = new Response();
        response.setSuccess(false);
        response.setStatus(429);
        response.setMessage("Rate limit exceeded. Please try again later.");
        return ResponseEntity.status(429).body(response);
    }

    public ResponseEntity<?> getAppointmentsByPatientIdFallback(String clinicId,String patientId,int page, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> searchAppointmentsByInputFallback(String input, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> getTodaysAppointmentsFallback(String clinicId,String doctorId,int page, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> getFilteredAppointmentsFallback(String clinicId,String doctorId,String number, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> getCompletedAppointmentsFallback(String clinicId,String doctorId, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> getConsultationTypeCountsFallback(String clinicId,String doctorId, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> getInProgressAppointmentsFallback(String mobileNumber, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> getAllBookedServicesByDoctorIdFallback(String doctorId,int page, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> getDoctorFutureAppointmentsFallback(String doctorId,int page, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> getInProgressBookingsByIdsFallback(String patientId,String bookingId, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> getDoctorAppointmentsonStatusFallback(String clinicId,String branchId,String doctorId,String status,int page, Exception ex) { return buildRateLimitResponse(ex); }
    public ResponseEntity<?> searchPatientFallback(String clinicId,String input, Exception ex) { return buildRateLimitResponse(ex); }

}