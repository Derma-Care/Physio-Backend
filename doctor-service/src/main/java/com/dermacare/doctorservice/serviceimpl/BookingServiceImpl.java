package com.dermacare.doctorservice.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.dermacare.doctorservice.dto.ExtractFeignMessage;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.feignclient.BookingFeignClient;
import com.dermacare.doctorservice.service.BookingService;
import feign.FeignException;


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
    public  ResponseEntity<?> getDoctorAppointmentsonStatus(String clinicId,
		String doctorId,String status) {
    	 Response res = new Response();
    	try {
            return bookingFeignClient.getDoctorAppointmentsonStatus(clinicId, doctorId, status);
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
        }

}}

