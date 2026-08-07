package com.dermacare.bookingService.service.Impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.dermacare.bookingService.dto.*;
import com.dermacare.bookingService.feign.AdminServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.dermacare.bookingService.entity.Booking;
import com.dermacare.bookingService.entity.ConsultationFees;
import com.dermacare.bookingService.entity.FollowupBooking;
import com.dermacare.bookingService.entity.Reports;
import com.dermacare.bookingService.entity.ReportsList;
import com.dermacare.bookingService.entity.Status;
import com.dermacare.bookingService.entity.TheraphyAnswersEntity;
import com.dermacare.bookingService.feign.ClinicAdminFeign;
import com.dermacare.bookingService.feign.NotificationFeign;
import com.dermacare.bookingService.feign.PhysioDoctorFeign;
import com.dermacare.bookingService.repository.BookingServiceRepository;
import com.dermacare.bookingService.service.BookingService_Service;
import com.dermacare.bookingService.service.S3Service;
import com.dermacare.bookingService.util.Response;
import com.dermacare.bookingService.util.ResponseStructure;
import com.dermacare.bookingService.util.geneateIds;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class BookingService_ServiceImpl implements BookingService_Service {

	@Autowired
	private BookingServiceRepository repository;

	@Autowired
	private PhysioDoctorFeign physioDoctorFeign;

	@Autowired
	private ClinicAdminFeign clinnicfeign;

    @Autowired
    private AdminServiceClient adminServiceClient;

    @Autowired
	private NotificationFeign notificationFeign;

	@Autowired
	private ClinicAdminFeign clinicAdminFeign;

	@Autowired
	private geneateIds sequenceGeneratorService;

	@Autowired
	private S3Service s3Service;

	@Autowired
	private WhatsAppService whatsAppService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private MongoTemplate mongoTemplate;

	// Cap for unbounded list endpoints (see CHANGE 3 / getAllBookedServices)
	private static final int MAX_UNPAGED_RESULTS = 500;

	@Override
	public ResponseEntity<?> addService(BookingResponse request) {
	    ResponseStructure<Map<String,String>> response = new ResponseStructure<>();
	    try {
	        Booking updatedBooking = updateForFollowup(request);
	        if (updatedBooking != null) {
	            try {
	                DoctorPushNotificationDTO dto = new DoctorPushNotificationDTO();
	                dto.setDoctorId(updatedBooking.getDoctorId());
	                dto.setBookingId(updatedBooking.getBookingId());
	                dto.setPatientName(updatedBooking.getName());
	                dto.setAppointmentDate(updatedBooking.getServiceDate());
	                dto.setAppointmentTime(updatedBooking.getServicetime());
	                dto.setAppointmentType(updatedBooking.getVisitType());

	                notificationFeign.sendDoctorPushNotification(dto);
	                log.info("Follow-up notification sent for booking {}", updatedBooking.getBookingId());
	            } catch (Exception ex) {
	                log.error("Failed to send follow-up notification for booking {} : {}", updatedBooking.getBookingId(), ex.getMessage());
	            }
                Map<String,String> map = new LinkedHashMap<>();
                try {
                    boolean slotupdate =
                            clinnicfeign.updateDoctorSlotWhileBooking(
                                    request.getDoctorId(),
                                    request.getBranchId(),
                                    request.getServiceDate(),
                                    request.getServicetime()
                            );
                    if(slotupdate) {
                        response.setMessage("Appointment Booked Successfully and slot blocked and");
                        map.put("slotStatus","200");
                    }else {
                        map.put("slotStatus","500");
                        response.setMessage("Appointment Booked Successfully and slot not blocked and");
                    }}catch(Exception e) {}
                BranchDTO branch = null;
                try {
                    ResponseEntity<ResponseStructure<BranchDTO>> branchResponse =
                            adminServiceClient.getBranchById(updatedBooking.getBranchId());

                    if (branchResponse != null
                            && branchResponse.getBody() != null
                            && branchResponse.getBody().getData() != null) {
                        branch = branchResponse.getBody().getData();
                    }

                } catch (Exception e) {
                    log.error("Branch fetch failed: {}", e.getMessage(), e);
                }
                map.put("Doctor",updatedBooking.getDoctorName());
                map.put("Date",updatedBooking.getServiceDate());
                map.put("Time",updatedBooking.getServicetime());
                map.put("Booking ID",updatedBooking.getBookingId());
                map.put("Branch",updatedBooking.getBranchname());
                map.put("Email",branch.getEmail());

                if(branch.getLocation() != null){
                    map.put("Location",branch.getLocation());}
                else{
                    String locationUrl =
                            "https://www.google.com/maps/search/?api=1&query="
                                    + branch.getLatitude()
                                    + ","
                                    + branch.getLongitude();
                    map.put("Location",locationUrl);
                }
                map.put("mobilenumber",branch.getContactNumber());
                map.put("patientmobilenumber",updatedBooking.getPatientMobileNumber());
                map.put("patientId",updatedBooking.getPatientId());
                map.put("patientname",updatedBooking.getName());
                response.setData(map);
                return ResponseEntity.ok(response);
            }else{
            log.warn("No follow-up bookings found for request with bookingId={}", request.getBookingId());
            response = ResponseStructure.buildResponse(
                    null,
                    "No follow-up bookings found",
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        return ResponseEntity.status(response.getHttpStatus().value()).body(response);

    } catch (Exception e) {
        log.error("Exception occurred while processing addService: {}", e.getMessage(), e);
        response = ResponseStructure.buildResponse(
                null,
                "Internal error: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}


	/**
	 * Clears large or sensitive fields from a Booking entity before returning it in API responses.
	 * This helps reduce payload size and avoids exposing unnecessary data.
	 */
	private void nullifyLargeFields(Booking booking) {
	    if (booking == null) {
	        log.warn("Attempted to nullify fields on a null Booking entity");
	        return;
	    }

	    try {
	        // ✅ Clear heavy collections
	        if (booking.getReports() != null) {
	            booking.setReports(null);
	            log.debug("Reports cleared for bookingId={}", booking.getBookingId());
	        }

	        if (booking.getAttachments() != null) {
	            booking.setAttachments(null);
	            log.debug("Attachments cleared for bookingId={}", booking.getBookingId());
	        }

	        // ✅ Clear large binary/pdf fields
	        if (booking.getConsentFormPdf() != null) {
	            booking.setConsentFormPdf(null);
	            log.debug("ConsentFormPdf cleared for bookingId={}", booking.getBookingId());
	        }

	        if (booking.getPrescriptionPdf() != null) {
	            booking.setPrescriptionPdf(null);
	            log.debug("PrescriptionPdf cleared for bookingId={}", booking.getBookingId());
	        }
	    } catch (Exception e) {
	        log.error("Error nullifying large fields for bookingId={}: {}", booking.getBookingId(), e.getMessage(), e);
	    }
	}


	private Booking toEntity(BookingRequset request) {
	    Booking entity = null;
	    try {
	        entity = mapper.convertValue(request, Booking.class);

	        // ✅ Default values
	        entity.setFollowupStatus("pending");
	        entity.setConsultationType("First-Time");

	        // ✅ Resolve customerId/patientId if missing
	        if ((request.getCustomerId() == null || request.getCustomerId().isEmpty()) ||
	            (request.getPatientId() == null || request.getPatientId().isEmpty())) {
	            try {
	                Map<String, String> res = clinnicfeign.getCustomerByMobilenumberAndName(
	                        request.getMobileNumber(), request.getName());
	                if (request.getCustomerId() == null || request.getCustomerId().isEmpty()) {
	                    entity.setCustomerId(res.get("customerId"));
	                }
	                if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
	                    entity.setPatientId(res.get("patientId"));
	                }
	            } catch (Exception e) {
	                log.warn("Failed to fetch customer/patient info: {}", e.getMessage());
	            }
	        }

	     // ✅ Booking timestamp and due amount calculation
	        ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

	        if (request.getTotalFee() > 0.0) {
	            double partAmt = request.getPartAmount();
	            double due = request.getTotalFee() - partAmt;
	            entity.setDueAmount(due);
	            entity.setBookedAt(istTime.format(formatter));
	        }


	        // ✅ Follow-up status logic
	        entity.setFreeFollowUpsLeft(request.getFreeFollowUps());
	        if (request.getFreeFollowUps() != null && request.getFreeFollowUps() == 0) {
	            entity.setIsFollowupStatus(true);
	        } else {
	            entity.setIsFollowupStatus(false);
	        }

	        try {
	            if (request.getConsultationExpiration() != null) {
	                int days = Integer.parseInt(request.getConsultationExpiration().replaceAll("[^0-9]", ""));
	                LocalDate serviceDate = LocalDate.parse(request.getServiceDate());
	                LocalDate expiryDate = serviceDate.plusDays(days);
	                LocalDate today = LocalDate.now();

	                if (!today.isAfter(expiryDate) && request.getFreeFollowUps() != null && request.getFreeFollowUps() == 0) {
	                    entity.setIsFollowupStatus(true);
	                } else if (today.isAfter(expiryDate)) {
	                    entity.setIsFollowupStatus(true);
	                }
	            }
	        } catch (Exception e) {
	            log.warn("Consultation expiration parsing failed: {}", e.getMessage());
	            entity.setIsFollowupStatus(false);
	        }

	        // ✅ Generate custom booking ID
	        String bookingId = sequenceGeneratorService.generateBookingId(
	                request.getClinicName().substring(0, 3),
	                request.getBranchname().substring(0, 3));
	        entity.setBookingId(bookingId);

	        // ✅ Channel ID logic
	        if (request.getConsultationType() != null &&
	            (request.getConsultationType().equalsIgnoreCase("video consultation") ||
	             request.getConsultationType().equalsIgnoreCase("online consultation"))) {
	            entity.setChannelId(randomNumber());
	        }

	        // ✅ Payment & status logic
	        if (request.getFoc() != null && request.getPaymentType() != null) {
	            if ("paid".equalsIgnoreCase(request.getFoc()) && "not paid".equalsIgnoreCase(request.getPaymentType())) {
	                entity.setStatus("pending");
	            } else if ("foc".equalsIgnoreCase(request.getFoc()) && "not paid".equalsIgnoreCase(request.getPaymentType())) {
	                entity.setStatus("confirmed");
	            } else if ("paid".equalsIgnoreCase(request.getFoc()) && !request.getPaymentType().isEmpty()) {
	                entity.setStatus("confirmed");
	            }
	        }

	        // ✅ Current status tracking
	        List<Status> statusList = new LinkedList<>();
	        Status s = new Status();
	        s.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
	        s.setStatus(entity.getStatus());
	        statusList.add(s);
	        entity.setCurrentStatus(statusList);

	        // ✅ Consultation fee tracking
	        if (request.getConsultationFee() != null && request.getConsultationFee() > 0.0) {
	            ConsultationFees fee = new ConsultationFees();
	            fee.setConsulationFee(request.getConsultationFee());
	            fee.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
	            entity.setListOfConsultationFee(Collections.singletonList(fee));
	        }

	        // ✅ Follow-up bookings initialization
	        if (entity.getFollwupBookings() == null) {
	            FollowupBooking followup = new FollowupBooking();
	            followup.setDoctorId(entity.getDoctorId());
	            followup.setDoctorName(entity.getDoctorName());
	            followup.setServiceDate(entity.getServiceDate());
	            followup.setServicetime(entity.getServicetime());
	            followup.setStatus(entity.getStatus());
	            followup.setVisitType(entity.getVisitType());
	            entity.setFollwupBookings(Collections.singletonList(followup));
	        }

	    } catch (Exception e) {
	        log.error("Error converting BookingRequest to Booking entity: {}", e.getMessage(), e);
	        throw new RuntimeException("Failed to convert request to Booking entity", e);
	    }
	    return entity;
	}


	private BookingResponse toResponse(Booking entity) {
	    BookingResponse response = mapper.convertValue(entity, BookingResponse.class);

	    // ✅ Follow-up status
	    response.setIsFollowupStatus(entity.getIsFollowupStatus());

	    // ✅ Consultation fee check
	    if (entity.getListOfConsultationFee() != null && !entity.getListOfConsultationFee().isEmpty()) {
	        response.setConsultationFee(entity.getListOfConsultationFee().get(0).getConsulationFee());
	    }

	    // ✅ Prescription PDF
	    try {
	        String dto = getPrescriptionpdf(response.getBookingId());
	        if (dto != null) {
	            response.setPrescriptionPdf(Collections.singletonList(dto));
	        }
	    } catch (Exception e) {
	        log.warn("Prescription PDF error for bookingId={}: {}", response.getBookingId(), e.getMessage());
	    }

	    response.setBookingId(String.valueOf(entity.getBookingId()));

	    // ✅ S3 signed URLs
	    try {
	        if (entity.getPartImage() != null && !entity.getPartImage().isEmpty()) {
	            response.setPartImageKey(entity.getPartImage());
	            response.setPartImage(s3Service.generateSignedUrl(entity.getPartImage()));
	        }
	    } catch (Exception e) {
	        log.warn("PartImage URL error for bookingId={}: {}", entity.getBookingId(), e.getMessage());
	    }

	    try {
	        if (entity.getConsentFormPdf() != null && !entity.getConsentFormPdf().isEmpty()) {
	            response.setConsentFormPdf(s3Service.generateSignedUrl(entity.getConsentFormPdf()));
	        }
	    } catch (Exception e) {
	        log.warn("ConsentFormPdf URL error for bookingId={}: {}", entity.getBookingId(), e.getMessage());
	    }

	    try {
	        if (entity.getAttachments() != null && !entity.getAttachments().isEmpty()) {
	            List<String> signedUrls = entity.getAttachments().stream().map(key -> {
	                try {
	                    return s3Service.generateSignedUrl(key);
	                } catch (Exception ex) {
	                    log.warn("Attachment signing failed for key={} bookingId={}", key, entity.getBookingId());
	                    return key;
	                }
	            }).collect(Collectors.toList());
	            response.setAttachments(signedUrls);
	        }
	    } catch (Exception e) {
	        log.warn("Attachments URL error for bookingId={}: {}", entity.getBookingId(), e.getMessage());
	    }

	    // ✅ Reports signing via Clinic Admin
	    try {
	        if (response.getReports() != null) {
	            for (ReportsDtoList reportsDtoList : response.getReports()) {
	                if (reportsDtoList.getReportsList() == null) continue;
	                for (ReportsDTO report : reportsDtoList.getReportsList()) {
	                    if (report.getReportFile() == null || report.getReportFile().isEmpty()) continue;
	                    List<String> signedUrls = report.getReportFile().stream()
	                            .filter(key -> key != null && !key.isBlank())
	                            .map(key -> {
	                                try {
	                                    return clinicAdminFeign.getSignedUrl(key);
	                                } catch (Exception ex) {
	                                    log.warn("Report signing failed for key={} bookingId={}", key, entity.getBookingId());
	                                    return key;
	                                }
	                            }).collect(Collectors.toList());
	                    report.setReportFile(signedUrls);
	                }
	            }
	        }
	    } catch (Exception e) {
	        log.warn("Reports URL signing error for bookingId={}: {}", entity.getBookingId(), e.getMessage());
	    }

	    return response;
	}

	/**
	 * Retrieves the prescription PDF for a given bookingId and generates a signed S3 URL.
	 * Returns null if no prescription is found or if signing fails.
	 */
	private String getPrescriptionpdf(String bookingId) {
	    if (bookingId == null || bookingId.trim().isEmpty()) {
	        log.warn("getPrescriptionpdf called with null/empty bookingId");
	        return null;
	    }

	    try {
	        String res = physioDoctorFeign.getByBookingId(bookingId);

	        if (res != null && !res.isBlank()) {
	            try {
	                String signedUrl = s3Service.generateSignedUrl(res);
	                log.debug("Prescription PDF signed successfully for bookingId={}", bookingId);
	                return signedUrl;
	            } catch (Exception ex) {
	                log.error("Failed to sign prescription PDF for bookingId={} : {}", bookingId, ex.getMessage(), ex);
	                return res; // fallback to raw key if signing fails
	            }
	        } else {
	            log.info("No prescription PDF found for bookingId={}", bookingId);
	            return null;
	        }

	    } catch (Exception e) {
	        log.error("Error fetching prescription PDF for bookingId={} : {}", bookingId, e.getMessage(), e);
	        return null;
	    }
	}


	private static String randomNumber() {
		Random random = new Random();
		int sixDigitNumber = 100000 + random.nextInt(900000);
		return String.valueOf(sixDigitNumber);
	}

	private List<BookingResponse> toResponses(List<Booking> bookings) {
	    if (bookings == null || bookings.isEmpty()) {
	        return new ArrayList<>();
	    }

	    List<BookingResponse> responses;
	    try {
	        responses = new ArrayList<>(mapper.convertValue(bookings, new TypeReference<List<BookingResponse>>() {}));
	    } catch (Exception e) {
	        log.error("Failed to map bookings to BookingResponse: {}", e.getMessage(), e);
	        throw new RuntimeException("Failed to convert bookings list", e);
	    }

	    for (BookingResponse bres : responses) {
	        // ✅ Part Image
	        try {
	            if (bres.getPartImage() != null && !bres.getPartImage().isEmpty()) {
	                bres.setPartImage(s3Service.generateSignedUrl(bres.getPartImage()));
	            }
	        } catch (Exception e) {
	            log.warn("PartImage URL error for bookingId={}: {}", bres.getBookingId(), e.getMessage());
	        }

	        // ✅ Consent Form PDF
	        try {
	            if (bres.getConsentFormPdf() != null && !bres.getConsentFormPdf().isEmpty()) {
	                bres.setConsentFormPdf(s3Service.generateSignedUrl(bres.getConsentFormPdf()));
	            }
	        } catch (Exception e) {
	            log.warn("ConsentFormPdf URL error for bookingId={}: {}", bres.getBookingId(), e.getMessage());
	        }

	        // ✅ Attachments
	        try {
	            if (bres.getAttachments() != null && !bres.getAttachments().isEmpty()) {
	                List<String> signedUrls = bres.getAttachments().stream().map(key -> {
	                    try {
	                        return s3Service.generateSignedUrl(key);
	                    } catch (Exception ex) {
	                        log.warn("Attachment signing failed for key={} bookingId={}", key, bres.getBookingId());
	                        return key;
	                    }
	                }).collect(Collectors.toList());
	                bres.setAttachments(signedUrls);
	            }
	        } catch (Exception e) {
	            log.warn("Attachments URL error for bookingId={}: {}", bres.getBookingId(), e.getMessage());
	        }

	        // ✅ Reports signing via Clinic Admin
	        try {
	            if (bres.getReports() != null) {
	                for (ReportsDtoList reportsDtoList : bres.getReports()) {
	                    if (reportsDtoList.getReportsList() == null) continue;
	                    for (ReportsDTO report : reportsDtoList.getReportsList()) {
	                        if (report.getReportFile() == null || report.getReportFile().isEmpty()) continue;
	                        List<String> signedUrls = report.getReportFile().stream()
	                                .filter(key -> key != null && !key.isBlank())
	                                .map(key -> {
	                                    try {
	                                        return clinicAdminFeign.getSignedUrl(key);
	                                    } catch (Exception ex) {
	                                        log.warn("Report signing failed for key={} bookingId={}", key, bres.getBookingId());
	                                        return key;
	                                    }
	                                }).collect(Collectors.toList());
	                        report.setReportFile(signedUrls);
	                    }
	                }
	            }
	        } catch (Exception e) {
	            log.warn("Reports URL signing error for bookingId={}: {}", bres.getBookingId(), e.getMessage());
	        }

	        // ✅ Prescription PDF
	        try {
	            String dto = getPrescriptionpdf(bres.getBookingId());
	            if (dto != null) {
	                bres.setPrescriptionPdf(Collections.singletonList(dto));
	            }
	        } catch (Exception e) {
	            log.warn("PrescriptionPdf error for bookingId={}: {}", bres.getBookingId(), e.getMessage());
	        }
	    }

	    return responses;
	}



	@Override
	public ResponseEntity<?> physioAppointment(BookingRequset request) {
	    Response res = new Response();

	    try {
	        // =========================
	        // VALIDATIONS
	        // =========================
	        if (request.getFreeFollowUps() == null) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Free FollowUps is mandatory");
	        }
	        if (request.getClinicId() == null || request.getClinicId().trim().isEmpty()) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clinic Id is mandatory");
	        }
	        if (request.getBranchId() == null || request.getBranchId().trim().isEmpty()) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch Id is mandatory");
	        }
	        if (request.getDoctorId() == null || request.getDoctorId().trim().isEmpty()) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor Id is mandatory");
	        }
	        if (request.getServiceDate() == null || request.getServiceDate().trim().isEmpty()) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service Date is mandatory");
	        }
	        if (request.getServicetime() == null || request.getServicetime().trim().isEmpty()) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service Time is mandatory");
	        }
	        if (request.getConsultationExpiration() == null || request.getConsultationExpiration().trim().isEmpty()) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consultation Expiration is mandatory");
	        }
	        boolean hasPatientMobile = request.getPatientMobileNumber() != null && !request.getPatientMobileNumber().trim().isEmpty();
	        boolean hasMobile = request.getMobileNumber() != null && !request.getMobileNumber().trim().isEmpty();
	        if (!hasPatientMobile && !hasMobile) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient Mobile Number or Mobile Number is mandatory");
	        }

	        // =========================
	        // SAVE BOOKING
	        // =========================
	        Booking entity = toEntity(request);
	        Booking updatedBooking = repository.save(entity);
	        if (updatedBooking == null) {
	            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save appointment");
	        }

	        // =========================
	        // Doctor Push Notification
	        // =========================
	        try {
	            DoctorPushNotificationDTO dto = new DoctorPushNotificationDTO();
	            dto.setDoctorId(updatedBooking.getDoctorId());
	            dto.setBookingId(updatedBooking.getBookingId());
	            dto.setPatientName(updatedBooking.getName());
	            dto.setAppointmentDate(updatedBooking.getServiceDate());
	            dto.setAppointmentTime(updatedBooking.getServicetime());
	            dto.setAppointmentType(updatedBooking.getVisitType());

	            notificationFeign.sendDoctorPushNotification(dto);
	            log.info("Doctor push notification sent successfully for booking {}", updatedBooking.getBookingId());
	        } catch (Exception ex) {
	            log.error("Failed to send doctor push notification for booking {} : {}", updatedBooking.getBookingId(), ex.getMessage());
	        }

	        // =========================
	        // Notification Service
	        // =========================
	        int notificationStatus = 0;
	        try {
	            Response notificationResponse = notificationFeign
	                    .createNotification(mapper.convertValue(updatedBooking, BookingResponse.class))
	                    .getBody();
	            if (notificationResponse != null) {
	                notificationStatus = notificationResponse.getStatus();
	            }
	        } catch (Exception e) {
	            log.warn("Notification service failed for booking {} : {}", updatedBooking.getBookingId(), e.getMessage());
	        }

	        // =========================
	        // WhatsApp Notification
	        // =========================
	        try {
	            request.setBookingId(updatedBooking.getBookingId());
	            request.setClinicId(updatedBooking.getClinicId());
	            request.setBranchId(updatedBooking.getBranchId());

	            whatsAppService.sendBookingConfirmation(request);
	            log.info("WhatsApp sent successfully for booking {}", updatedBooking.getBookingId());
	        } catch (Exception e) {
	            log.warn("WhatsApp notification failed for booking {} : {}", updatedBooking.getBookingId(), e.getMessage());
	        }

	        // =========================
	        // SUCCESS RESPONSE
	        // =========================
	        res.setStatus(200);
	        res.setSuccess(true);
            Map<String,String> map = new LinkedHashMap<>();
            try {
                boolean slotupdate =
                        clinnicfeign.updateDoctorSlotWhileBooking(
                                request.getDoctorId(),
                                request.getBranchId(),
                                request.getServiceDate(),
                                request.getServicetime()
                        );
                if(slotupdate) {
                    res.setMessage("Appointment Booked Successfully and slot blocked and");
                    map.put("slotStatus","200");
                }else {
                    map.put("slotStatus","500");
                    res.setMessage("Appointment Booked Successfully and slot not blocked and");
                }
                if (notificationStatus == 200 ) {
                    res.setMessage(res.getMessage()+" notification sent");
                } else {
                    res.setMessage(res.getMessage()+" Notification not sent");
                }}catch(Exception e) {}
            BranchDTO branch = null;
            try {
                ResponseEntity<ResponseStructure<BranchDTO>> response =
                        adminServiceClient.getBranchById(updatedBooking.getBranchId());

                if (response != null
                        && response.getBody() != null
                        && response.getBody().getData() != null) {
                    branch = response.getBody().getData();
                }

            } catch (Exception e) {
                log.error("Branch fetch failed: {}", e.getMessage(), e);
            }
            map.put("Doctor",updatedBooking.getDoctorName());
            map.put("Date",updatedBooking.getServiceDate());
            map.put("Time",updatedBooking.getServicetime());
            map.put("Booking ID",updatedBooking.getBookingId());
            map.put("Branch",updatedBooking.getBranchname());
            map.put("Email",branch.getEmail());
            if(branch.getLocation() != null){
                map.put("Location",branch.getLocation());}
            else{
                String locationUrl =
                        "https://www.google.com/maps/search/?api=1&query="
                                + branch.getLatitude()
                                + ","
                                + branch.getLongitude();
                map.put("Location",locationUrl);
            }
            map.put("mobilenumber",branch.getContactNumber());
            map.put("patientmobilenumber",updatedBooking.getPatientMobileNumber());
            map.put("patientId",updatedBooking.getPatientId());
            map.put("patientname",updatedBooking.getName());
            res.setData(map);


            return ResponseEntity.ok(res);
	    } catch (ResponseStatusException e) {
	        log.error("Validation failed: {}", e.getReason());
	        res.setStatus(e.getStatusCode().value());
	        res.setSuccess(false);
	        res.setMessage(e.getReason());
	        return ResponseEntity.status(e.getStatusCode()).body(res);
	    } catch (Exception e) {
	        log.error("Appointment booking failed : {}", e.getMessage(), e);
	        res.setStatus(500);
	        res.setSuccess(false);
	        res.setMessage("Internal error: " + e.getMessage());
	        return ResponseEntity.status(500).body(res);
	    }
	}


	@Override
	public ResponseEntity<?> getAppointsByPatientId(String patientId) {
	    ResponseStructure<List<Map<String, Object>>> res = new ResponseStructure<>();
	    List<Map<String, Object>> list = new ArrayList<>();

	    try {
	        // ✅ Uses patientId index
	        List<Booking> existingBookings = repository.findByPatientId(patientId);

	        if (existingBookings == null || existingBookings.isEmpty()) {
	            log.warn("No appointments found for patientId={}", patientId);
	            res.setStatusCode(200);
	            res.setMessage("Appointments Are Not Found");
	            res.setData(Collections.emptyList());
	            return ResponseEntity.ok(res);
	        }

	        List<BookingResponse> responses = mapper.convertValue(existingBookings, new TypeReference<List<BookingResponse>>() {});

	        responses.forEach(n -> {
	            Map<String, Object> map = new LinkedHashMap<>();
	            map.put("bookingId", n.getBookingId());
	            map.put("serviceDate", n.getServiceDate());
	            map.put("servicetime", n.getServicetime());
	            map.put("name", n.getName());
	            map.put("mobileNumber",
	                n.getPatientMobileNumber() != null && !n.getPatientMobileNumber().isEmpty()
	                    ? n.getPatientMobileNumber()
	                    : n.getMobileNumber());
	            map.put("doctorId", n.getDoctorId());
	            map.put("doctorName", n.getDoctorName());
	            map.put("paymentType", n.getPaymentType());
	            map.put("visitType", n.getVisitType());
	            map.put("status", n.getStatus());
	            map.put("followupStatus", n.getFollowupStatus());
	            map.put("patientId", n.getPatientId());
	            map.put("clinicId", n.getClinicId());
	            map.put("customerId", n.getCustomerId());
	            map.put("branchId", n.getBranchId());
	            map.put("age", n.getAge());
	            map.put("gender", n.getGender());
	            map.put("branchName", n.getBranchname());
	            map.put("session", n.getSession());
	            map.put("problem", n.getProblem());

	            list.add(map);
	        });

	        res.setStatusCode(200);
	        res.setMessage("Appointments Are Found");
	        res.setData(list);
	        log.info("Found {} appointments for patientId={}", list.size(), patientId);

	        return ResponseEntity.ok(res);

	    } catch (Exception e) {
	        log.error("Error fetching appointments for patientId={}: {}", patientId, e.getMessage(), e);
	        res.setStatusCode(500);
	        res.setMessage("Internal error: " + e.getMessage());
	        res.setData(Collections.emptyList());
	        return ResponseEntity.status(500).body(res);
	    }
	}


	@Override
	public ResponseEntity<?> getAppointsByInput(String input) {
	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

	    try {
	        // ✅ Uses patientId and bookingId indexes; name regex is slower
	        List<Booking> existingBookings = repository.findByNameIgnoreCaseOrBookingIdOrPatientId(input);

	        if (existingBookings == null || existingBookings.isEmpty()) {
	            log.warn("No appointments found for input={}", input);
	            res.setStatusCode(200);
	            res.setMessage("Appointments Are Not Found");
	            res.setData(Collections.emptyList());
	            return ResponseEntity.ok(res);
	        }

	        List<BookingResponse> responses = mapper.convertValue(existingBookings, new TypeReference<List<BookingResponse>>() {});

	        res.setStatusCode(200);
	        res.setMessage("Appointments Are Found");
	        res.setData(responses);
	        log.info("Found {} appointments for input={}", responses.size(), input);

	        return ResponseEntity.ok(res);

	    } catch (Exception e) {
	        log.error("Error fetching appointments for input={}: {}", input, e.getMessage(), e);
	        res.setStatusCode(500);
	        res.setMessage("Internal error: " + e.getMessage());
	        res.setData(Collections.emptyList());
	        return ResponseEntity.status(500).body(res);
	    }
	}

	@Override
	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(String clinicId, String doctorId) {
	    ResponseStructure<List<Map<String, Object>>> res = new ResponseStructure<>();
	    List<Map<String, Object>> list = new ArrayList<>();

	    try {
	        // ✅ Uses compound index (clinicId, doctorId, serviceDate)
	        List<Booking> existingBookings = repository.findByClinicIdAndDoctorId(clinicId, doctorId);

	        if (existingBookings == null || existingBookings.isEmpty()) {
	            log.warn("No appointments found for clinicId={} and doctorId={} on today", clinicId, doctorId);
	            res.setStatusCode(200);
	            res.setMessage("Appointments Are Not Found");
	            res.setData(Collections.emptyList());
	            return ResponseEntity.ok(res);
	        }

	        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	        LocalDate currentDate = LocalDate.now();

	        List<BookingResponse> responseList = new ArrayList<>();

	        for (Booking b : existingBookings) {
	            try {
	                if (b.getServiceDate() != null && b.getStatus() != null) {
	                    LocalDate bookingDate = LocalDate.parse(b.getServiceDate(), dateFormatter);

	                    if (bookingDate.equals(currentDate) &&
	                        (b.getStatus().equalsIgnoreCase("Confirmed") || b.getStatus().equalsIgnoreCase("pending"))) {
	                        BookingResponse temp = toResponse(b);
	                        responseList.add(temp);
	                    }
	                }
	            } catch (Exception e) {
	                log.error("Error parsing serviceDate for bookingId={}: {}", b.getBookingId(), e.getMessage());
	            }
	        }

	        if (responseList.isEmpty()) {
	            log.info("No confirmed/pending appointments found for clinicId={} and doctorId={} today", clinicId, doctorId);
	            res.setStatusCode(200);
	            res.setMessage("Appointments Are Not Found");
	            res.setData(Collections.emptyList());
	            return ResponseEntity.ok(res);
	        }

	        // ✅ Map BookingResponse → simplified map for API response
	        responseList.forEach(n -> {
	            Map<String, Object> map = new LinkedHashMap<>();
	            map.put("bookingId", n.getBookingId());
	            map.put("serviceDate", n.getServiceDate());
	            map.put("servicetime", n.getServicetime());
	            map.put("name", n.getName());
	            map.put("mobileNumber", 
	                n.getPatientMobileNumber() != null && !n.getPatientMobileNumber().isEmpty()
	                    ? n.getPatientMobileNumber()
	                    : n.getMobileNumber());
	            map.put("doctorId", n.getDoctorId());
	            map.put("doctorName", n.getDoctorName());
	            map.put("paymentType", n.getPaymentType());
	            map.put("visitType", n.getVisitType());
	            map.put("status", n.getStatus());
	            map.put("followupStatus", n.getFollowupStatus());
	            map.put("patientId", n.getPatientId());
	            map.put("clinicId", n.getClinicId());
	            map.put("customerId", n.getCustomerId());
	            map.put("branchId", n.getBranchId());
	            map.put("age", n.getAge());
	            map.put("gender", n.getGender());
	            map.put("branchName", n.getBranchname());
	            map.put("problem", n.getProblem());

	            // Extra fields
	            map.put("consultationFee", n.getConsultationFee());
	            map.put("freeFollowUpsLeft", n.getFreeFollowUpsLeft());
	            map.put("freeFollowUps", n.getFreeFollowUps());

	            list.add(map);
	        });

	        res.setStatusCode(200);
	        res.setMessage("Appointments Are Found");
	        res.setData(list);
	        log.info("Found {} appointments for clinicId={} and doctorId={} today", list.size(), clinicId, doctorId);

	        return ResponseEntity.ok(res);

	    } catch (Exception e) {
	        log.error("Error fetching today's appointments for clinicId={} and doctorId={}: {}", clinicId, doctorId, e.getMessage(), e);
	        res.setStatusCode(500);
	        res.setMessage("Internal error: " + e.getMessage());
	        res.setData(Collections.emptyList());
	        return ResponseEntity.status(500).body(res);
	    }
	}


	@Override
	public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(String hospitalId, String doctorId, String number) {

		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		List<BookingResponse> responses = new ArrayList<>();

		try {

			List<Booking> bookings = repository.findByClinicIdAndDoctorId(hospitalId, doctorId);

			if (bookings == null || bookings.isEmpty()) {
				res.setStatusCode(200);
				res.setData(responses);
				res.setMessage("Appointments Are Not Found");
				return ResponseEntity.ok(res);
			}

			LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

			for (Booking booking : bookings) {

				if (booking.getServiceDate() == null) {
					continue;
				}

				LocalDate appointmentDate = LocalDate.parse(booking.getServiceDate());

				boolean add = false;

				switch (number) {

				// Upcoming
				case "1":
					add = "Confirmed".equalsIgnoreCase(booking.getStatus()) && appointmentDate.isAfter(today);
					break;

				// Upcoming Online
				case "2":
					add = "Online Consultation".equalsIgnoreCase(booking.getConsultationType())
							&& "Confirmed".equalsIgnoreCase(booking.getStatus()) && appointmentDate.isAfter(today);
					break;

				// Completed
				case "3":
					add = "Completed".equalsIgnoreCase(booking.getStatus());
					break;

				// In Progress
				case "4":
					add = "In-Progress".equalsIgnoreCase(booking.getStatus());
					break;
				}

				if (add) {
					responses.add(toResponse(booking));
				}
			}

			res.setStatusCode(200);
			res.setData(responses);
			res.setMessage(responses.isEmpty() ? "Appointments Are Not Found" : "Appointments Are Found");

		} catch (Exception e) {

			res.setStatusCode(500);
			res.setData(null);
			res.setMessage(e.getMessage());
		}

		return ResponseEntity.status(res.getStatusCode()).body(res);
	}

	public ResponseEntity<?> getCompletedApntsByDoctorId(String hospitalId, String doctorId) {
		Map<String, Object> m = new LinkedHashMap<>();
		try {
			List<Booking> existingBooking = repository.findByClinicIdAndDoctorId(hospitalId, doctorId);
			List<BookingResponse> res = new ArrayList<>();
			if (existingBooking != null) {
				for (Booking b : existingBooking) {
					if (b.getStatus().equalsIgnoreCase("Completed")) {
						res.add(toResponse(b));
					}
				}
				m.put("completedAppointmentsCount", res.size());
				m.put("status", 200);
				return ResponseEntity.status(200).body(m);
			} else {
				m.put("Message", "No Appointsments Found");
				m.put("status", 200);
				return ResponseEntity.status(200).body(m);
			}
		} catch (Exception e) {
			m.put("Message", e.getMessage());
			m.put("status", 500);
			return ResponseEntity.status(500).body(m);
		}
	}

	public ResponseEntity<?> getSizeOfConsultationTypesByDoctorId(String hospitalId, String doctorId) {
		Map<String, Object> m = new LinkedHashMap<>();
		try {
			List<Booking> existingBooking = repository.findByClinicIdAndDoctorId(hospitalId, doctorId);
			List<BookingResponse> servicesAndConsul = new ArrayList<>();
			List<BookingResponse> inClinic = new ArrayList<>();
			List<BookingResponse> online = new ArrayList<>();
			if (existingBooking != null) {
				for (Booking b : existingBooking) {
					if (b.getStatus().equalsIgnoreCase("Completed")) {
						if (b.getConsultationType().equalsIgnoreCase("Services & Treatments")) {
							servicesAndConsul.add(toResponse(b));
						}
						if (b.getConsultationType().equalsIgnoreCase("In-Clinic Consultation")) {
							inClinic.add(toResponse(b));
						}
						if (b.getConsultationType().equalsIgnoreCase("Online Consultation")) {
							online.add(toResponse(b));
						}
					}
				}
				m.put("services & Treatments", servicesAndConsul.size());
				m.put("in-Clinic Consultation", inClinic.size());
				m.put("online Consultation", online.size());
				m.put("status", 200);
				return ResponseEntity.status(200).body(m);
			} else {
				m.put("Message", "No Appointsments Found");
				m.put("status", 200);
				return ResponseEntity.status(200).body(m);
			}
		} catch (Exception e) {
			m.put("Message", e.getMessage());
			m.put("status", 500);
			return ResponseEntity.status(500).body(m);
		}
	}

	public BookingResponse getBookedService(String bookingId) {
		try {
			Booking entity = repository.findByBookingId(bookingId).get();
			if (entity != null) {
				BookingResponse res = toResponse(entity);
				List<Session> lst = new ArrayList<>();
				try {
                   /// if(entity.getStatus() != null || !entity.getStatus().isEmpty()){
					lst = physioDoctorFeign.getPhysioByBookingId(res.getBookingId(), res.getServiceDate()).getBody();
                   ///// lst = lst.stream().filter(n->n.getSlot() != null).toList();
					res.setSession(lst);
				} catch (Exception e) {
				}
				return res;
			} else {
				return null;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
	}

	public void deleteBookedServiceReports(String bookingId, String index) {
		try {
			Booking entity = repository.findByBookingId(bookingId).get();
			if (entity != null && index.equalsIgnoreCase("null")) {
				try {
					entity.getReports().clear();
					repository.save(entity);
				} catch (Exception e) {
				}
			} else {
				if (entity != null && index != null) {
					entity.getReports().remove(Integer.valueOf(index).intValue());
					repository.save(entity);
				}
			}
		} catch (Exception e) {
		}
	}

	@Override
	public BookingResponse deleteService(String id) {
		Booking entity = repository.findByBookingId(id)
				.orElseThrow(() -> new RuntimeException("Invalid Booking Id Please provide Valid Id"));
		repository.deleteById(id);
		return toResponse(entity);
	}

	@Override
	public List<BookingResponse> getBookedServices(String mobileNumber) {
		List<Booking> bookings = repository.findByMobileNumber(mobileNumber);
		List<Booking> reversedBookings = new ArrayList<>();
		for (int i = bookings.size() - 1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if (bookings == null || bookings.isEmpty()) {
			return null;
		}
		return toResponses(reversedBookings);
	}

	// ✅ CHANGE 3: previously repository.findAll() with no bound — a clinic with
	// heavy booking volume could pull the entire collection (including nested
	// reports/attachments) into heap on a single request. Bounded with a
	// PageRequest so worst-case memory per call is capped. If callers need true
	// pagination (page/size params from the client), the interface method and
	// controller should be updated to accept a Pageable — flag this if you want
	// that follow-up change too.
	@Override
	public List<BookingResponse> getAllBookedServices() {
		List<Booking> bookings = repository.findAll(PageRequest.of(0, MAX_UNPAGED_RESULTS)).getContent();
		List<Booking> reversedBookings = new ArrayList<>();
		for (int i = bookings.size() - 1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if (bookings == null || bookings.isEmpty()) {
			return null;
		}
		return toResponses(reversedBookings);
	}

	@Override
	public List<BookingResponse> bookingByDoctorId(String doctorId) {
		List<Booking> bookings = repository.findByDoctorId(doctorId);
		List<Booking> reversedBookings = new ArrayList<>();
		for (int i = bookings.size() - 1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if (bookings == null || bookings.isEmpty()) {
			return null;
		}
		return toResponses(reversedBookings);
	}

	@Override
	public List<Map<String, Object>> bookingByCustomerId(String customerId) {

		List<Booking> bookings = repository.findByCustomerId(customerId);

		if (bookings == null || bookings.isEmpty()) {
			return Collections.emptyList();
		}

		bookings = bookings.stream().filter(booking -> !"COMPLETED".equalsIgnoreCase(booking.getStatus())).toList();

		List<BookingResponse> reversedBookings = toResponses(bookings);

		List<Map<String, Object>> list = new ArrayList<>();

		reversedBookings.forEach(n -> {
			Map<String, Object> map = new LinkedHashMap<>();

			map.put("bookingId", n.getBookingId());
			map.put("serviceDate", n.getServiceDate());
			map.put("servicetime", n.getServicetime());
			map.put("name", n.getName());
			map.put("mobileNumber",
					n.getPatientMobileNumber() != null && !n.getPatientMobileNumber().isEmpty()
							? n.getPatientMobileNumber()
							: n.getMobileNumber());
			map.put("doctorId", n.getDoctorId());
			map.put("doctorName", n.getDoctorName());
			map.put("paymentType", n.getPaymentType());
			map.put("visitType", n.getVisitType());
			map.put("status", n.getStatus());
			map.put("followupStatus", n.getFollowupStatus());
			map.put("patientId", n.getPatientId());
			map.put("clinicId", n.getClinicId());
			map.put("customerId", n.getCustomerId());
			map.put("branchId", n.getBranchId());
			map.put("age", n.getAge());
			map.put("gender", n.getGender());
			map.put("branchName", n.getBranchname());
			map.put("problem", n.getProblem());

			list.add(map);
		});

		return list;
	}

	@Override
	public List<Map<String, Object>> CompletedbookingByCustomerId(String customerId) {

		List<Booking> bookings = repository.findByCustomerId(customerId);

		if (bookings == null || bookings.isEmpty()) {
			return Collections.emptyList();
		}

		bookings = bookings.stream().filter(booking -> "COMPLETED".equalsIgnoreCase(booking.getStatus())).toList();

		List<BookingResponse> reversedBookings = toResponses(bookings);

		List<Map<String, Object>> list = new ArrayList<>();

		reversedBookings.forEach(n -> {
			Map<String, Object> map = new LinkedHashMap<>();

			map.put("bookingId", n.getBookingId());
			map.put("serviceDate", n.getServiceDate());
			map.put("servicetime", n.getServicetime());
			map.put("name", n.getName());
			map.put("mobileNumber",
					n.getPatientMobileNumber() != null && !n.getPatientMobileNumber().isEmpty()
							? n.getPatientMobileNumber()
							: n.getMobileNumber());
			map.put("doctorId", n.getDoctorId());
			map.put("doctorName", n.getDoctorName());
			map.put("paymentType", n.getPaymentType());
			map.put("visitType", n.getVisitType());
			map.put("status", n.getStatus());
			map.put("followupStatus", n.getFollowupStatus());
			map.put("patientId", n.getPatientId());
			map.put("clinicId", n.getClinicId());
			map.put("customerId", n.getCustomerId());
			map.put("branchId", n.getBranchId());
			map.put("age", n.getAge());
			map.put("gender", n.getGender());
			map.put("branchName", n.getBranchname());
			map.put("problem", n.getProblem());

			list.add(map);
		});

		return list;
	}

	@Override
	public List<BookingResponse> bookingByPatientId(String patientId) {
	    List<Booking> bookings = repository.findByPatientId(patientId);

	    if (bookings == null || bookings.isEmpty()) {
	        log.info("No bookings found for patientId={}", patientId);
	        return Collections.emptyList();
	    }

	    List<Booking> reversedBookings = new ArrayList<>(bookings);
	    Collections.reverse(reversedBookings);

	    return toResponses(reversedBookings);
	}

	@Override
	public List<BookingResponse> bookingByPatientIdAndBookingId(String patientId, String bookingId) {
	    List<Booking> bookings = repository.findByPatientIdAndBookingId(patientId, bookingId);

	    if (bookings == null || bookings.isEmpty()) {
	        log.info("No bookings found for patientId={} and bookingId={}", patientId, bookingId);
	        return Collections.emptyList();
	    }

	    List<Booking> reversedBookings = new ArrayList<>();
	    for (int i = bookings.size() - 1; i >= 0; i--) {
	        if ("In-Progress".equalsIgnoreCase(bookings.get(i).getStatus())) {
	            reversedBookings.add(bookings.get(i));
	        }
	    }

	    return toResponses(reversedBookings);
	}


	@Override
	public List<ReportsDTO> getReportsByPatientId(String patientId) {
	    if (patientId == null || patientId.trim().isEmpty()) {
	        log.warn("getReportsByPatientId called with null/empty patientId");
	        return Collections.emptyList();
	    }

	    List<Booking> bookings = repository.findByPatientId(patientId);
	    if (bookings == null || bookings.isEmpty()) {
	        log.info("No bookings found for patientId={}", patientId);
	        return Collections.emptyList();
	    }

	    List<ReportsDTO> responseList = new ArrayList<>();
	    for (Booking booking : bookings) {
	        if (booking.getReports() == null || booking.getReports().isEmpty()) {
	            continue;
	        }
	        for (ReportsList reportList : booking.getReports()) {
	            if (reportList.getReportsList() == null || reportList.getReportsList().isEmpty()) {
	                continue;
	            }
	            for (Reports reportEntity : reportList.getReportsList()) {
	                try {
	                    ReportsDTO dto = mapper.convertValue(reportEntity, ReportsDTO.class);
	                    responseList.add(dto);
	                } catch (Exception e) {
	                    log.warn("Failed to convert reportEntity for bookingId={} : {}", 
	                             booking.getBookingId(), e.getMessage());
	                }
	            }
	        }
	    }

	    return responseList;
	}


	private boolean isValidMobileNumber(String input) {
		if (input == null) {
			return false;
		}
		String regex = "^[6-9]\\d{9}$";
		return input.matches(regex);
	}

	@Override
	public List<BookingResponse> bookingByClinicId(String clinicId) {
		List<Booking> bookings = repository.findByClinicId(clinicId);
		List<Booking> reversedBookings = new ArrayList<>();
		for (int i = bookings.size() - 1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if (bookings == null || bookings.isEmpty()) {
			return null;
		}
		return toResponses(bookings);
	}

	// ✅ CHANGE 2: rewritten to avoid findAll() + nested N+1 query/save loops.
	// The previous version pulled the entire booking collection into heap every
	// hour, then re-queried and re-saved patients inside a triple-nested loop —
	// this was the single biggest recurring memory spike in the service. The
	// counting now happens inside MongoDB via aggregation, and the JVM only
	// ever holds the small (patientId -> count) result set, followed by a
	// single bulk updateMulti per patient.
	@Scheduled(fixedRate = 60 * 60 * 1000)
	public void autoCalculatePatientCompletedAppointments() {
		try {
			Aggregation agg = Aggregation.newAggregation(
					Aggregation.match(Criteria.where("status").regex("^completed$", "i")),
					Aggregation.group("patientId").count().as("visitCount")
			);

			AggregationResults<Map> results = mongoTemplate.aggregate(agg, "booking", Map.class);

			for (Map<String, Object> r : results.getMappedResults()) {
				Object patientId = r.get("_id");
				Object visitCount = r.get("visitCount");

				if (patientId == null || visitCount == null) {
					continue;
				}

				mongoTemplate.updateMulti(
						Query.query(Criteria.where("patientId").is(patientId)),
						Update.update("visitCount", visitCount),
						Booking.class
				);
			}

			log.info("autoCalculatePatientCompletedAppointments: updated visitCount for {} patients",
					results.getMappedResults().size());

		} catch (Exception e) {
			log.error("autoCalculatePatientCompletedAppointments failed: {}", e.getMessage(), e);
		}
	}

	// ---------------------------to get patientdetails by
	// bookingId,pateintId,mobileNumber---------------------------
	@Override
	public Response getPatientDetailsForConsetForm(String bookingId, String patientId, String mobileNumber) {
		try {
			Optional<Booking> optionalBooking = repository.findByBookingIdAndPatientIdAndMobileNumber(bookingId,
					patientId, mobileNumber);
			if (optionalBooking.isPresent()) {
				Booking booking = optionalBooking.get();
				if (booking.getStatus().equalsIgnoreCase("Confirmed")
						|| booking.getStatus().equalsIgnoreCase("Completed")) {
					BookingResponse response = mapper.convertValue(booking, BookingResponse.class);
					return Response.builder().success(true).status(200).message("Booking details fetched successfully.")
							.data(response).build();
				} else {
					return Response.builder().success(false).status(404)
							.message("No booking found with the given details.").build();
				}
			} else {
				return Response.builder().success(false).status(404).message("No booking found with the given details.")
						.build();
			}
		} catch (Exception e) {
			return Response.builder().success(false).status(500).message(e.getMessage()).build();
		}
	}

	public ResponseEntity<?> getInProgressAppointments(String number) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
		try {
			List<Booking> booked = repository.findByMobileNumber(number);
			List<BookingResponse> response = new ArrayList<>();
			if (booked != null && !booked.isEmpty()) {
				for (Booking b : booked) {
					if (b.getStatus().equalsIgnoreCase("In-Progress")) {
						response.add(toResponse(b));
					}
				}
				if (response != null && !response.isEmpty()) {
					res.setStatusCode(200);
					res.setHttpStatus(HttpStatus.OK);
					res.setData(response);
					res.setMessage("In-Progress appointments found");
				} else {
					res.setStatusCode(200);
					res.setHttpStatus(HttpStatus.OK);
					res.setData(response);
					res.setMessage("In-Progress appointments not found");
				}
			}
		} catch (Exception e) {
			res.setStatusCode(500);
			res.setMessage(e.getMessage());
		}
		return ResponseEntity.status(res.getStatusCode()).body(res);
	}

	@Override
	public ResponseEntity<?> getInProgressAppointmentsByCustomerId(String customerId) {

		try {

			List<Booking> bookings = repository.findByCustomerIdAndStatusIgnoreCase(customerId, "In-Progress");

			if (bookings == null || bookings.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseStructure.buildResponse(null,
						"No in-progress bookings found", HttpStatus.NOT_FOUND, 404));
			}

			List<BookingResponse> bookingResponses = bookings.stream().peek(this::nullifyLargeFields).map(booking -> {
				BookingResponse response = mapper.convertValue(booking, BookingResponse.class);

				String pdf = getPrescriptionpdf(response.getBookingId());

				if (pdf != null) {
					response.setPrescriptionPdf(Collections.singletonList(pdf));
				}

				return response;
			}).toList();

			return ResponseEntity.ok(ResponseStructure.buildResponse(bookingResponses, "In-Progress appointments found",
					HttpStatus.OK, HttpStatus.OK.value()));

		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseStructure.buildResponse(null,
					"Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, 500));
		}
	}

	@Override
	public ResponseEntity<?> getInProgressAppointmentsByPatientId(String patientId, String clinicId) {

		try {

			List<Booking> bookings = repository.findByPatientIdAndClinicId(patientId, clinicId);

			if (bookings == null || bookings.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseStructure.buildResponse(null,
						"No bookings found for this patient", HttpStatus.NOT_FOUND, 404));
			}

			List<BookingResponse> bookingResponses = bookings.stream()
					.filter(booking -> "In-Progress".equalsIgnoreCase(booking.getStatus()))
					.peek(this::nullifyLargeFields).map(booking -> {

						BookingResponse response = mapper.convertValue(booking, BookingResponse.class);

						String pdf = getPrescriptionpdf(booking.getBookingId());

						if (pdf != null) {
							response.setPrescriptionPdf(Collections.singletonList(pdf));
						}

						return response;
					}).toList();

			if (bookingResponses.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseStructure.buildResponse(null,
						"No In-Progress appointments found for this patient", HttpStatus.NOT_FOUND, 404));
			}

			return ResponseEntity.ok(ResponseStructure.buildResponse(bookingResponses, "In-Progress appointments found",
					HttpStatus.OK, HttpStatus.OK.value()));

		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseStructure.buildResponse(null,
					"Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, 500));
		}
	}

	/**
	 * ✅ Utility: Parse both yyyy-MM-dd and dd-MM-yyyy formats
	 */
	private LocalDate parseDate(String dateStr) {
		if (dateStr == null)
			return null;
		List<DateTimeFormatter> formatters = Arrays.asList(DateTimeFormatter.ofPattern("yyyy-MM-dd"),
				DateTimeFormatter.ofPattern("dd-MM-yyyy"));
		for (DateTimeFormatter fmt : formatters) {
			try {
				return LocalDate.parse(dateStr, fmt);
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	public List<BookingResponse> inprogressAppointmentsByConsultationExpiration(LocalDate exp, Booking booking,
			DoctorSaveDetailsDTO saveDetails) {
		List<BookingResponse> finalList = new ArrayList<>();
		try {
			LocalDate today = LocalDate.now();
			LocalDate sixthDate = today.plusDays(6);
			DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			if (saveDetails.getFollowUp() != null && saveDetails.getFollowUp().getNextFollowUpDate() != null) {
				try {
					int days = 0;
					if (exp == null) {
						days = Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", ""));
						LocalDate serviceDate = LocalDate.parse(booking.getServiceDate(), isoFormatter);
						exp = serviceDate.plusDays(days);
					}
					LocalDate followDate = LocalDate.parse(saveDetails.getFollowUp().getNextFollowUpDate(),
							DateTimeFormatter.ISO_LOCAL_DATE_TIME);
					if (!followDate.isBefore(today) && !followDate.isAfter(sixthDate) && !followDate.isAfter(exp)) {
						Booking bkng = new Booking(booking);
						bkng.setFollowupDate(followDate.format(isoFormatter));
						bkng.setStatus("In-Progress");
						finalList.add(toResponse(bkng));
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			} else {
				// ✅ Consultation expiration fallback
				if (booking.getConsultationExpiration() != null) {
					try {
						int days = 0;
						if (exp == null) {
							days = Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", ""));
							LocalDate serviceDate = LocalDate.parse(booking.getServiceDate(), isoFormatter);
							exp = serviceDate.plusDays(days);
						}
						for (int i = 0; i <= 6; i++) {
							LocalDate date = today.plusDays(i);
							if ((!date.isAfter(sixthDate)) && (date.isBefore(exp) || date.equals(exp))) {
								Booking bkng = new Booking(booking);
								bkng.setFollowupDate(date.format(isoFormatter));
								bkng.setStatus("In-Progress");
								finalList.add(toResponse(bkng));
							}
						}
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
		return finalList;
	}

	public ResponseEntity<?> getDoctorFutureAppointments(String doctorId) {
		ResponseStructure<List<Map<String, Object>>> res = new ResponseStructure<List<Map<String, Object>>>();
		List<Map<String, Object>> list = new ArrayList<>();
		try {
			List<Booking> booked = repository.findByDoctorId(doctorId);
			List<BookingResponse> response = new ArrayList<>();
			if (booked != null && !booked.isEmpty()) {
				for (Booking b : booked) {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					LocalDate serviceDate = LocalDate.parse(b.getServiceDate(), formatter);
					LocalDate currentDate = LocalDate.now();
					LocalDate plus = currentDate.plusDays(15);
					if (!serviceDate.isBefore(currentDate) && !serviceDate.isAfter(plus)) {
						response.add(toResponse(b));
					}
				}
				response.stream().map(n -> {
					Map<String, Object> map = new LinkedHashMap<>();
					map.put("bookingId", n.getBookingId());
					map.put("serviceDate", n.getServiceDate());
					map.put("servicetime", n.getServicetime());
					map.put("name", n.getName());
					map.put("mobileNumber",
							!n.getPatientMobileNumber().isEmpty() ? n.getPatientMobileNumber() : n.getMobileNumber());
					map.put("doctorId", n.getDoctorId());
					map.put("doctorName", n.getDoctorName());
					map.put("paymentType", n.getPaymentType());
					map.put("visitType", n.getVisitType());
					map.put("status", n.getStatus());
					map.put("followupStatus", n.getFollowupStatus());
					map.put("patientId", n.getPatientId());
					map.put("clinicId", n.getClinicId());
					map.put("customerId", n.getCustomerId());
					map.put("branchId", n.getBranchId());
					map.put("age", n.getAge());
					map.put("gender", n.getGender());
					map.put("branchName", n.getBranchname());
					map.put("problem", n.getProblem());
					list.add(map);
					return n;
				}).toList();
				if (response != null && !response.isEmpty()) {
					res.setStatusCode(200);
					res.setHttpStatus(HttpStatus.OK);
					res.setData(list);
					res.setMessage("appointments found");
				} else {
					res.setStatusCode(200);
					res.setHttpStatus(HttpStatus.OK);
					res.setMessage("appointments not found");
				}
			}
		} catch (Exception e) {
			res.setStatusCode(500);
			res.setMessage(e.getMessage());
		}
		return ResponseEntity.status(res.getStatusCode()).body(res);
	}

	@Override
	public List<BookingResponse> bookingByBranchId(String branchId) {
		List<Booking> bookings = repository.findByBranchId(branchId);
		List<Booking> reversedBookings = new ArrayList<>();
		for (int i = bookings.size() - 1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if (bookings == null || bookings.isEmpty()) {
			return null;
		}
		return toResponses(reversedBookings);
	}

	@Override
	public List<Map<String, Object>> getBookedServicesByClinicIdWithBranchId(String clinicId, String branchId) {
	    List<Booking> bookings = repository.findByClinicIdAndBranchId(clinicId, branchId);

	    if (bookings == null || bookings.isEmpty()) {
	        return new ArrayList<>();
	    }

	    List<Booking> reversedBookings = new ArrayList<>();
	    for (int i = bookings.size() - 1; i >= 0; i--) {
	        reversedBookings.add(bookings.get(i));
	    }

	    List<BookingResponse> rev = toResponses(reversedBookings);

	    List<Map<String, Object>> list = new ArrayList<>();
	    rev.forEach(n -> {
	        Map<String, Object> map = new LinkedHashMap<>();
	        map.put("bookingId", n.getBookingId());
	        map.put("serviceDate", n.getServiceDate());
	        map.put("servicetime", n.getServicetime());
	        map.put("name", n.getName());
	        map.put("mobileNumber",
	                (n.getPatientMobileNumber() != null && !n.getPatientMobileNumber().isEmpty())
	                        ? n.getPatientMobileNumber()
	                        : n.getMobileNumber());
	        map.put("doctorId", n.getDoctorId());
	        map.put("doctorName", n.getDoctorName());
	        map.put("paymentType", n.getPaymentType());
	        map.put("visitType", n.getVisitType());
	        map.put("status", n.getStatus());
	        map.put("followupStatus", n.getFollowupStatus());
	        map.put("patientId", n.getPatientId());
	        map.put("clinicId", n.getClinicId());
	        map.put("customerId", n.getCustomerId());
	        map.put("branchId", n.getBranchId());
	        map.put("age", n.getAge());
	        map.put("gender", n.getGender());
	        map.put("branchName", n.getBranchname());
	        map.put("problem", n.getProblem());
	        map.put("session", n.getSession());
	        map.put("referredDoctorId", n.getReferredDoctorId());
	        map.put("referredByType", n.getReferredByType());
	        map.put("referredByName", n.getReferredByName());
	        map.put("status", n.getStatus());
	        map.put("followupStatus", n.getFollowupStatus());
	        map.put("doctorRefCode", n.getDoctorRefCode());
	        map.put("consultationFee", n.getConsultationFee());
	       
	        map.put("totalFee", n.getTotalFee());
	      
	        list.add(map);
	    });

	    return list;
	}

	@Override
	public ResponseEntity<?> getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus(String clinicId,
			String branchId, String doctorId, String status) {
		try {
			List<Map<String, Object>> list = new ArrayList<>();
			List<BookingResponse> reversedBookings = new ArrayList<>();
			LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
			if (!branchId.equalsIgnoreCase("all")) {
				if (status.equalsIgnoreCase("pending")) {
					String requiredStatus = "confirmed";
					List<Booking> bookings = repository.findByClinicIdAndBranchIdAndDoctorIdAndStatusIgnoreCase(
							clinicId, branchId, doctorId, requiredStatus);
					reversedBookings = toResponses(bookings);
					reversedBookings = reversedBookings.stream().filter(b -> {
						LocalDate bookingDate = LocalDate.parse(b.getServiceDate());
						return bookingDate.isBefore(currentDate);
					}).toList();
				} else if (status.equalsIgnoreCase("confirmed")) {
					List<Booking> bookings = repository.findByClinicIdAndBranchIdAndDoctorIdAndStatusIgnoreCase(
							clinicId, branchId, doctorId, status);

					reversedBookings = toResponses(bookings);

					reversedBookings = reversedBookings.stream().filter(b -> {
						LocalDate bookingDate = LocalDate.parse(b.getServiceDate());
						return bookingDate.isAfter(currentDate);
					}).toList();
				} else {
					List<Booking> bookings = repository.findByClinicIdAndBranchIdAndDoctorIdAndStatusIgnoreCase(
							clinicId, branchId, doctorId, status);
					if (!bookings.isEmpty()) {
						reversedBookings = toResponses(bookings);
					} else {
						List<Booking> bkings = repository
								.findByClinicIdAndBranchIdAndDoctorIdAndFollowupStatusIgnoreCase(clinicId, branchId,
										doctorId, status);
						reversedBookings = toResponses(bkings);
					}
				}
				if (reversedBookings != null && !reversedBookings.isEmpty()) {

					reversedBookings.stream().map(n -> {

						Map<String, Object> map = new LinkedHashMap<>();

						map.put("bookingId", n.getBookingId());
						map.put("serviceDate", n.getServiceDate());
						map.put("servicetime", n.getServicetime());
						map.put("name", n.getName());

						map.put("mobileNumber", !n.getPatientMobileNumber().isEmpty() ? n.getPatientMobileNumber()
								: n.getMobileNumber());

						map.put("doctorId", n.getDoctorId());
						map.put("doctorName", n.getDoctorName());
						map.put("paymentType", n.getPaymentType());
						map.put("visitType", n.getVisitType());
						map.put("status", n.getStatus());
						map.put("followupStatus", n.getFollowupStatus());
						map.put("patientId", n.getPatientId());
						map.put("clinicId", n.getClinicId());
						map.put("customerId", n.getCustomerId());
						map.put("branchId", n.getBranchId());
						map.put("age", n.getAge());
						map.put("gender", n.getGender());
						map.put("branchName", n.getBranchname());
						map.put("problem", n.getProblem());
						map.put("session", n.getSession());

						list.add(map);

						return n;

					}).toList();
				}
			} else {
				if (status.equalsIgnoreCase("pending")) {
					String requiredStatus = "confirmed";
					List<Booking> bookings = repository.findByClinicIdAndDoctorIdAndStatusIgnoreCase(clinicId, doctorId,
							requiredStatus);
					reversedBookings = toResponses(bookings);
					reversedBookings = reversedBookings.stream().filter(b -> {
						LocalDate bookingDate = LocalDate.parse(b.getServiceDate());
						return bookingDate.isBefore(currentDate);
					}).toList();
				} else if (status.equalsIgnoreCase("confirmed")) {
					List<Booking> bookings = repository.findByClinicIdAndDoctorIdAndStatusIgnoreCase(clinicId, doctorId,
							status);

					reversedBookings = toResponses(bookings);

					reversedBookings = reversedBookings.stream().filter(b -> {
						LocalDate bookingDate = LocalDate.parse(b.getServiceDate());
						return bookingDate.isAfter(currentDate);
					}).toList();
				} else {
					List<Booking> bookings = repository.findByClinicIdAndDoctorIdAndStatusIgnoreCase(clinicId, doctorId,
							status);
					if (!bookings.isEmpty()) {
						reversedBookings = toResponses(bookings);
					} else {
						List<Booking> bkings = repository
								.findByClinicIdAndBranchIdAndDoctorIdAndFollowupStatusIgnoreCase(clinicId, branchId,
										doctorId, status);
						reversedBookings = toResponses(bkings);
					}
				}
				if (reversedBookings != null && !reversedBookings.isEmpty()) {

					reversedBookings.stream().map(n -> {

						Map<String, Object> map = new LinkedHashMap<>();

						map.put("bookingId", n.getBookingId());
						map.put("serviceDate", n.getServiceDate());
						map.put("servicetime", n.getServicetime());
						map.put("name", n.getName());

						map.put("mobileNumber", !n.getPatientMobileNumber().isEmpty() ? n.getPatientMobileNumber()
								: n.getMobileNumber());

						map.put("doctorId", n.getDoctorId());
						map.put("doctorName", n.getDoctorName());
						map.put("paymentType", n.getPaymentType());
						map.put("visitType", n.getVisitType());
						map.put("status", n.getStatus());
						map.put("followupStatus", n.getFollowupStatus());
						map.put("patientId", n.getPatientId());
						map.put("clinicId", n.getClinicId());
						map.put("customerId", n.getCustomerId());
						map.put("branchId", n.getBranchId());
						map.put("age", n.getAge());
						map.put("gender", n.getGender());
						map.put("branchName", n.getBranchname());
						map.put("problem", n.getProblem());
						map.put("session", n.getSession());

						list.add(map);

						return n;

					}).toList();
				}
			}
			if (!list.isEmpty()) {
				return ResponseEntity.status(HttpStatus.OK)
						.body(new Response(true, list, null, "appointments are found", 200, null, null));
			} else {
				return ResponseEntity.status(HttpStatus.OK)
						.body(new Response(true, null, null, "appointments are not found", 200, null, null));
			}
		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new Response(false, null, null, e.getMessage(), 500, null, null));
		}
	}

	@Override
	public ResponseEntity<?> retrieveOneWeekAppointments(String clinicId, String branchId) {

		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		List<BookingResponse> finalList = new ArrayList<>();

		try {

			List<Booking> bookings = repository.findByClinicIdAndBranchId(clinicId, branchId);

			LocalDate today = LocalDate.now();
			LocalDate weekEndDate = today.plusDays(6);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

			for (Booking booking : bookings) {

				String status = booking.getStatus();

				if (!"Confirmed".equalsIgnoreCase(status) && !"In-Progress".equalsIgnoreCase(status)) {
					continue;
				}

				if (booking.getServiceDate() == null) {
					continue;
				}

				LocalDate serviceDate = LocalDate.parse(booking.getServiceDate(), formatter);

				if (!serviceDate.isBefore(today) && !serviceDate.isAfter(weekEndDate)) {

					finalList.add(toResponse(booking));
				}
			}

			res.setStatusCode(200);
			res.setHttpStatus(HttpStatus.OK);
			res.setMessage("Weekly appointments retrieved successfully");
			res.setData(finalList);

		} catch (Exception e) {

			res.setStatusCode(500);
			res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			res.setMessage("Error: " + e.getMessage());
			res.setData(Collections.emptyList());
		}

		return ResponseEntity.status(res.getStatusCode()).body(res);
	}

	public ResponseEntity<?> retrieveAppointments(String cinicId, String branchId, String date) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
		try {
			List<Booking> bookings = repository.findByClinicIdAndBranchIdAndServiceDateOrderByServicetimeAsc(cinicId,
					branchId, date);
			bookings = bookings.stream().filter(n -> n.getStatus().equalsIgnoreCase("In-Progress")).toList();
			List<BookingResponse> todayBookingsDto = toResponses(bookings);
			if (todayBookingsDto != null && !todayBookingsDto.isEmpty()) {
				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setData(todayBookingsDto);
				res.setMessage("appointments found");
			} else {
				res.setStatusCode(404);
				res.setHttpStatus(HttpStatus.NOT_FOUND);
				res.setMessage("appointments Not found with date");
			}
		} catch (Exception e) {
			res.setStatusCode(500);
			res.setMessage(e.getMessage());
		}
		return ResponseEntity.status(res.getStatusCode()).body(res);
	}

	public ResponseEntity<ResponseStructure<BookingResponse>> updateAppointmentBasedOnBookingId(BookingResponse dto) {

		Booking updated = null;

		try {

			Booking entity = repository.findByBookingId(dto.getBookingId())
					.orElseThrow(() -> new RuntimeException("Invalid Booking Id"));

			// -------- BASIC --------

			if (dto.getBookingFor() != null && !dto.getBookingFor().isEmpty())
				entity.setBookingFor(dto.getBookingFor());

			if (dto.getName() != null && !dto.getName().isEmpty())
				entity.setName(dto.getName());

			if (dto.getReports() != null && !dto.getReports().isEmpty()) {
				entity.setReports(mapper.convertValue(dto.getReports(), new TypeReference<List<ReportsList>>() {
				}));
			}

			if (dto.getPatientMobileNumber() != null && !dto.getPatientMobileNumber().isEmpty())
				entity.setPatientMobileNumber(dto.getPatientMobileNumber());

			if (dto.getPatientId() != null && !dto.getPatientId().isEmpty())
				entity.setPatientId(dto.getPatientId());

			if (dto.getVisitType() != null && !dto.getVisitType().isEmpty())
				entity.setVisitType(dto.getVisitType());

			if (dto.getPatientAddress() != null && !dto.getPatientAddress().isEmpty())
				entity.setPatientAddress(dto.getPatientAddress());

			if (dto.getAge() != null && !dto.getAge().isEmpty())
				entity.setAge(dto.getAge());

			if (dto.getGender() != null && !dto.getGender().isEmpty())
				entity.setGender(dto.getGender());

			if (dto.getMobileNumber() != null && !dto.getMobileNumber().isEmpty())
				entity.setMobileNumber(dto.getMobileNumber());

			if (dto.getCustomerId() != null && !dto.getCustomerId().isEmpty())
				entity.setCustomerId(dto.getCustomerId());

			if (dto.getCustomerDeviceId() != null && !dto.getCustomerDeviceId().isEmpty())
				entity.setCustomerDeviceId(dto.getCustomerDeviceId());

			// -------- FOLLOWUPS --------

			if (dto.getFreeFollowUpsLeft() != null)
				entity.setFreeFollowUpsLeft(dto.getFreeFollowUpsLeft());

			if (dto.getFreeFollowUps() != null)
				entity.setFreeFollowUps(dto.getFreeFollowUps());

			if (dto.getFollowupDate() != null && !dto.getFollowupDate().isEmpty())
				entity.setFollowupDate(dto.getFollowupDate());

			if (dto.getFollowupStatus() != null) {
				if(dto.getFollowupStatus().equalsIgnoreCase("Completed")) {
					entity.setStatus("Completed");
				}
				entity.setFollowupStatus(dto.getFollowupStatus());}

			// -------- PROBLEM --------

			if (dto.getProblem() != null && !dto.getProblem().isEmpty())
				entity.setProblem(dto.getProblem());

			if (dto.getSymptomsDuration() != null && !dto.getSymptomsDuration().isEmpty())
				entity.setSymptomsDuration(dto.getSymptomsDuration());

			// -------- CLINIC --------

			if (dto.getClinicId() != null && !dto.getClinicId().isEmpty())
				entity.setClinicId(dto.getClinicId());

			if (dto.getClinicName() != null && !dto.getClinicName().isEmpty())
				entity.setClinicName(dto.getClinicName());

			if (dto.getClinicDeviceId() != null && !dto.getClinicDeviceId().isEmpty())
				entity.setClinicDeviceId(dto.getClinicDeviceId());

			if (dto.getBranchId() != null && !dto.getBranchId().isEmpty())
				entity.setBranchId(dto.getBranchId());

			if (dto.getBranchname() != null && !dto.getBranchname().isEmpty())
				entity.setBranchname(dto.getBranchname());

			// -------- DOCTOR --------

			if (dto.getDoctorId() != null && !dto.getDoctorId().isEmpty())
				entity.setDoctorId(dto.getDoctorId());

			if (dto.getDoctorName() != null && !dto.getDoctorName().isEmpty())
				entity.setDoctorName(dto.getDoctorName());

			if (dto.getDoctorMobileDeviceId() != null && !dto.getDoctorMobileDeviceId().isEmpty())
				entity.setDoctorDeviceId(dto.getDoctorMobileDeviceId());

			if (dto.getDoctorWebDeviceId() != null && !dto.getDoctorWebDeviceId().isEmpty())
				entity.setDoctorWebDeviceId(dto.getDoctorWebDeviceId());

			// -------- SERVICE --------

			if (dto.getServiceDate() != null && !dto.getServiceDate().isEmpty())
				entity.setServiceDate(dto.getServiceDate());

			if (dto.getServicetime() != null && !dto.getServicetime().isEmpty())
				entity.setServicetime(dto.getServicetime());

			if (dto.getConsultationType() != null && !dto.getConsultationType().isEmpty())
				entity.setConsultationType(dto.getConsultationType());

			// -------- CONSULTATION FEE --------

			if (dto.getConsultationFee() != 0.0) {

				List<ConsultationFees> list = entity.getListOfConsultationFee();
				if (list == null)
					list = new ArrayList<>();

				ConsultationFees fee = new ConsultationFees();
				fee.setConsulationFee(dto.getConsultationFee());
				fee.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

				list.add(fee);

				entity.setConsultationFee(dto.getConsultationFee());
				entity.setListOfConsultationFee(list);
			}

			if (dto.getListOfConsultationFee() != null && !dto.getListOfConsultationFee().isEmpty()) {

				List<ConsultationFees> list = entity.getListOfConsultationFee();
				if (list == null)
					list = new ArrayList<>();

				for (ConsultationFeesDTO c : dto.getListOfConsultationFee()) {
					ConsultationFees fee = mapper.convertValue(c, ConsultationFees.class);
					list.add(fee);
				}

				entity.setListOfConsultationFee(list);
			}

			if (dto.getConsultationExpiration() != null && !dto.getConsultationExpiration().isEmpty())
				entity.setConsultationExpiration(dto.getConsultationExpiration());

			// -------- STATUS --------

			if (dto.getStatus() != null) {

				entity.setStatus(dto.getStatus());

				List<Status> statusList = entity.getCurrentStatus();
				if (statusList == null)
					statusList = new ArrayList<>();

				Status s = new Status();
				s.setStatus(dto.getStatus());
				s.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

				statusList.add(s);
				entity.setCurrentStatus(statusList);
			}

			if (dto.getCurrentStatus() != null && !dto.getCurrentStatus().isEmpty()) {
				entity.setCurrentStatus(mapper.convertValue(dto.getCurrentStatus(), new TypeReference<List<Status>>() {
				}));
			}

			if (dto.getReasonForCancel() != null && !dto.getReasonForCancel().isEmpty())
				entity.setReasonForCancel(dto.getReasonForCancel());

			// -------- FILES --------

			if (dto.getAttachments() != null && !dto.getAttachments().isEmpty())
				entity.setAttachments(dto.getAttachments());

			if (dto.getConsentFormPdf() != null && !dto.getConsentFormPdf().isEmpty())
				entity.setConsentFormPdf(dto.getConsentFormPdf());

			if (dto.getPrescriptionPdf() != null && !dto.getPrescriptionPdf().isEmpty())
				entity.setPrescriptionPdf(dto.getPrescriptionPdf());

			// -------- PAYMENT --------

			if (dto.getPaymentType() != null && !dto.getPaymentType().isEmpty())
				entity.setPaymentType(dto.getPaymentType());

			if (dto.getPaymentStatus() != null && !dto.getPaymentStatus().isEmpty())
				entity.setPaymentStatus(dto.getPaymentStatus());

			if (dto.getTotalFee() > 0)
				entity.setTotalFee(dto.getTotalFee());

			if (dto.getDoctorRefCode() != null && !dto.getDoctorRefCode().isEmpty())
				entity.setDoctorRefCode(dto.getDoctorRefCode());

			// -------- BODY PART --------

			if (dto.getBodyPartId() != null && !dto.getBodyPartId().isEmpty())
				entity.setBodyPartId(dto.getBodyPartId());

			if (dto.getBodyPartName() != null && !dto.getBodyPartName().isEmpty())
				entity.setBodyPartName(dto.getBodyPartName());

			if (dto.getPartImage() != null && !dto.getPartImage().isEmpty())
				entity.setPartImage(dto.getPartImage());

			// -------- THERAPY --------

			if (dto.getTheraphyAnswers() != null) {
				entity.setTheraphyAnswers(mapper.convertValue(dto.getTheraphyAnswers(),
						new TypeReference<Map<String, List<TheraphyAnswersEntity>>>() {
						}));
			}

			if (dto.getParts() != null && !dto.getParts().isEmpty())
				entity.setParts(dto.getParts());

			if (dto.getPartAmount() > 0)
				entity.setPartAmount(dto.getPartAmount());

			if (dto.getDueAmount() >= 0)
				entity.setDueAmount(dto.getDueAmount());

			// -------- REFERRAL --------

			if (dto.getReferredByType() != null && !dto.getReferredByType().isEmpty())
				entity.setReferredByType(dto.getReferredByType());

			if (dto.getReferredByName() != null && !dto.getReferredByName().isEmpty())
				entity.setReferredByName(dto.getReferredByName());

			// -------- MEDICAL --------

			if (dto.getPreviousInjuries() != null && !dto.getPreviousInjuries().isEmpty())
				entity.setPreviousInjuries(dto.getPreviousInjuries());

			if (dto.getCurrentMedications() != null && !dto.getCurrentMedications().isEmpty())
				entity.setCurrentMedications(dto.getCurrentMedications());

			if (dto.getAllergies() != null && !dto.getAllergies().isEmpty())
				entity.setAllergies(dto.getAllergies());

			if (dto.getOccupation() != null && !dto.getOccupation().isEmpty())
				entity.setOccupation(dto.getOccupation());

			// -------- INSURANCE --------

			if (dto.getInsuranceProvider() != null && !dto.getInsuranceProvider().isEmpty())
				entity.setInsuranceProvider(dto.getInsuranceProvider());

			if (dto.getPolicyNumber() != null && !dto.getPolicyNumber().isEmpty())
				entity.setPolicyNumber(dto.getPolicyNumber());

			// -------- ACTIVITY --------

			if (dto.getActivityLevels() != null && !dto.getActivityLevels().isEmpty())
				entity.setActivityLevels(dto.getActivityLevels());

			// -------- FOC --------

			if (dto.getFoc() != null)
				entity.setFoc(dto.getFoc());

			// -------- FOLLOWUP LOGIC --------

			try {
				int days = 0;

				if (entity.getConsultationExpiration() != null)
					days = Integer.parseInt(entity.getConsultationExpiration().replaceAll("[^0-9]", ""));

				LocalDate serviceDate = LocalDate.parse(entity.getServiceDate());
				LocalDate expiryDate = serviceDate.plusDays(days);
				LocalDate today = LocalDate.now();

				if (!today.isAfter(expiryDate) && entity.getFreeFollowUps() != null && entity.getFreeFollowUps() == 0) {

					entity.setIsFollowupStatus(true);

				} else if (today.isAfter(expiryDate)) {

					entity.setIsFollowupStatus(true);

				} else {
					entity.setIsFollowupStatus(false);
				}

			} catch (Exception e) {
				entity.setIsFollowupStatus(false);
			}

			updated = repository.save(entity);

			return new ResponseEntity<>(ResponseStructure.buildResponse(toResponse(updated), "Updated Successfully",
					HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);

		} catch (Exception e) {

			return new ResponseEntity<>(ResponseStructure.buildResponse(null, e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<?> getRelationsByCustomerId(String customerId) {
		ResponseStructure<Map<String, List<RelationInfoDTO>>> res = new ResponseStructure<>();
		try {
			List<Booking> bookings = repository.findByCustomerId(customerId);

			Map<String, List<RelationInfoDTO>> data = bookings.stream().collect(Collectors.groupingBy(
					Booking::getRelation, LinkedHashMap::new, Collectors.collectingAndThen(Collectors.mapping(n -> {
						RelationInfoDTO dto = new RelationInfoDTO();
						dto.setAddress(n.getPatientAddress());
						dto.setAge(n.getAge());
						dto.setFullname(n.getName());
						dto.setMobileNumber(n.getMobileNumber());
						dto.setRelation(n.getRelation());
						dto.setGender(n.getGender());
						dto.setCustomerId(n.getCustomerId());
						dto.setPatientId(n.getPatientId());
						return dto;
					}, Collectors.toList()), list -> list.stream().distinct().collect(Collectors.toList())
					)));
			res.setStatusCode(200);
			res.setHttpStatus(HttpStatus.OK);
			res.setData(data);
			res.setMessage("Relations found successfully");
		} catch (Exception e) {
			res.setStatusCode(500);
			res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			res.setMessage("Error: " + e.getMessage());
		}

		return ResponseEntity.status(res.getStatusCode()).body(res);
	}

	@Override
	public BookingResponse checkBookingByDateAndTime(String date, String time, String doctorId) {
		Booking booking = repository.findByServiceDateAndServicetimeAndDoctorId(date, time, doctorId);
		if (booking != null) {
			return toResponse(booking);
		} else {
			return null;
		}

	}

	@Override
	public ResponseEntity<Response> getPatientAndPriceInfo(String clinicId, String branchId, Integer number,
			String startDate, String endDate) {

		try {

			List<Booking> bookings = repository.findByClinicIdAndBranchId(clinicId, branchId);

			if (bookings == null || bookings.isEmpty()) {
				return ResponseEntity.ok(Response.builder().success(true).message("No data found")
						.data(new PatientAndPriceInfo()).status(HttpStatus.OK.value()).build());
			}

			// 🔥 Step 1: Decide Date Range
			LocalDate start;
			LocalDate end;

			if (startDate.isEmpty() && endDate.isEmpty()) {
				start = LocalDate.parse(startDate);
				end = LocalDate.parse(endDate);
			} else {
				LocalDate today = LocalDate.now();

				if (number == 1) {
					start = today;
					end = today;
				} else if (number == 2) {
					start = today.minusDays(6);
					end = today;
				} else if (number == 3) {
					start = today.withDayOfMonth(1);
					end = today;
				} else {
					return ResponseEntity.badRequest().body(Response.builder().success(false)
							.message("Invalid number value").status(HttpStatus.BAD_REQUEST.value()).build());
				}
			}

			// 🔥 Step 2: Filter + Map
			List<PatientInfo> patientList = new ArrayList<>();

			double totalConsultation = 0;
			double totalTherapy = 0;
			double totalDue = 0;

			for (Booking booking : bookings) {

				if (booking.getServiceDate() == null)
					continue;

				LocalDate bookingDate = LocalDate.parse(booking.getServiceDate());

				if ((bookingDate.isEqual(start) || bookingDate.isAfter(start))
						&& (bookingDate.isEqual(end) || bookingDate.isBefore(end))) {

					PatientInfo info = new PatientInfo();

					info.setClinicId(booking.getClinicId());
					info.setBranchId(booking.getBranchId());
					info.setPatientName(booking.getName());
					info.setDate(booking.getServiceDate());
					info.setDoctorId(booking.getDoctorId());
					info.setConsultationFee(String.valueOf(booking.getConsultationFee()));
					info.setTheraphyFee(String.valueOf(booking.getTotalFee()));
					info.setFinalAmount(booking.getTotalFee());
					info.setDueAmount(booking.getDueAmount());
					info.setConsultationType(booking.getConsultationType());

					patientList.add(info);

					// 🔥 Aggregation
					totalConsultation += booking.getListOfConsultationFee().get(0).getConsulationFee();
					totalTherapy += booking.getTotalFee();
					totalDue += booking.getDueAmount();
				}
			}

			// 🔥 Step 3: Final Calculation
			double grandTotal = totalConsultation + totalTherapy + totalDue;
			double afterExpenses = 0.0;
			if (number.equals(1)) {
				Double value = clinicAdminFeign.getTodayExpenses(clinicId, branchId);
				afterExpenses = grandTotal - value;
			} else if (number.equals(2)) {
				Double value = clinicAdminFeign.getWeeklyExpenses(clinicId, branchId);
				afterExpenses = grandTotal - value;
			} else if (number.equals(3)) {
				Double value = clinicAdminFeign.getMonthlyExpenses(clinicId, branchId);
				afterExpenses = grandTotal - value;
			} else {
				if (!startDate.isEmpty() && !endDate.isEmpty()) {
					Double value = clinicAdminFeign.customFilter(startDate, endDate);
					afterExpenses = afterExpenses - value;
				}
			}

			PatientAndPriceInfo responseDto = new PatientAndPriceInfo();
			responseDto.setList(patientList);
			responseDto.setTotalConsultationFee(String.valueOf(totalConsultation));
			responseDto.setTotalTheraphyFee(String.valueOf(totalTherapy));
			responseDto.setTotalDueAmount(String.valueOf(totalDue));
			responseDto.setGrandTotalAmount(String.valueOf(grandTotal));
			responseDto.setPriceAfterExpenses(afterExpenses);

			return ResponseEntity
					.ok(Response.builder().success(true).data(responseDto).status(HttpStatus.OK.value()).build());

		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.builder().success(false)
					.message(e.getMessage()).status(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
		}
	}

	@Override
	public List<Map<String, Object>> getTodayBookings(String cId, String bId) {
		try {
			List<Map<String, Object>> list = new ArrayList<>();
			String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			List<Booking> b = repository.findByClinicIdAndBranchIdAndServiceDate(cId, bId, today);
			if (!b.isEmpty()) {
				List<BookingResponse> dto = toResponses(b);
				dto.stream().map(n -> {
					Map<String, Object> map = new LinkedHashMap<>();
					map.put("bookingId", n.getBookingId());
					map.put("serviceDate", n.getServiceDate());
					map.put("servicetime", n.getServicetime());
					map.put("name", n.getName());
					map.put("mobileNumber",
							!n.getPatientMobileNumber().isEmpty() ? n.getPatientMobileNumber() : n.getMobileNumber());
					map.put("doctorId", n.getDoctorId());
					map.put("doctorName", n.getDoctorName());
					map.put("paymentType", n.getPaymentType());
					map.put("visitType", n.getVisitType());
					map.put("status", n.getStatus());
					map.put("followupStatus", n.getFollowupStatus());
					map.put("patientId", n.getPatientId());
					map.put("clinicId", n.getClinicId());
					map.put("customerId", n.getCustomerId());
					map.put("branchId", n.getBranchId());
					map.put("problem", n.getProblem());
					list.add(map);
					return n;
				}).toList();
				return list;
			} else {
				return Collections.emptyList();
			}
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	public ResponseEntity<Response> getTodayAllBookings(String clinicId, String branchId) {
		try {

			String today = LocalDate.now().format(FORMATTER);

			List<Map<String, Object>> responseList = new ArrayList<>();

			// Today's bookings for logged-in clinic & branch
			List<Booking> data = repository.findByClinicIdAndBranchIdAndServiceDate(clinicId, branchId, today);
			List<BookingResponse> responses =  toResponses(data);
			// Follow-up booking IDs
			List<Map<String,String>> bookingIds =
					physioDoctorFeign.getPhysioRecordsByTodayDate(clinicId,branchId,today);

			log.info("Found {} follow-up booking ids",
					bookingIds != null ? bookingIds.size() : 0);

			if (bookingIds != null && !bookingIds.isEmpty()) {

				List<String> keys = bookingIds.stream()
						.flatMap(map -> map.keySet().stream())
						.collect(Collectors.toList());

				List<Booking> followUpBookings =
						repository.findByBookingIdIn(keys);

				log.info("Found {} follow-up bookings",
						followUpBookings != null ? followUpBookings.size() : 0);

				if (followUpBookings != null && !followUpBookings.isEmpty()) {
					followUpBookings.forEach(booking -> {

						String value =	bookingIds.stream().filter(f->f.containsKey(booking.getBookingId()))
								.map(n->n.get(booking.getBookingId())).findFirst().orElse(null);

						booking.setStatus("follow-up Pending");
						booking.setFollowupDate(value);

						List<Status> statusList =
								booking.getCurrentStatus() == null
										? new ArrayList<>()
										: new ArrayList<>(booking.getCurrentStatus());

						boolean alreadyExists =
								statusList.stream()
										.anyMatch(status ->
												"follow-up Pending".equalsIgnoreCase(
														status.getStatus()));

						if (!alreadyExists) {
							Status status = new Status();
							status.setDATE_TIME(
									LocalDateTime.now(
											ZoneId.of("Asia/Kolkata")));
							status.setStatus("follow-up Pending");

							statusList.add(status);
						}

						booking.setCurrentStatus(statusList);
					});

					repository.saveAll(followUpBookings);

					// ✅ Safe now: toResponses() returns a mutable ArrayList
					// even for the empty-input case, so this addAll() can no
					// longer throw UnsupportedOperationException.
					responses.addAll(
							followUpBookings.stream()
									.map(this::toResponse)
									.collect(Collectors.toList()));
				}
			}
			// Session details
			for (BookingResponse booking : responses) {
				try {
					ResponseEntity<List<Session>> sessionResponse = physioDoctorFeign
							.getPhysioByBookingId(booking.getBookingId(), booking.getServiceDate());

					List<Session> sessions = sessionResponse != null ? sessionResponse.getBody() : null;
                   //// sessions = sessions.stream().filter(n->n.getSlot() != null).toList();
					if (sessions != null && !sessions.isEmpty()) {
						booking.setSession(sessions);
						booking.setVisitType("session");
					} else {
						booking.setSession(null);
					}

				} catch (Exception ex) {

					System.out.println("Session fetch failed for BookingId : " + booking.getBookingId() + " Error : "
							+ ex.getMessage());
				}
			}
			// Build response list
			for (BookingResponse n : responses) {

				Map<String, Object> map = new LinkedHashMap<>();

				map.put("bookingId", n.getBookingId());
				map.put("serviceDate", n.getServiceDate());
				map.put("servicetime", n.getServicetime());
				map.put("name", n.getName());

				map.put("mobileNumber",
						n.getPatientMobileNumber() != null && !n.getPatientMobileNumber().isEmpty()
								? n.getPatientMobileNumber()
								: n.getMobileNumber());

				map.put("doctorId", n.getDoctorId());
				map.put("doctorName", n.getDoctorName());
				map.put("paymentType", n.getPaymentType());
				map.put("visitType", n.getVisitType());
				map.put("status", n.getStatus());
				map.put("followupStatus", n.getFollowupStatus());
				map.put("patientId", n.getPatientId());
				map.put("clinicId", n.getClinicId());
				map.put("customerId", n.getCustomerId());
				map.put("branchId", n.getBranchId());
				map.put("session", n.getSession());
				map.put("problem", n.getProblem());

				responseList.add(map);
			}
			// Summary counts
			long totalCount = responses.size();

			long pendingCount = responses.stream()
					.filter(b -> "PENDING".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			long confirmedCount = responses.stream()
					.filter(b -> "CONFIRMED".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			long inProgressCount = responses.stream()
					.filter(b -> "IN-PROGRESS".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			Map<String, Object> summary = new HashMap<>();
			summary.put("totalAppointments", totalCount);
			summary.put("pending", pendingCount);
			summary.put("confirmed", confirmedCount);
			summary.put("inProgress", inProgressCount);

			if (responses.isEmpty()) {
				return ResponseEntity
						.ok(new Response(true, Collections.emptyList(), summary, "No bookings found", 200, null, null));
			}
			return ResponseEntity
					.ok(new Response(true, responseList, summary, "Today bookings fetched", 200, null, null));

		} catch (Exception e) {
			e.printStackTrace();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Response(false, null, null,
					"Error fetching today bookings : " + e.getMessage(), 500, null, null));
		}

	}
	
	
	@Override
	public ResponseEntity<Response> getFilteredBookingsByStatus(
	        String clinicId,
	        String branchId) {

	    try {

	        List<Booking> bookings =
	                repository.findByClinicIdAndBranchId(
	                        clinicId,
	                        branchId);

	        List<BookingResponse> bookingResponses =
	                bookings != null && !bookings.isEmpty()
	                        ? toResponses(bookings)
	                        : Collections.emptyList();

	        List<Map<String, Object>> filteredBookings =
	                new ArrayList<>();

	        long inProgressCount = 0;
	        long completedCount = 0;
	        long dueForInvestigationCount = 0;
	        long investigationDoneCount = 0;

	        for (BookingResponse booking : bookingResponses) {

	            String followupStatus =
	                    booking.getStatus();

	            boolean includeBooking = false;

	            if ("IN-PROGRESS".equalsIgnoreCase(followupStatus)) {

	                inProgressCount++;
	                includeBooking = true;

	            } else if ("COMPLETED".equalsIgnoreCase(followupStatus)) {

	                completedCount++;
	                includeBooking = true;

	            } else if ("DUE FOR INVESTIGATION".equalsIgnoreCase(followupStatus)) {

	                dueForInvestigationCount++;
	                includeBooking = true;

	            } else if ("INVESTIGATION DONE".equalsIgnoreCase(followupStatus)) {

	                investigationDoneCount++;
	                includeBooking = true;
	            }

	            if (!includeBooking) {
	                continue;
	            }

	            Map<String, Object> map = new LinkedHashMap<>();

	            map.put("bookingId", booking.getBookingId());
	            map.put("serviceDate", booking.getServiceDate());
	            map.put("servicetime", booking.getServicetime());
	            map.put("name", booking.getName());

	            map.put("mobileNumber",
	                    booking.getPatientMobileNumber() != null
	                            && !booking.getPatientMobileNumber().isEmpty()
	                                    ? booking.getPatientMobileNumber()
	                                    : booking.getMobileNumber());

	            map.put("doctorId", booking.getDoctorId());
	            map.put("doctorName", booking.getDoctorName());
	            map.put("paymentType", booking.getPaymentType());
	            map.put("visitType", booking.getVisitType());
	            map.put("status", booking.getStatus());
	            map.put("followupStatus", booking.getFollowupStatus());
	            map.put("patientId", booking.getPatientId());
	            map.put("clinicId", booking.getClinicId());
	            map.put("customerId", booking.getCustomerId());
	            map.put("branchId", booking.getBranchId());
	            map.put("problem", booking.getProblem());

	            filteredBookings.add(map);
	        }

	        Map<String, Object> summary = new LinkedHashMap<>();

	        summary.put("totalBookings", filteredBookings.size());
	        summary.put("inProgressCount", inProgressCount);
	        summary.put("completedCount", completedCount);
	        summary.put("dueForInvestigationCount", dueForInvestigationCount);
	        summary.put("investigationDoneCount", investigationDoneCount);

	        if (filteredBookings.isEmpty()) {

	            return ResponseEntity.ok(
	                    new Response(
	                            true,
	                            Collections.emptyList(),
	                            summary,
	                            "No bookings found",
	                            200,
	                            null,
	                            null));
	        }

	        return ResponseEntity.ok(
	                new Response(
	                        true,
	                        filteredBookings,
	                        summary,
	                        "Bookings fetched successfully",
	                        200,
	                        null,
	                        null));

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching filtered bookings. clinicId={}, branchId={}, error={}",
	                clinicId,
	                branchId,
	                e.getMessage(),
	                e);

	        return ResponseEntity.status(
	                HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(
	                        new Response(
	                                false,
	                                null,
	                                null,
	                                "Error fetching bookings : " + e.getMessage(),
	                                500,
	                                null,
	                                null));
	    }
	}

	@Override
	public ResponseEntity<Response> getUpcomingBookings(String clinicId, String branchId, int option) {
		List<Map<String, Object>> list = new ArrayList<>();
		try {
			int days;

			if (option == 1) {
				days = 3;
			} else if (option == 2) {
				days = 7;
			} else {
				return ResponseEntity.badRequest()
						.body(new Response(false, null, null, "Invalid option (1=3days, 2=7days)", 400, null, null));
			}

			LocalDate startDate = LocalDate.now().minusDays(1);
			LocalDate endDate = startDate.plusDays(days + 1);

			List<Booking> bookings = repository.findByClinicIdAndBranchIdAndServiceDateBetween(clinicId, branchId,
					startDate.format(FORMATTER), endDate.format(FORMATTER));
			List<BookingResponse> res = toResponses(bookings);
			try {
				res = res.stream().map(n -> {

					List<Session> lst = physioDoctorFeign.getPhysioByBookingId(n.getBookingId(), n.getServiceDate())
							.getBody();
                  ///  lst = lst.stream().filter(p->p.getSlot()!= null).toList();
					if (lst != null && !lst.isEmpty()) {

						n.setSession(lst);

						n.setVisitType("session");

					} else {

						n.setSession(null);
					}

					return n;

				}).toList();
				res.stream().map(n -> {
					Map<String, Object> map = new LinkedHashMap<>();
					map.put("bookingId", n.getBookingId());
					map.put("serviceDate", n.getServiceDate());
					map.put("servicetime", n.getServicetime());
					map.put("name", n.getName());
					map.put("mobileNumber",
							!n.getPatientMobileNumber().isEmpty() ? n.getPatientMobileNumber() : n.getMobileNumber());
					map.put("doctorId", n.getDoctorId());
					map.put("doctorName", n.getDoctorName());
					map.put("paymentType", n.getPaymentType());
					map.put("visitType", n.getVisitType());
					map.put("status", n.getStatus());
					map.put("followupStatus", n.getFollowupStatus());
					map.put("patientId", n.getPatientId());
					map.put("clinicId", n.getClinicId());
					map.put("customerId", n.getCustomerId());
					map.put("branchId", n.getBranchId());
					map.put("age", n.getAge());
					map.put("gender", n.getGender());
					map.put("branchName", n.getBranchname());
					map.put("session", n.getSession());
					map.put("problem", n.getProblem());

					list.add(map);
					return n;
				}).toList();
			} catch (Exception e) {
				System.out.println("Error while fetching session details: " + e.getMessage());
			}
			long totalCount = bookings.size();

			long pendingCount = bookings.stream()
					.filter(b -> "PENDING".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			long confirmedCount = bookings.stream()
					.filter(b -> "CONFIRMED".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			long inProgressCount = bookings.stream()
					.filter(b -> "IN-PROGRESS".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			Map<String, Object> summary = new HashMap<>();
			summary.put("totalAppointments", totalCount);
			summary.put("pending", pendingCount);
			summary.put("confirmed", confirmedCount);
			summary.put("inProgress", inProgressCount);
			summary.put("startDate", startDate.toString());
			summary.put("endDate", endDate.toString());

			return ResponseEntity.ok(new Response(true, list, summary, "Upcoming bookings fetched", 200, null, null));

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Response(false, null, null,
					"Error fetching upcoming bookings: " + e.getMessage(), 500, null, null));
		}
	}

	@Override
	public ResponseEntity<Response> getBookingByDate(String clinicId, String branchId, String date) {

		try {
			LocalDate dte = LocalDate.parse(date);

			List<Booking> bookings = repository.findByClinicIdAndBranchIdAndServiceDate(clinicId, branchId,
					dte.format(FORMATTER));

			List<BookingResponse> res = toResponses(bookings);

			try {
				res = res.stream().map(n -> {
					List<Session> lst = physioDoctorFeign.getPhysioByBookingId(n.getBookingId(), n.getServiceDate())
							.getBody();
                   //// lst = lst.stream().filter(p->p.getSlot()!= null).toList();
					if (lst != null) {
						n.setSession(lst);
						n.setVisitType("session");
					} else {
						n.setSession(null);
					}
					return n;
				}).toList();

			} catch (Exception e) {
				System.out.println("Error while fetching session details: " + e.getMessage());
			}

			long totalCount = bookings.size();

			long pendingCount = bookings.stream()
					.filter(b -> "PENDING".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			long confirmedCount = bookings.stream()
					.filter(b -> "CONFIRMED".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			long inProgressCount = bookings.stream()
					.filter(b -> "IN-PROGRESS".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			Map<String, Object> summary = new HashMap<>();
			summary.put("totalAppointments", totalCount);
			summary.put("pending", pendingCount);
			summary.put("confirmed", confirmedCount);
			summary.put("inProgress", inProgressCount);

			return ResponseEntity.ok(new Response(true, res, summary, "Bookings fetched", 200, null, null));

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					new Response(false, null, null, "Error fetching bookings: " + e.getMessage(), 500, null, null));
		}
	}

	@Override
	public ResponseEntity<Response> getBookingByCustomRange(
			String clinicId,
			String branchId,
			String start,
			String end) {

		List<Map<String, Object>> list = new ArrayList<>();

		try {

			log.info("Fetching bookings for clinicId: {}, branchId: {}, start: {}, end: {}",
					clinicId, branchId, start, end);

			LocalDate startDate = LocalDate.parse(start);
			LocalDate endDate = LocalDate.parse(end);

			String fromDate = startDate.minusDays(1).format(FORMATTER);
			String toDate = endDate.plusDays(1).format(FORMATTER);

			List<Booking> bookings =
					repository.findByClinicIdAndBranchIdAndServiceDateBetween(
							clinicId,
							branchId,
							fromDate,
							toDate);

			log.info("Found {} bookings", bookings != null ? bookings.size() : 0);

			List<BookingResponse> responses = new ArrayList<>();

			if (bookings != null && !bookings.isEmpty()) {
				responses.addAll(toResponses(bookings));
			}

			// Populate session details
			responses = responses.stream()
					.map(response -> {
						try {
							ResponseEntity<List<Session>> sessionResponse =
									physioDoctorFeign.getPhysioByBookingId(
											response.getBookingId(),
											response.getServiceDate());

							List<Session> sessions =
									sessionResponse != null
											? sessionResponse.getBody()
											: null;
                            ////sessions = sessions.stream().filter(n->n.getSlot()!=null).toList();
							if (sessions != null && !sessions.isEmpty()) {
								response.setSession(sessions);
								response.setVisitType("session");
							}

						} catch (Exception ex) {

							log.error(
									"Error fetching sessions for bookingId: {}",
									response.getBookingId(),
									ex);
						}

						return response;

					})
					.collect(Collectors.toCollection(ArrayList::new));

			// Fetch follow-up booking ids
			List<Map<String,String>> bookingIds =
					physioDoctorFeign.getPhysioRecordsByFollowUpDateRange(
							clinicId,
							branchId,
							start,
							end);

			log.info("Found {} follow-up booking ids",
					bookingIds != null ? bookingIds.size() : 0);

			if (bookingIds != null && !bookingIds.isEmpty()) {

				List<String> keys = list.stream()
						.flatMap(map -> map.keySet().stream())
						.collect(Collectors.toList());

				List<Booking> followUpBookings =
						repository.findByBookingIdIn(keys);

				log.info("Found {} follow-up bookings",
						followUpBookings != null ? followUpBookings.size() : 0);
				if (followUpBookings != null && !followUpBookings.isEmpty()) {
					followUpBookings.forEach(booking -> {

					String value =	bookingIds.stream().filter(f->f.containsKey(booking.getBookingId()))
								.map(n->n.get(booking.getBookingId())).findFirst().orElse(null);

						booking.setStatus("follow-up Pending");
						booking.setFollowupDate(value);

					List<Status> statusList =
								booking.getCurrentStatus() == null
										? new ArrayList<>()
										: new ArrayList<>(booking.getCurrentStatus());

						boolean alreadyExists =
								statusList.stream()
										.anyMatch(status ->
												"follow-up Pending".equalsIgnoreCase(
														status.getStatus()));

						if (!alreadyExists) {

							Status status = new Status();
							status.setDATE_TIME(
									LocalDateTime.now(
											ZoneId.of("Asia/Kolkata")));
							status.setStatus("follow-up Pending");

							statusList.add(status);
						}

						booking.setCurrentStatus(statusList);
					});

					repository.saveAll(followUpBookings);

					responses.addAll(
							followUpBookings.stream()
									.map(this::toResponse)
									.collect(Collectors.toList()));
				}
			}

			// Build response payload
			for (BookingResponse response : responses) {

				Map<String, Object> map = new LinkedHashMap<>();

				map.put("bookingId", response.getBookingId());
				map.put("serviceDate", response.getServiceDate());
				map.put("servicetime", response.getServicetime());
				map.put("name", response.getName());

				map.put(
						"mobileNumber",
						response.getPatientMobileNumber() != null
								&& !response.getPatientMobileNumber().isBlank()
								? response.getPatientMobileNumber()
								: response.getMobileNumber());

				map.put("doctorId", response.getDoctorId());
				map.put("doctorName", response.getDoctorName());
				map.put("paymentType", response.getPaymentType());
				map.put("visitType", response.getVisitType());
				map.put("status", response.getStatus());
				map.put("followupStatus", response.getFollowupStatus());
				map.put("patientId", response.getPatientId());
				map.put("clinicId", response.getClinicId());
				map.put("customerId", response.getCustomerId());
				map.put("branchId", response.getBranchId());
				map.put("age", response.getAge());
				map.put("gender", response.getGender());
				map.put("branchName", response.getBranchname());
				map.put("session", response.getSession());
				map.put("problem", response.getProblem());

				list.add(map);
			}

			// Summary counts
			long totalCount = responses.size();

			long pendingCount = responses.stream()
					.filter(r ->
							"PENDING".equalsIgnoreCase(r.getFollowupStatus()))
					.count();

			long confirmedCount = responses.stream()
					.filter(r ->
							"CONFIRMED".equalsIgnoreCase(r.getFollowupStatus()))
					.count();

			long inProgressCount = responses.stream()
					.filter(r ->
							"IN-PROGRESS".equalsIgnoreCase(r.getFollowupStatus()))
					.count();

			Map<String, Object> summary = new HashMap<>();
			summary.put("totalAppointments", totalCount);
			summary.put("pending", pendingCount);
			summary.put("confirmed", confirmedCount);
			summary.put("inProgress", inProgressCount);

			log.info(
					"Custom range bookings fetched successfully. Total appointments: {}",
					totalCount);

			return ResponseEntity.ok(
					new Response(
							true,
							list,
							summary,
							"Custom range bookings fetched",
							200,
							null,
							null));

		} catch (Exception e) {

			log.error(
					"Error fetching custom range bookings for clinicId: {}, branchId: {}",
					clinicId,
					branchId,
					e);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(
							new Response(
									false,
									null,
									null,
									e.getMessage(),
									500,
									null,
									null));
		}
	}
	public ResponseEntity<Response> getBookingById(String bookingId) {
		try {
			Optional<Booking> booking = repository.findByBookingId(bookingId);
			if (booking.isPresent()) {
				if (!booking.get().getFollwupBookings().isEmpty()) {
					BookingResponse res = null;
					if (booking.get().getFollwupBookings().get(booking.get().getFollwupBookings().size() - 1)
							.getStatus().equalsIgnoreCase("in-progress")) {
						res = mapper.convertValue(
								booking.get().getFollwupBookings().get(booking.get().getFollwupBookings().size() - 1),
								BookingResponse.class);
						List<Session> lst = new ArrayList<>();
						try {
							lst = physioDoctorFeign.getPhysioByBookingId(res.getBookingId(), res.getServiceDate())
									.getBody();
                               //// lst = 	lst.stream().filter(n->n.getSlot()!= null).toList();
							res.setSession(lst);
						} catch (Exception e) {
						}
					}
					return ResponseEntity.ok(new Response(true, // success
							res, null, // data
							"Booking fetched successfully", // message
							200, null, null // status
					));
				} else {
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body(new Response(false, null, null, "follow up appoiintment not found", 404, null, null));
				}
			} else {
				return ResponseEntity.status(HttpStatus.OK)
						.body(new Response(false, null, null, "Booking not found", 200, null, null));
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new Response(false, null, null, e.getMessage(), 500, null, null));
		}
	}

	private Booking updateForFollowup(BookingResponse dto) {
		try {
			Booking entity = repository.findByBookingIdIgnoreCase(dto.getBookingId())
					.orElseThrow(() -> new RuntimeException("Invalid Booking Id"));
			List<FollowupBooking> lst = new LinkedList<>();
			if (entity.getFollwupBookings() == null) {
				lst = new LinkedList<>();
			} else {
				lst = entity.getFollwupBookings();
			}
			entity.setVisitType("follow-up");
			if (dto.getBookingFor() != null && !dto.getBookingFor().isEmpty())
				entity.setBookingFor(dto.getBookingFor());

			if (dto.getName() != null && !dto.getName().isEmpty())
				entity.setName(dto.getName());

			if (dto.getPatientMobileNumber() != null && !dto.getPatientMobileNumber().isEmpty())
				entity.setPatientMobileNumber(dto.getPatientMobileNumber());

			if (dto.getPatientId() != null && !dto.getPatientId().isEmpty())
				entity.setPatientId(dto.getPatientId());

			if (dto.getVisitType() != null && !dto.getVisitType().isEmpty())
				entity.setVisitType(dto.getVisitType());

			if (dto.getPatientAddress() != null && !dto.getPatientAddress().isEmpty())
				entity.setPatientAddress(dto.getPatientAddress());

			if (dto.getAge() != null && !dto.getAge().isEmpty())
				entity.setAge(dto.getAge());

			if (dto.getGender() != null && !dto.getGender().isEmpty())
				entity.setGender(dto.getGender());

			if (dto.getMobileNumber() != null && !dto.getMobileNumber().isEmpty())
				entity.setMobileNumber(dto.getMobileNumber());

			if (dto.getCustomerId() != null && !dto.getCustomerId().isEmpty())
				entity.setCustomerId(dto.getCustomerId());

			if (dto.getCustomerDeviceId() != null && !dto.getCustomerDeviceId().isEmpty())
				entity.setCustomerDeviceId(dto.getCustomerDeviceId());

			// -------- FOLLOWUPS -------

			if (dto.getFreeFollowUps() != null)
				entity.setFreeFollowUps(dto.getFreeFollowUps());

			if (dto.getFollowupDate() != null && !dto.getFollowupDate().isEmpty())
				entity.setFollowupDate(dto.getFollowupDate());

			if (dto.getFollowupStatus() != null) {
				entity.setFollowupStatus(dto.getFollowupStatus());
			}
			// -------- PROBLEM --------
			if (dto.getProblem() != null && !dto.getProblem().isEmpty())
				entity.setProblem(dto.getProblem());

			if (dto.getSymptomsDuration() != null && !dto.getSymptomsDuration().isEmpty())
				entity.setSymptomsDuration(dto.getSymptomsDuration());

			// -------- CLINIC --------
			if (dto.getClinicId() != null && !dto.getClinicId().isEmpty())
				entity.setClinicId(dto.getClinicId());

			if (dto.getClinicName() != null && !dto.getClinicName().isEmpty())
				entity.setClinicName(dto.getClinicName());

			if (dto.getClinicDeviceId() != null && !dto.getClinicDeviceId().isEmpty())
				entity.setClinicDeviceId(dto.getClinicDeviceId());

			if (dto.getBranchId() != null && !dto.getBranchId().isEmpty())
				entity.setBranchId(dto.getBranchId());

			if (dto.getBranchname() != null && !dto.getBranchname().isEmpty())
				entity.setBranchname(dto.getBranchname());

			// -------- DOCTOR --------
			if (dto.getDoctorId() != null && !dto.getDoctorId().isEmpty())
				entity.setDoctorId(dto.getDoctorId());

			if (dto.getDoctorName() != null && !dto.getDoctorName().isEmpty())
				entity.setDoctorName(dto.getDoctorName());

			if (dto.getDoctorWebDeviceId() != null && !dto.getDoctorWebDeviceId().isEmpty())
				entity.setDoctorWebDeviceId(dto.getDoctorWebDeviceId());

			if (dto.getStatus() != null) {

				entity.setStatus(dto.getStatus());

				List<Status> statusList = entity.getCurrentStatus();
				if (statusList == null)
					statusList = new ArrayList<>();

				Status s = new Status();
				s.setStatus(dto.getStatus());
				s.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

				statusList.add(s);
				entity.setCurrentStatus(statusList);
			}

			if (dto.getServiceDate() != null && !dto.getServiceDate().isEmpty())
				entity.setServiceDate(dto.getServiceDate());

			if (dto.getServicetime() != null && !dto.getServicetime().isEmpty())
				entity.setServicetime(dto.getServicetime());

			if (dto.getConsultationType() != null && !dto.getConsultationType().isEmpty())
				entity.setConsultationType(dto.getConsultationType());
			if (dto.getConsultationFee() != null) {
				List<ConsultationFees> consultationFees = entity.getListOfConsultationFee();
				ConsultationFees fee = new ConsultationFees();
				fee.setConsulationFee(dto.getConsultationFee());
				fee.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
				consultationFees.add(fee);
				Collections.reverse(consultationFees);
				entity.setConsultationFee(consultationFees.get(0).getConsulationFee());
				entity.setListOfConsultationFee(consultationFees);
			}
			if (dto.getConsultationExpiration() != null && !dto.getConsultationExpiration().isEmpty())
				entity.setConsultationExpiration(dto.getConsultationExpiration());

			// -------- STATUS --------
			if (dto.getStatus() != null) {
				entity.setStatus(dto.getStatus());
			}

			// -------- FILES --------
			if (dto.getAttachments() != null && !dto.getAttachments().isEmpty())
				entity.setAttachments(dto.getAttachments());

			if (dto.getConsentFormPdf() != null && !dto.getConsentFormPdf().isEmpty())
				entity.setConsentFormPdf(dto.getConsentFormPdf());
			// -------- PAYMENT --------
			if (dto.getPaymentType() != null && !dto.getPaymentType().isEmpty()) {
				entity.setPaymentType(dto.getPaymentType());
			}
			if (dto.getPaymentStatus() != null && !dto.getPaymentStatus().isEmpty())
				entity.setPaymentStatus(dto.getPaymentStatus());

			if (dto.getTotalFee() > 0)
				entity.setTotalFee(dto.getTotalFee());

			if (dto.getDoctorRefCode() != null && !dto.getDoctorRefCode().isEmpty())
				entity.setDoctorRefCode(dto.getDoctorRefCode());

			// -------- BODY PART --------
			if (dto.getBodyPartId() != null && !dto.getBodyPartId().isEmpty())
				entity.setBodyPartId(dto.getBodyPartId());

			if (dto.getBodyPartName() != null && !dto.getBodyPartName().isEmpty())
				entity.setBodyPartName(dto.getBodyPartName());

			if (dto.getPartImage() != null && !dto.getPartImage().isEmpty())
				entity.setPartImage(dto.getPartImage());

			// -------- THERAPY --------
			if (dto.getTheraphyAnswers() != null)
				entity.setTheraphyAnswers(mapper.convertValue(dto.getTheraphyAnswers(),
						new TypeReference<Map<String, List<TheraphyAnswersEntity>>>() {
						}));

			if (dto.getParts() != null && !dto.getParts().isEmpty())
				entity.setParts(dto.getParts());

			if (dto.getPartAmount() > 0)
				entity.setPartAmount(dto.getPartAmount());

			if (dto.getDueAmount() >= 0)
				entity.setDueAmount(dto.getDueAmount());

			// -------- REFERRAL --------
			if (dto.getReferredByType() != null && !dto.getReferredByType().isEmpty())
				entity.setReferredByType(dto.getReferredByType());

			if (dto.getReferredByName() != null && !dto.getReferredByName().isEmpty())
				entity.setReferredByName(dto.getReferredByName());

			// -------- MEDICAL --------
			if (dto.getPreviousInjuries() != null && !dto.getPreviousInjuries().isEmpty())
				entity.setPreviousInjuries(dto.getPreviousInjuries());

			if (dto.getCurrentMedications() != null && !dto.getCurrentMedications().isEmpty())
				entity.setCurrentMedications(dto.getCurrentMedications());

			if (dto.getAllergies() != null && !dto.getAllergies().isEmpty())
				entity.setAllergies(dto.getAllergies());

			if (dto.getOccupation() != null && !dto.getOccupation().isEmpty())
				entity.setOccupation(dto.getOccupation());

			// -------- INSURANCE --------
			if (dto.getInsuranceProvider() != null && !dto.getInsuranceProvider().isEmpty())
				entity.setInsuranceProvider(dto.getInsuranceProvider());

			if (dto.getPolicyNumber() != null && !dto.getPolicyNumber().isEmpty())
				entity.setPolicyNumber(dto.getPolicyNumber());

			// -------- ACTIVITY --------
			if (dto.getActivityLevels() != null && !dto.getActivityLevels().isEmpty())
				entity.setActivityLevels(dto.getActivityLevels());

			// -------- TREATMENTS --------
			if (dto.getFoc() != null)
				entity.setFoc(dto.getFoc());
			if (entity.getFreeFollowUps() != null && entity.getFreeFollowUps() == 0) {
				entity.setIsFollowupStatus(true);
			}
			int days = 0;
			try {
				if (entity.getConsultationExpiration() != null) {
					String consultationExp = entity.getConsultationExpiration();
					days = Integer.parseInt(consultationExp.replaceAll("[^0-9]", ""));
				}

				LocalDate serviceDate = LocalDate.parse(entity.getServiceDate());

				LocalDate expiryDate = serviceDate.plusDays(days);

				LocalDate today = LocalDate.now();

				if (!today.isAfter(expiryDate) && entity.getFreeFollowUps() != null && entity.getFreeFollowUps() == 0) {
					entity.setIsFollowupStatus(true);
				} else if (today.isAfter(expiryDate)) {
					entity.setIsFollowupStatus(true);
				} else {
					entity.setIsFollowupStatus(false);
				}
			} catch (Exception e) {
				entity.setIsFollowupStatus(false);
			}

			if (dto.getFoc() != null && dto.getPaymentType() != null) {
				if ("paid".equalsIgnoreCase(dto.getFoc()) && "not paid".equalsIgnoreCase(dto.getPaymentType())) {
					entity.setStatus("pending");
				} else if ("foc".equalsIgnoreCase(dto.getFoc()) && "not paid".equalsIgnoreCase(dto.getPaymentType())) {
					entity.setStatus("confirmed");
				} else {
					if ("paid".equalsIgnoreCase(dto.getFoc()) && !dto.getPaymentType().isEmpty()) {
						entity.setStatus("confirmed");
					}
				}
			}
			FollowupBooking followup = new FollowupBooking();
			followup.setDoctorId(entity.getDoctorId());
			followup.setDoctorName(entity.getDoctorName());
			followup.setServiceDate(entity.getServiceDate());
			followup.setServicetime(entity.getServicetime());
			followup.setStatus(entity.getStatus());
			followup.setVisitType(entity.getVisitType());
			lst.add(followup);
			entity.setFollwupBookings(lst);
			Booking booking = repository.save(entity);
			booking.setFollwupBookings(null);
			return booking;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<Map<String, Object>> searchBookings(String clinicId, String input) {

		try {

			List<Booking> bookings = new ArrayList<>();

			// Mobile Number Search
			if (input.matches("^[6-9]\\d{9}$")) {

				bookings = repository.findByMobileNumberAndClinicId(input, clinicId);

				if (bookings.isEmpty()) {
					bookings = repository.findByPatientMobileNumberAndClinicId(input, clinicId);
				}

			} else {

				if (input.length() < 3) {
					throw new IllegalArgumentException("Please enter at least 3 characters to search by patient name");
				}

				bookings = repository.findByPatientIdAndClinicId(input, clinicId);

				if (bookings.isEmpty()) {
					bookings = repository.findByNameContainingIgnoreCaseAndClinicId(input, clinicId);
				}
			}

			if (bookings.isEmpty()) {
				return new ArrayList<>();
			}

			List<BookingResponse> dto = toResponses(bookings);

			List<Map<String, Object>> list = new ArrayList<>();

			dto.forEach(n -> {

				Map<String, Object> map = new LinkedHashMap<>();

				map.put("bookingId", n.getBookingId());
				map.put("serviceDate", n.getServiceDate());
				map.put("servicetime", n.getServicetime());
				map.put("name", n.getName());

				map.put("mobileNumber",
						n.getPatientMobileNumber() != null && !n.getPatientMobileNumber().isEmpty()
								? n.getPatientMobileNumber()
								: n.getMobileNumber());

				map.put("doctorId", n.getDoctorId());
				map.put("doctorName", n.getDoctorName());
				map.put("paymentType", n.getPaymentType());
				map.put("visitType", n.getVisitType());
				map.put("status", n.getStatus());
				map.put("followupStatus", n.getFollowupStatus());
				map.put("patientId", n.getPatientId());
				map.put("clinicId", n.getClinicId());
				map.put("customerId", n.getCustomerId());
				map.put("branchId", n.getBranchId());
				map.put("problem", n.getProblem());

				list.add(map);
			});

			return list;

		} catch (Exception e) {
			throw e;
		}
	}
}