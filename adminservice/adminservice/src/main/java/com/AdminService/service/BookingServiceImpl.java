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
import org.springframework.security.access.annotation.Secured;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j

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
    @Secured("ROLE_ADMIN")
    public ResponseEntity<?> physioAppointment(BookingRequset req) {

        log.info("Received request to book physio appointment. PatientId: {}, DoctorId: {}, BranchId: {}",
                req.getPatientId(), req.getDoctorId(), req.getBranchId());

        ResponseEntity<Response> res = null;
        Response response = new Response();

        try {

            if (req.getTheraphyAnswers() != null && !req.getTheraphyAnswers().isEmpty()) {

                log.info("Processing therapy answers. Total body parts: {}", req.getTheraphyAnswers().size());

                Map<String, List<TheraphyAnswersDTO>> map = req.getTheraphyAnswers();

                for (Map.Entry<String, List<TheraphyAnswersDTO>> entry : map.entrySet()) {

                    String key = entry.getKey();
                    List<TheraphyAnswersDTO> answersList = entry.getValue();

                    log.debug("Fetching questions for body part: {}", key);

                    QuestionsByPartEntity entity = null;

                    try {

                        entity = customerFeign.getByKey(key).getBody();

                        log.debug("Questions fetched successfully for body part: {}", key);

                    } catch (Exception ex) {

                        log.warn("Unable to fetch questions for body part: {}. Error: {}",
                                key, ex.getMessage());
                    }

                    if (entity == null || entity.getQuestionsByPart() == null) {

                        log.warn("No questions found for body part: {}", key);
                        continue;
                    }

                    List<QuestionsEntity> questionsList = entity.getQuestionsByPart().get(key);

                    if (questionsList == null || questionsList.isEmpty()) {

                        log.warn("Question list is empty for body part: {}", key);
                        continue;
                    }

                    for (TheraphyAnswersDTO dto : answersList) {

                        for (QuestionsEntity q : questionsList) {

                            if (q.getQuestionId() == dto.getQuestionId()) {

                                dto.setQuestion(q.getQuestion());

                                log.debug("Mapped QuestionId {} successfully.", dto.getQuestionId());

                                break;
                            }
                        }
                    }
                }
            } else {

                log.info("No therapy answers provided.");
            }

            log.info("Calling Booking Service to create appointment.");

            res = bookingFeign.bookPhysioAppointment(
                    keyCloakTokenStore.getAccess_token(),
                    req);

            log.info("Booking Service responded with status: {}",
                    res.getBody().getStatus());

            if (res.getBody().getStatus() == 200) {

                log.info("Appointment booked successfully. Updating doctor slot.");

                clinicAdminFeign.updateDoctorSlotWhileBooking(
                        keyCloakTokenStore.getAccess_token(),
                        req.getDoctorId(),
                        req.getBranchId(),
                        req.getServiceDate(),
                        req.getServicetime());

                log.info("Doctor slot updated successfully. DoctorId: {}, Date: {}, Time: {}",
                        req.getDoctorId(),
                        req.getServiceDate(),
                        req.getServicetime());

            } else {

                log.warn("Appointment booking failed.");

                response.setStatus(200);
                response.setMessage("error occured");
                response.setSuccess(false);
            }

            log.info("Physio appointment process completed successfully.");

            return res;

        } catch (FeignException e) {

            log.error("Failed to book physio appointment. PatientId: {}, DoctorId: {}. Error: {}",
                    req.getPatientId(),
                    req.getDoctorId(),
                    e.getMessage(),
                    e);

            response.setStatus(e.status());
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setSuccess(false);

            return ResponseEntity.status(response.getStatus()).body(response);
        }
    }
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Page<BookingResponse>> getAllBookedServices(int page) {

        log.info("Received request to fetch all booked services. Page: {}, Size: {}", page, 10);

        try {

            log.info("Calling Booking Service to fetch booked services.");

            ResponseEntity<Page<BookingResponse>> res =
                    bookingFeign.getAllBookings(
                            keyCloakTokenStore.getAccess_token(),
                            page,
                            10);

            if (res.getBody() != null) {
                log.info("Booked services fetched successfully. Page: {}", page);
            } else {
                log.warn("Booking Service returned an empty response. Page: {}", page);
            }

            return res;

        } catch (FeignException e) {

            log.error("Failed to fetch booked services. Page: {}. Error: {}",
                    page,
                    e.getMessage(),
                    e);

            throw e;
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response deleteBookedService(String id) {

        log.info("Received request to delete booked service. BookingId: {}", id);

        try {

            log.info("Calling Booking Service to delete booking. BookingId: {}", id);

            ResponseEntity<ResponseStructure<BookingResponse>> res =
                    bookingFeign.deleteBookedService(
                            keyCloakTokenStore.getAccess_token(),
                            id);

            Response response = new Response();

            response.setData(res.getBody());
            response.setStatus(
                    res.getBody() != null
                            ? res.getBody().getStatusCode()
                            : res.getStatusCode().value());

            if (res.getStatusCode().is2xxSuccessful()) {

                log.info("Booked service deleted successfully. BookingId: {}", id);

            } else {

                log.warn("Booking Service returned non-success status while deleting BookingId: {}. Status: {}",
                        id,
                        res.getStatusCode().value());
            }

            return response;

        } catch (FeignException e) {

            log.error("Failed to delete booked service. BookingId: {}. Error: {}",
                    id,
                    e.getMessage(),
                    e);

            throw e;
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<?> getBookingByDoctorId(String doctorId,
                                                  int page,
                                                  int size) {

        log.info("Received request to fetch bookings by DoctorId: {}, Page: {}, Size: {}",
                doctorId, page, size);

        try {

            log.info("Calling Booking Service to fetch bookings for DoctorId: {}", doctorId);

            ResponseEntity<?> response = bookingFeign.bookingByDoctorId(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId,
                    page,
                    10);

            if (response.getStatusCode().is2xxSuccessful()) {

                log.info("Bookings fetched successfully for DoctorId: {}",
                        doctorId);

            } else {

                log.warn("Booking Service returned non-success status for DoctorId: {}. Status: {}",
                        doctorId,
                        response.getStatusCode().value());
            }

            return response;

        } catch (FeignException e) {

            log.error("Failed to fetch bookings for DoctorId: {}. Error: {}",
                    doctorId,
                    e.getMessage(),
                    e);

            throw e;
        }
    }
    @Override
    @Secured("ROLE_ADMIN")
    public Response getBookedServiceById(String bookingId) {

        log.info("Received request to fetch booked service. BookingId: {}", bookingId);

        try {

            log.info("Calling Booking Service to fetch booking details. BookingId: {}", bookingId);

            ResponseEntity<ResponseStructure<BookingResponseDTO>> res =
                    bookingFeign.getBookedService(
                            keyCloakTokenStore.getAccess_token(),
                            bookingId);

            Response response = new Response();

            response.setData(res.getBody());
            response.setStatus(
                    res.getBody() != null
                            ? res.getBody().getStatusCode()
                            : res.getStatusCode().value());

            if (res.getStatusCode().is2xxSuccessful()) {

                log.info("Booked service fetched successfully. BookingId: {}", bookingId);

            } else {

                log.warn("Booking Service returned non-success status for BookingId: {}. Status: {}",
                        bookingId,
                        res.getStatusCode().value());
            }

            return response;

        } catch (FeignException e) {

            log.error("Failed to fetch booked service. BookingId: {}. Error: {}",
                    bookingId,
                    e.getMessage(),
                    e);

            throw e;
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response getAppointmentsByPatientId(String clinicId, String patientId, int page) {

        log.info("Received request to fetch appointments. ClinicId: {}, PatientId: {}, Page: {}",
                clinicId, patientId, page);

        try {

            log.info("Calling Booking Service to fetch appointments for PatientId: {}",
                    patientId);

            ResponseEntity<?> res = bookingFeign.bookingByPatientId(
                    keyCloakTokenStore.getAccess_token(),
                    clinicId,
                    patientId,
                    page,
                    10);

            Response response = new Response();

            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());

            if (res.getStatusCode().is2xxSuccessful()) {

                log.info("Appointments fetched successfully. ClinicId: {}, PatientId: {}",
                        clinicId, patientId);

            } else {

                log.warn("Booking Service returned non-success status. ClinicId: {}, PatientId: {}, Status: {}",
                        clinicId,
                        patientId,
                        res.getStatusCode().value());
            }

            return response;

        } catch (FeignException e) {

            log.error("Failed to fetch appointments. ClinicId: {}, PatientId: {}. Error: {}",
                    clinicId,
                    patientId,
                    e.getMessage(),
                    e);

            throw e;
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response updateAppointment(BookingResponseDTO bookingResponseDTO) {

        log.info("Received request to update appointment. BookingId: {}",
                bookingResponseDTO.getBookingId());

        try {

            log.info("Calling Booking Service to update appointment. BookingId: {}",
                    bookingResponseDTO.getBookingId());

            ResponseEntity<?> res = bookingFeign.updateAppointmentBasedOnBookingId(
                    keyCloakTokenStore.getAccess_token(),
                    bookingResponseDTO);

            Response response = new Response();

            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());

            if (res.getStatusCode().is2xxSuccessful()) {

                log.info("Appointment updated successfully. BookingId: {}",
                        bookingResponseDTO.getBookingId());

            } else {

                log.warn("Booking Service returned non-success status while updating appointment. BookingId: {}, Status: {}",
                        bookingResponseDTO.getBookingId(),
                        res.getStatusCode().value());
            }

            return response;

        } catch (FeignException e) {

            log.error("Failed to update appointment. BookingId: {}. Error: {}",
                    bookingResponseDTO.getBookingId(),
                    e.getMessage(),
                    e);

            throw e;
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response getPatientDetailsForConsent(String bookingId, String patientId, String mobileNumber) {

        log.info("Received request to fetch patient details for consent. BookingId: {}, PatientId: {}",
                bookingId, patientId);

        try {

            log.info("Calling Booking Service to fetch consent details. BookingId: {}, PatientId: {}",
                    bookingId, patientId);

            ResponseEntity<Response> res = bookingFeign.getPatientDetailsForConsentForm(
                    keyCloakTokenStore.getAccess_token(),
                    bookingId,
                    patientId,
                    mobileNumber);

            Response response = new Response();

            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());

            if (res.getStatusCode().is2xxSuccessful()) {

                log.info("Patient consent details fetched successfully. BookingId: {}, PatientId: {}",
                        bookingId, patientId);

            } else {

                log.warn("Booking Service returned non-success status while fetching consent details. BookingId: {}, PatientId: {}, Status: {}",
                        bookingId,
                        patientId,
                        res.getStatusCode().value());
            }

            return response;

        } catch (FeignException e) {

            log.error("Failed to fetch patient consent details. BookingId: {}, PatientId: {}. Error: {}",
                    bookingId,
                    patientId,
                    e.getMessage(),
                    e);

            throw e;
        }
    }
    @Override
    @Secured("ROLE_ADMIN")
    public Response getInProgressAppointments(String mobileNumber) {

        log.info("Received request to fetch in-progress appointments.");

        try {

            log.info("Calling Booking Service to fetch in-progress appointments.");

            ResponseEntity<?> res = bookingFeign.inProgressAppointments(
                    keyCloakTokenStore.getAccess_token(),
                    mobileNumber);

            Response response = new Response();

            response.setData(res.getBody());
            response.setStatus(res.getStatusCode().value());

            if (res.getStatusCode().is2xxSuccessful()) {

                log.info("In-progress appointments fetched successfully.");

            } else {

                log.warn("Booking Service returned non-success status while fetching in-progress appointments. Status: {}",
                        res.getStatusCode().value());
            }

            return response;

        } catch (FeignException e) {

            log.error("Failed to fetch in-progress appointments. Error: {}",
                    e.getMessage(),
                    e);

            throw e;
        }
    }
}
