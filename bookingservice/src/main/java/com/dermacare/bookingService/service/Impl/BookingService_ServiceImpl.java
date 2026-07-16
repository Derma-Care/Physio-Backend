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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.dermacare.bookingService.dto.BookingRequset;
import com.dermacare.bookingService.dto.BookingResponse;
import com.dermacare.bookingService.dto.ConsultationFeesDTO;
import com.dermacare.bookingService.dto.DoctorPushNotificationDTO;
import com.dermacare.bookingService.dto.DoctorSaveDetailsDTO;
import com.dermacare.bookingService.dto.PatientAndPriceInfo;
import com.dermacare.bookingService.dto.PatientInfo;
import com.dermacare.bookingService.dto.RelationInfoDTO;
import com.dermacare.bookingService.dto.ReportsDTO;
import com.dermacare.bookingService.dto.ReportsDtoList;
import com.dermacare.bookingService.dto.Session;
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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

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
	private NotificationFeign notificationFeign;

	@Autowired
	private ClinicAdminFeign clinicAdminFeign;

	@Autowired
	private geneateIds sequenceGeneratorService;

	@Autowired
	private S3Service s3Service;

	@Autowired
	private WhatsAppService whatsAppService;

	@Override
	public ResponseEntity<?> addService(BookingResponse request) {
		ResponseStructure<Booking> response = new ResponseStructure<>();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
					log.error("Failed to send follow-up notification for booking {} : {}",
							updatedBooking.getBookingId(), ex.getMessage());
				}
			}
			if (updatedBooking != null) {
				response = ResponseStructure.buildResponse(updatedBooking,
						"Last follow-up booking retrieved successfully", HttpStatus.CREATED,
						HttpStatus.CREATED.value());
			} else {
				response = ResponseStructure.buildResponse(null, "No follow-up bookings found", HttpStatus.BAD_REQUEST,
						HttpStatus.BAD_REQUEST.value());
			}
		} catch (Exception e) {
			log.error("addService failed : {}", e.getMessage(), e);
			response = ResponseStructure.buildResponse(null, "Exception occurred: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return ResponseEntity.status(response.getHttpStatus().value()).body(response);
	}

	private void nullifyLargeFields(Booking booking) {
		if (booking == null)
			return;
		booking.setReports(null);
		booking.setAttachments(null);
		booking.setConsentFormPdf(null);
		booking.setPrescriptionPdf(null);
	}

	private Booking toEntity(BookingRequset request) {
		Booking entity = null;
		try {
			entity = new ObjectMapper().convertValue(request, Booking.class);

			entity.setFollowupStatus("pending");
			String patientId = null;
			String customerId = null;
			Map<String, String> res = new LinkedHashMap<>();
			try {
				if (request.getCustomerId().isEmpty() || request.getPatientId().isEmpty()) {
					res = clinnicfeign.getCustomerByMobilenumberAndName(request.getMobileNumber(), request.getName());
					customerId = res.get("customerId");
					patientId = res.get("patientId");
				}
			} catch (Exception e) {
				log.warn("Lookup of customer by mobile/name failed : {}", e.getMessage());
			}
			if (request.getCustomerId().isEmpty()) {
				entity.setCustomerId(customerId);
			}
			if (request.getPatientId().isEmpty()) {
				entity.setPatientId(patientId);
			}
			entity.setConsultationType("First-Time");
			ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
			if (request.getTotalFee() != 0.0) {
				double due = request.getTotalFee() - request.getPartAmount();
				entity.setDueAmount(due);
				entity.setBookedAt(istTime.format(formatter));
			}
			entity.setFreeFollowUpsLeft(request.getFreeFollowUps());
			entity.setFollowupStatus("pending");
			if (request.getFreeFollowUps() != null && request.getFreeFollowUps() == 0) {
				entity.setIsFollowupStatus(true);
			}
			int days = 0;
			try {
				if (request.getConsultationExpiration() != null) {
					String consultationExp = request.getConsultationExpiration();
					days = Integer.parseInt(consultationExp.replaceAll("[^0-9]", ""));
				}

				LocalDate serviceDate = LocalDate.parse(request.getServiceDate());
				LocalDate expiryDate = serviceDate.plusDays(days);
				LocalDate today = LocalDate.now();

				if (!today.isAfter(expiryDate) && request.getFreeFollowUps() != null
						&& request.getFreeFollowUps() == 0) {
					entity.setIsFollowupStatus(true);
				} else if (today.isAfter(expiryDate)) {
					entity.setIsFollowupStatus(true);
				} else {
					entity.setIsFollowupStatus(false);
				}
			} catch (Exception e) {
				entity.setIsFollowupStatus(false);
			}

			String bookingId = sequenceGeneratorService.generateBookingId(request.getClinicName().substring(0, 3),
					request.getBranchname().substring(0, 3));
			entity.setBookingId(bookingId);

			if (request.getConsultationType() != null
					&& (request.getConsultationType().equalsIgnoreCase("video consultation")
							|| request.getConsultationType().equalsIgnoreCase("online consultation"))) {
				entity.setChannelId(randomNumber());
			}
			if (request.getFoc() != null && request.getPaymentType() != null) {
				if ("paid".equalsIgnoreCase(request.getFoc())
						&& "not paid".equalsIgnoreCase(request.getPaymentType())) {
					entity.setStatus("pending");
				} else if ("foc".equalsIgnoreCase(request.getFoc())
						&& "not paid".equalsIgnoreCase(request.getPaymentType())) {
					entity.setStatus("confirmed");
				} else {
					if ("paid".equalsIgnoreCase(request.getFoc()) && !request.getPaymentType().isEmpty()) {
						entity.setStatus("confirmed");
					}
				}
			}
			List<Status> status = new LinkedList<>();
			Status s = new Status();
			ZoneId zone = ZoneId.of("Asia/Kolkata");
			LocalDateTime dateTime = LocalDateTime.now(zone);
			s.setDATE_TIME(dateTime);
			s.setStatus(entity.getStatus());
			status.add(s);
			Collections.reverse(status);
			entity.setCurrentStatus(status);
			// ✅ FIXED: consultationFee is a boxed Double — guard against null before
			// unboxing to avoid an NPE when a request omits the consultation fee.
			if (request.getConsultationFee() != null && request.getConsultationFee() != 0.0) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.registerModule(new JavaTimeModule());
				mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
				List<ConsultationFees> lst = new LinkedList<>();
				ConsultationFees fee = new ConsultationFees();
				fee.setConsulationFee(request.getConsultationFee());
				fee.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
				lst.add(fee);
				Collections.reverse(lst);
				entity.setListOfConsultationFee(lst);
			}
			if (entity.getFollwupBookings() == null) {
				List<FollowupBooking> lst = new LinkedList<>();
				FollowupBooking followup = new FollowupBooking();
				followup.setDoctorId(entity.getDoctorId());
				followup.setDoctorName(entity.getDoctorName());
				followup.setServiceDate(entity.getServiceDate());
				followup.setServicetime(entity.getServicetime());
				followup.setStatus(entity.getStatus());
				followup.setVisitType(entity.getVisitType());
				lst.add(followup);
				entity.setFollwupBookings(lst);
			}
		} catch (Exception e) {
			log.error("toEntity mapping failed : {}", e.getMessage(), e);
		}
		return entity;
	}

	private BookingResponse toResponse(Booking entity) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		BookingResponse response = mapper.convertValue(entity, BookingResponse.class);
		response.setIsFollowupStatus(entity.getIsFollowupStatus());
		// ✅ FIXED: guard against null/empty listOfConsultationFee. A booking whose
		// consultation fee was never set (e.g. FOC bookings, or older documents
		// saved before the null-check above existed) previously threw an NPE /
		// IndexOutOfBoundsException here on the very first read.
		if (entity.getListOfConsultationFee() != null && !entity.getListOfConsultationFee().isEmpty()) {
			response.setConsultationFee(entity.getListOfConsultationFee().get(0).getConsulationFee());
		}

		String dto = getPrescriptionpdf(response.getBookingId());
		if (dto != null) {
			response.setPrescriptionPdf(Collections.singletonList(dto));
		}

		response.setBookingId(String.valueOf(entity.getBookingId()));

		try {
			if (entity.getPartImage() != null && !entity.getPartImage().isEmpty()) {
				String key = entity.getPartImage();
				response.setPartImageKey(key);
				response.setPartImage(s3Service.generateSignedUrl(entity.getPartImage()));
			}
		} catch (Exception e) {
			log.warn("partImage URL error for bookingId={} : {}", entity.getBookingId(), e.getMessage());
		}

		try {
			if (entity.getConsentFormPdf() != null && !entity.getConsentFormPdf().isEmpty()) {
				response.setConsentFormPdf(s3Service.generateSignedUrl(entity.getConsentFormPdf()));
			}
		} catch (Exception e) {
			log.warn("consentFormPdf URL error for bookingId={} : {}", entity.getBookingId(), e.getMessage());
		}

		try {
			if (entity.getAttachments() != null && !entity.getAttachments().isEmpty()) {
				List<String> signedUrls = entity.getAttachments().stream().map(key -> {
					try {
						return s3Service.generateSignedUrl(key);
					} catch (Exception ex) {
						return key;
					}
				}).collect(Collectors.toList());
				response.setAttachments(signedUrls);
			}
		} catch (Exception e) {
			log.warn("attachments URL error for bookingId={} : {}", entity.getBookingId(), e.getMessage());
		}

		try {
			if (response.getReports() != null) {
				for (com.dermacare.bookingService.dto.ReportsDtoList reportsDtoList : response.getReports()) {
					if (reportsDtoList.getReportsList() == null)
						continue;
					for (com.dermacare.bookingService.dto.ReportsDTO report : reportsDtoList.getReportsList()) {
						if (report.getReportFile() == null || report.getReportFile().isEmpty())
							continue;
						List<String> signedUrls = report.getReportFile().stream()
								.filter(key -> key != null && !key.isBlank()).map(key -> {
									try {
										return clinicAdminFeign.getSignedUrl(key);
									} catch (Exception ex) {
										log.warn("report sign error for bookingId={} key={} : {}",
												entity.getBookingId(), key, ex.getMessage());
										return key;
									}
								}).collect(Collectors.toList());
						report.setReportFile(signedUrls);
					}
				}
			}
		} catch (Exception e) {
			log.warn("reports URL signing error for bookingId={} : {}", entity.getBookingId(), e.getMessage());
		}

		return response;
	}

	private String getPrescriptionpdf(String bid) {
		try {
			String res = physioDoctorFeign.getByBookingId(bid);
			if (res != null && !res.isBlank()) {
				return s3Service.generateSignedUrl(res);
			}
			return null;
		} catch (Exception e) {
			log.warn("getPrescriptionpdf failed for bookingId={} : {}", bid, e.getMessage());
			return null;
		}
	}

	private static String randomNumber() {
		Random random = new Random();
		int sixDigitNumber = 100000 + random.nextInt(900000);
		return String.valueOf(sixDigitNumber);
	}

	/**
	 * ✅ FIXED: converts one Booking at a time instead of the whole list in a
	 * single convertValue call. Previously, if ANY single document in the list
	 * failed Jackson conversion (bad date, unexpected type, malformed nested
	 * object, etc.), the RuntimeException thrown here propagated all the way up
	 * to the controller uncaught, producing a 500 / Whitelabel error page for the
	 * ENTIRE request — even though the other 100+ documents were perfectly fine.
	 * Now a single bad document is logged and skipped; the caller still gets
	 * every other valid booking back.
	 */
	private List<BookingResponse> toResponses(List<Booking> bookings) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		List<BookingResponse> res = new ArrayList<>();

		if (bookings == null) {
			return res;
		}

		for (Booking booking : bookings) {

			BookingResponse bres;
			try {
				bres = mapper.convertValue(booking, BookingResponse.class);
			} catch (Exception e) {
				log.error("Skipping unmappable booking, bookingId={} : {}",
						booking != null ? booking.getBookingId() : "null", e.getMessage(), e);
				continue;
			}

			// ── partImage ───────────────────────────────────
			try {
				if (bres.getPartImage() != null && !bres.getPartImage().isEmpty()) {
					bres.setPartImage(s3Service.generateSignedUrl(bres.getPartImage()));
				}
			} catch (Exception e) {
				log.warn("partImage URL error for bookingId={} : {}", bres.getBookingId(), e.getMessage());
			}

			// ── consentFormPdf ──────────────────────────────
			try {
				if (bres.getConsentFormPdf() != null && !bres.getConsentFormPdf().isEmpty()) {
					bres.setConsentFormPdf(s3Service.generateSignedUrl(bres.getConsentFormPdf()));
				}
			} catch (Exception e) {
				log.warn("consentFormPdf URL error for bookingId={} : {}", bres.getBookingId(), e.getMessage());
			}

			// ── attachments ─────────────────────────────────
			try {
				if (bres.getAttachments() != null && !bres.getAttachments().isEmpty()) {
					List<String> signedUrls = bres.getAttachments().stream().map(key -> {
						try {
							return s3Service.generateSignedUrl(key);
						} catch (Exception ex) {
							return key;
						}
					}).collect(Collectors.toList());
					bres.setAttachments(signedUrls);
				}
			} catch (Exception e) {
				log.warn("attachments URL error for bookingId={} : {}", bres.getBookingId(), e.getMessage());
			}

			// ── reports — sign raw S3 keys via Clinic Admin Feign ──
			try {
				if (bres.getReports() != null) {
					for (ReportsDtoList reportsDtoList : bres.getReports()) {
						if (reportsDtoList.getReportsList() == null)
							continue;
						for (ReportsDTO report : reportsDtoList.getReportsList()) {
							if (report.getReportFile() == null || report.getReportFile().isEmpty())
								continue;
							List<String> signedUrls = report.getReportFile().stream()
									.filter(key -> key != null && !key.isBlank()).map(key -> {
										try {
											return clinicAdminFeign.getSignedUrl(key);
										} catch (Exception ex) {
											log.warn("report sign error for bookingId={} key={} : {}",
													bres.getBookingId(), key, ex.getMessage());
											return key;
										}
									}).collect(Collectors.toList());
							report.setReportFile(signedUrls);
						}
					}
				}
			} catch (Exception e) {
				log.warn("reports URL signing error for bookingId={} : {}", bres.getBookingId(), e.getMessage());
			}

			// ── prescriptionPdf ─────────────────────────────
			try {
				String dto = getPrescriptionpdf(bres.getBookingId());
				if (dto != null) {
					bres.setPrescriptionPdf(Collections.singletonList(dto));
				}
			} catch (Exception e) {
				log.warn("prescriptionPdf error for bookingId={} : {}", bres.getBookingId(), e.getMessage());
			}

			res.add(bres);
		}

		return res;
	}

	@Override
	public ResponseEntity<?> physioAppointment(BookingRequset request) {

		Response res = new Response();

		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

			if (request.getFreeFollowUps() == null) {
				throw new RuntimeException("Free FollowUps is mandatory");
			}
			if (request.getClinicId() == null || request.getClinicId().trim().isEmpty()) {
				throw new RuntimeException("Clinic Id is mandatory");
			}
			if (request.getBranchId() == null || request.getBranchId().trim().isEmpty()) {
				throw new RuntimeException("Branch Id is mandatory");
			}
			if (request.getDoctorId() == null || request.getDoctorId().trim().isEmpty()) {
				throw new RuntimeException("Doctor Id is mandatory");
			}
			if (request.getServiceDate() == null || request.getServiceDate().trim().isEmpty()) {
				throw new RuntimeException("Service Date is mandatory");
			}
			if (request.getServicetime() == null || request.getServicetime().trim().isEmpty()) {
				throw new RuntimeException("Service Time is mandatory");
			}
			if (request.getConsultationExpiration() == null || request.getConsultationExpiration().trim().isEmpty()) {
				throw new RuntimeException("Consultation Expiration is mandatory");
			}

			boolean hasPatientMobile = request.getPatientMobileNumber() != null
					&& !request.getPatientMobileNumber().trim().isEmpty();
			boolean hasMobile = request.getMobileNumber() != null && !request.getMobileNumber().trim().isEmpty();

			if (!hasPatientMobile && !hasMobile) {
				throw new RuntimeException("Patient Mobile Number or Mobile Number is mandatory");
			}

			Booking entity = toEntity(request);
			Booking updatedBooking = repository.save(entity);

			if (updatedBooking == null) {
				throw new RuntimeException("Unable to save appointment");
			}

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
				log.error("Failed to send doctor push notification for booking {} : {}", updatedBooking.getBookingId(),
						ex.getMessage());
			}

			int notificationStatus = 0;
			try {
				Response notificationResponse = notificationFeign
						.createNotification(mapper.convertValue(updatedBooking, BookingResponse.class)).getBody();
				if (notificationResponse != null) {
					notificationStatus = notificationResponse.getStatus();
				}
			} catch (Exception e) {
				log.warn("Notification service failed for booking {} : {}", updatedBooking.getBookingId(),
						e.getMessage());
			}

			try {
				request.setBookingId(updatedBooking.getBookingId());
				request.setClinicId(updatedBooking.getClinicId());
				request.setBranchId(updatedBooking.getBranchId());

				whatsAppService.sendBookingConfirmation(request);
				log.info("WhatsApp sent successfully for booking {}", updatedBooking.getBookingId());
			} catch (Exception e) {
				log.warn("WhatsApp notification failed for booking {} : {}", updatedBooking.getBookingId(),
						e.getMessage());
			}

			res.setStatus(200);
			res.setSuccess(true);
			if (notificationStatus == 200) {
				res.setMessage("Appointment Booked Successfully and notification sent");
			} else {
				res.setMessage("Appointment Booked Successfully but Notification not sent");
			}
			return ResponseEntity.ok(res);

		} catch (Exception e) {
			log.error("Appointment booking failed : {}", e.getMessage(), e);
			res.setStatus(500);
			res.setSuccess(false);
			res.setMessage(e.getMessage());
			return ResponseEntity.status(500).body(res);
		}
	}

	public ResponseEntity<?> getAppointsByPatientId(String patientId) {
		ResponseStructure<List<Map<String, Object>>> res = new ResponseStructure<List<Map<String, Object>>>();
		List<Map<String, Object>> list = new ArrayList<>();
		try {
			List<Booking> existingBooking = repository.findByPatientId(patientId);
			if (existingBooking != null && !existingBooking.isEmpty()) {
				List<BookingResponse> respnse = toResponses(existingBooking);
				respnse.forEach(n -> {
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
				res.setData(list);
				res.setMessage("Appointments Are Found");
				return ResponseEntity.status(200).body(res);
			} else {
				res.setStatusCode(200);
				res.setMessage("Appointments Are Not Found");
				return ResponseEntity.status(200).body(res);
			}
		} catch (Exception e) {
			log.error("getAppointsByPatientId failed for patientId={} : {}", patientId, e.getMessage(), e);
			res.setStatusCode(500);
			res.setMessage(e.getMessage());
			return ResponseEntity.status(500).body(res);
		}
	}

	public ResponseEntity<?> getAppointsByInput(String input) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
		try {
			List<Booking> existingBooking = repository.findByNameIgnoreCaseOrBookingIdOrPatientId(input);
			if (existingBooking != null && !existingBooking.isEmpty()) {
				List<BookingResponse> respnse = toResponses(existingBooking);
				res.setStatusCode(200);
				res.setData(respnse);
				res.setMessage("Appointments Are Found");
				return ResponseEntity.status(200).body(res);
			} else {
				res.setStatusCode(200);
				res.setMessage("Appointments Are Not Found");
				return ResponseEntity.status(200).body(res);
			}
		} catch (Exception e) {
			log.error("getAppointsByInput failed for input={} : {}", input, e.getMessage(), e);
			res.setStatusCode(500);
			res.setMessage(e.getMessage());
			return ResponseEntity.status(500).body(res);
		}
	}

	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(String clinicId, String doctorId) {
		List<Map<String, Object>> list = new ArrayList<>();
		ResponseStructure<List<Map<String, Object>>> res = new ResponseStructure<>();
		List<BookingResponse> responseList = new ArrayList<>();
		try {
			List<Booking> existingBookings = repository.findByClinicIdAndDoctorId(clinicId, doctorId);
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			LocalDate currentDate = LocalDate.now();

			if (!existingBookings.isEmpty()) {
				for (Booking b : existingBookings) {
					if (b.getServiceDate() != null && b.getStatus() != null) {
						LocalDate bookingDate = LocalDate.parse(b.getServiceDate(), dateFormatter);
						if (bookingDate.equals(currentDate)) {
							if (b.getStatus().equalsIgnoreCase("Confirmed") || b.getStatus().equalsIgnoreCase("pending")) {
								BookingResponse temp = toResponse(b);
								responseList.add(temp);
							}
						}
					}
				}
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
					map.put("consultationFee", n.getConsultationFee());
					map.put("freeFollowUpsLeft", n.getFreeFollowUpsLeft());
					map.put("freeFollowUps", n.getFreeFollowUps());
					list.add(map);
				});
				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setData(list);
				res.setMessage(!list.isEmpty() ? "Today's Appointments Found" : "No Appointments for Today");
			} else {
				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setMessage("Appointments Not Found");
			}
		} catch (Exception e) {
			log.error("getTodayDoctorAppointmentsByDoctorId failed for clinicId={} doctorId={} : {}", clinicId,
					doctorId, e.getMessage(), e);
			res.setStatusCode(500);
			res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			res.setMessage("Error occurred : " + e.getMessage());
		}
		return ResponseEntity.status(res.getStatusCode()).body(res);
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
				case "1":
					add = "Confirmed".equalsIgnoreCase(booking.getStatus()) && appointmentDate.isAfter(today);
					break;
				case "2":
					add = "Online Consultation".equalsIgnoreCase(booking.getConsultationType())
							&& "Confirmed".equalsIgnoreCase(booking.getStatus()) && appointmentDate.isAfter(today);
					break;
				case "3":
					add = "Completed".equalsIgnoreCase(booking.getStatus());
					break;
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
			log.error("filterDoctorAppointmentsByDoctorId failed for hospitalId={} doctorId={} : {}", hospitalId,
					doctorId, e.getMessage(), e);
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
			log.error("getCompletedApntsByDoctorId failed : {}", e.getMessage(), e);
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
			log.error("getSizeOfConsultationTypesByDoctorId failed : {}", e.getMessage(), e);
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
					lst = physioDoctorFeign.getPhysioByBookingId(res.getBookingId(), res.getServiceDate()).getBody();
					res.setSession(lst);
				} catch (Exception e) {
					log.warn("Session fetch failed for bookingId={} : {}", bookingId, e.getMessage());
				}
				return res;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.warn("getBookedService failed for bookingId={} : {}", bookingId, e.getMessage());
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
					log.warn("Failed to clear reports for bookingId={} : {}", bookingId, e.getMessage());
				}
			} else {
				if (entity != null && index != null) {
					entity.getReports().remove(Integer.valueOf(index).intValue());
					repository.save(entity);
				}
			}
		} catch (Exception e) {
			log.warn("deleteBookedServiceReports failed for bookingId={} : {}", bookingId, e.getMessage());
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
		try {
			List<Booking> bookings = repository.findByMobileNumber(mobileNumber);
			if (bookings == null || bookings.isEmpty()) {
				return Collections.emptyList();
			}
			List<Booking> reversedBookings = new ArrayList<>();
			for (int i = bookings.size() - 1; i >= 0; i--) {
				reversedBookings.add(bookings.get(i));
			}
			return toResponses(reversedBookings);
		} catch (Exception e) {
			log.error("getBookedServices failed for mobileNumber={} : {}", mobileNumber, e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<BookingResponse> getAllBookedServices() {
		try {
			List<Booking> bookings = repository.findAll();
			if (bookings == null || bookings.isEmpty()) {
				return Collections.emptyList();
			}
			List<Booking> reversedBookings = new ArrayList<>();
			for (int i = bookings.size() - 1; i >= 0; i--) {
				reversedBookings.add(bookings.get(i));
			}
			return toResponses(reversedBookings);
		} catch (Exception e) {
			log.error("getAllBookedServices failed : {}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<BookingResponse> bookingByDoctorId(String doctorId) {
		try {
			List<Booking> bookings = repository.findByDoctorId(doctorId);
			if (bookings == null || bookings.isEmpty()) {
				return Collections.emptyList();
			}
			List<Booking> reversedBookings = new ArrayList<>();
			for (int i = bookings.size() - 1; i >= 0; i--) {
				reversedBookings.add(bookings.get(i));
			}
			return toResponses(reversedBookings);
		} catch (Exception e) {
			log.error("bookingByDoctorId failed for doctorId={} : {}", doctorId, e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	/**
	 * ✅ FIXED: wrapped in try/catch. Previously an unmappable document or a
	 * downstream exception in toResponses() would propagate uncaught straight to
	 * the controller and surface as a raw Whitelabel 500 page.
	 */
	@Override
	public List<Map<String, Object>> bookingByCustomerId(String customerId) {
		try {
			List<Booking> bookings = repository.findByCustomerId(customerId);

			if (bookings == null || bookings.isEmpty()) {
				return Collections.emptyList();
			}

			bookings = bookings.stream().filter(booking -> !"COMPLETED".equalsIgnoreCase(booking.getStatus()))
					.toList();

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
		} catch (Exception e) {
			log.error("bookingByCustomerId failed for customerId={} : {}", customerId, e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	/**
	 * ✅ FIXED: same guard as bookingByCustomerId above.
	 */
	@Override
	public List<Map<String, Object>> CompletedbookingByCustomerId(String customerId) {
		try {
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
		} catch (Exception e) {
			log.error("CompletedbookingByCustomerId failed for customerId={} : {}", customerId, e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<BookingResponse> bookingByPatientId(String patientId) {
		try {
			List<Booking> bookings = repository.findByPatientId(patientId);
			if (bookings == null || bookings.isEmpty()) {
				return Collections.emptyList();
			}
			List<Booking> reversedBookings = new ArrayList<>();
			for (int i = bookings.size() - 1; i >= 0; i--) {
				reversedBookings.add(bookings.get(i));
			}
			return toResponses(reversedBookings);
		} catch (Exception e) {
			log.error("bookingByPatientId failed for patientId={} : {}", patientId, e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<BookingResponse> bookingByPatientIdAndBookingId(String patientId, String bookingId) {
		try {
			List<Booking> bookings = repository.findByPatientIdAndBookingId(patientId, bookingId);
			if (bookings == null || bookings.isEmpty()) {
				return Collections.emptyList();
			}
			List<Booking> reversedBookings = new ArrayList<>();
			for (int i = bookings.size() - 1; i >= 0; i--) {
				if (bookings.get(i).getStatus().equalsIgnoreCase("In-Progress")) {
					reversedBookings.add(bookings.get(i));
				}
			}
			return toResponses(reversedBookings);
		} catch (Exception e) {
			log.error("bookingByPatientIdAndBookingId failed for patientId={} bookingId={} : {}", patientId, bookingId,
					e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<ReportsDTO> getReportsByPatientId(String patientId) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		List<ReportsDTO> responseList = new ArrayList<>();
		try {
			List<Booking> bookings = repository.findByPatientId(patientId);
			for (Booking booking : bookings) {
				if (booking.getReports() != null) {
					for (ReportsList report : booking.getReports()) {
						for (Reports reportEntity : report.getReportsList()) {
							try {
								ReportsDTO dto = mapper.convertValue(reportEntity, ReportsDTO.class);
								responseList.add(dto);
							} catch (Exception e) {
								log.warn("Skipping unmappable report for bookingId={} : {}", booking.getBookingId(),
										e.getMessage());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("getReportsByPatientId failed for patientId={} : {}", patientId, e.getMessage(), e);
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
		try {
			List<Booking> bookings = repository.findByClinicId(clinicId);
			if (bookings == null || bookings.isEmpty()) {
				return Collections.emptyList();
			}
			return toResponses(bookings);
		} catch (Exception e) {
			log.error("bookingByClinicId failed for clinicId={} : {}", clinicId, e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Scheduled(fixedRate = 60 * 60 * 1000)
	public void autoCalculatePatientCompletedAppointments() {
		Map<String, Integer> map = new LinkedHashMap<>();
		Set<String> ids = new LinkedHashSet<>();
		try {
			List<Booking> existingBooking = repository.findAll();
			if (existingBooking != null && !existingBooking.isEmpty()) {
				for (Booking b : existingBooking) {
					if (b.getStatus().equalsIgnoreCase("Completed")) {
						if (!ids.contains(b.getPatientId())) {
							List<Booking> bookings = repository.findByPatientId(b.getPatientId());
							ids.add(b.getPatientId());
							for (Booking c : bookings) {
								if (c.getStatus().equalsIgnoreCase("Completed")) {
									if (map.containsKey(b.getPatientId())) {
										Integer value = map.get(b.getPatientId());
										int vlue = value.intValue();
										vlue += 1;
										value = Integer.valueOf(vlue);
										map.put(b.getPatientId(), value);
									} else {
										map.put(b.getPatientId(), 1);
									}
									for (String key : map.keySet()) {
										List<Booking> bkings = repository.findByPatientId(key);
										for (Booking bkng : bkings) {
											bkng.setVisitCount(map.get(key));
											repository.save(bkng);
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("autoCalculatePatientCompletedAppointments failed : {}", e.getMessage(), e);
		}
	}

	@Override
	public Response getPatientDetailsForConsetForm(String bookingId, String patientId, String mobileNumber) {
		try {
			Optional<Booking> optionalBooking = repository.findByBookingIdAndPatientIdAndMobileNumber(bookingId,
					patientId, mobileNumber);
			if (optionalBooking.isPresent()) {
				Booking booking = optionalBooking.get();
				if (booking.getStatus().equalsIgnoreCase("Confirmed")
						|| booking.getStatus().equalsIgnoreCase("Completed")) {
					BookingResponse response = new ObjectMapper().convertValue(booking, BookingResponse.class);
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
			log.error("getPatientDetailsForConsetForm failed for bookingId={} : {}", bookingId, e.getMessage(), e);
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
				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setData(response);
				res.setMessage(!response.isEmpty() ? "In-Progress appointments found"
						: "In-Progress appointments not found");
			} else {
				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setData(response);
				res.setMessage("In-Progress appointments not found");
			}
		} catch (Exception e) {
			log.error("getInProgressAppointments failed for number={} : {}", number, e.getMessage(), e);
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
				BookingResponse response;
				try {
					response = new ObjectMapper().convertValue(booking, BookingResponse.class);
				} catch (Exception e) {
					log.warn("Skipping unmappable booking, bookingId={} : {}", booking.getBookingId(), e.getMessage());
					return null;
				}
				String pdf = getPrescriptionpdf(response.getBookingId());
				if (pdf != null) {
					response.setPrescriptionPdf(Collections.singletonList(pdf));
				}
				return response;
			}).filter(java.util.Objects::nonNull).toList();

			return ResponseEntity.ok(ResponseStructure.buildResponse(bookingResponses, "In-Progress appointments found",
					HttpStatus.OK, HttpStatus.OK.value()));

		} catch (Exception e) {
			log.error("getInProgressAppointmentsByCustomerId failed for customerId={} : {}", customerId,
					e.getMessage(), e);
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
						BookingResponse response;
						try {
							response = new ObjectMapper().convertValue(booking, BookingResponse.class);
						} catch (Exception e) {
							log.warn("Skipping unmappable booking, bookingId={} : {}", booking.getBookingId(),
									e.getMessage());
							return null;
						}
						String pdf = getPrescriptionpdf(booking.getBookingId());
						if (pdf != null) {
							response.setPrescriptionPdf(Collections.singletonList(pdf));
						}
						return response;
					}).filter(java.util.Objects::nonNull).toList();

			if (bookingResponses.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseStructure.buildResponse(null,
						"No In-Progress appointments found for this patient", HttpStatus.NOT_FOUND, 404));
			}

			return ResponseEntity.ok(ResponseStructure.buildResponse(bookingResponses, "In-Progress appointments found",
					HttpStatus.OK, HttpStatus.OK.value()));

		} catch (Exception e) {
			log.error("getInProgressAppointmentsByPatientId failed for patientId={} clinicId={} : {}", patientId,
					clinicId, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseStructure.buildResponse(null,
					"Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, 500));
		}
	}

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
					log.warn("inprogressAppointmentsByConsultationExpiration (followUp branch) failed : {}",
							e.getMessage());
				}
			} else {
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
						log.warn("inprogressAppointmentsByConsultationExpiration (expiration branch) failed : {}",
								e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			log.error("inprogressAppointmentsByConsultationExpiration failed : {}", e.getMessage(), e);
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
				response.forEach(n -> {
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
				});
				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setData(list);
				res.setMessage(!response.isEmpty() ? "appointments found" : "appointments not found");
			}
		} catch (Exception e) {
			log.error("getDoctorFutureAppointments failed for doctorId={} : {}", doctorId, e.getMessage(), e);
			res.setStatusCode(500);
			res.setMessage(e.getMessage());
		}
		return ResponseEntity.status(res.getStatusCode()).body(res);
	}

	@Override
	public List<BookingResponse> bookingByBranchId(String branchId) {
		try {
			List<Booking> bookings = repository.findByBranchId(branchId);
			if (bookings == null || bookings.isEmpty()) {
				return Collections.emptyList();
			}
			List<Booking> reversedBookings = new ArrayList<>();
			for (int i = bookings.size() - 1; i >= 0; i--) {
				reversedBookings.add(bookings.get(i));
			}
			return toResponses(reversedBookings);
		} catch (Exception e) {
			log.error("bookingByBranchId failed for branchId={} : {}", branchId, e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<Map<String, Object>> getBookedServicesByClinicIdWithBranchId(String clinicId, String branchId) {
		try {
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
				map.put("totalFee", n.getTotalFee());
				list.add(map);
			});

			return list;
		} catch (Exception e) {
			log.error("getBookedServicesByClinicIdWithBranchId failed for clinicId={} branchId={} : {}", clinicId,
					branchId, e.getMessage(), e);
			return new ArrayList<>();
		}
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
					reversedBookings.forEach(n -> {
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
					});
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
					reversedBookings.forEach(n -> {
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
					});
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
			log.error(
					"getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus failed for clinicId={} branchId={} doctorId={} status={} : {}",
					clinicId, branchId, doctorId, status, e.getMessage(), e);
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
			log.error("retrieveOneWeekAppointments failed for clinicId={} branchId={} : {}", clinicId, branchId,
					e.getMessage(), e);
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
			log.error("retrieveAppointments failed for clinicId={} branchId={} date={} : {}", cinicId, branchId, date,
					e.getMessage(), e);
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

			ObjectMapper mapper = new ObjectMapper();

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

			if (dto.getFreeFollowUpsLeft() != null)
				entity.setFreeFollowUpsLeft(dto.getFreeFollowUpsLeft());
			if (dto.getFreeFollowUps() != null)
				entity.setFreeFollowUps(dto.getFreeFollowUps());
			if (dto.getFollowupDate() != null && !dto.getFollowupDate().isEmpty())
				entity.setFollowupDate(dto.getFollowupDate());
			if (dto.getFollowupStatus() != null) {
				if (dto.getFollowupStatus().equalsIgnoreCase("Completed")) {
					entity.setStatus("Completed");
				}
				entity.setFollowupStatus(dto.getFollowupStatus());
			}

			if (dto.getProblem() != null && !dto.getProblem().isEmpty())
				entity.setProblem(dto.getProblem());
			if (dto.getSymptomsDuration() != null && !dto.getSymptomsDuration().isEmpty())
				entity.setSymptomsDuration(dto.getSymptomsDuration());

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

			if (dto.getDoctorId() != null && !dto.getDoctorId().isEmpty())
				entity.setDoctorId(dto.getDoctorId());
			if (dto.getDoctorName() != null && !dto.getDoctorName().isEmpty())
				entity.setDoctorName(dto.getDoctorName());
			if (dto.getDoctorMobileDeviceId() != null && !dto.getDoctorMobileDeviceId().isEmpty())
				entity.setDoctorDeviceId(dto.getDoctorMobileDeviceId());
			if (dto.getDoctorWebDeviceId() != null && !dto.getDoctorWebDeviceId().isEmpty())
				entity.setDoctorWebDeviceId(dto.getDoctorWebDeviceId());

			if (dto.getServiceDate() != null && !dto.getServiceDate().isEmpty())
				entity.setServiceDate(dto.getServiceDate());
			if (dto.getServicetime() != null && !dto.getServicetime().isEmpty())
				entity.setServicetime(dto.getServicetime());
			if (dto.getConsultationType() != null && !dto.getConsultationType().isEmpty())
				entity.setConsultationType(dto.getConsultationType());

			// ✅ FIXED: consultationFee is a boxed Double — guard against null before
			// unboxing to avoid an NPE when the incoming dto omits the fee.
			if (dto.getConsultationFee() != null && dto.getConsultationFee() != 0.0) {
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

			if (dto.getAttachments() != null && !dto.getAttachments().isEmpty())
				entity.setAttachments(dto.getAttachments());
			if (dto.getConsentFormPdf() != null && !dto.getConsentFormPdf().isEmpty())
				entity.setConsentFormPdf(dto.getConsentFormPdf());
			if (dto.getPrescriptionPdf() != null && !dto.getPrescriptionPdf().isEmpty())
				entity.setPrescriptionPdf(dto.getPrescriptionPdf());

			if (dto.getPaymentType() != null && !dto.getPaymentType().isEmpty())
				entity.setPaymentType(dto.getPaymentType());
			if (dto.getPaymentStatus() != null && !dto.getPaymentStatus().isEmpty())
				entity.setPaymentStatus(dto.getPaymentStatus());
			if (dto.getTotalFee() > 0)
				entity.setTotalFee(dto.getTotalFee());
			if (dto.getDoctorRefCode() != null && !dto.getDoctorRefCode().isEmpty())
				entity.setDoctorRefCode(dto.getDoctorRefCode());

			if (dto.getBodyPartId() != null && !dto.getBodyPartId().isEmpty())
				entity.setBodyPartId(dto.getBodyPartId());
			if (dto.getBodyPartName() != null && !dto.getBodyPartName().isEmpty())
				entity.setBodyPartName(dto.getBodyPartName());
			if (dto.getPartImage() != null && !dto.getPartImage().isEmpty())
				entity.setPartImage(dto.getPartImage());

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

			if (dto.getReferredByType() != null && !dto.getReferredByType().isEmpty())
				entity.setReferredByType(dto.getReferredByType());
			if (dto.getReferredByName() != null && !dto.getReferredByName().isEmpty())
				entity.setReferredByName(dto.getReferredByName());

			if (dto.getPreviousInjuries() != null && !dto.getPreviousInjuries().isEmpty())
				entity.setPreviousInjuries(dto.getPreviousInjuries());
			if (dto.getCurrentMedications() != null && !dto.getCurrentMedications().isEmpty())
				entity.setCurrentMedications(dto.getCurrentMedications());
			if (dto.getAllergies() != null && !dto.getAllergies().isEmpty())
				entity.setAllergies(dto.getAllergies());
			if (dto.getOccupation() != null && !dto.getOccupation().isEmpty())
				entity.setOccupation(dto.getOccupation());

			if (dto.getInsuranceProvider() != null && !dto.getInsuranceProvider().isEmpty())
				entity.setInsuranceProvider(dto.getInsuranceProvider());
			if (dto.getPolicyNumber() != null && !dto.getPolicyNumber().isEmpty())
				entity.setPolicyNumber(dto.getPolicyNumber());

			if (dto.getActivityLevels() != null && !dto.getActivityLevels().isEmpty())
				entity.setActivityLevels(dto.getActivityLevels());

			if (dto.getFoc() != null)
				entity.setFoc(dto.getFoc());

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
			log.error("updateAppointmentBasedOnBookingId failed for bookingId={} : {}", dto.getBookingId(),
					e.getMessage(), e);
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
					}, Collectors.toList()), list -> list.stream().distinct().collect(Collectors.toList()))));
			res.setStatusCode(200);
			res.setHttpStatus(HttpStatus.OK);
			res.setData(data);
			res.setMessage("Relations found successfully");
		} catch (Exception e) {
			log.error("getRelationsByCustomerId failed for customerId={} : {}", customerId, e.getMessage(), e);
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

					// ✅ FIXED: guard against null/empty listOfConsultationFee. A booking with
					// no consultation fee recorded previously threw NPE / IndexOutOfBounds
					// here, which would abort the entire summary computation for the range.
					if (booking.getListOfConsultationFee() != null && !booking.getListOfConsultationFee().isEmpty()) {
						totalConsultation += booking.getListOfConsultationFee().get(0).getConsulationFee();
					}
					totalTherapy += booking.getTotalFee();
					totalDue += booking.getDueAmount();
				}
			}

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
			log.error("getPatientAndPriceInfo failed for clinicId={} branchId={} : {}", clinicId, branchId,
					e.getMessage(), e);
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
			if (b != null && !b.isEmpty()) {
				List<BookingResponse> dto = toResponses(b);
				dto.forEach(n -> {
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
				});
				return list;
			} else {
				return Collections.emptyList();
			}
		} catch (Exception e) {
			log.error("getTodayBookings failed for clinicId={} branchId={} : {}", cId, bId, e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	public ResponseEntity<Response> getTodayAllBookings(String clinicId, String branchId) {
		try {
			String today = LocalDate.now().format(FORMATTER);
			List<Map<String, Object>> responseList = new ArrayList<>();

			List<Booking> bookings = repository.findByClinicIdAndBranchIdAndServiceDate(clinicId, branchId, today);
			List<String> followupIds = physioDoctorFeign.getTodayFollowUpBookingIds();
			List<Booking> followupBookings = new ArrayList<>();

			if (followupIds != null && !followupIds.isEmpty()) {
				followupBookings = repository.findByBookingIdInAndClinicIdAndBranchId(followupIds, clinicId, branchId);

				if (!followupBookings.isEmpty()) {
					followupBookings.forEach(b -> {
						b.setStatus("follow-up");
						List<Status> statusList = b.getCurrentStatus();
						if (statusList == null) {
							statusList = new ArrayList<>();
						}
						Status status = new Status();
						status.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
						status.setStatus("follow-up");
						statusList.add(status);
						b.setCurrentStatus(statusList);
					});
					repository.saveAll(followupBookings);
				}
			}

			List<BookingResponse> bookingResponses = new ArrayList<>();
			if (!bookings.isEmpty()) {
				bookingResponses.addAll(toResponses(bookings));
			}
			if (!followupBookings.isEmpty()) {
				bookingResponses.addAll(toResponses(followupBookings));
			}

			bookingResponses = bookingResponses.stream().collect(Collectors.toMap(BookingResponse::getBookingId,
					Function.identity(), (oldValue, newValue) -> oldValue, LinkedHashMap::new)).values().stream()
					.toList();

			for (BookingResponse booking : bookingResponses) {
				try {
					ResponseEntity<List<Session>> sessionResponse = physioDoctorFeign
							.getPhysioByBookingId(booking.getBookingId(), booking.getServiceDate());
					List<Session> sessions = sessionResponse != null ? sessionResponse.getBody() : null;
					if (sessions != null && !sessions.isEmpty()) {
						booking.setSession(sessions);
						booking.setVisitType("session");
					} else {
						booking.setSession(null);
					}
				} catch (Exception ex) {
					log.warn("Session fetch failed for BookingId={} : {}", booking.getBookingId(), ex.getMessage());
				}
			}

			for (BookingResponse n : bookingResponses) {
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

			long totalCount = bookingResponses.size();
			long pendingCount = bookingResponses.stream()
					.filter(b -> "PENDING".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();
			long confirmedCount = bookingResponses.stream()
					.filter(b -> "CONFIRMED".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();
			long inProgressCount = bookingResponses.stream()
					.filter(b -> "IN-PROGRESS".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			Map<String, Object> summary = new HashMap<>();
			summary.put("totalAppointments", totalCount);
			summary.put("pending", pendingCount);
			summary.put("confirmed", confirmedCount);
			summary.put("inProgress", inProgressCount);

			if (bookingResponses.isEmpty()) {
				return ResponseEntity
						.ok(new Response(true, Collections.emptyList(), summary, "No bookings found", 200, null, null));
			}

			return ResponseEntity
					.ok(new Response(true, responseList, summary, "Today bookings fetched", 200, null, null));

		} catch (Exception e) {
			log.error("getTodayAllBookings failed for clinicId={} branchId={} : {}", clinicId, branchId,
					e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Response(false, null, null,
					"Error fetching today bookings : " + e.getMessage(), 500, null, null));
		}
	}

	@Override
	public ResponseEntity<Response> getFilteredBookingsByStatus(String clinicId, String branchId) {
		try {
			List<Booking> bookings = repository.findByClinicIdAndBranchId(clinicId, branchId);

			List<BookingResponse> bookingResponses = bookings != null && !bookings.isEmpty() ? toResponses(bookings)
					: Collections.emptyList();

			List<Map<String, Object>> filteredBookings = new ArrayList<>();

			long inProgressCount = 0;
			long completedCount = 0;
			long dueForInvestigationCount = 0;
			long investigationDoneCount = 0;

			for (BookingResponse booking : bookingResponses) {
				String followupStatus = booking.getStatus();
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
						booking.getPatientMobileNumber() != null && !booking.getPatientMobileNumber().isEmpty()
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
				return ResponseEntity
						.ok(new Response(true, Collections.emptyList(), summary, "No bookings found", 200, null, null));
			}

			return ResponseEntity.ok(
					new Response(true, filteredBookings, summary, "Bookings fetched successfully", 200, null, null));

		} catch (Exception e) {
			log.error("Error while fetching filtered bookings. clinicId={}, branchId={}, error={}", clinicId, branchId,
					e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					new Response(false, null, null, "Error fetching bookings : " + e.getMessage(), 500, null, null));
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
					if (lst != null && !lst.isEmpty()) {
						n.setSession(lst);
						n.setVisitType("session");
					} else {
						n.setSession(null);
					}
					return n;
				}).toList();

				res.forEach(n -> {
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
				});
			} catch (Exception e) {
				log.warn("Error while fetching session details for upcoming bookings : {}", e.getMessage());
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
			log.error("getUpcomingBookings failed for clinicId={} branchId={} option={} : {}", clinicId, branchId,
					option, e.getMessage(), e);
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
					if (lst != null) {
						n.setSession(lst);
						n.setVisitType("session");
					} else {
						n.setSession(null);
					}
					return n;
				}).toList();
			} catch (Exception e) {
				log.warn("Error while fetching session details for getBookingByDate : {}", e.getMessage());
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
			log.error("getBookingByDate failed for clinicId={} branchId={} date={} : {}", clinicId, branchId, date,
					e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					new Response(false, null, null, "Error fetching bookings: " + e.getMessage(), 500, null, null));
		}
	}

	@Override
	public ResponseEntity<Response> getBookingByCustomRange(String clinicId, String branchId, String start,
			String end) {
		List<Map<String, Object>> list = new ArrayList<>();
		try {
			log.info("Fetching bookings for clinicId: {}, branchId: {}, start: {}, end: {}", clinicId, branchId, start,
					end);

			LocalDate startDate = LocalDate.parse(start);
			LocalDate endDate = LocalDate.parse(end);

			String fromDate = startDate.minusDays(1).format(FORMATTER);
			String toDate = endDate.plusDays(1).format(FORMATTER);

			List<Booking> bookings = repository.findByClinicIdAndBranchIdAndServiceDateBetween(clinicId, branchId,
					fromDate, toDate);

			log.info("Found {} bookings", bookings != null ? bookings.size() : 0);

			List<BookingResponse> responses = new ArrayList<>();
			if (bookings != null && !bookings.isEmpty()) {
				responses.addAll(toResponses(bookings));
			}

			responses = responses.stream().map(response -> {
				try {
					ResponseEntity<List<Session>> sessionResponse = physioDoctorFeign
							.getPhysioByBookingId(response.getBookingId(), response.getServiceDate());
					List<Session> sessions = sessionResponse != null ? sessionResponse.getBody() : null;
					if (sessions != null && !sessions.isEmpty()) {
						response.setSession(sessions);
						response.setVisitType("session");
					}
				} catch (Exception ex) {
					log.error("Error fetching sessions for bookingId: {}", response.getBookingId(), ex);
				}
				return response;
			}).collect(Collectors.toCollection(ArrayList::new));

			List<String> bookingIds = physioDoctorFeign.getPhysioRecordsByFollowUpDateRange(clinicId, branchId, start,
					end);

			log.info("Found {} follow-up booking ids", bookingIds != null ? bookingIds.size() : 0);

			if (bookingIds != null && !bookingIds.isEmpty()) {
				List<Booking> followUpBookings = repository.findByBookingIdIn(bookingIds);

				log.info("Found {} follow-up bookings", followUpBookings != null ? followUpBookings.size() : 0);

				if (followUpBookings != null && !followUpBookings.isEmpty()) {
					followUpBookings.forEach(booking -> {
						booking.setStatus("follow-up");

						List<Status> statusList = booking.getCurrentStatus() == null ? new ArrayList<>()
								: new ArrayList<>(booking.getCurrentStatus());

						boolean alreadyExists = statusList.stream()
								.anyMatch(status -> "follow-up".equalsIgnoreCase(status.getStatus()));

						if (!alreadyExists) {
							Status status = new Status();
							status.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
							status.setStatus("follow-up");
							statusList.add(status);
						}

						booking.setCurrentStatus(statusList);
					});

					repository.saveAll(followUpBookings);

					responses.addAll(followUpBookings.stream().map(this::toResponse).collect(Collectors.toList()));
				}
			}

			for (BookingResponse response : responses) {
				Map<String, Object> map = new LinkedHashMap<>();
				map.put("bookingId", response.getBookingId());
				map.put("serviceDate", response.getServiceDate());
				map.put("servicetime", response.getServicetime());
				map.put("name", response.getName());
				map.put("mobileNumber",
						response.getPatientMobileNumber() != null && !response.getPatientMobileNumber().isBlank()
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

			long totalCount = responses.size();
			long pendingCount = responses.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getFollowupStatus()))
					.count();
			long confirmedCount = responses.stream().filter(r -> "CONFIRMED".equalsIgnoreCase(r.getFollowupStatus()))
					.count();
			long inProgressCount = responses.stream()
					.filter(r -> "IN-PROGRESS".equalsIgnoreCase(r.getFollowupStatus())).count();

			Map<String, Object> summary = new HashMap<>();
			summary.put("totalAppointments", totalCount);
			summary.put("pending", pendingCount);
			summary.put("confirmed", confirmedCount);
			summary.put("inProgress", inProgressCount);

			log.info("Custom range bookings fetched successfully. Total appointments: {}", totalCount);

			return ResponseEntity
					.ok(new Response(true, list, summary, "Custom range bookings fetched", 200, null, null));

		} catch (Exception e) {
			log.error("Error fetching custom range bookings for clinicId: {}, branchId: {}", clinicId, branchId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new Response(false, null, null, e.getMessage(), 500, null, null));
		}
	}

	public ResponseEntity<Response> getBookingById(String bookingId) {
		try {
			Optional<Booking> booking = repository.findByBookingId(bookingId);
			if (booking.isPresent()) {
				if (!booking.get().getFollwupBookings().isEmpty()) {
					ObjectMapper mapper = new ObjectMapper();
					mapper.registerModule(new JavaTimeModule());
					mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
							res.setSession(lst);
						} catch (Exception e) {
							log.warn("Session fetch failed for bookingId={} : {}", bookingId, e.getMessage());
						}
					}
					return ResponseEntity.ok(new Response(true, res, null, "Booking fetched successfully", 200, null,
							null));
				} else {
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body(new Response(false, null, null, "follow up appoiintment not found", 404, null, null));
				}
			} else {
				return ResponseEntity.status(HttpStatus.OK)
						.body(new Response(false, null, null, "Booking not found", 200, null, null));
			}
		} catch (Exception e) {
			log.error("getBookingById failed for bookingId={} : {}", bookingId, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new Response(false, null, null, e.getMessage(), 500, null, null));
		}
	}

	private Booking updateForFollowup(BookingResponse dto) {
		try {
			Booking entity = repository.findByBookingIdIgnoreCase(dto.getBookingId())
					.orElseThrow(() -> new RuntimeException("Invalid Booking Id"));
			List<FollowupBooking> lst;
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

			if (dto.getFreeFollowUps() != null)
				entity.setFreeFollowUps(dto.getFreeFollowUps());
			if (dto.getFollowupDate() != null && !dto.getFollowupDate().isEmpty())
				entity.setFollowupDate(dto.getFollowupDate());
			if (dto.getFollowupStatus() != null) {
				entity.setFollowupStatus(dto.getFollowupStatus());
			}

			if (dto.getProblem() != null && !dto.getProblem().isEmpty())
				entity.setProblem(dto.getProblem());
			if (dto.getSymptomsDuration() != null && !dto.getSymptomsDuration().isEmpty())
				entity.setSymptomsDuration(dto.getSymptomsDuration());

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

			if (dto.getStatus() != null) {
				entity.setStatus(dto.getStatus());
			}

			if (dto.getAttachments() != null && !dto.getAttachments().isEmpty())
				entity.setAttachments(dto.getAttachments());
			if (dto.getConsentFormPdf() != null && !dto.getConsentFormPdf().isEmpty())
				entity.setConsentFormPdf(dto.getConsentFormPdf());

			if (dto.getPaymentType() != null && !dto.getPaymentType().isEmpty()) {
				entity.setPaymentType(dto.getPaymentType());
			}
			if (dto.getPaymentStatus() != null && !dto.getPaymentStatus().isEmpty())
				entity.setPaymentStatus(dto.getPaymentStatus());
			if (dto.getTotalFee() > 0)
				entity.setTotalFee(dto.getTotalFee());
			if (dto.getDoctorRefCode() != null && !dto.getDoctorRefCode().isEmpty())
				entity.setDoctorRefCode(dto.getDoctorRefCode());

			if (dto.getBodyPartId() != null && !dto.getBodyPartId().isEmpty())
				entity.setBodyPartId(dto.getBodyPartId());
			if (dto.getBodyPartName() != null && !dto.getBodyPartName().isEmpty())
				entity.setBodyPartName(dto.getBodyPartName());
			if (dto.getPartImage() != null && !dto.getPartImage().isEmpty())
				entity.setPartImage(dto.getPartImage());

			if (dto.getTheraphyAnswers() != null)
				entity.setTheraphyAnswers(new ObjectMapper().convertValue(dto.getTheraphyAnswers(),
						new TypeReference<Map<String, List<TheraphyAnswersEntity>>>() {
						}));

			if (dto.getParts() != null && !dto.getParts().isEmpty())
				entity.setParts(dto.getParts());
			if (dto.getPartAmount() > 0)
				entity.setPartAmount(dto.getPartAmount());
			if (dto.getDueAmount() >= 0)
				entity.setDueAmount(dto.getDueAmount());

			if (dto.getReferredByType() != null && !dto.getReferredByType().isEmpty())
				entity.setReferredByType(dto.getReferredByType());
			if (dto.getReferredByName() != null && !dto.getReferredByName().isEmpty())
				entity.setReferredByName(dto.getReferredByName());

			if (dto.getPreviousInjuries() != null && !dto.getPreviousInjuries().isEmpty())
				entity.setPreviousInjuries(dto.getPreviousInjuries());
			if (dto.getCurrentMedications() != null && !dto.getCurrentMedications().isEmpty())
				entity.setCurrentMedications(dto.getCurrentMedications());
			if (dto.getAllergies() != null && !dto.getAllergies().isEmpty())
				entity.setAllergies(dto.getAllergies());
			if (dto.getOccupation() != null && !dto.getOccupation().isEmpty())
				entity.setOccupation(dto.getOccupation());

			if (dto.getInsuranceProvider() != null && !dto.getInsuranceProvider().isEmpty())
				entity.setInsuranceProvider(dto.getInsuranceProvider());
			if (dto.getPolicyNumber() != null && !dto.getPolicyNumber().isEmpty())
				entity.setPolicyNumber(dto.getPolicyNumber());

			if (dto.getActivityLevels() != null && !dto.getActivityLevels().isEmpty())
				entity.setActivityLevels(dto.getActivityLevels());

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
			log.error("updateForFollowup failed for bookingId={} : {}", dto.getBookingId(), e.getMessage(), e);
			return null;
		}
	}

	@Override
	public List<Map<String, Object>> searchBookings(String clinicId, String input) {
		try {
			List<Booking> bookings = new ArrayList<>();

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
			log.error("searchBookings failed for clinicId={} input={} : {}", clinicId, input, e.getMessage(), e);
			throw e;
		}
	}
}