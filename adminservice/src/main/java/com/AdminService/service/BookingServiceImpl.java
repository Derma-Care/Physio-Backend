package com.AdminService.service;

import java.util.List;
import java.util.Map;

import com.AdminService.dto.TheraphyAnswersDTO;
import com.AdminService.entity.QuestionsByPartEntity;
import com.AdminService.entity.QuestionsEntity;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.feign.CustomerFeign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.AdminService.dto.BookingRequset;
import com.AdminService.dto.BookingResponse;
import com.AdminService.dto.BookingResponseDTO;
import com.AdminService.feign.BookingFeign;
import com.AdminService.util.ExtractFeignMessage;
import com.AdminService.util.KeyCloakTokenStore;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;

import feign.FeignException;
import org.springframework.web.bind.annotation.PathVariable;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingFeign bookingFeign;

    @Autowired
    private ClinicAdminFeign clinicAdminFeign;

    @Autowired
    private CustomerFeign customerFeign;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
    
   
    @Override
    public ResponseEntity<?> physioAppointment(BookingRequset req) {
        ResponseEntity<Response> res = null;
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
                            entity = customerFeign.getByKey(key).getBody();
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
                res = bookingFeign.bookPhysioAppointment(req);
            }else {
                res = bookingFeign.bookPhysioAppointment(req);}
            //System.out.println(res);
            if(res.getBody().getStatus() == 200) {
//    		 System.out.println( req.getDoctorId());
//    		 System.out.println(req.getBranchId());
//    		 System.out.println( req.getServiceDate());
//    		 System.out.println( req.getServicetime() );
                clinicAdminFeign.updateDoctorSlotWhileBooking(
                        req.getDoctorId(),
                        req.getBranchId(),
                        req.getServiceDate(),
                        req.getServicetime()
                );
            }else {
                response.setStatus(200);
                response.setMessage("error occured");
                response.setSuccess(false);
                //response.setData(Collections.emptyList());
            }
            return res;
        } catch (FeignException e) {
            response.setStatus(e.status());
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setSuccess(false);
            //response.setData(Collections.emptyList());
            return ResponseEntity.status(response.getStatus()).body(response);}
    }


    @Override
    public ResponseEntity<Page<BookingResponse>>  getAllBookedServices(int page) {
        try {
        	System.out.println(keyCloakTokenStore.access_token);
            ResponseEntity<Page<BookingResponse>> res = bookingFeign.getAllBookings(page,10);
            return res; // return exactly what BookingService sends
        } catch (FeignException e) {
            throw e; // propagate exception to controller
        }
    }

    @Override
    public Response deleteBookedService(String id) {
        try {
            ResponseEntity<ResponseStructure<BookingResponse>> res = bookingFeign.deleteBookedService(id);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getBody() != null ? res.getBody().getStatusCode() : res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e; // propagate exception to controller
        }
    }

    @Override
    public ResponseEntity<?> getBookingByDoctorId(String doctorId,
                                         int page,
                                         int size) {
        try {
           return  bookingFeign.bookingByDoctorId(doctorId,page,10);
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    public Response getBookedServiceById(String bookingId) {
        try {
            ResponseEntity<ResponseStructure<BookingResponseDTO>> res = bookingFeign.getBookedService(bookingId);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getBody() != null ? res.getBody().getStatusCode() : res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    public Response getAppointmentsByPatientId(String clinicId,String patientId,int page) {
        try {
            ResponseEntity<?> res = bookingFeign.bookingByPatientId(clinicId,patientId,page,10);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    public Response updateAppointment(BookingResponseDTO bookingResponseDTO) {
        try {
            ResponseEntity<?> res = bookingFeign.updateAppointmentBasedOnBookingId(bookingResponseDTO);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    public Response getPatientDetailsForConsent(String bookingId, String patientId, String mobileNumber) {
        try {
            ResponseEntity<Response> res = bookingFeign.getPatientDetailsForConsentForm(bookingId, patientId, mobileNumber);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());
            return response;
        } catch (FeignException e) {
            throw e;
        }
    }

    @Override
    public Response getInProgressAppointments(String mobileNumber) {
        try {
            ResponseEntity<?> res = bookingFeign.inProgressAppointments(mobileNumber);
            Response response = new Response();
            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());
            return response;
        } catch (FeignException e) {throw e;
        }
}
}
