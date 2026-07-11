package physiotherapydoctor.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import feign.FeignException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.service.BookingService;
import physiotherapydoctor.util.BookingFeignImpl;
import physiotherapydoctor.util.ExtractFeignMessage;
import physiotherapydoctor.util.KeyCloakTokenStore;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingFeignImpl bookingFeignClient;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
    

    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "getAppointmentsByPatientIdFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getAppointmentsByPatientId(
            String clinicId,
            String patientId,
            int page) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Get appointments by patient request received. ClinicId={}, PatientId={}, Page={}",
                clinicId,
                patientId,
                page);

        Response res = new Response();

        try {

            log.debug(
                    "Calling booking service. ClinicId={}, PatientId={}, PageSize={}",
                    clinicId,
                    patientId,
                    10);

            ResponseEntity<?> response =
                    bookingFeignClient.bookingByPatientId(
                            clinicId,
                            patientId,
                            page,
                            10);

            log.info(
                    "Appointments fetched successfully. ClinicId={}, PatientId={}, HttpStatus={}",
                    clinicId,
                    patientId,
                    response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error(
                    "Feign exception while fetching appointments. ClinicId={}, PatientId={}, Error={}",
                    clinicId,
                    patientId,
                    ex.getMessage(),
                    ex);

            res.setStatus(ex.status());
            res.setMessage(ExtractFeignMessage.clearMessage(ex));
            res.setSuccess(false);

            return ResponseEntity.status(ex.status()).body(res);

        } finally {

            log.info(
                    "getAppointmentsByPatientId completed. ClinicId={}, PatientId={}, ExecutionTime={} ms",
                    clinicId,
                    patientId,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "searchAppointmentsByInputFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> searchAppointmentsByInput(String input) {

        long startTime = System.currentTimeMillis();

        log.info("Search appointments request received. SearchInput={}", input);

        Response res = new Response();

        try {

            log.debug("Calling booking service search API");

            ResponseEntity<?> response =
                    bookingFeignClient.getAppointsByInput(input);

            log.info(
                    "Appointment search completed successfully. SearchInput={}, HttpStatus={}",
                    input,
                    response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error(
                    "Error while searching appointments. SearchInput={}, Error={}",
                    input,
                    ex.getMessage(),
                    ex);

            res.setStatus(ex.status());
            res.setMessage(ExtractFeignMessage.clearMessage(ex));
            res.setSuccess(false);

            return ResponseEntity.status(ex.status()).body(res);

        } finally {

            log.info(
                    "searchAppointmentsByInput completed. SearchInput={}, ExecutionTime={} ms",
                    input,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "getTodaysAppointmentsFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getTodaysAppointments(
            String clinicId,
            String doctorId,
            int page) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Today's appointments request received. ClinicId={}, DoctorId={}, Page={}",
                clinicId,
                doctorId,
                page);

        Response res = new Response();

        try {

            log.debug(
                    "Calling booking service for today's appointments. ClinicId={}, DoctorId={}",
                    clinicId,
                    doctorId);

            ResponseEntity<?> response =
                    bookingFeignClient.getTodayDoctorAppointmentsByDoctorId(
                            clinicId,
                            doctorId,
                            page,
                            10);

            log.info(
                    "Today's appointments fetched successfully. DoctorId={}, HttpStatus={}",
                    doctorId,
                    response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error(
                    "Error while fetching today's appointments. ClinicId={}, DoctorId={}, Error={}",
                    clinicId,
                    doctorId,
                    ex.getMessage(),
                    ex);

            res.setStatus(ex.status());
            res.setMessage(ExtractFeignMessage.clearMessage(ex));
            res.setSuccess(false);

            return ResponseEntity.status(ex.status()).body(res);

        } finally {

            log.info(
                    "getTodaysAppointments completed. DoctorId={}, ExecutionTime={} ms",
                    doctorId,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "getFilteredAppointmentsFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getFilteredAppointments(
            String clinicId,
            String doctorId,
            String number) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Filtered appointments request received. ClinicId={}, DoctorId={}, Filter={}",
                clinicId,
                doctorId,
                number);

        Response res = new Response();

        try {

            ResponseEntity<?> response =
                    bookingFeignClient.filterDoctorAppointmentsByDoctorId(
                            clinicId,
                            doctorId,
                            number);

            log.info(
                    "Filtered appointments fetched successfully. DoctorId={}, Filter={}",
                    doctorId,
                    number);

            return response;

        } catch (FeignException ex) {

            log.error(
                    "Error while fetching filtered appointments. ClinicId={}, DoctorId={}, Filter={}, Error={}",
                    clinicId,
                    doctorId,
                    number,
                    ex.getMessage(),
                    ex);

            res.setStatus(ex.status());
            res.setMessage(ExtractFeignMessage.clearMessage(ex));
            res.setSuccess(false);

            return ResponseEntity.status(ex.status()).body(res);

        } finally {

            log.info(
                    "getFilteredAppointments completed. DoctorId={}, ExecutionTime={} ms",
                    doctorId,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "getCompletedAppointmentsFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getCompletedAppointments(
            String clinicId,
            String doctorId) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Completed appointments request received. ClinicId={}, DoctorId={}",
                clinicId,
                doctorId);

        Response res = new Response();

        try {

            log.debug(
                    "Calling booking service completed appointments API. DoctorId={}",
                    doctorId);

            ResponseEntity<?> response =
                    bookingFeignClient.filterDoctorAppointmentsByDoctorId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            doctorId);

            log.info(
                    "Completed appointments fetched successfully. DoctorId={}, HttpStatus={}",
                    doctorId,
                    response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error(
                    "Error while fetching completed appointments. ClinicId={}, DoctorId={}, Error={}",
                    clinicId,
                    doctorId,
                    ex.getMessage(),
                    ex);

            res.setStatus(ex.status());
            res.setMessage(ExtractFeignMessage.clearMessage(ex));
            res.setSuccess(false);

            return ResponseEntity.status(ex.status()).body(res);

        } finally {

            log.info(
                    "getCompletedAppointments completed. DoctorId={}, ExecutionTime={} ms",
                    doctorId,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "getConsultationTypeCountsFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getConsultationTypeCounts(
            String clinicId,
            String doctorId) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Consultation type count request received. ClinicId={}, DoctorId={}",
                clinicId,
                doctorId);

        Response res = new Response();

        try {

            log.debug(
                    "Calling booking service consultation count API. DoctorId={}",
                    doctorId);

            ResponseEntity<?> response =
                    bookingFeignClient.getSizeOfConsultationTypesByDoctorId(
                            clinicId,
                            doctorId);

            log.info(
                    "Consultation counts fetched successfully. DoctorId={}, HttpStatus={}",
                    doctorId,
                    response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error(
                    "Error while fetching consultation counts. ClinicId={}, DoctorId={}, Error={}",
                    clinicId,
                    doctorId,
                    ex.getMessage(),
                    ex);

            res.setStatus(ex.status());
            res.setMessage(ExtractFeignMessage.clearMessage(ex));
            res.setSuccess(false);

            return ResponseEntity.status(ex.status()).body(res);

        } finally {

            log.info(
                    "getConsultationTypeCounts completed. DoctorId={}, ExecutionTime={} ms",
                    doctorId,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "getInProgressAppointmentsFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getInProgressAppointments(String mobileNumber) {

        long startTime = System.currentTimeMillis();

        log.info("Get in-progress appointments request received. MobileNumber={}",
                mobileNumber);

        Response res = new Response();

        try {

            log.debug(
                    "Calling booking service for in-progress appointments. MobileNumber={}",
                    mobileNumber);

            ResponseEntity<?> response =
                    bookingFeignClient.inProgressAppointments(mobileNumber);

            log.info(
                    "In-progress appointments fetched successfully. MobileNumber={}, HttpStatus={}",
                    mobileNumber,
                    response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error(
                    "Error while fetching in-progress appointments. MobileNumber={}, Error={}",
                    mobileNumber,
                    ex.getMessage(),
                    ex);

            res.setStatus(ex.status());
            res.setMessage(ExtractFeignMessage.clearMessage(ex));
            res.setSuccess(false);

            return ResponseEntity.status(ex.status()).body(res);

        } finally {

            log.info(
                    "getInProgressAppointments completed. MobileNumber={}, ExecutionTime={} ms",
                    mobileNumber,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "getAllBookedServicesByDoctorIdFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getAllBookedServicesByDoctorId(
            String doctorId,
            int page) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Get booked services request received. DoctorId={}, Page={}",
                doctorId,
                page);

        Response res = new Response();

        try {

            log.debug(
                    "Calling booking service. DoctorId={}, Page={}, PageSize={}",
                    doctorId,
                    page,
                    10);

            ResponseEntity<?> response =
                    bookingFeignClient.bookingByDoctorId(
                            doctorId,
                            page,
                            10);

            log.info(
                    "Booked services fetched successfully. DoctorId={}, HttpStatus={}",
                    doctorId,
                    response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error(
                    "Error while fetching booked services. DoctorId={}, Error={}",
                    doctorId,
                    ex.getMessage(),
                    ex);

            res.setStatus(ex.status());
            res.setMessage(ExtractFeignMessage.clearMessage(ex));
            res.setSuccess(false);

            return ResponseEntity.status(ex.status()).body(res);

        } finally {

            log.info(
                    "getAllBookedServicesByDoctorId completed. DoctorId={}, ExecutionTime={} ms",
                    doctorId,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "getDoctorFutureAppointmentsFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getDoctorFutureAppointments(
            String doctorId,
            int page) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Future appointments request received. DoctorId={}, Page={}",
                doctorId,
                page);

        Response res = new Response();

        try {

            log.debug(
                    "Calling future appointments API. DoctorId={}, Page={}",
                    doctorId,
                    page);

            ResponseEntity<?> response =
                    bookingFeignClient.getDoctorFutureAppointments(
                            doctorId,
                            page,
                            10);

            log.info(
                    "Future appointments fetched successfully. DoctorId={}, HttpStatus={}",
                    doctorId,
                    response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error(
                    "Error while fetching future appointments. DoctorId={}, Error={}",
                    doctorId,
                    ex.getMessage(),
                    ex);

            res.setStatus(ex.status());
            res.setMessage(ExtractFeignMessage.clearMessage(ex));
            res.setSuccess(false);

            return ResponseEntity.status(ex.status()).body(res);

        } finally {

            log.info(
                    "getDoctorFutureAppointments completed. DoctorId={}, ExecutionTime={} ms",
                    doctorId,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "getInProgressBookingsByIdsFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getInProgressBookingsByIds(
            String patientId,
            String bookingId) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Get in-progress booking request received. PatientId={}, BookingId={}",
                patientId,
                bookingId);

        Response response = new Response();

        try {

            log.debug(
                    "Calling booking service. PatientId={}, BookingId={}",
                    patientId,
                    bookingId);

            ResponseEntity<?> apiResponse =
                    bookingFeignClient
                            .getInProgressAppointmentByPatientIdAndBookingId(
                                    patientId,
                                    bookingId);

            log.info(
                    "In-progress booking fetched successfully. BookingId={}, HttpStatus={}",
                    bookingId,
                    apiResponse.getStatusCode());

            return apiResponse;

        } catch (FeignException e) {

            log.error(
                    "Error while fetching booking. PatientId={}, BookingId={}, Error={}",
                    patientId,
                    bookingId,
                    e.getMessage(),
                    e);

            response.setStatus(e.status());
            response.setMessage(e.getMessage());
            response.setSuccess(false);

            return ResponseEntity.status(response.getStatus()).body(response);

        } finally {

            log.info(
                    "getInProgressBookingsByIds completed. BookingId={}, ExecutionTime={} ms",
                    bookingId,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "getDoctorAppointmentsonStatusFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> getDoctorAppointmentsonStatus(
            String clinicId,
            String branchId,
            String doctorId,
            String status,
            int page) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Doctor appointments by status request received. ClinicId={}, BranchId={}, DoctorId={}, Status={}, Page={}",
                clinicId,
                branchId,
                doctorId,
                status,
                page);

        Response res = new Response();

        try {

            ResponseEntity<?> response =
                    bookingFeignClient
                            .getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus(
                                    clinicId,
                                    branchId,
                                    doctorId,
                                    status,
                                    page,
                                    10);

            log.info(
                    "Appointments fetched successfully. DoctorId={}, Status={}, HttpStatus={}",
                    doctorId,
                    status,
                    response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error(
                    "Error while fetching appointments by status. DoctorId={}, Status={}, Error={}",
                    doctorId,
                    status,
                    ex.getMessage(),
                    ex);

            res.setStatus(ex.status());
            res.setMessage(ExtractFeignMessage.clearMessage(ex));
            res.setSuccess(false);

            return ResponseEntity.status(ex.status()).body(res);

        } finally {

            log.info(
                    "getDoctorAppointmentsonStatus completed. DoctorId={}, ExecutionTime={} ms",
                    doctorId,
                    (System.currentTimeMillis() - startTime));
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService",
            fallbackMethod = "searchPatientFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<?> searchPatient(
            String clinicId,
            String input) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Patient search request received. ClinicId={}, SearchInput={}",
                clinicId,
                input);

        try {

            log.debug(
                    "Calling booking service patient search API. ClinicId={}, SearchInput={}",
                    clinicId,
                    input);

            ResponseEntity<ResponseStructure<List<Map<String, Object>>>> response =
                    bookingFeignClient.searchBookings(
                            clinicId,
                            input);

            log.info(
                    "Patient search completed successfully. ClinicId={}, HttpStatus={}",
                    clinicId,
                    response.getStatusCode());

            return ResponseEntity.status(response.getStatusCode())
                    .body(response.getBody());

        } catch (FeignException ex) {

            log.error(
                    "Feign exception during patient search. ClinicId={}, SearchInput={}, Error={}",
                    clinicId,
                    input,
                    ex.getMessage(),
                    ex);

            ResponseStructure<List<Map<String, Object>>> errorResponse =
                    ResponseStructure.buildResponse(
                            new ArrayList<>(),
                            ExtractFeignMessage.clearMessage(ex),
                            HttpStatus.valueOf(ex.status()),
                            ex.status());

            return ResponseEntity.status(ex.status())
                    .body(errorResponse);

        } catch (Exception e) {

            log.error(
                    "Unexpected exception during patient search. ClinicId={}, SearchInput={}, Error={}",
                    clinicId,
                    input,
                    e.getMessage(),
                    e);

            ResponseStructure<List<Map<String, Object>>> errorResponse =
                    ResponseStructure.buildResponse(
                            new ArrayList<>(),
                            "Internal Server Error : " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            500);

            return ResponseEntity.internalServerError()
                    .body(errorResponse);

        } finally {

            log.info(
                    "searchPatient completed. ClinicId={}, SearchInput={}, ExecutionTime={} ms",
                    clinicId,
                    input,
                    (System.currentTimeMillis() - startTime));
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