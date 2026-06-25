package com.dermaCare.customerService.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import com.dermaCare.customerService.dto.BookingRequset;
import com.dermaCare.customerService.dto.BookingResponse;
import com.dermaCare.customerService.dto.CustomerDTO;
import com.dermaCare.customerService.dto.DoctorSaveDetailsDTO;
import com.dermaCare.customerService.dto.FirstVisitHistoryRequest;
import com.dermaCare.customerService.dto.NotificationToCustomer;
import com.dermaCare.customerService.dto.PatientFeedbackDTO;
import com.dermaCare.customerService.dto.ReportsAndDoctorSaveDetailsDto;
import com.dermaCare.customerService.dto.ReportsDtoList;
import com.dermaCare.customerService.dto.TheraphyAnswersDTO;
import com.dermaCare.customerService.dto.TherapistRecordRequest;
import com.dermaCare.customerService.dto.VisitHistoryRequest;
import com.dermaCare.customerService.entity.Customer;
import com.dermaCare.customerService.entity.QuestionsByPartEntity;
import com.dermaCare.customerService.entity.QuestionsEntity;
import com.dermaCare.customerService.feignClient.AdminFeign;
import com.dermaCare.customerService.feignClient.BookingFeign;
import com.dermaCare.customerService.feignClient.ClinicAdminFeign;
//import com.dermaCare.customerService.feignClient.DoctorServiceFeign;
import com.dermaCare.customerService.feignClient.NotificationFeign;
import com.dermaCare.customerService.feignClient.PhysioFeign;
import com.dermaCare.customerService.repository.ConsultationRep;
import com.dermaCare.customerService.repository.CustomerRatingRepository;
import com.dermaCare.customerService.repository.CustomerRepository;
import com.dermaCare.customerService.util.ExtractFeignMessage;
import com.dermaCare.customerService.util.GetByKey;
import com.dermaCare.customerService.util.KeyCloakTokenStore;
import com.dermaCare.customerService.util.ResBody;
import com.dermaCare.customerService.util.Response;
import com.dermaCare.customerService.util.ResponseStructure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    public CustomerRepository customerRepository;
    
    @Autowired
    private BookingFeign bookingFeign;
    
    @Autowired
    private GetByKey getByKey;
    
    @Autowired
    private ClinicAdminFeign clinicAdminFeign;
    
    @Autowired
    private NotificationFeign notificationFeign;
       
    @Autowired
    private PhysioFeign physioFeign;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
    
     
    @Secured("ROLE_CUSTOMER")
	public Response getDoctorsSlots(String hid, String branchId, String doctorId) {

	    log.info("GET_DOCTOR_SLOTS :: START :: hospitalId={}, branchId={}, doctorId={}",
	            hid, branchId, doctorId);

	    Response response = new Response();

	    try {
	        ResponseEntity<Response> res = clinicAdminFeign.getDoctorSlot(hid, branchId, doctorId);

	        log.info("GET_DOCTOR_SLOTS :: SUCCESS :: hospitalId={}, branchId={}, doctorId={}",
	                hid, branchId, doctorId);

	        return res.getBody();

	    } catch (FeignException e) {
	        log.error("GET_DOCTOR_SLOTS :: FEIGN_ERROR :: hospitalId={}, branchId={}, doctorId={}",
	                hid, branchId, doctorId, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}



// BOOKING MANAGEMENT
    @Secured("ROLE_CUSTOMER")
	public Response bookService(BookingRequset req) throws JsonProcessingException {

	    log.info("BOOK_SERVICE :: START :: customerMobile={}, serviceId={}, doctorId={}",
	            req.getMobileNumber(), req.getSubServiceId(), req.getDoctorId());

	    Response response = new Response();
	    BookingResponse bookingResponse = null;
	    ResponseEntity<ResponseStructure<BookingResponse>> res = null;
	    try {
	        log.debug("BOOK_SERVICE :: CALLING_BOOKING_SERVICE");

	        if(req.getTheraphyAnswers()!= null) {
	        
	        	if (req.getTheraphyAnswers() != null && !req.getTheraphyAnswers().isEmpty()) {

	        	    Map<String, List<TheraphyAnswersDTO>> map = req.getTheraphyAnswers();

	        	    for (Map.Entry<String, List<TheraphyAnswersDTO>> entry : map.entrySet()) {

	        	        String key = entry.getKey(); // e.g., "back"
	        	        List<TheraphyAnswersDTO> answersList = entry.getValue();
	        	        QuestionsByPartEntity entity = null;
	        	        try {
	        	        entity = getByKey.getByKey(key);
	        	        }catch(Exception e) {}
	        	        if (entity == null || entity.getQuestionsByPart() == null) {
	        	            continue;
	        	        }

	        	        List<QuestionsEntity> questionsList = entity.getQuestionsByPart().get(key);

	        	        if (questionsList == null || questionsList.isEmpty()  ) {
	        	            continue;
	        	        }

	        	        // 🔁 Match questionId and set question
	        	        for (TheraphyAnswersDTO dto : answersList) {

	        	            for (QuestionsEntity q : questionsList) {

	        	                if (q.getQuestionId() == dto.getQuestionId()) {
	        	                    dto.setQuestion(q.getQuestion());
	        	                    break; // stop once matched
	        	                }
	        	            }
	        	        }
	        	    }
	        	}
	        res = bookingFeign.bookService(req);
	        bookingResponse = res.getBody().getData();
	        }else {
	        res = bookingFeign.bookService(req);
	        bookingResponse = res.getBody().getData();}

	        
	        if (bookingResponse != null) {

	            log.info("BOOK_SERVICE :: BOOKING_SUCCESS :: bookingId={}",
	                    bookingResponse.getBookingId());

	            log.debug("BOOK_SERVICE :: UPDATING_DOCTOR_SLOT :: doctorId={}, branchId={}",
	                    bookingResponse.getDoctorId(), bookingResponse.getBranchId());

	            clinicAdminFeign.updateDoctorSlotWhileBooking(keyCloakTokenStore.getAccess_token(),
	                    bookingResponse.getDoctorId(),
	                    bookingResponse.getBranchId(),
	                    bookingResponse.getServiceDate(),
	                    bookingResponse.getServicetime()
	            );

	            response.setData(res.getBody());
	            response.setStatus(res.getBody().getStatusCode());

	        } else {
	            log.warn("BOOK_SERVICE :: BOOKING_FAILED :: NULL_RESPONSE");

	            response.setStatus(res.getBody().getHttpStatus().value());
	            response.setData(res.getBody());
	        }

	    } catch (FeignException e) {
	        log.error("BOOK_SERVICE :: FEIGN_ERROR :: status={}", e.status(), e);

	        response.setStatus(e.status());
	        response.setMessage(e.getMessage());
	        response.setSuccess(false);
	    }

	    return response;
	}
	

//private static final double EARTH_RADIUS_KM = 6371.0;
//private double haversine(double lat1, double lon1, double lat2, double lon2) {
//    // Convert degrees to radians
//    double dLat = Math.toRadians(lat2 - lat1);
//    double dLon = Math.toRadians(lon2 - lon1);
//    
//    lat1 = Math.toRadians(lat1);
//    lat2 = Math.toRadians(lat2);
//
//    // Haversine formula
//    double a = Math.pow(Math.sin(dLat / 2), 2)
//             + Math.cos(lat1) * Math.cos(lat2)
//             * Math.pow(Math.sin(dLon / 2), 2);
//
//    double c = 2 * Math.asin(Math.sqrt(a));
//
//    return EARTH_RADIUS_KM * c; // Distance in KM
//}



@Override
@Secured("ROLE_CUSTOMER")
public ResponseEntity<?> getBookingsByCustomerId(String customerId) {

    log.info("GET_BOOKINGS_BY_CUSTOMER :: START :: customerId={}", customerId);

    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

    try {
         return bookingFeign.getBookingByCustomerId(keyCloakTokenStore.getAccess_token(),customerId);

    } catch (FeignException e) {

        log.error("GET_BOOKINGS_BY_CUSTOMER :: FEIGN_ERROR :: customerId={}",
                customerId, e);

        res = new ResponseStructure<>(
                null,
                ExtractFeignMessage.clearMessage(e),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.status()
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}


@Override
@Secured("ROLE_CUSTOMER")
public ResponseEntity<?> getCompletedBookingsByCustomerId(String customerId) {

    log.info("GET_BOOKINGS_BY_CUSTOMER :: START :: customerId={}", customerId);

    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

    try {
         return bookingFeign.getCompletedBookingByCustomerId(customerId);

    } catch (FeignException e) {

        log.error("GET_BOOKINGS_BY_CUSTOMER :: FEIGN_ERROR :: customerId={}",
                customerId, e);

        res = new ResponseStructure<>(
                null,
                ExtractFeignMessage.clearMessage(e),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.status()
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}


@Secured("ROLE_CUSTOMER")
public CustomerDTO getCustomerByToken(String token) {

    log.info("GET_CUSTOMER_BY_TOKEN :: START");

    try {
        Customer cstmr = customerRepository.findByDeviceId(token);

        if (cstmr != null) {

            log.info("GET_CUSTOMER_BY_TOKEN :: FOUND :: customerId={}",
                    cstmr.getCustomerId());

            CustomerDTO cusmrdto =
                    new ObjectMapper().convertValue(cstmr, CustomerDTO.class);

            return cusmrdto;

        } else {
            log.warn("GET_CUSTOMER_BY_TOKEN :: NOT_FOUND");
            return null;
        }

    } catch (FeignException e) {

        log.error("GET_CUSTOMER_BY_TOKEN :: FEIGN_ERROR", e);
        return null;
    }
}

@Override
@Secured("ROLE_CUSTOMER")
public ResponseEntity<Response> getTherapistSessionDetails(TherapistRecordRequest request) {
    Response response = new Response();
    try {
    	return clinicAdminFeign.getTherapistSessionDetails(keyCloakTokenStore.getAccess_token(),request);  
    } catch (FeignException e) {      
        response.setStatus(e.status());
        response.setMessage(ExtractFeignMessage.clearMessage(e));
        response.setSuccess(false);
    } return ResponseEntity.status(response.getStatus()).body(response);}


@Override
@Secured("ROLE_CUSTOMER")
public ResponseEntity<Response> getVisitHistoryByDoctor(VisitHistoryRequest request) {
    Response response = new Response();
    try {
    	return physioFeign.getVisitHistoryByDoctor(keyCloakTokenStore.getAccess_token(),request);  
    } catch (FeignException e) {      
        response.setStatus(e.status());
        response.setMessage(ExtractFeignMessage.clearMessage(e));
        response.setSuccess(false);
    } return ResponseEntity.status(response.getStatus()).body(response);}

@Override
@Secured("ROLE_CUSTOMER")
public ResponseEntity<Response> getFirstVisitHistory(FirstVisitHistoryRequest request) {
    Response response = new Response();
    try {
    	return physioFeign.getFirstVisitHistory(keyCloakTokenStore.getAccess_token(),request);  
    } catch (FeignException e) {      
        response.setStatus(e.status());
        response.setMessage(ExtractFeignMessage.clearMessage(e));
        response.setSuccess(false);
    } return ResponseEntity.status(response.getStatus()).body(response);}


@Override
@Secured("ROLE_CUSTOMER")
public ResponseEntity<?> bookPhysioAppointment(BookingRequset req) {   	
        Response response = new Response();
        try {
        	 if(req.getTheraphyAnswers()!= null) {
     	        
    	        	if (req.getTheraphyAnswers() != null && !req.getTheraphyAnswers().isEmpty()) {

    	        	    Map<String, List<TheraphyAnswersDTO>> map = req.getTheraphyAnswers();

    	        	    for (Map.Entry<String, List<TheraphyAnswersDTO>> entry : map.entrySet()) {

    	        	        String key = entry.getKey(); // e.g., "back"
    	        	        List<TheraphyAnswersDTO> answersList = entry.getValue();

    	        	        // 🔍 Fetch DB data based on key
    	        	        QuestionsByPartEntity entity = null;
    	        	        try {
    		        	        entity = getByKey.getByKey(key);
    		        	        }catch(Exception e) {}
    	        	        if (entity == null || entity.getQuestionsByPart() == null) {
    	        	            continue;
    	        	        }	        	       
    	        	        List<QuestionsEntity> questionsList = entity.getQuestionsByPart().get(key);

    	        	        if (questionsList == null || questionsList.isEmpty()  ) {
    	        	            continue;
    	        	        }

    	        	        // 🔁 Match questionId and set question
    	        	        for (TheraphyAnswersDTO dto : answersList) {

    	        	            for (QuestionsEntity q : questionsList) {

    	        	                if (q.getQuestionId() == dto.getQuestionId()) {
    	        	                    dto.setQuestion(q.getQuestion());
    	        	                    break; // stop once matched
    	        	                }
    	        	            }
    	        	        }
    	        	    }
    	        	} if(req!= null) {
    	        		 clinicAdminFeign.updateDoctorSlotWhileBooking( keyCloakTokenStore.getAccess_token(),        
    	        				 req.getDoctorId(),
    	    	                    req.getBranchId(),
    	    	                    req.getServiceDate(),
    	    	                    req.getServicetime()
    	    	            );} 
    	        return bookingFeign.bookPhysioAppointment(req);
    	        }else {
    	        	 if(req!= null) {
    	        		 clinicAdminFeign.updateDoctorSlotWhileBooking(keyCloakTokenStore.getAccess_token(),         
    	        				 req.getDoctorId(),
    	    	                    req.getBranchId(),
    	    	                    req.getServiceDate(),
    	    	                    req.getServicetime()
    	    	            );} 
        	    return bookingFeign.bookPhysioAppointment(req);}        	       	
    } catch (FeignException e) {      
        response.setStatus(e.status());
        response.setMessage(ExtractFeignMessage.clearMessage(e));
        response.setSuccess(false);
    } return ResponseEntity.status(response.getStatus()).body(response);}


@Secured("ROLE_CUSTOMER")
public ResponseEntity<ResBody<List<NotificationToCustomer>>> notificationToCustomer(
        String customerMobileNumber) {

    log.info("CUSTOMER_NOTIFICATION :: START :: mobileNumber={}", customerMobileNumber);

    try {
        log.info("CUSTOMER_NOTIFICATION :: SUCCESS :: mobileNumber={}", customerMobileNumber);
        return notificationFeign.customerNotification(customerMobileNumber);

    } catch (FeignException e) {
        log.error("CUSTOMER_NOTIFICATION :: FEIGN_ERROR :: mobileNumber={}",
                customerMobileNumber, e);

        ResBody<List<NotificationToCustomer>> res =
                new ResBody<>(ExtractFeignMessage.clearMessage(e), e.status(), null);

        return ResponseEntity.status(e.status()).body(res);
    }
}


@Override
@Secured("ROLE_CUSTOMER")
public ResponseEntity<Response> getStaffInfo(
       String hospitalId,
        String branchId){
    Response response = new Response();
    try {
    	return clinicAdminFeign.getStaffInfo(keyCloakTokenStore.getAccess_token(),hospitalId, branchId);  
    } catch (FeignException e) {      
        response.setStatus(e.status());
        response.setMessage(ExtractFeignMessage.clearMessage(e));
        response.setSuccess(false);
    } return ResponseEntity.status(response.getStatus()).body(response);}



@Override
@Secured("ROLE_CUSTOMER")
public Response createFeedback(
        PatientFeedbackDTO dto){
    Response response = new Response();
    try {
    	return clinicAdminFeign.createFeedback(keyCloakTokenStore.getAccess_token(),dto);  
    } catch (FeignException e) {      
        response.setStatus(e.status());
        response.setMessage(ExtractFeignMessage.clearMessage(e));
        response.setSuccess(false);
    } return response;}


@Override
@Secured("ROLE_CUSTOMER")
public ResponseEntity<Response> getByClinicIdAndBranchId(
      String clinicId,
      String branchId,
      String patientId){
    Response res = new Response();
    try {
    	return clinicAdminFeign.getByClinicIdAndBranchIdAndPatirntId(keyCloakTokenStore.getAccess_token(),clinicId, branchId, patientId);  
    } catch (FeignException e) {      
        res.setStatus(e.status());
        res.setMessage(ExtractFeignMessage.clearMessage(e));
        res.setSuccess(false);
    } return ResponseEntity.status(res.getStatus()).body(res);}


@Secured("ROLE_CUSTOMER")
public Response getReportsAndDoctorSaveDetails(String customerId) {

    log.info("GET_REPORTS_AND_DOCTOR_DETAILS :: START :: customerId={}", customerId);

    Response response = new Response();

    try {
        log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: CALLING_CLINIC_ADMIN :: customerId={}", customerId);

        Response res = clinicAdminFeign.getReportsBycustomerId(customerId).getBody();

        log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: REPORTS_RESPONSE_RECEIVED :: customerId={}", customerId);

        List<ReportsDtoList> repots =
                new ObjectMapper().convertValue(res.getData(), new TypeReference<List<ReportsDtoList>>() {});

        log.info("GET_REPORTS_AND_DOCTOR_DETAILS :: REPORTS_COUNT :: customerId={}, count={}",
                customerId, repots != null ? repots.size() : 0);

        log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: CALLING_DOCTOR_SERVICE :: customerId={}", customerId);

        Response rs = physioFeign.getDoctorSaveDetailsByCustomerId(customerId).getBody();

        log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: DOCTOR_DETAILS_RESPONSE_RECEIVED :: customerId={}", customerId);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<DoctorSaveDetailsDTO> doctorSaveDetailsDTO =
                mapper.convertValue(rs.getData(), new TypeReference<List<DoctorSaveDetailsDTO>>() {});

        log.info("GET_REPORTS_AND_DOCTOR_DETAILS :: DOCTOR_DETAILS_COUNT :: customerId={}, count={}",
                customerId, doctorSaveDetailsDTO != null ? doctorSaveDetailsDTO.size() : 0);

        ReportsAndDoctorSaveDetailsDto rd = new ReportsAndDoctorSaveDetailsDto();

        if (repots != null && !repots.isEmpty()) {
            log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: SETTING_REPORTS_DATA :: customerId={}", customerId);
            rd.setReportsDtoList(repots);
        }

        if (doctorSaveDetailsDTO != null && !doctorSaveDetailsDTO.isEmpty()) {
            log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: SETTING_DOCTOR_DETAILS_DATA :: customerId={}", customerId);
            rd.setDoctorSaveDetailsDTO(doctorSaveDetailsDTO);
        }

        response.setStatus(200);
        response.setMessage("Data fetched Successfully");
        response.setSuccess(true);
        response.setData(rd);

        log.info("GET_REPORTS_AND_DOCTOR_DETAILS :: SUCCESS :: customerId={}", customerId);

        return response;

    } catch (FeignException e) {

        log.error("GET_REPORTS_AND_DOCTOR_DETAILS :: FEIGN_ERROR :: customerId={}, status={}",
                customerId, e.status(), e);

        response.setStatus(e.status());
        response.setMessage(ExtractFeignMessage.clearMessage(e));
        response.setSuccess(false);
        return response;
    }
}

}