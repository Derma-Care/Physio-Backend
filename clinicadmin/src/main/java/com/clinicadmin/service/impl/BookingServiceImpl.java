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
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j

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
	public ResponseEntity<?> deleteBookedService(String id) {

	    log.info("Delete booked service request received. BookingId: {}", id);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.deleteBookedService(id);

	        log.info("Booked service deleted successfully. BookingId: {}", id);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to delete booked service. BookingId: {}, Status: {}, Error: {}",
	                id,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        response.setData(null);

	        return ResponseEntity.status(e.status()).body(response);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getAllBookedServicesDetailsByBranchId(String branchId, int page) {

	    log.info("Fetching booked services by BranchId: {}, Page: {}", branchId, page);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.bookingByBranchId(
	                keyCloakTokenStore.getAccess_token(),
	                branchId,
	                page,
	                10);

	        log.info("Booked services fetched successfully. BranchId: {}, Page: {}", branchId, page);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch booked services. BranchId: {}, Page: {}, Status: {}, Error: {}",
	                branchId,
	                page,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        response.setData(null);

	        return ResponseEntity.status(e.status()).body(response);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getBookingsByClinicIdWithBranchId(String clinicId,
	        String branchId, int page) {

	    log.info("Fetching bookings by ClinicId: {}, BranchId: {}, Page: {}",
	            clinicId, branchId, page);

	    ResponseStructure<List<Map<String, Object>>> res = new ResponseStructure<>();

	    try {

	        ResponseEntity<?> result = bookingFeign.getBookedServicesByClinicIdWithBranchId(
	                keyCloakTokenStore.getAccess_token(),
	                clinicId,
	                branchId,
	                page,
	                10);

	        log.info("Bookings fetched successfully. ClinicId: {}, BranchId: {}, Page: {}",
	                clinicId, branchId, page);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch bookings. ClinicId: {}, BranchId: {}, Page: {}, Status: {}, Error: {}",
	                clinicId,
	                branchId,
	                page,
	                e.status(),
	                e.getMessage(),
	                e);

	        res = new ResponseStructure<>(
	                null,
	                ExtractFeignMessage.clearMessage(e),
	                HttpStatus.INTERNAL_SERVER_ERROR,
	                e.status());

	        return ResponseEntity.status(res.getStatusCode()).body(res);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> retrieveOneWeekAppointments(String clinicId, String branchId, int page) {

	    log.info("Retrieving one week appointments. ClinicId: {}, BranchId: {}, Page: {}",
	            clinicId, branchId, page);

	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

	    try {

	        ResponseEntity<?> result = bookingFeign.retrieveOneWeekAppointments(
	                keyCloakTokenStore.getAccess_token(),
	                clinicId,
	                branchId,
	                page,
	                10);

	        log.info("One week appointments retrieved successfully. ClinicId: {}, BranchId: {}, Page: {}",
	                clinicId, branchId, page);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to retrieve one week appointments. ClinicId: {}, BranchId: {}, Page: {}, Status: {}, Error: {}",
	                clinicId,
	                branchId,
	                page,
	                e.status(),
	                e.getMessage(),
	                e);

	        res = new ResponseStructure<>(
	                null,
	                ExtractFeignMessage.clearMessage(e),
	                null,
	                e.status());

	        return ResponseEntity.status(res.getStatusCode()).body(res);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> retrieveAppointnmentsByServiceDate(String clinicId, String branchId, String date) {

	    log.info("Retrieving appointments by service date. ClinicId: {}, BranchId: {}, ServiceDate: {}",
	            clinicId, branchId, date);

	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

	    try {

	        ResponseEntity<?> result = bookingFeign.retrieveAppointnmentsByServiceDate(
	                keyCloakTokenStore.getAccess_token(),
	                clinicId,
	                branchId,
	                date);

	        log.info("Appointments retrieved successfully. ClinicId: {}, BranchId: {}, ServiceDate: {}",
	                clinicId, branchId, date);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to retrieve appointments by service date. ClinicId: {}, BranchId: {}, ServiceDate: {}, Status: {}, Error: {}",
	                clinicId,
	                branchId,
	                date,
	                e.status(),
	                e.getMessage(),
	                e);

	        res = new ResponseStructure<>(
	                null,
	                ExtractFeignMessage.clearMessage(e),
	                HttpStatus.INTERNAL_SERVER_ERROR,
	                e.status());

	        return ResponseEntity.status(res.getStatusCode()).body(res);
	    }
	}
	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> updateAppointmentBasedOnBookingId(BookingResponse response) {

	    log.info("Update appointment request received. BookingId: {}, DoctorId: {}, BranchId: {}",
	            response.getBookingId(),
	            response.getDoctorId(),
	            response.getBranchId());

	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

	    try {

	        ResponseEntity<ResponseStructure<BookingResponse>> bookingResponse =
	                bookingFeign.updateAppointmentBasedOnBookingId(
	                        keyCloakTokenStore.getAccess_token(),
	                        response);

	        log.info("Appointment updated successfully in Booking Service. BookingId: {}",
	                response.getBookingId());

	        if (bookingResponse.getBody() != null
	                && bookingResponse.getBody().getData() != null) {

	            log.info("Checking doctor slot update for BookingId: {}",
	                    response.getBookingId());

	            if (response.getDoctorId() != null
	                    && response.getBranchId() != null
	                    && response.getServiceDate() != null
	                    && response.getServicetime() != null) {

	                log.info("Updating doctor slot. DoctorId: {}, BranchId: {}, ServiceDate: {}, ServiceTime: {}",
	                        bookingResponse.getBody().getData().getDoctorId(),
	                        bookingResponse.getBody().getData().getBranchId(),
	                        bookingResponse.getBody().getData().getServiceDate(),
	                        bookingResponse.getBody().getData().getServicetime());

	                doctorServiceImpl.updateSlot(
	                        bookingResponse.getBody().getData().getDoctorId(),
	                        bookingResponse.getBody().getData().getBranchId(),
	                        bookingResponse.getBody().getData().getServiceDate(),
	                        bookingResponse.getBody().getData().getServicetime());

	                log.info("Doctor slot updated successfully. DoctorId: {}",
	                        bookingResponse.getBody().getData().getDoctorId());

	            } else {

	                log.warn("Doctor slot update skipped due to missing doctor/branch/service details. BookingId: {}",
	                        response.getBookingId());
	            }
	        } else {

	            log.warn("Booking response data is null. BookingId: {}",
	                    response.getBookingId());
	        }

	        return bookingResponse;

	    } catch (FeignException e) {

	        log.error("Failed to update appointment. BookingId: {}, Status: {}, Error: {}",
	                response.getBookingId(),
	                e.status(),
	                e.getMessage(),
	                e);

	        res = new ResponseStructure<>(
	                null,
	                ExtractFeignMessage.clearMessage(e),
	                HttpStatus.INTERNAL_SERVER_ERROR,
	                e.status());

	        return ResponseEntity.status(res.getStatusCode()).body(res);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> retrieveAppointnmentsByPatientId(String patientId, int page) {

	    log.info("Retrieving appointments by PatientId: {}, Page: {}",
	            patientId, page);

	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

	    try {

	        ResponseEntity<?> result = bookingFeign.getAppointmentsByPatientId(
	                keyCloakTokenStore.getAccess_token(),
	                patientId,
	                page,
	                10);

	        log.info("Appointments retrieved successfully. PatientId: {}, Page: {}",
	                patientId, page);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to retrieve appointments by PatientId: {}, Page: {}, Status: {}, Error: {}",
	                patientId,
	                page,
	                e.status(),
	                e.getMessage(),
	                e);

	        res = new ResponseStructure<>(
	                null,
	                ExtractFeignMessage.clearMessage(e),
	                HttpStatus.INTERNAL_SERVER_ERROR,
	                e.status());

	        return ResponseEntity.status(res.getStatusCode()).body(res);
	    }
	}

		// BOOKING MANAGEMENT
	@Override
	@Secured("ROLE_CLINICADMIN")
	public Response bookService(BookingResponse req) throws JsonProcessingException {

	    log.info("Book service request received. PatientId: {}, DoctorId: {}, BranchId: {}, ServiceDate: {}",
	            req.getPatientId(),
	            req.getDoctorId(),
	            req.getBranchId(),
	            req.getServiceDate());

	    Response response = new Response();

	    try {

	        ResponseEntity<ResponseStructure<BookingResponse>> res =
	                bookingFeign.bookService(keyCloakTokenStore.getAccess_token(), req);

	        log.info("Booking service API executed successfully.");

	        BookingResponse bookingResponse = res.getBody().getData();

	        if (bookingResponse != null) {

	            log.info("Booking created successfully. BookingId: {}, DoctorId: {}",
	                    bookingResponse.getBookingId(),
	                    bookingResponse.getDoctorId());

	            log.info("Updating doctor slot. DoctorId: {}, BranchId: {}, ServiceDate: {}, ServiceTime: {}",
	                    bookingResponse.getDoctorId(),
	                    bookingResponse.getBranchId(),
	                    bookingResponse.getServiceDate(),
	                    bookingResponse.getServicetime());

	            doctorServiceImpl.updateSlot(
	                    bookingResponse.getDoctorId(),
	                    bookingResponse.getBranchId(),
	                    bookingResponse.getServiceDate(),
	                    bookingResponse.getServicetime());

	            log.info("Doctor slot updated successfully. DoctorId: {}",
	                    bookingResponse.getDoctorId());

	            response.setData(bookingResponse);
	            response.setMessage("follow up appointment found");
	            response.setSuccess(true);
	            response.setStatus(res.getBody().getStatusCode());

	            try {

	                log.info("Publishing booking notification to WebSocket. BookingId: {}",
	                        bookingResponse.getBookingId());

	                messagingTemplate.convertAndSend(
	                        "/topic/bookings",
	                        response
	                );

	                log.info("Booking notification published successfully.");

	            } catch (Exception e) {

	                log.error("Failed to publish booking notification. BookingId: {}, Error: {}",
	                        bookingResponse.getBookingId(),
	                        e.getMessage(),
	                        e);
	            }

	        } else {

	            log.warn("Booking service returned null data.");

	            response.setMessage("follow up appointment not found");
	            response.setSuccess(false);
	            response.setStatus(res.getStatusCode().value());
	        }

	    } catch (FeignException e) {

	        log.error("Failed to book service. PatientId: {}, DoctorId: {}, Status: {}, Error: {}",
	                req.getPatientId(),
	                req.getDoctorId(),
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	    }

	    return response;
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getInprogressBookingsByPatientId(String patientId) {

	    log.info("Fetching in-progress bookings for PatientId: {}", patientId);

	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

	    try {

	        ResponseEntity<?> result = bookingFeign.getInprogressAppointmentsByPatientId(
	                keyCloakTokenStore.getAccess_token(),
	                patientId);

	        log.info("In-progress bookings fetched successfully for PatientId: {}",
	                patientId);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch in-progress bookings. PatientId: {}, Status: {}, Error: {}",
	                patientId,
	                e.status(),
	                e.getMessage(),
	                e);

	        res = new ResponseStructure<>(
	                null,
	                ExtractFeignMessage.clearMessage(e),
	                HttpStatus.INTERNAL_SERVER_ERROR,
	                e.status());

	        return ResponseEntity.status(res.getStatusCode()).body(res);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getInprogressBookingsByPatientIdAndClinicId(String patientId, String clinicId) {

	    log.info("Fetching in-progress bookings. PatientId: {}, ClinicId: {}",
	            patientId, clinicId);

	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

	    try {

	        ResponseEntity<?> result =
	                bookingFeign.getInprogressAppointmentsByPatientIdAndClinicId(
	                        keyCloakTokenStore.getAccess_token(),
	                        patientId,
	                        clinicId);

	        log.info("In-progress bookings fetched successfully. PatientId: {}, ClinicId: {}",
	                patientId, clinicId);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch in-progress bookings. PatientId: {}, ClinicId: {}, Status: {}, Error: {}",
	                patientId,
	                clinicId,
	                e.status(),
	                e.getMessage(),
	                e);

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
	public ResponseEntity<?> getReprts(String clinicId,
	        String branchId,
	        Integer number,
	        String startDate,
	        String endDate) {

	    log.info("Fetching reports. ClinicId: {}, BranchId: {}, Number: {}, StartDate: {}, EndDate: {}",
	            clinicId, branchId, number, startDate, endDate);

	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

	    try {

	        ResponseEntity<?> result = bookingFeign.getReport(
	                keyCloakTokenStore.getAccess_token(),
	                clinicId,
	                branchId,
	                number,
	                startDate,
	                endDate);

	        log.info("Reports fetched successfully. ClinicId: {}, BranchId: {}, Number: {}",
	                clinicId, branchId, number);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch reports. ClinicId: {}, BranchId: {}, Number: {}, StartDate: {}, EndDate: {}, Status: {}, Error: {}",
	                clinicId,
	                branchId,
	                number,
	                startDate,
	                endDate,
	                e.status(),
	                e.getMessage(),
	                e);

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
	public ResponseEntity<?> getTodayPhysioBookings(String clinicId,
	                                                String branchId) {

	    log.info("Fetching today's physiotherapy bookings. ClinicId: {}, BranchId: {}",
	            clinicId, branchId);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.getTodayPhysioBookings(
	                keyCloakTokenStore.getAccess_token(),
	                clinicId,
	                branchId);

	        log.info("Today's physiotherapy bookings fetched successfully. ClinicId: {}, BranchId: {}",
	                clinicId, branchId);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch today's physiotherapy bookings. ClinicId: {}, BranchId: {}, Status: {}, Error: {}",
	                clinicId,
	                branchId,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getInProgressBookingsByIds(String patientId,
	        String bookingId) {

	    log.info("Fetching in-progress booking. PatientId: {}, BookingId: {}",
	            patientId, bookingId);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.getInProgressAppointmentByPatientIdAndBookingId(
	                keyCloakTokenStore.getAccess_token(),
	                patientId,
	                bookingId);

	        log.info("In-progress booking fetched successfully. PatientId: {}, BookingId: {}",
	                patientId, bookingId);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch in-progress booking. PatientId: {}, BookingId: {}, Status: {}, Error: {}",
	                patientId,
	                bookingId,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getReportsByPatientId(String patientId) {

	    log.info("Fetching reports by PatientId: {}", patientId);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.getReportsByPatientId(
	                keyCloakTokenStore.getAccess_token(),
	                patientId);

	        log.info("Reports fetched successfully for PatientId: {}", patientId);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch reports by PatientId: {}, Status: {}, Error: {}",
	                patientId,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getUpcomingBookings(String clinicId,
	        String branchId,
	        int option) {

	    log.info("Fetching upcoming bookings. ClinicId: {}, BranchId: {}, Option: {}",
	            clinicId, branchId, option);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.getUpcomingBookings(
	                keyCloakTokenStore.getAccess_token(),
	                clinicId,
	                branchId,
	                option);

	        log.info("Upcoming bookings fetched successfully. ClinicId: {}, BranchId: {}, Option: {}",
	                clinicId, branchId, option);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch upcoming bookings. ClinicId: {}, BranchId: {}, Option: {}, Status: {}, Error: {}",
	                clinicId,
	                branchId,
	                option,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getBookingsByDate(String clinicId,
	        String branchId,
	        String date) {

	    log.info("Fetching physiotherapy bookings by date. ClinicId: {}, BranchId: {}, Date: {}",
	            clinicId, branchId, date);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.getPhysioBookingBasedOnDate(
	                keyCloakTokenStore.getAccess_token(),
	                clinicId,
	                branchId,
	                date);

	        log.info("Physiotherapy bookings fetched successfully. ClinicId: {}, BranchId: {}, Date: {}",
	                clinicId, branchId, date);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch physiotherapy bookings. ClinicId: {}, BranchId: {}, Date: {}, Status: {}, Error: {}",
	                clinicId,
	                branchId,
	                date,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	}


	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getBookingsByDateRange(String clinicId,
	        String branchId,
	        String start,
	        String end) {

	    log.info("Fetching physiotherapy bookings by date range. ClinicId: {}, BranchId: {}, StartDate: {}, EndDate: {}",
	            clinicId, branchId, start, end);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.getPhysioBookingsByCustomeRange(
	                keyCloakTokenStore.getAccess_token(),
	                clinicId,
	                branchId,
	                start,
	                end);

	        log.info("Physiotherapy bookings fetched successfully. ClinicId: {}, BranchId: {}, StartDate: {}, EndDate: {}",
	                clinicId, branchId, start, end);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch physiotherapy bookings by date range. ClinicId: {}, BranchId: {}, StartDate: {}, EndDate: {}, Status: {}, Error: {}",
	                clinicId,
	                branchId,
	                start,
	                end,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	}


	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getBookedServiceById(String bookingId) {

	    log.info("Fetching booked service details. BookingId: {}", bookingId);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.getBookedService(
	                keyCloakTokenStore.getAccess_token(),
	                bookingId);

	        log.info("Booked service details fetched successfully. BookingId: {}",
	                bookingId);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch booked service details. BookingId: {}, Status: {}, Error: {}",
	                bookingId,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	}


	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getBookingById(String bookingId) {

	    log.info("Fetching booking details. BookingId: {}", bookingId);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.getBookingById(
	                keyCloakTokenStore.getAccess_token(),
	                bookingId);

	        log.info("Booking details fetched successfully. BookingId: {}", bookingId);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch booking details. BookingId: {}, Status: {}, Error: {}",
	                bookingId,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	}


	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> getTodayBookingsByClinicIdAndBranchId(String clinicId,
	        String branchId,
	        int page) {

	    log.info("Fetching today's bookings. ClinicId: {}, BranchId: {}, Page: {}",
	            clinicId, branchId, page);

	    Response response = new Response();

	    try {

	        ResponseEntity<?> result = bookingFeign.getTodayBookings(
	                keyCloakTokenStore.getAccess_token(),
	                clinicId,
	                branchId,
	                page,
	                10);

	        log.info("Today's bookings fetched successfully. ClinicId: {}, BranchId: {}, Page: {}",
	                clinicId, branchId, page);

	        return result;

	    } catch (FeignException e) {

	        log.error("Failed to fetch today's bookings. ClinicId: {}, BranchId: {}, Page: {}, Status: {}, Error: {}",
	                clinicId,
	                branchId,
	                page,
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<?> physioAppointment(BookingRequset req) {

	    log.info("Physiotherapy appointment request received. PatientId: {}, DoctorId: {}, ClinicId: {}, BranchId: {}, ServiceDate: {}",
	            req.getPatientId(),
	            req.getDoctorId(),
	            req.getClinicId(),
	            req.getBranchId(),
	            req.getServiceDate());

	    ResponseEntity<Response> res = null;
	    Response response = new Response();

	    try {

	        if (req.getTheraphyAnswers() != null) {

	            log.info("Processing therapy answers for PatientId: {}", req.getPatientId());

	            if (!req.getTheraphyAnswers().isEmpty()) {

	                Map<String, List<TheraphyAnswersDTO>> map = req.getTheraphyAnswers();

	                for (Map.Entry<String, List<TheraphyAnswersDTO>> entry : map.entrySet()) {

	                    String key = entry.getKey();
	                    List<TheraphyAnswersDTO> answersList = entry.getValue();

	                    log.debug("Fetching questions for body part: {}", key);

	                    QuestionsByPartEntity entity = null;

	                    try {

	                        entity = customerServiceFeignClient.getByKey(
	                                keyCloakTokenStore.getAccess_token(),
	                                key).getBody();

	                        log.debug("Questions fetched successfully for body part: {}", key);

	                    } catch (Exception ex) {

	                        log.error("Failed to fetch questions for body part: {}, Error: {}",
	                                key,
	                                ex.getMessage(),
	                                ex);
	                    }

	                    if (entity == null || entity.getQuestionsByPart() == null) {

	                        log.warn("No questions found for body part: {}", key);
	                        continue;
	                    }

	                    List<QuestionsEntity> questionsList =
	                            entity.getQuestionsByPart().get(key);

	                    if (questionsList == null || questionsList.isEmpty()) {

	                        log.warn("Question list is empty for body part: {}", key);
	                        continue;
	                    }

	                    for (TheraphyAnswersDTO dto : answersList) {

	                        for (QuestionsEntity q : questionsList) {

	                            if (q.getQuestionId() == dto.getQuestionId()) {

	                                dto.setQuestion(q.getQuestion());

	                                log.debug("Mapped QuestionId: {} with question text.",
	                                        dto.getQuestionId());

	                                break;
	                            }
	                        }
	                    }
	                }
	            }

	            log.info("Calling Booking Service to create physiotherapy appointment.");

	            res = bookingFeign.bookPhysioAppointment(
	                    keyCloakTokenStore.getAccess_token(),
	                    req);

	        } else {

	            log.info("No therapy answers provided. Creating appointment directly.");

	            res = bookingFeign.bookPhysioAppointment(
	                    keyCloakTokenStore.getAccess_token(),
	                    req);
	        }

	        if (res.getBody().getStatus() == 200) {

	            log.info("Physiotherapy appointment booked successfully. Updating doctor slot. DoctorId: {}",
	                    req.getDoctorId());

	            doctorServiceImpl.updateSlot(
	                    req.getDoctorId(),
	                    req.getBranchId(),
	                    req.getServiceDate(),
	                    req.getServicetime());

	            log.info("Doctor slot updated successfully. DoctorId: {}",
	                    req.getDoctorId());

	            try {

	                log.info("Sending WebSocket notification for physiotherapy appointment.");

	                messagingTemplate.convertAndSend(
	                        "/topic/clinic-admin/bookings",
	                        res.getBody().getData());

	                log.info("WebSocket notification sent successfully.");

	            } catch (Exception ex) {

	                log.error("Failed to send WebSocket notification. Error: {}",
	                        ex.getMessage(),
	                        ex);
	            }

	        } else {

	            log.warn("Physiotherapy appointment booking failed.");

	            response.setStatus(200);
	            response.setMessage("error occured");
	            response.setSuccess(false);
	        }

	        return res;

	    } catch (FeignException e) {

	        log.error("Failed to book physiotherapy appointment. PatientId: {}, DoctorId: {}, Status: {}, Error: {}",
	                req.getPatientId(),
	                req.getDoctorId(),
	                e.status(),
	                e.getMessage(),
	                e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	}}