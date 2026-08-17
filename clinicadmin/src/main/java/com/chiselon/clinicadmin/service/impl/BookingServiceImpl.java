package com.chiselon.clinicadmin.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import com.chiselon.clinicadmin.dto.BookingRequset;
import com.chiselon.clinicadmin.dto.BookingResponse;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.dto.ResponseStructure;
import com.chiselon.clinicadmin.dto.TheraphyAnswersDTO;
import com.chiselon.clinicadmin.entity.QuestionsByPartEntity;
import com.chiselon.clinicadmin.entity.QuestionsEntity;
import com.chiselon.clinicadmin.service.BookingService;
import com.chiselon.clinicadmin.utils.ExtractFeignMessage;
import com.chiselon.clinicadmin.utils.FeignImpl;
import com.chiselon.clinicadmin.utils.KeyCloakTokenStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import feign.FeignException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {
	
	
	@Autowired
	private FeignImpl bookingFeign;

	@Autowired	
	private DoctorServiceImpl doctorServiceImpl;
	
	@Autowired	
	private KeyCloakTokenStore keyCloakTokenStore;
	
	@Autowired
	private FeignImpl customerServiceFeignClient;
	
	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteBookedServiceFallback")
	public ResponseEntity<?> deleteBookedService(String id) {
		log.info("Deleting booked service bookingId={}", id);
		Response response = new Response();
		try {
			return bookingFeign.deleteBookedService(id);					
		} catch (FeignException e) {
			log.error("Feign exception occurred", e);
			log.error("Feign call failed", e);
			response.setStatus(e.status());
			response.setMessage(ExtractFeignMessage.clearMessage(e));
			response.setSuccess(false);
			response.setData(null);
			return ResponseEntity.status(e.status()).body(response);
		}
		
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllBookedServicesDetailsByBranchIdFallback")
	public ResponseEntity<?> getAllBookedServicesDetailsByBranchId(String branchId,int page) {
		log.info("Fetching bookings by branchId={} page={}", branchId, page);
		Response response = new Response();
		try {
			return bookingFeign
					.bookingByBranchId(keyCloakTokenStore.getAccess_token(),branchId, page, 10);		

		} catch (FeignException e) {
			log.error("Feign exception occurred", e);
			response.setStatus(e.status());
			response.setMessage(ExtractFeignMessage.clearMessage(e));
			response.setSuccess(false);
			response.setData(null);
			return ResponseEntity.status(e.status()).body(response);
		}
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getBookingsByClinicIdWithBranchIdFallback")
	public ResponseEntity<?> getBookingsByClinicIdWithBranchId(String clinicId,
			String branchId,int page) {
		log.info("Fetching bookings clinicId={} branchId={} page={}", clinicId, branchId, page);

		ResponseStructure<List<Map<String,Object>>> res = new ResponseStructure<>();
		try {
			return bookingFeign.getBookedServicesByClinicIdWithBranchId(keyCloakTokenStore.getAccess_token(),clinicId, branchId, page, 10);
		} catch (FeignException e) {
			log.error("Feign exception occurred", e);
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
   @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "retrieveOneWeekAppointmentsFallback")
	public ResponseEntity<?> retrieveOneWeekAppointments(String clinicId, String branchId,int page) {
		log.info("Fetching one week appointments clinicId={} branchId={} page={}", clinicId, branchId, page);
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.retrieveOneWeekAppointments(keyCloakTokenStore.getAccess_token(),clinicId, branchId,page,10);
		} catch (FeignException e) {
			log.error("Feign exception occurred", e);
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), null, e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "retrieveAppointnmentsByServiceDateFallback")
	public ResponseEntity<?> retrieveAppointnmentsByServiceDate(String clinicId, String branchId, String date) {
		log.info("Fetching appointments clinicId={} branchId={} date={}", clinicId, branchId, date);
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.retrieveAppointnmentsByServiceDate(keyCloakTokenStore.getAccess_token(),clinicId, branchId, date);
		} catch (FeignException e) {
			log.error("Feign exception occurred", e);
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateAppointmentBasedOnBookingIdFallback")
	public ResponseEntity<?> updateAppointmentBasedOnBookingId(BookingResponse response) {
		log.info("Updating appointment bookingId={}", response.getBookingId());
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			ResponseEntity<ResponseStructure<BookingResponse>> bookingResponse = bookingFeign.updateAppointmentBasedOnBookingId(keyCloakTokenStore.getAccess_token(),response);
			if(bookingResponse.getBody().getData() != null) {
			if( response.getDoctorId() != null&&
						 response.getBranchId()!= null&&
						 response.getServiceDate()!= null&&
						 response.getServicetime()!= null) {
				 log.info("Updating doctor slot doctorId={} date={} time={}",  bookingResponse.getBody().getData().getDoctorId(), 	 bookingResponse.getBody().getData().getServiceDate(), bookingResponse.getBody().getData().getServicetime());					
		          		 doctorServiceImpl.updateSlot(         
						 bookingResponse.getBody().getData().getDoctorId(),
						 bookingResponse.getBody().getData().getBranchId(),
						 bookingResponse.getBody().getData().getServiceDate(),
						 bookingResponse.getBody().getData().getServicetime());			
			  }} return bookingResponse;
			} catch (FeignException e) {
			log.error("Feign exception occurred", e);
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "retrieveAppointnmentsByPatientIdFallback")
	public ResponseEntity<?> retrieveAppointnmentsByPatientId(String patientId,int page) {
		log.info("Fetching appointments patientId={} page={}", patientId, page);
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.getAppointmentsByPatientId(keyCloakTokenStore.getAccess_token(),patientId, page, 10);
		} catch (FeignException e) {
			log.error("Feign exception occurred", e);
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}

	}

		// BOOKING MANAGEMENT
		@Override
		 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "bookServiceFallback")
		public Response bookService(BookingResponse req) throws JsonProcessingException {
			log.info("Booking service request patientId={} doctorId={}", req.getPatientId(), req.getDoctorId());
			Response response = new Response();
			try {
				ResponseEntity<ResponseStructure<BookingResponse>> res = bookingFeign.bookService(keyCloakTokenStore.getAccess_token(),req);
				BookingResponse bookingResponse = res.getBody().getData();
				if (bookingResponse != null) {
					 doctorServiceImpl.updateSlot(         
							 bookingResponse.getDoctorId(),
							 bookingResponse.getBranchId(),
							 bookingResponse.getServiceDate(),
							 bookingResponse.getServicetime());
					response.setData(bookingResponse);
					response.setMessage("follow up appointment found");
					response.setSuccess(true);
					response.setStatus(res.getBody().getStatusCode());
					
					try {
						log.info("Publishing websocket booking notification");
						messagingTemplate.convertAndSend(
								"/topic/bookings",
								response
						);
					} catch (Exception e) {
						
					}

				} else {				
					response.setMessage("follow up appointment not found");
					response.setSuccess(false);
					response.setStatus(res.getStatusCode().value());
				}
			} catch (FeignException e) {
			log.error("Feign exception occurred", e);
				response.setStatus(e.status());
				response.setMessage( ExtractFeignMessage.clearMessage(e));
				response.setSuccess(false);
			}
			return response;
		}
	

//@Override
//@Secured("ROLE_CLINICADMIN")
//    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
//public ResponseEntity<?> getInprogressBookingsByPatientId(String patientId) {
//    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
//    try {
//        return bookingFeign.getInprogressAppointmentsByPatientId(keyCloakTokenStore.getAccess_token(),patientId);
//    } catch (FeignException e) {
			//log.error("Feign exception occurred", e);
//        res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR, e.status());
//        return ResponseEntity.status(res.getStatusCode()).body(res);
//    }
//}
//
//@Override
//@Secured("ROLE_CLINICADMIN")
//    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
//public ResponseEntity<?> getInprogressBookingsByPatientIdAndClinicId(String patientId, String clinicId) {
//    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
//    try {
//        return bookingFeign.getInprogressAppointmentsByPatientIdAndClinicId(keyCloakTokenStore.getAccess_token(),patientId, clinicId);
//    } catch (FeignException e) {
			//log.error("Feign exception occurred", e);
//        res = new ResponseStructure<>(
//                null,
//                ExtractFeignMessage.clearMessage(e),
//                HttpStatus.INTERNAL_SERVER_ERROR,
//                e.status()
//        );
//        return ResponseEntity.status(res.getStatusCode()).body(res);
//    }
//}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getReprtsFallback")
public ResponseEntity<?> getReprts(String clinicId,
		String branchId,
		Integer number,
	    String startDate,
		String endDate) {
		log.info("Generating report clinicId={} branchId={} startDate={} endDate={}", clinicId, branchId, startDate, endDate);
    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
    try {
        return bookingFeign.getReport(keyCloakTokenStore.getAccess_token(),clinicId, branchId, number, startDate, endDate);
    } catch (FeignException e) {
			log.error("Feign exception occurred", e);
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
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getTodayPhysioBookingsFallback")
public ResponseEntity<?> getTodayPhysioBookings(String clinicId,
		String branchId) {
		log.info("Fetching today physio bookings clinicId={} branchId={}", clinicId, branchId);
	Response response = new Response();
    try {
        return bookingFeign.getTodayPhysioBookings(keyCloakTokenStore.getAccess_token(),clinicId, branchId);
    } catch (FeignException e) {
			log.error("Feign exception occurred", e);
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getInProgressBookingsByIdsFallback")
public ResponseEntity<?> getInProgressBookingsByIds(String patientId,
		String bookingId) {
		log.info("Fetching in-progress booking patientId={} bookingId={}", patientId, bookingId);
	Response response = new Response();
    try {
        return bookingFeign.getInProgressAppointmentByPatientIdAndBookingId(keyCloakTokenStore.getAccess_token(),patientId, bookingId);
    } catch (FeignException e) {
			log.error("Feign exception occurred", e);
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getReportsByPatientIdFallback")
public ResponseEntity<?> getReportsByPatientId(String patientId) {
		log.info("Fetching reports patientId={}", patientId);
	Response response = new Response();
    try {
        return bookingFeign.getReportsByPatientId(keyCloakTokenStore.getAccess_token(),patientId);
    } catch (FeignException e) {
			log.error("Feign exception occurred", e);
    	response.setStatus(e.status());
		response.setMessage(ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getUpcomingBookingsFallback")
public ResponseEntity<?> getUpcomingBookings(String clinicId,
		String branchId,int option) {
		log.info("Fetching upcoming bookings clinicId={} branchId={} option={}", clinicId, branchId, option);
	Response response = new Response();
    try {
        return bookingFeign.getUpcomingBookings(keyCloakTokenStore.getAccess_token(),clinicId, branchId, option);
    } catch (FeignException e) {
			log.error("Feign exception occurred", e);
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getBookingsByDateFallback")
public ResponseEntity<?> getBookingsByDate(String clinicId,
		String branchId, String date) {
		log.info("Fetching bookings by date clinicId={} branchId={} date={}", clinicId, branchId, date);
	Response response = new Response();
    try {
        return bookingFeign.getPhysioBookingBasedOnDate(keyCloakTokenStore.getAccess_token(),clinicId, branchId, date);
    } catch (FeignException e) {
			log.error("Feign exception occurred", e);
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getBookingsByDateRangeFallback")
public ResponseEntity<?> getBookingsByDateRange(String clinicId,
		String branchId,String start, String end) {
		log.info("Fetching bookings by range clinicId={} branchId={} start={} end={}", clinicId, branchId, start, end);
	Response response = new Response();
    try {
        return bookingFeign.getPhysioBookingsByCustomeRange(keyCloakTokenStore.getAccess_token(),clinicId, branchId, start, end);
    } catch (FeignException e) {
			log.error("Feign exception occurred", e);
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getBookedServiceByIdFallback")
public ResponseEntity<?> getBookedServiceById(String bookingId) {
		log.info("Fetching booked service bookingId={}", bookingId);
	Response response = new Response();
    try {
        return bookingFeign.getBookedService(keyCloakTokenStore.getAccess_token(),bookingId);
    } catch (FeignException e) {
			log.error("Feign exception occurred", e);
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getBookingByIdFallback")
public ResponseEntity<?> getBookingById(String bookingId){
		log.info("Fetching booking bookingId={}", bookingId);
	Response response = new Response();
    try {
        return bookingFeign.getBookingById(keyCloakTokenStore.getAccess_token(),bookingId);
    } catch (FeignException e) {
			log.error("Feign exception occurred", e);
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getTodayBookingsByClinicIdAndBranchIdFallback")
public ResponseEntity<?> getTodayBookingsByClinicIdAndBranchId(String clinicId,String branchId,int page){
		log.info("Fetching today bookings clinicId={} branchId={} page={}", clinicId, branchId, page);
	Response response = new Response();
    try {
        return bookingFeign.getTodayBookings(keyCloakTokenStore.getAccess_token(),clinicId, branchId, page, 10);
    } catch (FeignException e) {
			log.error("Feign exception occurred", e);
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "physioAppointmentFallback")
public ResponseEntity<?> getFilteredBookingsByStatus(String clinicId,String branchId){
	Response response = new Response();
    try {
        return bookingFeign.getFilteredBookingsByStatus(keyCloakTokenStore.getAccess_token(),clinicId, branchId);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

@Override
public ResponseEntity<?> physioAppointment(BookingRequset req) {
    log.info("Physio appointment request clinicId={} branchId={} patientId={}", req.getClinicId(), req.getBranchId(), req.getPatientId());
    ResponseEntity<Response> res = null;
    //System.out.println(keyCloakTokenStore.access_token);
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
	        	        entity = customerServiceFeignClient.getByKey(keyCloakTokenStore.getAccess_token(),key).getBody();
	        	        }catch(Exception e) { log.warn("Failed to fetch questionnaire data", e); }
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
	        res = bookingFeign.bookPhysioAppointment(keyCloakTokenStore.getAccess_token(),req);
	        }else {
    	    res = bookingFeign.bookPhysioAppointment(keyCloakTokenStore.getAccess_token(),req);}
    	//System.out.println(res);
    	 if(res.getBody().getStatus() == 200) {
//    		 System.out.println( req.getDoctorId());
//    		 System.out.println(req.getBranchId());
//    		 System.out.println( req.getServiceDate());
//    		 System.out.println( req.getServicetime() );
    		 doctorServiceImpl.updateSlot(         
    				 req.getDoctorId(),
	                    req.getBranchId(),
	                    req.getServiceDate(),
	                    req.getServicetime()
	            );
    			try {
    				log.info("WebSocket notification triggered: /topic/clinic-admin/bookings");

        			messagingTemplate.convertAndSend(
        					  "/topic/clinic-admin/bookings",
        					res.getBody().getData()
        			);
        		} catch (Exception e) {
        			// Do nothing.
        			// WebSocket errors should not affect the normal API flow.
        		}
    			}else {
	            	response.setStatus(200);
	       			response.setMessage("error occured");
	       			response.setSuccess(false);
	       			//response.setData(Collections.emptyList());
	            }
    	return res;
      } catch (FeignException e) {
			log.error("Feign exception occurred", e);
    	    response.setStatus(e.status());
			response.setMessage(ExtractFeignMessage.clearMessage(e));
			response.setSuccess(false);
			//response.setData(Collections.emptyList());
        return ResponseEntity.status(response.getStatus()).body(response);}
}

    
    

    // ================= RATE LIMIT FALLBACK METHODS =================

    private ResponseEntity<?> rateLimitResponse(String methodName, Exception ex) {
        log.error("Rate limiter triggered in {}", methodName, ex);
        Response response = new Response();
        response.setStatus(429);
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again later.");
        return ResponseEntity.status(429).body(response);
    }

    public ResponseEntity<?> deleteBookedServiceFallback(String id, Exception ex){ return rateLimitResponse("deleteBookedService", ex); }
    public ResponseEntity<?> getAllBookedServicesDetailsByBranchIdFallback(String branchId,int page, Exception ex){ return rateLimitResponse("getAllBookedServicesDetailsByBranchId", ex); }
    public ResponseEntity<?> getBookingsByClinicIdWithBranchIdFallback(String clinicId,String branchId,int page, Exception ex){ return rateLimitResponse("getBookingsByClinicIdWithBranchId", ex); }
    public ResponseEntity<?> retrieveOneWeekAppointmentsFallback(String clinicId,String branchId,int page, Exception ex){ return rateLimitResponse("retrieveOneWeekAppointments", ex); }
    public ResponseEntity<?> retrieveAppointnmentsByServiceDateFallback(String clinicId,String branchId,String date, Exception ex){ return rateLimitResponse("retrieveAppointnmentsByServiceDate", ex); }
    public ResponseEntity<?> updateAppointmentBasedOnBookingIdFallback(BookingResponse responseObj, Exception ex){ return rateLimitResponse("updateAppointmentBasedOnBookingId", ex); }
    public ResponseEntity<?> retrieveAppointnmentsByPatientIdFallback(String patientId,int page, Exception ex){ return rateLimitResponse("retrieveAppointnmentsByPatientId", ex); }
    public Response bookServiceFallback(BookingResponse req, Exception ex){ log.error("Rate limiter triggered in bookService", ex); Response r=new Response(); r.setStatus(429); r.setSuccess(false); r.setMessage("Too many requests. Please try again later."); return r; }
    public ResponseEntity<?> getReprtsFallback(String clinicId,String branchId,Integer number,String startDate,String endDate, Exception ex){ return rateLimitResponse("getReprts", ex); }
    public ResponseEntity<?> getTodayPhysioBookingsFallback(String clinicId,String branchId, Exception ex){ return rateLimitResponse("getTodayPhysioBookings", ex); }
    public ResponseEntity<?> getInProgressBookingsByIdsFallback(String patientId,String bookingId, Exception ex){ return rateLimitResponse("getInProgressBookingsByIds", ex); }
    public ResponseEntity<?> getReportsByPatientIdFallback(String patientId, Exception ex){ return rateLimitResponse("getReportsByPatientId", ex); }
    public ResponseEntity<?> getUpcomingBookingsFallback(String clinicId,String branchId,int option, Exception ex){ return rateLimitResponse("getUpcomingBookings", ex); }
    public ResponseEntity<?> getBookingsByDateFallback(String clinicId,String branchId,String date, Exception ex){ return rateLimitResponse("getBookingsByDate", ex); }
    public ResponseEntity<?> getBookingsByDateRangeFallback(String clinicId,String branchId,String start,String end, Exception ex){ return rateLimitResponse("getBookingsByDateRange", ex); }
    public ResponseEntity<?> getBookedServiceByIdFallback(String bookingId, Exception ex){ return rateLimitResponse("getBookedServiceById", ex); }
    public ResponseEntity<?> getBookingByIdFallback(String bookingId, Exception ex){ return rateLimitResponse("getBookingById", ex); }
    public ResponseEntity<?> getTodayBookingsByClinicIdAndBranchIdFallback(String clinicId,String branchId,int page, Exception ex){ return rateLimitResponse("getTodayBookingsByClinicIdAndBranchId", ex); }
    public ResponseEntity<?> physioAppointmentFallback(BookingRequset req, Exception ex){ return rateLimitResponse("physioAppointment", ex); }
}
