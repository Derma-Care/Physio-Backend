package physiotherapydoctor.serviceImpl;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import feign.FeignException;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.feign.BookingFeignClient;
import physiotherapydoctor.service.BookingService;
import physiotherapydoctor.util.ExtractFeignMessage;


@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingFeignClient bookingFeignClient;

    @Override
    public  ResponseEntity<?> getAppointmentsByPatientId(String patientId) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.getAppointmentByPatientId(patientId);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }

    @Override
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
    public  ResponseEntity<?> getTodaysAppointments(String clinicId, String doctorId) {
       Response res = new Response();
    	try {
            return bookingFeignClient.getTodayDoctorAppointmentsByDoctorId(clinicId, doctorId);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }

    @Override
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
    public  ResponseEntity<?> getCompletedAppointments(String clinicId, String doctorId) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.filterDoctorAppointmentsByDoctorId(clinicId, doctorId);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }

    @Override
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
    public ResponseEntity<?> getAllBookedServicesByDoctorId(String doctorId) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.getBookingByDoctorId(doctorId);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }
    @Override
    public ResponseEntity<?> getDoctorFutureAppointments(String doctorId) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.getDoctorFutureAppointments(doctorId);
        } catch (FeignException ex) {
        	res.setStatus(ex.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(ex));
        	res.setSuccess(false);
            return ResponseEntity.status(ex.status()).body(res);
        }
    }
    
    @Override
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
        public  ResponseEntity<?> getDoctorAppointmentsonStatus(String clinicId,String branchId,
    		String doctorId,String status) {
        	 Response res = new Response();
        	try {
                return bookingFeignClient.getDoctorAppointmentsonStatus(clinicId, branchId, doctorId, status);
            } catch (FeignException ex) {
            	res.setStatus(ex.status());
            	res.setMessage(ExtractFeignMessage.clearMessage(ex));
            	res.setSuccess(false);
                return ResponseEntity.status(ex.status()).body(res);
            }
        }
        @Override
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
}

