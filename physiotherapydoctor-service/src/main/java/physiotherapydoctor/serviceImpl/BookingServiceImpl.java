package physiotherapydoctor.serviceImpl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import feign.FeignException;
import org.springframework.web.bind.annotation.PathVariable;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.feign.BookingFeignClient;
import physiotherapydoctor.service.BookingService;
import physiotherapydoctor.util.ExtractFeignMessage;


@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingFeignClient bookingFeignClient;

    @Override
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
}

