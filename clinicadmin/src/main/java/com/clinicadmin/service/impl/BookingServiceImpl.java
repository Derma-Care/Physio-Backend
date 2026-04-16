package com.clinicadmin.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.clinicadmin.dto.BookingRequset;
import com.clinicadmin.dto.BookingResponse;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.feignclient.BookingFeign;
import com.clinicadmin.service.BookingService;
import com.clinicadmin.service.DoctorService;
import com.clinicadmin.utils.ExtractFeignMessage;
import com.fasterxml.jackson.core.JsonProcessingException;

import feign.FeignException;

@Service
public class BookingServiceImpl implements BookingService {
	@Autowired
	BookingFeign bookingFeign;

	@Autowired
	DoctorService doctorService;

	
	@Autowired	
	DoctorServiceImpl doctorServiceImpl;

	@Override
	public Response deleteBookedService(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response getAllBookedServicesDetailsByBranchId(String branchId) {
		Response response = new Response();
		try {
			ResponseEntity<ResponseStructure<List<BookingResponse>>> res = bookingFeign
					.getAllBookedServicesByBranchId(branchId);

			if (res == null || !res.hasBody() || res.getBody().getData() == null || res.getBody().getData().isEmpty()) {
				response.setStatus(200);
				response.setMessage("Bookings Not Found");
				response.setSuccess(true);
				response.setData(Collections.emptyList());
				return response;
			}

			// ✅ Convert ResponseStructure to Response
			ResponseStructure<List<BookingResponse>> body = res.getBody();
			response.setStatus(body.getStatusCode());
			response.setMessage(body.getMessage());
			response.setSuccess(body.getHttpStatus().is2xxSuccessful());
			response.setData(body.getData());

			return response;

		} catch (FeignException e) {
			response.setStatus(e.status());
			response.setMessage(ExtractFeignMessage.clearMessage(e));
			response.setSuccess(false);
			response.setData(null);
			return response;
		}
	}

	@Override
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingsByClinicIdWithBranchId(String clinicId,
			String branchId) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.getBookedServicesByClinicIdWithBranchId(clinicId, branchId);
		} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
	public ResponseEntity<?> retrieveOneWeekAppointments(String clinicId, String branchId) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.retrieveOneWeekAppointments(clinicId, branchId);
		} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), null, e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
	public ResponseEntity<?> retrieveAppointnmentsByServiceDate(String clinicId, String branchId, String date) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.retrieveAppointnmentsByServiceDate(clinicId, branchId, date);
		} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
	public ResponseEntity<?> updateAppointmentBasedOnBookingId(BookingResponse bookingResponse) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.updateAppointmentBasedOnBookingId(bookingResponse);
		} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
	public ResponseEntity<?> retrieveAppointnmentsByInput(String input, String clinicId) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.retrieveAppointnmentsByInput(input, clinicId);
		} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
	}

	@Override
	public ResponseEntity<?> retrieveAppointnmentsByPatientId(String patientId) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		try {
			return bookingFeign.getBookingByPatientId(patientId);
		} catch (FeignException e) {
			res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR,
					e.status());
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}

	}

	// BOOKING MANAGEMENT
	@Override
	public Response bookService(BookingRequset req) throws JsonProcessingException {
		Response response = new Response();
		try {
			ResponseEntity<ResponseStructure<BookingResponse>> res = bookingFeign.bookService(req);
			BookingResponse bookingResponse = res.getBody().getData();
			if (bookingResponse != null) {
				doctorService.updateSlot(bookingResponse.getDoctorId(), bookingResponse.getBranchId(),
						bookingResponse.getServiceDate(), bookingResponse.getServicetime());
				response.setData(res.getBody());
				response.setStatus(res.getBody().getStatusCode());
			} else {
				response.setStatus(res.getBody().getHttpStatus().value());
				response.setData(res.getBody());
			}
		} catch (FeignException e) {
			response.setStatus(e.status());
			response.setMessage(e.getMessage());
			response.setSuccess(false);
		}
		return response;
	}
	

@Override
public ResponseEntity<?> getInprogressBookingsByPatientId(String patientId) {
    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
    try {
        return bookingFeign.getInprogressAppointmentsByPatientId(patientId);
    } catch (FeignException e) {
        res = new ResponseStructure<>(null, ExtractFeignMessage.clearMessage(e), HttpStatus.INTERNAL_SERVER_ERROR, e.status());
        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}

@Override
public ResponseEntity<?> getInprogressBookingsByPatientIdAndClinicId(String patientId, String clinicId) {
    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
    try {
        return bookingFeign.getInprogressAppointmentsByPatientIdAndClinicId(patientId, clinicId);
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
public ResponseEntity<?> getReprts(String clinicId,
		String branchId,
		Integer number,
	    String startDate,
		String endDate) {
    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
    try {
        return bookingFeign.getReport(clinicId, branchId, number, startDate, endDate);
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
public ResponseEntity<?> getTodayPhysioBookings(String clinicId,
		String branchId) {
	Response response = new Response();
    try {
        return bookingFeign.getTodayPhysioBookings(clinicId, branchId);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage(e.getMessage());
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
public ResponseEntity<?> getUpcomingBookings(String clinicId,
		String branchId,int option) {
	Response response = new Response();
    try {
        return bookingFeign.getUpcomingBookings(clinicId, branchId, option);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage(e.getMessage());
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

@Override
public ResponseEntity<?> getBookingsByDate(String clinicId,
		String branchId,String patientId, String date) {
	Response response = new Response();
    try {
        return bookingFeign.getPhysioBookingBasedOnDate(clinicId, branchId, patientId, date);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage(e.getMessage());
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
public ResponseEntity<?> getBookingsByDateRange(String clinicId,
		String branchId,String start, String end) {
	Response response = new Response();
    try {
        return bookingFeign.getPhysioBookingsByCustomeRange(clinicId, branchId, start, end);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage(e.getMessage());
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
public ResponseEntity<?> getBookingById(String bookingId){
	Response response = new Response();
    try {
        return bookingFeign.getBookingById(bookingId);
    } catch (FeignException e) {
    	response.setStatus(e.status());
		response.setMessage(e.getMessage());
		response.setSuccess(false);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


@Override
public ResponseEntity<?> physioAppointment(BookingRequset bookingResponse) {
    ResponseEntity<Response> res = null;
    Response response = new Response();
    try {
    	res = bookingFeign.bookPhysioAppointment(bookingResponse);
    	//System.out.println(res);
    	 if(res.getBody().getData() != null) {
    		 doctorServiceImpl.updateSlot(         
	                    bookingResponse.getDoctorId(),
	                    bookingResponse.getBranchId(),
	                    bookingResponse.getServiceDate(),
	                    bookingResponse.getServicetime()
	            );}else {
	            	response.setStatus(400);
	       			response.setMessage("error occured");
	       			response.setSuccess(false);
	       			//response.setData(Collections.emptyList());
	            }
    	return res;
      } catch (FeignException e) {
    	    response.setStatus(e.status());
			response.setMessage(e.getMessage());
			response.setSuccess(false);
			//response.setData(Collections.emptyList());
        return ResponseEntity.status(response.getStatus()).body(response);}
}
}
