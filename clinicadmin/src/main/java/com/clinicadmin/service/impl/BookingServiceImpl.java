package com.clinicadmin.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import com.clinicadmin.dto.BookingRequset;
import com.clinicadmin.dto.BookingResponse;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.TheraphyAnswersDTO;
import com.clinicadmin.entity.QuestionsByPartEntity;
import com.clinicadmin.entity.QuestionsEntity;
import com.clinicadmin.feignclient.BookingFeign;
import com.clinicadmin.feignclient.CustomerServiceFeignClient;
import com.clinicadmin.service.BookingService;
import com.clinicadmin.utils.ExtractFeignMessage;
import com.clinicadmin.utils.KeyCloakTokenStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import feign.FeignException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
public class BookingServiceImpl implements BookingService {
	
	
	@Autowired
	private BookingFeign bookingFeign;

	@Autowired	
	private DoctorServiceImpl doctorServiceImpl;
	
	@Autowired	
	private KeyCloakTokenStore keyCloakTokenStore;
	
	@Autowired
	private CustomerServiceFeignClient customerServiceFeignClient;
	
	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> deleteBookedService(String id) {
		Response response = new Response();
		try {
			return bookingFeign.deleteBookedService(id);					
		} catch (FeignException e) {
			response.setStatus(e.status());
			response.setMessage(ExtractFeignMessage.clearMessage(e));
			response.setSuccess(false);
			response.setData(null);
			return ResponseEntity.status(e.status()).body(response);
		}
		
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getAllBookedServicesDetailsByBranchId(String branchId,int page) {
		Response response = new Response();
		try {
			return bookingFeign
					.bookingByBranchId(keyCloakTokenStore.getAccess_token(),branchId, page, 10);		

		} catch (FeignException e) {
			response.setStatus(e.status());
			response.setMessage(ExtractFeignMessage.clearMessage(e));
			response.setSuccess(false);
			response.setData(null);
			return ResponseEntity.status(e.status()).body(response);
		}
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getBookingsByClinicIdWithBranchId(String clinicId,
			String branchId,int page) {

		ResponseStructure<List<Map<String,Object>>> res = new ResponseStructure<>();
		try {
			return bookingFeign.getBookedServicesByClinicIdWithBranchId(keyCloakTokenStore.getAccess_token(),clinicId, branchId, page, 10);
		} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
   @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> retrieveOneWeekAppointments(String clinicId, String branchId,int page) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.retrieveOneWeekAppointments(keyCloakTokenStore.getAccess_token(),clinicId, branchId,page,10);
		} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), null, e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> retrieveAppointnmentsByServiceDate(String clinicId, String branchId, String date) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.retrieveAppointnmentsByServiceDate(keyCloakTokenStore.getAccess_token(),clinicId, branchId, date);
		} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> updateAppointmentBasedOnBookingId(BookingResponse response) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			ResponseEntity<ResponseStructure<BookingResponse>> bookingResponse = bookingFeign.updateAppointmentBasedOnBookingId(keyCloakTokenStore.getAccess_token(),response);
			if(bookingResponse.getBody().getData() != null) {
			if( response.getDoctorId() != null&&
						 response.getBranchId()!= null&&
						 response.getServiceDate()!= null&&
						 response.getServicetime()!= null) {
				 doctorServiceImpl.updateSlot(         
						 bookingResponse.getBody().getData().getDoctorId(),
						 bookingResponse.getBody().getData().getBranchId(),
						 bookingResponse.getBody().getData().getServiceDate(),
						 bookingResponse.getBody().getData().getServicetime());			
			  }} return bookingResponse;
			} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> retrieveAppointnmentsByPatientId(String patientId,int page) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.getAppointmentsByPatientId(keyCloakTokenStore.getAccess_token(),patientId, page, 10);
		} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}

	}

		// BOOKING MANAGEMENT
		@Override
		 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
		public Response bookService(BookingResponse req) throws JsonProcessingException {
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
				response.setStatus(e.status());
				response.setMessage( ExtractFeignMessage.clearMessage(e));
				response.setSuccess(false);
			}
			return response;
		}
	

@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getInprogressBookingsByPatientId(String patientId) {
    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
    try {
        return bookingFeign.getInprogressAppointmentsByPatientId(keyCloakTokenStore.getAccess_token(),patientId);
    } catch (FeignException e) {
        res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR, e.status());
        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}

@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getInprogressBookingsByPatientIdAndClinicId(String patientId, String clinicId) {
    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
    try {
        return bookingFeign.getInprogressAppointmentsByPatientIdAndClinicId(keyCloakTokenStore.getAccess_token(),patientId, clinicId);
    } catch (FeignException e) {
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
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getReprts(String clinicId,
		String branchId,
		Integer number,
	    String startDate,
		String endDate) {
    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
    try {
        return bookingFeign.getReport(keyCloakTokenStore.getAccess_token(),clinicId, branchId, number, startDate, endDate);
    } catch (FeignException e) {
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
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getTodayPhysioBookings(String clinicId,
		String branchId) {
	Response response = new Response();
    try {
        return bookingFeign.getTodayPhysioBookings(keyCloakTokenStore.getAccess_token(),clinicId, branchId);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getInProgressBookingsByIds(String patientId,
		String bookingId) {
	Response response = new Response();
    try {
        return bookingFeign.getInProgressAppointmentByPatientIdAndBookingId(keyCloakTokenStore.getAccess_token(),patientId, bookingId);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getReportsByPatientId(String patientId) {
	Response response = new Response();
    try {
        return bookingFeign.getReportsByPatientId(keyCloakTokenStore.getAccess_token(),patientId);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage(ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getUpcomingBookings(String clinicId,
		String branchId,int option) {
	Response response = new Response();
    try {
        return bookingFeign.getUpcomingBookings(keyCloakTokenStore.getAccess_token(),clinicId, branchId, option);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getBookingsByDate(String clinicId,
		String branchId, String date) {
	Response response = new Response();
    try {
        return bookingFeign.getPhysioBookingBasedOnDate(keyCloakTokenStore.getAccess_token(),clinicId, branchId, date);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getBookingsByDateRange(String clinicId,
		String branchId,String start, String end) {
	Response response = new Response();
    try {
        return bookingFeign.getPhysioBookingsByCustomeRange(keyCloakTokenStore.getAccess_token(),clinicId, branchId, start, end);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getBookedServiceById(String bookingId) {
	Response response = new Response();
    try {
        return bookingFeign.getBookedService(keyCloakTokenStore.getAccess_token(),bookingId);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getBookingById(String bookingId){
	Response response = new Response();
    try {
        return bookingFeign.getBookingById(keyCloakTokenStore.getAccess_token(),bookingId);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getTodayBookingsByClinicIdAndBranchId(String clinicId,String branchId,int page){
	Response response = new Response();
    try {
        return bookingFeign.getTodayBookings(keyCloakTokenStore.getAccess_token(),clinicId, branchId, page, 10);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage( ExtractFeignMessage.clearMessage(e));
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

@Override
@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> physioAppointment(BookingRequset req) {
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
    				System.out.println("WebSocket notification triggered: /topic/clinic-admin/bookings");

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
    	    response.setStatus(e.status());
			response.setMessage(ExtractFeignMessage.clearMessage(e));
			response.setSuccess(false);
			//response.setData(Collections.emptyList());
        return ResponseEntity.status(response.getStatus()).body(response);}
}

    
    // ================= RATE LIMIT FALLBACK METHODS =================
    
    public ResponseEntity<?> rateLimitFallback(String id, Exception ex) {
        Response response = new Response();
        response.setStatus(429);
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again later.");
        return ResponseEntity.status(429).body(response);
    }

    public ResponseEntity<?> rateLimitFallback(String p1, String p2, Exception ex) {
        Response response = new Response();
        response.setStatus(429);
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again later.");
        return ResponseEntity.status(429).body(response);
    }

    public ResponseEntity<?> rateLimitFallback(String p1, String p2, int page, Exception ex) {
        Response response = new Response();
        response.setStatus(429);
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again later.");
        return ResponseEntity.status(429).body(response);
    }

    public ResponseEntity<?> rateLimitFallback(String p1, String p2, String p3, Exception ex) {
        Response response = new Response();
        response.setStatus(429);
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again later.");
        return ResponseEntity.status(429).body(response);
    }

    public ResponseEntity<?> rateLimitFallback(String p1, String p2, String p3, int page, Exception ex) {
        Response response = new Response();
        response.setStatus(429);
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again later.");
        return ResponseEntity.status(429).body(response);
    }

    public ResponseEntity<?> rateLimitFallback(BookingRequset req, Exception ex) {
        Response response = new Response();
        response.setStatus(429);
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again later.");
        return ResponseEntity.status(429).body(response);
    }

    public Response rateLimitFallback(BookingResponse req, Exception ex) {
        Response response = new Response();
        response.setStatus(429);
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again later.");
        return response;
    }

}