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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import com.dermacare.bookingService.dto.BookingRequset;
import com.dermacare.bookingService.dto.BookingResponse;
import com.dermacare.bookingService.dto.ConsultationFeesDTO;
import com.dermacare.bookingService.dto.DoctorSaveDetailsDTO;
import com.dermacare.bookingService.dto.PatientAndPriceInfo;
import com.dermacare.bookingService.dto.PatientInfo;
import com.dermacare.bookingService.dto.RelationInfoDTO;
import com.dermacare.bookingService.dto.ReportsDTO;
import com.dermacare.bookingService.dto.ReportsDtoList;
import com.dermacare.bookingService.dto.Session;
import com.dermacare.bookingService.dto.SessionForBooking;
import com.dermacare.bookingService.entity.Booking;
import com.dermacare.bookingService.entity.ConsultationFees;
import com.dermacare.bookingService.entity.FollowupBooking;
import com.dermacare.bookingService.entity.Reports;
import com.dermacare.bookingService.entity.ReportsList;
import com.dermacare.bookingService.entity.Status;
import com.dermacare.bookingService.entity.TheraphyAnswersEntity;
import com.dermacare.bookingService.feign.NotificationFeign;
import com.dermacare.bookingService.feign.PhysioDoctorFeign;
import com.dermacare.bookingService.repository.BookingServiceRepository;
import com.dermacare.bookingService.service.BookingService_Service;
import com.dermacare.bookingService.service.S3Service;
import com.dermacare.bookingService.util.ExternalServiceClient;
import com.dermacare.bookingService.util.KeyCloakTokenStore;
import com.dermacare.bookingService.util.Response;
import com.dermacare.bookingService.util.ResponseStructure;
import com.dermacare.bookingService.util.geneateIds;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public class BookingService_ServiceImpl implements BookingService_Service {

	@Autowired
	private BookingServiceRepository repository;

	@Autowired
	private ExternalServiceClient physioDoctorFeign;

	@Autowired
	private ExternalServiceClient clinnicfeign;

//	@Autowired
//	private KafkaProducer kafkaProducer;

	@Autowired
	private ExternalServiceClient notificationFeign;

//	@Autowired
//	private DoctorFeign doctorFeign;

	@Autowired
	private ExternalServiceClient clinicAdminFeign;

	@Autowired
	private geneateIds sequenceGeneratorService;

	@Autowired
	private S3Service s3Service;
	
	@Autowired
	private KeyCloakTokenStore keyCloakTokenStore;
	
	@Autowired
	private WhatsAppService whatsAppService;

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> followUpBooking(BookingResponse request) {
		ResponseStructure<BookingResponse> response = new ResponseStructure<>();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		try {
			Booking updatedBooking =
					updateForFollowup(request);
			if (updatedBooking != null) {
				
				response = ResponseStructure.buildResponse(
						mapper.convertValue(updatedBooking, BookingResponse.class),				
						"Last follow-up booking retrieved successfully",
						HttpStatus.CREATED,
						HttpStatus.CREATED.value());
			} else {
				response = ResponseStructure.buildResponse(
						null,
						"No follow-up bookings found",
						HttpStatus.BAD_REQUEST,
						HttpStatus.BAD_REQUEST.value());}
		}catch (Exception e){
			// Log properly (avoid System.out in real apps)
			e.printStackTrace();
			response = ResponseStructure.buildResponse(
					null,
					"Exception occurred: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR,
					HttpStatus.INTERNAL_SERVER_ERROR.value()
			);
		}return ResponseEntity.status(response.getHttpStatus().value()).body(response);}


	private void nullifyLargeFields(Booking booking) {
		if (booking == null) return;
		booking.setReports(null);
		///booking.setNotes(null);
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
			Map<String,String> res = new LinkedHashMap<>();
			try {
				if(request.getCustomerId().isEmpty() ||request.getPatientId().isEmpty() ) {
					res = clinnicfeign.getCustomerByMobilenumberAndName(keyCloakTokenStore.getAccess_token(),request.getMobileNumber(), request.getName());
					customerId = res.get("customerId");
					patientId = res.get("patientId");}
			}catch(Exception e) {System.out.println(e.getMessage());}
			if(request.getCustomerId().isEmpty()){
				entity.setCustomerId(customerId);}
			if(request.getPatientId().isEmpty()){
				entity.setPatientId(patientId);}
			entity.setConsultationType("First-Time");
			ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
			if(request.getTotalFee() != 0.0) {
				double due = request.getTotalFee() - request.getPartAmount();
				entity.setDueAmount(due);
				entity.setBookedAt(istTime.format(formatter));}
			entity.setFreeFollowUpsLeft(request.getFreeFollowUps());
			entity.setFollowupStatus("pending");
			// Case 1: No free follow-ups → always true
			if (request.getFreeFollowUps() != null && request.getFreeFollowUps() == 0) {
				entity.setIsFollowupStatus(true);}
			int days = 0;
			try{
				if(request.getConsultationExpiration() != null) {
					String consultationExp = request.getConsultationExpiration(); // e.g. "8 days"
					days = Integer.parseInt(consultationExp.replaceAll("[^0-9]", ""));}

				// Parse serviceDate (assumes format: yyyy-MM-dd)
				LocalDate serviceDate = LocalDate.parse(request.getServiceDate());

				// Add extracted days
				LocalDate expiryDate = serviceDate.plusDays(days);

				LocalDate today = LocalDate.now();

				if(!today.isAfter(expiryDate) && request.getFreeFollowUps() != null && request.getFreeFollowUps() == 0  ){
					entity.setIsFollowupStatus(true);
				}else if(today.isAfter(expiryDate)){
					entity.setIsFollowupStatus(true);
				}else {
					entity.setIsFollowupStatus(false);
				}} catch (Exception e) {
				// fallback safety
				entity.setIsFollowupStatus(false);}

			// ✅ Generate Custom Booking ID
			String bookingId = sequenceGeneratorService.generateBookingId(request.getClinicName().substring(0, 3),request.getBranchname().substring(0, 3));
			entity.setBookingId(bookingId);

			// Channel ID logic
			if (request.getConsultationType() != null &&
					(request.getConsultationType().equalsIgnoreCase("video consultation") ||
							request.getConsultationType().equalsIgnoreCase("online consultation"))) {
				entity.setChannelId(randomNumber());
			}
			if(request.getFoc() != null && request.getPaymentType() != null) {

				if ("paid".equalsIgnoreCase(request.getFoc())&&"not paid".equalsIgnoreCase(request.getPaymentType())) {
					entity.setStatus("pending");
				} else if("foc".equalsIgnoreCase(request.getFoc())&&"not paid".equalsIgnoreCase(request.getPaymentType()))  {
					entity.setStatus("confirmed");
				}else {
					if("paid".equalsIgnoreCase(request.getFoc()) && !request.getPaymentType().isEmpty()){
						entity.setStatus("confirmed");}}}
			List<Status> status = new LinkedList<>();
			Status s = new Status();
			ZoneId zone = ZoneId.of("Asia/Kolkata");
			LocalDateTime dateTime = LocalDateTime.now(zone);
			s.setDATE_TIME(dateTime);
			s.setStatus(entity.getStatus());
			status.add(s);
			Collections.reverse(status);
			entity.setCurrentStatus(status);
			if(request.getConsultationFee() != 0.0) {
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
			}if(entity.getFollwupBookings() == null) {
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
			}}catch (Exception e) {
			System.out.println(e.getMessage());
		}return entity;}

	private BookingResponse toResponse(Booking entity) {
		    ObjectMapper mapper = new ObjectMapper();
		    mapper.registerModule(new JavaTimeModule());
		    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		    BookingResponse response = mapper.convertValue(entity, BookingResponse.class);
		    response.setIsFollowupStatus(entity.getIsFollowupStatus());
		    response.setConsultationFee(entity.getListOfConsultationFee().get(0).getConsulationFee());

		    String dto = getPrescriptionpdf(response.getBookingId());
		    if (dto != null) {
		        response.setPrescriptionPdf(Collections.singletonList(dto));
		    }

		    response.setBookingId(String.valueOf(entity.getBookingId()));

//		    if (entity.getTreatments() != null && entity.getTreatments().getGeneratedData() != null) {
//		        entity.getTreatments().getGeneratedData().forEach((name, t) -> {
//		            if (t.getPendingSittings() != null && t.getPendingSittings() > 0) {
//		                t.setStatus("In-Progress");
//		            } else {
//		                t.setStatus("Confirmed");
//		            }
//		        });
//		    }

		    // ── S3 signed URLs ──────────────────────────────
		    try {
		        if (entity.getPartImage() != null && !entity.getPartImage().isEmpty()) {
		            response.setPartImage(s3Service.generateSignedUrl(entity.getPartImage()));
		        }
		    } catch (Exception e) {
		        System.out.println("partImage URL error: " + e.getMessage());
		    }

		    try {
		        if (entity.getConsentFormPdf() != null && !entity.getConsentFormPdf().isEmpty()) {
		            response.setConsentFormPdf(s3Service.generateSignedUrl(entity.getConsentFormPdf()));
		        }
		    } catch (Exception e) {
		        System.out.println("consentFormPdf URL error: " + e.getMessage());
		    }

		    try {
		        if (entity.getAttachments() != null && !entity.getAttachments().isEmpty()) {
		            List<String> signedUrls = entity.getAttachments().stream()
		                    .map(key -> {
		                        try { return s3Service.generateSignedUrl(key); }
		                        catch (Exception ex) { return key; }
		                    })
		                    .collect(Collectors.toList());
		            response.setAttachments(signedUrls);
		        }
		    } catch (Exception e) {
		        System.out.println("attachments URL error: " + e.getMessage());
		    }

		    // ── ✅ NEW: Sign report file keys → signed URLs ──
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
		                                    return clinicAdminFeign.getSignedUrl(keyCloakTokenStore.getAccess_token(),key); // ✅ calls Clinic Admin
		                                } catch (Exception ex) {
		                                    System.out.println("report sign error: " + ex.getMessage());
		                                    return key; // fallback to raw key
		                                }
		                            })
		                            .collect(Collectors.toList());
		                    report.setReportFile(signedUrls);
		                }
		            }
		        }
		    } catch (Exception e) {
		        System.out.println("reports URL signing error: " + e.getMessage());
		    }

		    return response;
		}


	private static String randomNumber() {
        Random random = new Random();    
        int sixDigitNumber = 100000 + random.nextInt(900000); // Generates number from 100000 to 999999
        return String.valueOf(sixDigitNumber);
    }

	private String getPrescriptionpdf(String bid) {
		try {
			String res = physioDoctorFeign.getByBookingId(keyCloakTokenStore.getAccess_token(),bid);
			return res;
		}catch(Exception e) {
			return null;
		}
	}

	private List<BookingResponse> toResponses(List<Booking> bookings) {
	    ObjectMapper mapper = new ObjectMapper();
	    mapper.registerModule(new JavaTimeModule());
	    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	    List<BookingResponse> res = mapper.convertValue(bookings, new TypeReference<List<BookingResponse>>() {});

	    for (BookingResponse bres : res) {

	        // ── partImage ───────────────────────────────────
	        try {
	            if (bres.getPartImage() != null && !bres.getPartImage().isEmpty()) {
	                bres.setPartImage(s3Service.generateSignedUrl(bres.getPartImage()));
	            }
	        } catch (Exception e) {
	            System.out.println("partImage URL error: " + e.getMessage());
	        }

	        // ── consentFormPdf ──────────────────────────────
	        try {
	            if (bres.getConsentFormPdf() != null && !bres.getConsentFormPdf().isEmpty()) {
	                bres.setConsentFormPdf(s3Service.generateSignedUrl(bres.getConsentFormPdf()));
	            }
	        } catch (Exception e) {
	            System.out.println("consentFormPdf URL error: " + e.getMessage());
	        }

	        // ── attachments ─────────────────────────────────
	        try {
	            if (bres.getAttachments() != null && !bres.getAttachments().isEmpty()) {
	                List<String> signedUrls = bres.getAttachments().stream()
	                        .map(key -> {
	                            try { return s3Service.generateSignedUrl(key); }
	                            catch (Exception ex) { return key; }
	                        })
	                        .collect(Collectors.toList());
	                bres.setAttachments(signedUrls);
	            }
	        } catch (Exception e) {
	            System.out.println("attachments URL error: " + e.getMessage());
	        }

	        // ── ✅ NEW: reports — sign raw S3 keys via Clinic Admin Feign ──
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
	                                        return clinicAdminFeign.getSignedUrl(keyCloakTokenStore.getAccess_token(),key); // ✅ Clinic Admin signs it
	                                    } catch (Exception ex) {
	                                        System.out.println("report sign error: " + ex.getMessage());
	                                        return key; // fallback to raw key
	                                    }
	                                })
	                                .collect(Collectors.toList());
	                        report.setReportFile(signedUrls);
	                    }
	                }
	            }
	        } catch (Exception e) {
	            System.out.println("reports URL signing error: " + e.getMessage());
	        }

	        // ── prescriptionPdf ─────────────────────────────
	        String dto = getPrescriptionpdf(bres.getBookingId());
	        if (dto != null) {
	            bres.setPrescriptionPdf(Collections.singletonList(dto));
	        }
	    }

	    return res;
	}

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_CUSTOMER"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	 public ResponseEntity<?> physioAppointment(BookingRequset request) {
		 Response res = new Response();
		  ObjectMapper mapper = new ObjectMapper();
	         mapper.registerModule(new JavaTimeModule());
	         mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		// ResponseEntity<?> repnse = null;
		 try {
			
			 if (request.getClinicName() == null || request.getClinicName().isEmpty() ) {
			        throw new RuntimeException("ClinicName is mandatory");
			    }
			 if (request.getBranchname() == null || request.getBranchname().isEmpty() ) {
			        throw new RuntimeException("Branchname is mandatory");
			    }	 
			 if (request.getFreeFollowUps() == null) {
			        throw new RuntimeException("Free FollowUps is mandatory");
			    }
			   if (request.getPatientMobileNumber() == null || request.getPatientMobileNumber().trim().isEmpty()) {
			    	if(request.getMobileNumber() == null  || request.getMobileNumber().trim().isEmpty()) {
			        throw new RuntimeException("patientmobilenumber or Mobile Number is mandatory");
			    }}

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

			// =====================================================
			// SAVE BOOKING
			// =====================================================

			Booking entity = toEntity(request);

			Booking updatedBooking = repository.save(entity);

			if (updatedBooking == null) {
				throw new RuntimeException("Unable to save appointment");
			}

			// =====================================================
			// SEND NOTIFICATION
			// =====================================================

			int notificationStatus = 0;

			try {

				Response notificationResponse = notificationFeign

						.createNotification(keyCloakTokenStore.getAccess_token(),mapper.convertValue(updatedBooking, BookingResponse.class));

				if (notificationResponse != null) {
					notificationStatus = notificationResponse.getStatus();
				}

			} catch (Exception e) {

				log.warn("Notification service failed for booking {} : {}", updatedBooking.getBookingId(),
						e.getMessage());
			}

			// =====================================================
			// SEND WHATSAPP
			// =====================================================

			try {

				request.setBookingId(updatedBooking.getBookingId());

				request.setClinicId(updatedBooking.getClinicId());

				request.setBranchId(updatedBooking.getBranchId());

				whatsAppService.sendBookingConfirmation(request);

				log.info("WhatsApp sent successfully for booking {}", updatedBooking.getBookingId());

			} catch (Exception e) {

				log.warn("WhatsApp notification failed for booking {} : {}", updatedBooking.getBookingId(),
						e.getMessage());

				// Do not fail booking if WhatsApp fails
			}

			// =====================================================
			// SUCCESS RESPONSE
			// =====================================================

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

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getAppointsByPatientId(String patientId, int page, int size) {

		ResponseStructure<Map<String, Object>> res =
				new ResponseStructure<Map<String, Object>>();
		try {
			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.DESC, "createdAt")
			);

			Page<Booking> bookingPage =
                    (Page<Booking>) repository.findByPatientId(patientId, pageable);

			if (bookingPage != null && !bookingPage.isEmpty()) {

				ObjectMapper mapper = new ObjectMapper();
				mapper.registerModule(new JavaTimeModule());
				mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

				List<BookingResponse> response =
						mapper.convertValue(
								bookingPage.getContent(),
								new TypeReference<List<BookingResponse>>() {}
						);

				List<Map<String, Object>> list = new ArrayList<>();

				response.stream().map(n -> {

					Map<String, Object> map = new LinkedHashMap<>();

					map.put("bookingId", n.getBookingId());
					map.put("serviceDate", n.getServiceDate());
					map.put("servicetime", n.getServicetime());
					map.put("name", n.getName());

					map.put(
							"mobileNumber",
							n.getPatientMobileNumber() != null
									&& !n.getPatientMobileNumber().isEmpty()
									? n.getPatientMobileNumber()
									: n.getMobileNumber()
					);

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

				Map<String, Object> responseMap = new LinkedHashMap<>();

				responseMap.put("content", list);
				responseMap.put("currentPage", bookingPage.getNumber());
				responseMap.put("totalItems", bookingPage.getTotalElements());
				responseMap.put("totalPages", bookingPage.getTotalPages());
				responseMap.put("pageSize", bookingPage.getSize());

				res.setStatusCode(200);
				res.setData(responseMap);
				res.setMessage("Appointments Are Found");

				return ResponseEntity.status(200).body(res);

			} else {

				res.setStatusCode(200);
				res.setMessage("Appointments Are Not Found");

				return ResponseEntity.status(200).body(res);
			}

		} catch (Exception e) {

			res.setStatusCode(500);
			res.setMessage(e.getMessage());

			return ResponseEntity.status(500).body(res);
		}
	}


	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getAppointsByInput(
			String input,
			int page,
			int size) {

		ResponseStructure<Map<String, Object>> res =
				new ResponseStructure<>();

		try {

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.DESC, "createdAt")
			);

			Page<Booking> existingBooking =
					repository.findByNameIgnoreCaseOrBookingIdOrPatientId(
							input,
							pageable
					);

			if (existingBooking != null && !existingBooking.isEmpty()) {

				List<BookingResponse> response =
						new ObjectMapper().convertValue(
								existingBooking.getContent(),
								new TypeReference<List<BookingResponse>>() {}
						);

				Map<String, Object> map = new LinkedHashMap<>();

				map.put("content", response);
				map.put("currentPage", existingBooking.getNumber());
				map.put("totalItems", existingBooking.getTotalElements());
				map.put("totalPages", existingBooking.getTotalPages());
				map.put("pageSize", existingBooking.getSize());

				res.setStatusCode(200);
				res.setData(map);
				res.setMessage("Appointments Are Found");

				return ResponseEntity.status(200).body(res);

			} else {

				res.setStatusCode(200);
				res.setMessage("Appointments Are Not Found");

				return ResponseEntity.status(200).body(res);
			}

		} catch (Exception e) {

			res.setStatusCode(500);
			res.setMessage(e.getMessage());
			return ResponseEntity.status(500).body(res);
		}
	}


	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(
			String clinicId,
			String doctorId,
			int page,
			int size) {

		ResponseStructure<Map<String, Object>> res =
				new ResponseStructure<>();

		List<Map<String, Object>> list = new ArrayList<>();

		try {

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.DESC, "createdAt")
			);

			LocalDate currentDate =
					LocalDate.now(ZoneId.of("Asia/Kolkata"));

			String todayDate = currentDate.toString();

			Page<Booking> existingBookings =
					repository.findByClinicIdAndDoctorIdAndServiceDateAndStatusIgnoreCase(
							clinicId,
							doctorId,
							todayDate,
							"Confirmed",
							pageable
					);

			if (existingBookings != null && !existingBookings.isEmpty()) {

				List<BookingResponse> responseList =
						existingBookings.getContent()
								.stream()
								.map(this::toResponse)
								.toList();

				responseList.stream().map(n -> {

					Map<String, Object> map =
							new LinkedHashMap<>();
					map.put("bookingId", n.getBookingId());
					map.put("serviceDate", n.getServiceDate());
					map.put("servicetime", n.getServicetime());
					map.put("name", n.getName());
					map.put(
							"mobileNumber",
							n.getPatientMobileNumber() != null
									&& !n.getPatientMobileNumber().isEmpty()
									? n.getPatientMobileNumber()
									: n.getMobileNumber()
					);
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

				Map<String, Object> responseMap =
						new LinkedHashMap<>();

				responseMap.put("content", list);
				responseMap.put("currentPage", existingBookings.getNumber());
				responseMap.put("totalItems", existingBookings.getTotalElements());
				responseMap.put("totalPages", existingBookings.getTotalPages());
				responseMap.put("pageSize", existingBookings.getSize());

				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setData(responseMap);
				if (!list.isEmpty()) {
					res.setMessage("Today's Appointments Found");
				} else {
					res.setMessage("No Appointments for Today");
				}

			} else {

				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setMessage("Appointments Not Found");
			}

		} catch (Exception e) {
			res.setStatusCode(500);
			res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			res.setMessage("Error occurred : " + e.getMessage());
		}
		return ResponseEntity
				.status(res.getStatusCode())
				.body(res);
	}


@Override
@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(
        String hospitalId,
        String doctorId,
        String number) {

    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
    List<BookingResponse> responses = new ArrayList<>();

    try {

        List<Booking> bookings =
                repository.findByClinicIdAndDoctorId(hospitalId, doctorId);

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

            LocalDate appointmentDate =
                    LocalDate.parse(booking.getServiceDate());

            boolean add = false;

            switch (number) {

                // Upcoming
                case "1":
                    add = "Confirmed".equalsIgnoreCase(booking.getStatus())
                            && appointmentDate.isAfter(today);
                    break;

                // Upcoming Online
                case "2":
                    add = "Online Consultation".equalsIgnoreCase(booking.getConsultationType())
                            && "Confirmed".equalsIgnoreCase(booking.getStatus())
                            && appointmentDate.isAfter(today);
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
        res.setMessage(
                responses.isEmpty()
                        ? "Appointments Are Not Found"
                        : "Appointments Are Found"
        );

    } catch (Exception e) {
        res.setStatusCode(500);
        res.setData(null);
        res.setMessage(e.getMessage());
    }

    return ResponseEntity.status(res.getStatusCode()).body(res);
}

@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getCompletedApntsByDoctorId(String hospitalId,String doctorId) {
		Map<String,Object> m = new LinkedHashMap<>();
		try {
			List<Booking> existingBooking = repository.findByClinicIdAndDoctorId(hospitalId, doctorId);
			List<BookingResponse> res = new ArrayList<>();
			if(existingBooking != null) {
				for(Booking b : existingBooking) {
					if(b.getStatus().equalsIgnoreCase("Completed")) {
						res.add(toResponse(b));}}
				m.put("completedAppointmentsCount",res.size());
				m.put("status",200);
				return ResponseEntity.status(200).body(m);
			}else {
				m.put("Message","No Appointsments Found");
				m.put("status",200);
				return ResponseEntity.status(200).body(m);
			}
		}catch(Exception e) {
			m.put("Message",e.getMessage());
			m.put("status",500);
			return ResponseEntity.status(500).body(m);}
	}


@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getSizeOfConsultationTypesByDoctorId(String hospitalId,String doctorId) {
		Map<String,Object> m = new LinkedHashMap<>();
		try {
			List<Booking> existingBooking = repository.findByClinicIdAndDoctorId(hospitalId, doctorId);
			List<BookingResponse> servicesAndConsul = new ArrayList<>();
			List<BookingResponse> inClinic = new ArrayList<>();
			List<BookingResponse> online = new ArrayList<>();
			if(existingBooking != null) {
				for(Booking b : existingBooking) {
					if(b.getStatus().equalsIgnoreCase("Completed")) {
						if(b.getConsultationType().equalsIgnoreCase("Services & Treatments")) {
							servicesAndConsul.add(toResponse(b));}
						if(b.getConsultationType().equalsIgnoreCase("In-Clinic Consultation")){
							inClinic.add(toResponse(b));}
						if(b.getConsultationType().equalsIgnoreCase("Online Consultation")){
							online.add(toResponse(b));}
					}}
				m.put("services & Treatments",servicesAndConsul.size());
				m.put("in-Clinic Consultation",inClinic.size());
				m.put("online Consultation",online.size());
				m.put("status",200);
				return ResponseEntity.status(200).body(m);
			}else {
				m.put("Message","No Appointsments Found");
				m.put("status",200);
				return ResponseEntity.status(200).body(m);}
		}catch(Exception e) {
			m.put("Message",e.getMessage());
			m.put("status",500);
			return ResponseEntity.status(500).body(m);}
	}


@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public BookingResponse getBookedService(String bookingId) {
		try {
			Booking entity = repository.findByBookingIdIgnoreCase(bookingId).get();
			System.out.println(entity);
			if(entity != null) {
				BookingResponse res = toResponse(entity);
				List<SessionForBooking> lst = new ArrayList<>();
				try {
					lst = physioDoctorFeign.getPhysioByBookingId(keyCloakTokenStore.getAccess_token(),res.getBookingId(),res.getServiceDate());
					res.setSession(lst);
				}catch(Exception e) {}
				return res;
			}else{
				return null;}
		}catch(Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
	}

@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public void deleteBookedServiceReports(String bookingId,String index) {
		try {
			Booking entity = repository.findByBookingIdIgnoreCase(bookingId).get();
			if(entity != null && index.equalsIgnoreCase("null")) {
				try {
					entity.getReports().clear();
					repository.save(entity);
				}catch(Exception e) {}
			}else{
				if(entity != null && index != null) {
					entity.getReports().remove(Integer.valueOf(index).intValue());
					repository.save(entity);
				}}}catch(Exception e) {}
	}

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public BookingResponse deleteService(String id) {
		Booking entity = repository.findByBookingIdIgnoreCase(id)
				.orElseThrow(() -> new RuntimeException("Invalid Booking Id Please provide Valid Id"));
		repository.deleteById(id);
		return toResponse(entity);
	}

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public Page<BookingResponse> getBookedServices(
			String mobileNumber,
			int page,
			int size) {

		Pageable pageable = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Direction.DESC, "createdAt")
		);

		Page<Booking> bookings =
				repository.findByMobileNumber(
						mobileNumber,
						pageable
				);

		if (bookings == null || bookings.isEmpty()) {
			return Page.empty();
		}

		return bookings.map(this::toResponse);
	}

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public Page<BookingResponse> getAllBookedServices(int page, int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<Booking> bookingPage = repository.findAll(pageable);

		if (bookingPage.isEmpty()) {
			return Page.empty();
		}

		return bookingPage.map(this::toResponse);
	}

	// @Override
	// public List<BookingResponse> bookingByServiceId(String serviceId) {
	// List<Booking> bookings = repository.findBySubServiceId(serviceId);
	// List<Booking> reversedBookings = new ArrayList<>();
	// for(int i = bookings.size()-1; i >= 0; i--) {
	// reversedBookings.add(bookings.get(i));
	// }
	// if (bookings == null || bookings.isEmpty()) {
	// return null;
	// }
	// return toResponses(reversedBookings);
	// }

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public Page<BookingResponse> bookingByDoctorId(
			String doctorId,
			int page,
			int size) {

		// ================= PAGINATION =================

		Pageable pageable = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Direction.DESC, "createdAt"));

		// If createdAt field is not available use "_id"

		Page<Booking> bookingPage =
				repository.findByDoctorId(
						doctorId,
						pageable);

		// ================= EMPTY CHECK =================

		if (bookingPage.isEmpty()) {

			return Page.empty();
		}

		// ================= ENTITY → DTO =================

		return bookingPage.map(this::toResponse);
	}

//	@Override
//	public List<BookingResponse> bookingByServiceId(String serviceId) {
//		List<Booking> bookings = repository.findBySubServiceId(serviceId);
//		List<Booking> reversedBookings = new ArrayList<>();
//		for(int i = bookings.size()-1; i >= 0; i--) {
//			reversedBookings.add(bookings.get(i));
//		}
//		if (bookings == null  || bookings.isEmpty()) {
//			return null;
//		}
//		return toResponses(reversedBookings);
//	}


//	@Override
//	public ResponseEntity<?> bookingByCustomerId(
//			String customerId,
//			int page,
//			int size) {
//
//		try {
//
//			Pageable pageable = PageRequest.of(
//					page,
//					size,
//					Sort.by(Sort.Direction.DESC, "createdAt"));
//
//			Page<Booking> bookingPage =
//					repository.findByCustomerId(customerId, pageable);
//
//			if (bookingPage == null || bookingPage.isEmpty()) {
//
//				Map<String, Object> response = new LinkedHashMap<>();
//
//				response.put("success", false);
//				response.put("message", "No bookings found");
//				response.put("data", Collections.emptyList());
//
//				return ResponseEntity
//						.status(HttpStatus.NOT_FOUND)
//						.body(response);
//			}
//
//			Page<Map<String, Object>> responseData = bookingPage.map(booking -> {
//
//				BookingResponse n = toResponse(booking);
//
//				Map<String, Object> map = new LinkedHashMap<>();
//
//				map.put("bookingId", n.getBookingId());
//				map.put("serviceDate", n.getServiceDate());
//				map.put("servicetime", n.getServicetime());
//				map.put("name", n.getName());
//
//				map.put(
//						"mobileNumber",
//						n.getPatientMobileNumber() != null
//								&& !n.getPatientMobileNumber().isEmpty()
//								? n.getPatientMobileNumber()
//								: n.getMobileNumber());
//
//				map.put("doctorId", n.getDoctorId());
//				map.put("doctorName", n.getDoctorName());
//				map.put("paymentType", n.getPaymentType());
//				map.put("visitType", n.getVisitType());
//				map.put("status", n.getStatus());
//				map.put("followupStatus", n.getFollowupStatus());
//				map.put("patientId", n.getPatientId());
//				map.put("clinicId", n.getClinicId());
//				map.put("customerId", n.getCustomerId());
//				map.put("branchId", n.getBranchId());
//				map.put("age", n.getAge());
//				map.put("gender", n.getGender());
//				map.put("branchName", n.getBranchname());
//				map.put("problem", n.getProblem());
//
//				return map;
//			});
//
//			Map<String, Object> response = new LinkedHashMap<>();
//
//			response.put("success", true);
//			response.put("message", "Bookings retrieved successfully");
//			response.put("data", responseData.getContent());
//			response.put("currentPage", responseData.getNumber());
//			response.put("totalItems", responseData.getTotalElements());
//			response.put("totalPages", responseData.getTotalPages());
//
//			return ResponseEntity.ok(response);
//
//		} catch (Exception e) {
//
////	        log.error(
////	                "Error occurred while retrieving bookings for customerId : {}",
////	                customerId,
////	                e);
//
//			Map<String, Object> response = new LinkedHashMap<>();
//
//			response.put("success", false);
//			response.put("message", "Failed to retrieve bookings");
//			response.put("error", e.getMessage());
//
//			return ResponseEntity
//					.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body(response);
//		}
//
//	}
//

	
	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_CUSTOMER"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public List<Map<String, Object>> bookingByCustomerId(String customerId) {

	    List<Booking> bookings = repository.findByCustomerId(customerId);

	    if (bookings == null || bookings.isEmpty()) {
	        return Collections.emptyList();
	    }

	    bookings = bookings.stream()
	            .filter(booking -> !"COMPLETED".equalsIgnoreCase(booking.getStatus()))
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
	}
	
	
	
	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public Page<BookingResponse> bookingByPatientId(String clincId,String patientId, int page, int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<Booking> bookingPage = repository.findByClinicIdAndPatientId(clincId,patientId, pageable);

		List<BookingResponse> responses = toResponses(bookingPage.getContent());

		return new PageImpl<>(
				responses,
				pageable,
				bookingPage.getTotalElements()
		);
	}

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public Page<BookingResponse> bookingByPatientIdAndBookingId(
			String patientId,
			String bookingId,
			int page,
			int size) {

		Pageable pageable = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Direction.DESC, "createdAt")
		);

		Page<Booking> bookings =
				repository
						.findByPatientIdAndBookingIdAndStatusIgnoreCase(
								patientId,
								bookingId,
								"In-Progress",
								pageable
						);

		if (bookings == null || bookings.isEmpty()) {
			return Page.empty();
		}

		return bookings.map(this::toResponse);
	}
	

	 @Override
	 @Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	  public List<ReportsDTO> getReportsByPatientId(String patientId) {
		  ObjectMapper mapper = new ObjectMapper();
	         mapper.registerModule(new JavaTimeModule());
	         mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);	        
	        List<Booking> bookings = repository.findByPatientId(patientId);
	        List<ReportsDTO> responseList = new ArrayList<>();
	        for (Booking booking : bookings) {
	            if (booking.getReports() != null) {
	                for (ReportsList report : booking.getReports()) {
	                for(Reports reportEntity : report.getReportsList()) {          
	                    ReportsDTO dto = mapper.convertValue(reportEntity, ReportsDTO.class);
	                    responseList.add(dto);
	                }}}}
	        return responseList;
	    }
		
	 @Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_CUSTOMER"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public List<Map<String, Object>> CompletedbookingByCustomerId(String customerId) {

	    List<Booking> bookings = repository.findByCustomerId(customerId);

	    if (bookings == null || bookings.isEmpty()) {
	        return Collections.emptyList();
	    }

	    bookings = bookings.stream()
	            .filter(booking -> "COMPLETED".equalsIgnoreCase(booking.getStatus()))
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
	}
	

	private boolean isValidMobileNumber(String input) {
		if (input == null) {
			return false;
		}
		String regex = "^[6-9]\\d{9}$";
		return input.matches(regex);
	}


	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> bookingByClinicId(
			String clinicId,
			int page,
			int size) {

		Response response = new Response();

		try {

			// ================= PAGINATION =================

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.DESC, "createdAt"));

			// If createdAt field not available use "_id"

			Page<Booking> bookingPage =
					repository.findByClinicId(
							clinicId,
							pageable);

			// ================= EMPTY CHECK =================

			if (bookingPage.isEmpty()) {

				response.setMessage("No Bookings Found");
				response.setStatus(HttpStatus.NOT_FOUND.value());
				response.setSuccess(false);
				response.setData(null);

				return new ResponseEntity<>(
						response,
						HttpStatus.NOT_FOUND);
			}

			// ================= ENTITY TO DTO =================

			Page<BookingResponse> bookingResponses =
					bookingPage.map(this::toResponse);

			// ================= SUCCESS RESPONSE =================

			response.setMessage(
					"Bookings Retrieved Successfully");
			response.setStatus(HttpStatus.OK.value());
			response.setSuccess(true);
			response.setData(bookingResponses);

			return new ResponseEntity<>(
					response,
					HttpStatus.OK);

		} catch (Exception e) {

			response.setMessage(
					e.getMessage());
			response.setStatus(
					HttpStatus.INTERNAL_SERVER_ERROR.value());
			response.setSuccess(false);

			return new ResponseEntity<>(
					response,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


//	@Override
//	public ResponseEntity<?> updateAppointment(BookingResponse bookingResponse) {
//	    try {
//	        // --- Fetch booking from DB ---
//	        Booking entity = repository.findByBookingId(bookingResponse.getBookingId())
//	                .orElseThrow(() -> new RuntimeException("Invalid Booking Id. Please provide a valid Id."));
//
//	        // --- Update fields from bookingResponse to entity ---
//	        if (bookingResponse.getAge() != null) entity.setAge(bookingResponse.getAge());
//	        if (bookingResponse.getBookedAt() != null) entity.setBookedAt(bookingResponse.getBookedAt());
//	        if (bookingResponse.getBookingFor() != null) entity.setBookingFor(bookingResponse.getBookingFor());
//	        if (bookingResponse.getClinicId() != null) entity.setClinicId(bookingResponse.getClinicId());
//	        if (bookingResponse.getConsultationFee() != 0) entity.setConsultationFee(bookingResponse.getConsultationFee());
//	        if (bookingResponse.getConsultationType() != null) entity.setConsultationType(bookingResponse.getConsultationType());
//	        if (bookingResponse.getDoctorId() != null) entity.setDoctorId(bookingResponse.getDoctorId());
//	        if (bookingResponse.getGender() != null) entity.setGender(bookingResponse.getGender());
//	        if (bookingResponse.getMobileNumber() != null) entity.setMobileNumber(bookingResponse.getMobileNumber());
//	        if (bookingResponse.getName() != null) entity.setName(bookingResponse.getName());
//	        if (bookingResponse.getProblem() != null) entity.setProblem(bookingResponse.getProblem());
//	        if (bookingResponse.getServiceDate() != null) entity.setServiceDate(bookingResponse.getServiceDate());
//	        if (bookingResponse.getServicetime() != null) entity.setServicetime(bookingResponse.getServicetime());
//	        if (bookingResponse.getStatus() != null) {entity.setStatus(bookingResponse.getStatus());
//	        List<Status> status = entity.getCurrentStatus();
//        	Status s = new Status();
//        	ZoneId zone = ZoneId.of("Asia/Kolkata");
//        	LocalDateTime dateTime = LocalDateTime.now(zone);
//        	s.setDATE_TIME(dateTime);
//        	s.setStatus(bookingResponse.getStatus());
//        	status.add(s);
//        	 Collections.reverse(status);
//        	 entity.setCurrentStatus(status);
//	        }
//	        if (bookingResponse.getNotes() != null) entity.setNotes(bookingResponse.getNotes());
//	        if (bookingResponse.getReports() != null) {
//	            entity.setReports(new ObjectMapper().convertValue(
//	                    bookingResponse.getReports(),
//	                    new TypeReference<List<ReportsList>>() {}));
//	        }
//	        if( bookingResponse.getPaymentType() != null && bookingResponse.getPaymentType().equalsIgnoreCase("paid")) {
//	        	entity.setStatus("confirmed");
//	        	List<Status> status = new LinkedList<>();
//            	Status s = new Status();
//            	ZoneId zone = ZoneId.of("Asia/Kolkata");
//            	LocalDateTime dateTime = LocalDateTime.now(zone);
//            	s.setDATE_TIME(dateTime);
//            	s.setStatus(entity.getStatus());
//            	status.add(s);
//            	 Collections.reverse(status);
//            	 entity.setCurrentStatus(status);}
//	        if (bookingResponse.getSubServiceId() != null) entity.setSubServiceId(bookingResponse.getSubServiceId());
//	        if (bookingResponse.getSubServiceName() != null) entity.setSubServiceName(bookingResponse.getSubServiceName());
//	        if (bookingResponse.getReasonForCancel() != null) entity.setReasonForCancel(bookingResponse.getReasonForCancel());
//	        if (bookingResponse.getTotalFee() != 0) entity.setTotalFee(bookingResponse.getTotalFee());
//	        if (bookingResponse.getFreeFollowUpsLeft() != null) entity.setFreeFollowUpsLeft(bookingResponse.getFreeFollowUpsLeft());
//	        if (bookingResponse.getFreeFollowUps() != null) entity.setFreeFollowUps(bookingResponse.getFreeFollowUps());
//	        if (bookingResponse.getVisitCount() != null) entity.setVisitCount(bookingResponse.getVisitCount());
//	        if (bookingResponse.getFollowupDate() != null) entity.setFollowupDate(bookingResponse.getFollowupDate());
//
//	        // --- Update sitting summary ---
//	        if (bookingResponse.getTotalSittings() != null) entity.setTotalSittings(bookingResponse.getTotalSittings());
//	        if (bookingResponse.getTakenSittings() != null) entity.setTakenSittings(bookingResponse.getTakenSittings());
//	        if (bookingResponse.getPendingSittings() != null) entity.setPendingSittings(bookingResponse.getPendingSittings());
//	        if (bookingResponse.getCurrentSitting() != null) entity.setCurrentSitting(bookingResponse.getCurrentSitting());
//
//	        // --- Update treatments (map + sitting summary inside) ---
//	        if (bookingResponse.getTreatments() != null) {
//	            entity.setTreatments(bookingResponse.getTreatments());
//	        }
//
//	        // --- Save updated booking ---
//	        Booking updatedBooking = repository.save(entity);
//
//	        // --- Build response DTO ---
//	        BookingResponse responseDTO = new BookingResponse();
//	        responseDTO.setBookingId(updatedBooking.getBookingId());
//	        responseDTO.setBookingFor(updatedBooking.getBookingFor());
//	        responseDTO.setName(updatedBooking.getName());
//	        responseDTO.setRelation(updatedBooking.getRelation());
//	        responseDTO.setPatientMobileNumber(updatedBooking.getPatientMobileNumber());
//	        responseDTO.setPatientId(updatedBooking.getPatientId());
//	        responseDTO.setVisitType(updatedBooking.getVisitType());
//	        responseDTO.setFreeFollowUpsLeft(updatedBooking.getFreeFollowUpsLeft());
//	        responseDTO.setFreeFollowUps(updatedBooking.getFreeFollowUps());
//	        responseDTO.setPatientAddress(updatedBooking.getPatientAddress());
//	        responseDTO.setAge(updatedBooking.getAge());
//	        responseDTO.setGender(updatedBooking.getGender());
//	        responseDTO.setMobileNumber(updatedBooking.getMobileNumber());
//	        responseDTO.setCustomerId(updatedBooking.getCustomerId());
//	        responseDTO.setConsultationExpiration(updatedBooking.getConsultationExpiration());
//	        responseDTO.setCustomerDeviceId(updatedBooking.getCustomerDeviceId());
//	        responseDTO.setProblem(updatedBooking.getProblem());
//	        responseDTO.setSymptomsDuration(updatedBooking.getSymptomsDuration());
//	        responseDTO.setClinicId(updatedBooking.getClinicId());
//	        responseDTO.setClinicName(updatedBooking.getClinicName());
//	        responseDTO.setBranchId(updatedBooking.getBranchId());
//	        responseDTO.setBranchname(updatedBooking.getBranchname());
//	        responseDTO.setDoctorId(updatedBooking.getDoctorId());
//	        responseDTO.setDoctorName(updatedBooking.getDoctorName());
//	        responseDTO.setSubServiceId(updatedBooking.getSubServiceId());
//	        responseDTO.setSubServiceName(updatedBooking.getSubServiceName());
//	        responseDTO.setServiceDate(updatedBooking.getServiceDate());
//	        responseDTO.setServicetime(updatedBooking.getServicetime());
//	        responseDTO.setConsultationType(updatedBooking.getConsultationType());
//	        responseDTO.setConsultationFee(updatedBooking.getConsultationFee());
//	        responseDTO.setStatus(updatedBooking.getStatus());
//	        responseDTO.setTotalFee(updatedBooking.getTotalFee());
//
//	        // ✅ Sitting summary
//	        responseDTO.setTotalSittings(updatedBooking.getTotalSittings());
//	        responseDTO.setPendingSittings(updatedBooking.getPendingSittings());
//	        responseDTO.setTakenSittings(updatedBooking.getTakenSittings());
//	        responseDTO.setCurrentSitting(updatedBooking.getCurrentSitting());
//
//	        responseDTO.setBookedAt(updatedBooking.getBookedAt());
//
//	        // ✅ Treatments (with dates, sittings, status)
//	        responseDTO.setTreatments(updatedBooking.getTreatments());
//
//	        // --- Return wrapped response ---
//	        return new ResponseEntity<>(
//	                ResponseStructure.buildResponse(
//	                        responseDTO,
//	                        "Booking updated successfully",
//	                        HttpStatus.OK,
//	                        HttpStatus.OK.value()
//	                ),
//	                HttpStatus.OK
//	        );
//
//	    } catch (Exception e) {
//	        return new ResponseEntity<>(
//	                ResponseStructure.buildResponse(
//	                        null,
//	                        e.getMessage(),
//	                        HttpStatus.INTERNAL_SERVER_ERROR,
//	                        HttpStatus.INTERNAL_SERVER_ERROR.value()
//	                ),
//	                HttpStatus.INTERNAL_SERVER_ERROR
//	        );
//	    }
//	}
//
//
//	@Scheduled(cron = "0 01 0 * * ?")
//	////@Scheduled(fixedRate = 20000)
//	private void changingStatusFromConfirmedToCompleted() {
//	    try {
//	        List<Booking> bookings = repository.findAll();
//	        for (Booking b : bookings) {
//	            if (b.getStatus().equalsIgnoreCase("In-Progress")) {
//
//	                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
//	                LocalDateTime bookedDateTime = LocalDateTime.parse(b.getBookedAt(), inputFormatter);
//                   // System.out.println(bookedDateTime);
//
//	                ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
//	                LocalDate todayDate = istTime.toLocalDate(); // only date part
//	                LocalDate bookedDate = bookedDateTime.toLocalDate(); // only date part
//                   // System.out.println(bookedDate);
//	                long gap = ChronoUnit.DAYS.between(bookedDate, todayDate);
//                   // System.out.println(gap);
//	                int expirationDays = Integer.parseInt(Character.toString(b.getConsultationExpiration().charAt(0)) +
//	            			Character.toString(b.getConsultationExpiration().charAt(1)));
//                   // System.out.println(expirationDays);
//
//	                if (gap > expirationDays) {
//	                    b.setStatus("Completed");
//	                    repository.save(b);
//
//	                    NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
//	                    n.getData().setStatus("Completed");
//	                    notificationFeign.updateNotification(n);
//	                    //System.out.println("Updated to Completed for bookingId: " + b.getBookingId());
//	                    }}}}catch (Exception e) {}}
//
//
//
//	@Scheduled(cron = "0 30 0 * * ?")
//	////@Scheduled(fixedRate = 20000)
//	private void secondTimeChangingStatusFromConfirmedToCompleted() {
//	    try {
//	        List<Booking> bookings = repository.findAll();
//	        for (Booking b : bookings) {
//	            if (b.getStatus().equalsIgnoreCase("In-Progress")) {
//
//	                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
//	                LocalDateTime bookedDateTime = LocalDateTime.parse(b.getBookedAt(), inputFormatter);
//	               // System.out.println(bookedDateTime);
//	                ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
//	                LocalDate todayDate = istTime.toLocalDate(); // only date part
//	                LocalDate bookedDate = bookedDateTime.toLocalDate(); // only date part
//	               // System.out.println(bookedDate);
//	                long gap = ChronoUnit.DAYS.between(bookedDate, todayDate);
//	                int expirationDays = Integer.parseInt(Character.toString(b.getConsultationExpiration().charAt(0)) +
//	            			Character.toString(b.getConsultationExpiration().charAt(1)));
//	                //System.out.println(gap);
//	               // System.out.println(expirationDays);
//	                if (gap > expirationDays) {
//	                    b.setStatus("Completed");
//	                    repository.save(b);
//
//	                    NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
//	                    n.getData().setStatus("Completed");
//	                    notificationFeign.updateNotification(n);
//
//	                    //System.out.println("Updated to Completed for bookingId: " + b.getBookingId());
//	                }}}}catch (Exception e) {}}
//
//

	@Scheduled(fixedRate = 60 * 60 * 1000)
		@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public void autoCalculatePatientCompletedAppointments() {
		Map<String,Integer> map = new LinkedHashMap<>();
		Set<String> ids = new LinkedHashSet<>();
		try {
			List<Booking> existingBooking = repository.findAll();
			//System.out.println("existingBooking");
			if(existingBooking != null && !existingBooking.isEmpty()){
				//System.out.println("not null");
				for(Booking b:existingBooking) {
					if(b.getStatus().equalsIgnoreCase("Completed")) {
						//System.out.println("find complted");
						if(!ids.contains(b.getPatientId())){
							List<Booking> bookings = repository.findByPatientId(b.getPatientId());
							ids.add(b.getPatientId());
							//System.out.println("got obj by patient id");
							for(Booking c:bookings) {
								if(c.getStatus().equalsIgnoreCase("Completed")) {
									//System.out.println("patient id with cmplted");
									if(map.containsKey(b.getPatientId())){
										//System.out.println("adding to map");
										Integer value = map.get(b.getPatientId());
										int vlue = value.intValue();
										vlue += 1;
										value = Integer.valueOf(vlue);
										map.put(b.getPatientId(),value);
									}else{
										map.put(b.getPatientId(),1);
									}
									for(String key : map.keySet()) {
										List<Booking> bkings = repository.findByPatientId(key);
										//System.out.println("got obj with key in map");
										for(Booking bkng : bkings ) {
											bkng.setVisitCount(map.get(key));
											repository.save(bkng);}
									}}}}}}}}catch(Exception e) {}
	}



	//---------------------------to get patientdetails by bookingId,pateintId,mobileNumber---------------------------
	@Override
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public Response getPatientDetailsForConsetForm(String bookingId, String patientId, String mobileNumber) {
		try {
			Optional<Booking> optionalBooking = repository.findByBookingIdAndPatientIdAndMobileNumber(bookingId, patientId, mobileNumber);
			if (optionalBooking.isPresent()) {
				Booking booking = optionalBooking.get();
				if(booking.getStatus().equalsIgnoreCase("Confirmed") || booking.getStatus().equalsIgnoreCase("Completed")){
					BookingResponse response = new ObjectMapper().convertValue(booking, BookingResponse.class);
					return Response.builder()
							.success(true)
							.status(200)
							.message("Booking details fetched successfully.")
							.data(response)
							.build();
				}else{
					return Response.builder()
							.success(false)
							.status(404)
							.message("No booking found with the given details.")
							.build();
				}}else{
				return Response.builder()
						.success(false)
						.status(404)
						.message("No booking found with the given details.")
						.build();}
		}catch(Exception e){
			return Response.builder()
					.success(false)
					.status(500)
					.message(e.getMessage())
					.build();}}

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getInProgressAppointments(
			String number,
			int page,
			int size) {

		ResponseStructure<Map<String, Object>> res =
				new ResponseStructure<>();

		try {

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.DESC, "createdAt")
			);

			Page<Booking> booked =
					repository
							.findByMobileNumberAndStatusIgnoreCase(
									number,
									"In-Progress",
									pageable
							);

			if (booked != null && !booked.isEmpty()) {

				List<BookingResponse> response =
						booked.getContent()
								.stream()
								.map(this::toResponse)
								.toList();

				Map<String, Object> map =
						new LinkedHashMap<>();

				map.put("content", response);
				map.put("currentPage", booked.getNumber());
				map.put("totalItems", booked.getTotalElements());
				map.put("totalPages", booked.getTotalPages());
				map.put("pageSize", booked.getSize());

				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setData(map);
				res.setMessage("In-Progress appointments found");

			} else {

				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setMessage("In-Progress appointments not found");
			}

		} catch (Exception e) {
		return ResponseEntity
				.status(res.getStatusCode())
				.body(res);
	}	return ResponseEntity.status(res.getStatusCode()).body(res);}


		@Override
		@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
		public ResponseEntity<?> getInProgressAppointmentsByCustomerId(String customerId) {

		    try {

		        List<Booking> bookings =
		                repository.findByCustomerIdAndStatusIgnoreCase(
		                        customerId,
		                        "In-Progress"
		                );

		        if (bookings == null || bookings.isEmpty()) {
		            return ResponseEntity.status(HttpStatus.NOT_FOUND)
		                    .body(ResponseStructure.buildResponse(
		                            null,
		                            "No in-progress bookings found",
		                            HttpStatus.NOT_FOUND,
		                            404
		                    ));
		        }

		        List<BookingResponse> bookingResponses = bookings.stream()
		                .peek(this::nullifyLargeFields)
		                .map(booking -> {
		                    BookingResponse response =
		                            new ObjectMapper().convertValue(
		                                    booking,
		                                    BookingResponse.class
		                            );

		                    String pdf = getPrescriptionpdf(response.getBookingId());

		                    if (pdf != null) {
		                        response.setPrescriptionPdf(
		                                Collections.singletonList(pdf)
		                        );
		                    }

		                    return response;
		                })
		                .toList();

		        return ResponseEntity.ok(
		                ResponseStructure.buildResponse(
		                        bookingResponses,
		                        "In-Progress appointments found",
		                        HttpStatus.OK,
		                        HttpStatus.OK.value()
		                )
		        );

		    } catch (Exception e) {

		        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
		                .body(ResponseStructure.buildResponse(
		                        null,
		                        "Internal server error: " + e.getMessage(),
		                        HttpStatus.INTERNAL_SERVER_ERROR,
		                        500
		                ));
		    }
		}



		@Override
		@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
		public ResponseEntity<?> getInProgressAppointmentsByPatientId(String patientId, String clinicId) {

		    try {

		        List<Booking> bookings =
		                repository.findByPatientIdAndClinicId(patientId, clinicId);

			if (bookings == null || bookings.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseStructure.buildResponse(
								null,
								"No bookings found for this patient",
								HttpStatus.NOT_FOUND,
								404
						));
			}
		        List<BookingResponse> bookingResponses = bookings.stream()
		                .filter(booking ->
		                        "In-Progress".equalsIgnoreCase(booking.getStatus()))
		                .peek(this::nullifyLargeFields)
		                .map(booking -> {

		                    BookingResponse response =
		                            new ObjectMapper().convertValue(
		                                    booking,
		                                    BookingResponse.class);

		                    String pdf = getPrescriptionpdf(booking.getBookingId());

		                    if (pdf != null) {
		                        response.setPrescriptionPdf(
		                                Collections.singletonList(pdf));
		                    }

		                    return response;
		                })
		                .toList();

		        if (bookingResponses.isEmpty()) {
		            return ResponseEntity.status(HttpStatus.NOT_FOUND)
		                    .body(ResponseStructure.buildResponse(
		                            null,
		                            "No In-Progress appointments found for this patient",
		                            HttpStatus.NOT_FOUND,
		                            404
		                    ));
		        }

		        return ResponseEntity.ok(
		                ResponseStructure.buildResponse(
		                        bookingResponses,
		                        "In-Progress appointments found",
		                        HttpStatus.OK,
		                        HttpStatus.OK.value()
		                )
		        );

		    } catch (Exception e) {

		        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
		                .body(ResponseStructure.buildResponse(
		                        null,
		                        "Internal server error: " + e.getMessage(),
		                        HttpStatus.INTERNAL_SERVER_ERROR,
		                        500
		                ));
		    }
		}
		
		/**
		 * ✅ Utility: Parse both yyyy-MM-dd and dd-MM-yyyy formats
		 */
		private LocalDate parseDate(String dateStr) {
		    if (dateStr == null) return null;
		    List<DateTimeFormatter> formatters = Arrays.asList(
		            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
		            DateTimeFormatter.ofPattern("dd-MM-yyyy")
		    );
		    for (DateTimeFormatter fmt : formatters) {
		        try {
		            return LocalDate.parse(dateStr, fmt);
		        } catch (Exception ignored) {}
		    }
		    return null;
		}


@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public List<BookingResponse> inprogressAppointmentsByConsultationExpiration(LocalDate exp,Booking booking, DoctorSaveDetailsDTO saveDetails ) {
		List<BookingResponse> finalList = new ArrayList<>();
		try {
			LocalDate today = LocalDate.now();
			LocalDate sixthDate = today.plusDays(6);
			DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			if(saveDetails.getFollowUp() != null &&
					saveDetails.getFollowUp().getNextFollowUpDate() != null) {
				try{
					int days = 0;
					if(exp == null ){
						days = Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", ""));
						//System.out.println(days);
						LocalDate serviceDate  = LocalDate.parse(booking.getServiceDate(), isoFormatter);
						exp = serviceDate.plusDays(days);}
					LocalDate followDate = LocalDate.parse(saveDetails.getFollowUp().getNextFollowUpDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
					if (!followDate.isBefore(today) && !followDate.isAfter(sixthDate) && !followDate.isAfter(exp)) {
						Booking bkng = new Booking(booking);
						bkng.setFollowupDate(followDate.format(isoFormatter));
						bkng.setStatus("In-Progress");
						finalList.add(toResponse(bkng));
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}else {
				// ✅ Consultation expiration fallback
				if (booking.getConsultationExpiration() != null) {
					try {
						int days = 0;
						if(exp == null ){
							days = Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", ""));
							// System.out.println(days);
							LocalDate serviceDate  = LocalDate.parse(booking.getServiceDate(), isoFormatter);
							exp = serviceDate.plusDays(days);}
						// System.out.println(expDate);
						for (int i = 0; i <= 6; i++) {
							LocalDate date = today.plusDays(i);
							//System.out.println(date);
							// System.out.println(sixthDate);
							if ((!date.isAfter(sixthDate)) && (date.isBefore(exp) || date.equals(exp))) {
								//System.out.println("hii");
								Booking bkng = new Booking(booking);
								//bkng.setConsentFormPdf(null);
								//bkng.setAttachments(null);
								//bkng.setReports(null);
								// System.out.println(bkng);
								bkng.setFollowupDate(date.format(isoFormatter));
								bkng.setStatus("In-Progress");
								finalList.add(toResponse(bkng));
							}}
					}catch (Exception e){
						System.out.println(e.getMessage());
					}}}}catch(Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
		return finalList;
	}


	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getDoctorFutureAppointments(
			String doctorId,
			int page,
			int size) {

		ResponseStructure<Map<String, Object>> res =
				new ResponseStructure<>();

		List<Map<String, Object>> list =
				new ArrayList<>();

		try {

			LocalDate currentDate = LocalDate.now();
			LocalDate plusDate = currentDate.plusDays(15);

			String fromDate = currentDate.toString();
			String toDate = plusDate.toString();

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.ASC, "serviceDate")
			);

			Page<Booking> booked =
					repository
							.findByDoctorIdAndServiceDateBetween(
									doctorId,
									fromDate,
									toDate,
									pageable
							);

			if (booked != null && !booked.isEmpty()) {

				List<BookingResponse> response =
						booked.getContent()
								.stream()
								.map(this::toResponse)
								.toList();

				response.stream().map(n -> {

					Map<String, Object> map =
							new LinkedHashMap<>();
					map.put("bookingId", n.getBookingId());
					map.put("serviceDate", n.getServiceDate());
					map.put("servicetime", n.getServicetime());
					map.put("name", n.getName());

					map.put(
							"mobileNumber",
							n.getPatientMobileNumber() != null
									&& !n.getPatientMobileNumber().isEmpty()
									? n.getPatientMobileNumber()
									: n.getMobileNumber()
					);
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

				Map<String, Object> responseMap =
						new LinkedHashMap<>();

				responseMap.put("content", list);
				responseMap.put("currentPage", booked.getNumber());
				responseMap.put("totalItems", booked.getTotalElements());
				responseMap.put("totalPages", booked.getTotalPages());
				responseMap.put("pageSize", booked.getSize());

				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setData(responseMap);
				res.setMessage("appointments found");

			} else {

				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setMessage("appointments not found");
			}

		} catch (Exception e) {

			res.setStatusCode(500);
			res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			res.setMessage(e.getMessage());
		}

		return ResponseEntity
				.status(res.getStatusCode())
				.body(res);
	}



	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public Page<BookingResponse> bookingByBranchId(
			String branchId,
			int page,
			int size) {

		// ================= PAGINATION =================

		Pageable pageable = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Direction.DESC, "createdAt"));

		// If createdAt field not available use "_id"

		Page<Booking> bookingPage =
				repository.findByBranchId(
						branchId,
						pageable);

		// ================= EMPTY CHECK =================

		if (bookingPage.isEmpty()) {

			return Page.empty();
		}

		// ================= ENTITY TO DTO =================

		return bookingPage.map(this::toResponse);
	}


	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getBookedServicesByClinicIdWithBranchId(
			String clinicId,
			String branchId,
			int page,
			int size) {

		ResponseStructure<Map<String, Object>> res =
				new ResponseStructure<>();

		List<Map<String, Object>> list =
				new ArrayList<>();

		try {

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.DESC, "createdAt")
			);

			Page<Booking> bookings =
					repository.findByClinicIdAndBranchId(
							clinicId,
							branchId,
							pageable
					);

			if (bookings == null || bookings.isEmpty()) {

				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setMessage("Appointments Not Found");

				return ResponseEntity
						.status(200)
						.body(res);
			}

			List<BookingResponse> response =
					bookings.getContent()
							.stream()
							.map(this::toResponse)
							.toList();

			response.stream().map(n -> {

				Map<String, Object> map =
						new LinkedHashMap<>();

				map.put("bookingId", n.getBookingId());
				map.put("serviceDate", n.getServiceDate());
				map.put("servicetime", n.getServicetime());
				map.put("name", n.getName());

				map.put(
						"mobileNumber",
						n.getPatientMobileNumber() != null
								&& !n.getPatientMobileNumber().isEmpty()
								? n.getPatientMobileNumber()
								: n.getMobileNumber()
				);

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

			Map<String, Object> responseMap =
					new LinkedHashMap<>();

			responseMap.put("content", list);
			responseMap.put("currentPage", bookings.getNumber());
			responseMap.put("totalItems", bookings.getTotalElements());
			responseMap.put("totalPages", bookings.getTotalPages());
			responseMap.put("pageSize", bookings.getSize());

			res.setStatusCode(200);
			res.setHttpStatus(HttpStatus.OK);
			res.setData(responseMap);
			res.setMessage("Appointments Found");

			return ResponseEntity
					.status(200)
					.body(res);

		} catch (Exception e) {

			res.setStatusCode(500);
			res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			res.setMessage(e.getMessage());

			return ResponseEntity
					.status(500)
					.body(res);
		}
	}


	
	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus(
			String clinicId,
			String branchId,
			String doctorId,
			String status,
			int page,
			int size) {

		try {

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.DESC, "createdAt")
			);

			List<Map<String, Object>> list = new ArrayList<>();

			Page<Booking> bookingsPage = Page.empty();

			LocalDate currentDate =
					LocalDate.now(ZoneId.of("Asia/Kolkata"));

			// ================= BRANCH != ALL =================

			if (!branchId.equalsIgnoreCase("all")) {

				// ---------- PENDING ----------
				if (status.equalsIgnoreCase("pending")) {

					String requiredStatus = "confirmed";

					bookingsPage =
							repository
									.findByClinicIdAndBranchIdAndDoctorIdAndStatusIgnoreCase(
											clinicId,
											branchId,
											doctorId,
											requiredStatus,
											pageable
									);

					List<Booking> filtered =
							bookingsPage.getContent()
									.stream()
									.filter(b -> {
										LocalDate bookingDate =
												LocalDate.parse(
														b.getServiceDate()
												);

										return bookingDate.isBefore(currentDate);
									})
									.toList();

					bookingsPage =
							new PageImpl<>(
									filtered,
									pageable,
									filtered.size()
							);

				}

				// ---------- CONFIRMED ----------
				else if (status.equalsIgnoreCase("confirmed")) {

					bookingsPage =
							repository
									.findByClinicIdAndBranchIdAndDoctorIdAndStatusIgnoreCase(
											clinicId,
											branchId,
											doctorId,
											status,
											pageable
									);

					List<Booking> filtered =
							bookingsPage.getContent()
									.stream()
									.filter(b -> {

										LocalDate bookingDate =
												LocalDate.parse(
														b.getServiceDate()
												);

										return bookingDate.isAfter(currentDate);

									})
									.toList();

					bookingsPage =
							new PageImpl<>(
									filtered,
									pageable,
									filtered.size()
							);
				}

				// ---------- OTHER STATUS ----------
				else {

					bookingsPage =
							repository
									.findByClinicIdAndBranchIdAndDoctorIdAndStatusIgnoreCase(
											clinicId,
											branchId,
											doctorId,
											status,
											pageable
									);

					if (bookingsPage.isEmpty()) {

						bookingsPage =
								repository
										.findByClinicIdAndBranchIdAndDoctorIdAndFollowupStatusIgnoreCase(
												clinicId,
												branchId,
												doctorId,
												status,
												pageable
										);
					}
				}

			}

			// ================= BRANCH = ALL =================

			else {

				// ---------- PENDING ----------
				if (status.equalsIgnoreCase("pending")) {

					String requiredStatus = "confirmed";

					bookingsPage =
							repository
									.findByClinicIdAndDoctorIdAndStatusIgnoreCase(
											clinicId,
											doctorId,
											requiredStatus,
											pageable
									);

					List<Booking> filtered =
							bookingsPage.getContent()
									.stream()
									.filter(b -> {

										LocalDate bookingDate =
												LocalDate.parse(
														b.getServiceDate()
												);

										return bookingDate.isBefore(currentDate);

									})
									.toList();

					bookingsPage =
							new PageImpl<>(
									filtered,
									pageable,
									filtered.size()
							);
				}

				// ---------- CONFIRMED ----------
				else if (status.equalsIgnoreCase("confirmed")) {

					bookingsPage =
							repository
									.findByClinicIdAndDoctorIdAndStatusIgnoreCase(
											clinicId,
											doctorId,
											status,
											pageable
									);

					List<Booking> filtered =
							bookingsPage.getContent()
									.stream()
									.filter(b -> {

										LocalDate bookingDate =
												LocalDate.parse(
														b.getServiceDate()
												);

										return bookingDate.isAfter(currentDate);

									})
									.toList();

					bookingsPage =
							new PageImpl<>(
									filtered,
									pageable,
									filtered.size()
							);
				}

				// ---------- OTHER STATUS ----------
				else {

					bookingsPage =
							repository
									.findByClinicIdAndDoctorIdAndStatusIgnoreCase(
											clinicId,
											doctorId,
											status,
											pageable
									);

					if (bookingsPage.isEmpty()) {

						bookingsPage =
								repository
										.findByClinicIdAndDoctorIdAndFollowupStatusIgnoreCase(
												clinicId,
												doctorId,
												status,
												pageable
										);
					}
				}
			}

			// ================= RESPONSE MAPPING =================

			if (!bookingsPage.isEmpty()) {

				List<BookingResponse> responses =
						bookingsPage.getContent()
								.stream()
								.map(this::toResponse)
								.toList();

				responses.stream().map(n -> {

					Map<String, Object> map =
							new LinkedHashMap<>();

					map.put("bookingId", n.getBookingId());
					map.put("serviceDate", n.getServiceDate());
					map.put("servicetime", n.getServicetime());
					map.put("name", n.getName());

					map.put(
							"mobileNumber",
							n.getPatientMobileNumber() != null
									&& !n.getPatientMobileNumber().isEmpty()
									? n.getPatientMobileNumber()
									: n.getMobileNumber()
					);

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

				Map<String, Object> responseMap =
						new LinkedHashMap<>();

				responseMap.put("content", list);
				responseMap.put("currentPage", bookingsPage.getNumber());
				responseMap.put("totalItems", bookingsPage.getTotalElements());
				responseMap.put("totalPages", bookingsPage.getTotalPages());
				responseMap.put("pageSize", bookingsPage.getSize());

				return ResponseEntity.status(HttpStatus.OK)
						.body(new Response(
								true,
								responseMap,
								null,
								"appointments are found",
								200,
								null,
								null
						));
			}

			else {

				return ResponseEntity.status(HttpStatus.OK)
						.body(new Response(
								true,
								null,
								null,
								"appointments are not found",
								200,
								null,
								null
						));
			}

		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new Response(
							false,
							null,
							null,
							e.getMessage(),
							500,
							null,
							null
					));
		}
	}

//		@Scheduled(cron = "0 02 00 * * ?")
//		////@Scheduled(fixedRate = 20000)
//		private void changingStatusFromInprogressToCompletedForSittings() {
//			 try {
//		        List<Booking> bookings = repository.findAll();
//			        for (Booking b : bookings) {
//			            if (b.getStatus().equalsIgnoreCase("In-Progress")) {
//			            	Response res = doctorFeign.getDoctorSaveDetailsByBookingId(b.getBookingId()).getBody();
//			            	//System.out.println(b.getBookingId());
//			            	DoctorSaveDetailsDTO dto = new ObjectMapper().convertValue(res.getData(),DoctorSaveDetailsDTO.class);
//			            	//System.out.println(dto);
//			            	if(dto != null) {
//			            	if(dto.getTreatments() != null) {
//			            	for(Map.Entry<String,TreatmentDetailsDTO> mp : dto.getTreatments().getGeneratedData().entrySet()){
//			            	TreatmentDetailsDTO treatments = mp.getValue();
//			            	//System.out.println(treatments);
//			            	if(treatments != null) {
//			            	 List<DatesDTO> dates =	treatments.getDates();
//			            	 //System.out.println(dates.size());
//			            	 int lastIndex = dates.size()-1;
//			            	 DatesDTO datesDTO = dates.get(lastIndex);
//			            	// System.out.println("last index"+datesDTO );
//			            	 String date = datesDTO.getDate();
//			            	 //System.out.println(date);
//			                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//			                LocalDate lastSitting = LocalDate.parse(date, inputFormatter);
//		                    //System.out.println(lastSitting);
//			                LocalDate todayDate = LocalDate.now();
//		                  // System.out.println(todayDate);
//		                   // System.out.println(gap);
//			                int expirationDays = Integer.parseInt(Character.toString(b.getConsultationExpiration().charAt(0)) +
//			            			Character.toString(b.getConsultationExpiration().charAt(1)));
//		                    //System.out.println(expirationDays);
//			                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//			                LocalDate serviceDate = LocalDate.parse(b.getServiceDate(),formatter);
//			                LocalDate plusedTime = serviceDate.plusDays(expirationDays);
//			                if(!lastSitting.isBefore(plusedTime)) {  /// if it is before plustime they its status should be in inprogress
//			                	  if(!lastSitting.isBefore(serviceDate) && lastSitting.isBefore(todayDate)) {
//			                		b.setStatus("Completed");
//			                		//System.out.println("status changed");
//			                		  repository.save(b);
//			                		  try {
//			                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
//					                    n.getData().setStatus("Completed");
//					                    notificationFeign.updateNotification(n);
//			                		  }catch(Exception e) {
//			                			  System.out.println(e.getMessage());
//			                		  }
//			                	}}else{
//			                		if(todayDate.isAfter(plusedTime)) {
//				                		b.setStatus("Completed");
//				                		//System.out.println("status changed");
//				                		  repository.save(b);
//				                		  try {
//					                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
//							                    n.getData().setStatus("Completed");
//							                    notificationFeign.updateNotification(n);
//					                		  }catch(Exception e) {
//					                			  System.out.println(e.getMessage());
//					                		  }}}}}}else{
//				                	DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//						            LocalDate followUpDate = LocalDate.parse(dto.getFollowUp().getNextFollowUpDate(), inputFormatter);
//					                    //System.out.println(followUpDate);
//						             DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//						             LocalDate serviceDate = LocalDate.parse(b.getServiceDate(),formatter);
//						             LocalDate todayDate = LocalDate.now();
//						             if(!followUpDate.isBefore(serviceDate) && followUpDate.isBefore(todayDate)) {
//						                	b.setStatus("Completed");
//						                	//System.out.println("status changed");
//					                		  repository.save(b);
//					                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
//							                    n.getData().setStatus("Completed");
//							                    notificationFeign.updateNotification(n);
//						               }}}}}}catch (Exception e) {
//						            	   System.out.println(e.getMessage());
//						               }}
//
//
//
//
//		@Scheduled(cron = "0 30 00 * * ?")
//		private void SecondTimeChangingStatusFromInprogressToCompletedForSittings() {
//		    try {
//		        List<Booking> bookings = repository.findAll();
//		        for (Booking b : bookings) {
//		            if (b.getStatus().equalsIgnoreCase("In-Progress")) {
//		            	Response res = doctorFeign.getDoctorSaveDetailsByBookingId(b.getBookingId()).getBody();
//		            	//System.out.println(b.getBookingId());
//		            	DoctorSaveDetailsDTO dto = new ObjectMapper().convertValue(res.getData(),DoctorSaveDetailsDTO.class);
//		            	//System.out.println(dto);
//		            	if(dto != null) {
//		            	if(dto.getTreatments() != null) {
//		            	for(Map.Entry<String,TreatmentDetailsDTO> mp : dto.getTreatments().getGeneratedData().entrySet()){
//		            	TreatmentDetailsDTO treatments = mp.getValue();
//		            	//System.out.println(treatments);
//		            	if(treatments != null) {
//		            	 List<DatesDTO> dates =	treatments.getDates();
//		            	 //System.out.println(dates.size());
//		            	 int lastIndex = dates.size()-1;
//		            	 DatesDTO datesDTO = dates.get(lastIndex);
//		            	// System.out.println("last index"+datesDTO );
//		            	 String date = datesDTO.getDate();
//		            	 //System.out.println(date);
//		                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//		                LocalDate lastSitting = LocalDate.parse(date, inputFormatter);
//	                    //System.out.println(lastSitting);
//		                LocalDate todayDate = LocalDate.now();
//	                  // System.out.println(todayDate);
//	                   // System.out.println(gap);
//		                int expirationDays = Integer.parseInt(Character.toString(b.getConsultationExpiration().charAt(0)) +
//		            			Character.toString(b.getConsultationExpiration().charAt(1)));
//	                    //System.out.println(expirationDays);
//		                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//		                LocalDate serviceDate = LocalDate.parse(b.getServiceDate(),formatter);
//		                LocalDate plusedTime = serviceDate.plusDays(expirationDays);
//		                if(!lastSitting.isBefore(plusedTime)) {  /// if it is before plustime they its status should be in inprogress
//		                	  if(!lastSitting.isBefore(serviceDate) && lastSitting.isBefore(todayDate)) {
//		                		b.setStatus("Completed");
//		                		//System.out.println("status changed");
//		                		  repository.save(b);
//		                		  try {
//		                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
//				                    n.getData().setStatus("Completed");
//				                    notificationFeign.updateNotification(n);
//		                		  }catch(Exception e) {
//		                			  System.out.println(e.getMessage());
//		                		  }
//		                	}}else{
//		                		if(todayDate.isAfter(plusedTime)) {
//			                		b.setStatus("Completed");
//			                		//System.out.println("status changed");
//			                		  repository.save(b);
//			                		  try {
//				                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
//						                    n.getData().setStatus("Completed");
//						                    notificationFeign.updateNotification(n);
//				                		  }catch(Exception e) {
//				                			  System.out.println(e.getMessage());
//				                		  }}}}}}else{
//			                	DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//					            LocalDate followUpDate = LocalDate.parse(dto.getFollowUp().getNextFollowUpDate(), inputFormatter);
//				                    //System.out.println(followUpDate);
//					             DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//					             LocalDate serviceDate = LocalDate.parse(b.getServiceDate(),formatter);
//					             LocalDate todayDate = LocalDate.now();
//					             if(!followUpDate.isBefore(serviceDate) && followUpDate.isBefore(todayDate)) {
//					                	b.setStatus("Completed");
//					                	//System.out.println("status changed");
//				                		  repository.save(b);
//				                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
//						                    n.getData().setStatus("Completed");
//						                    notificationFeign.updateNotification(n);
//					               }}}}}}catch (Exception e) {
//					            	   System.out.println(e.getMessage());
//					               }}
//

//    public ResponseEntity<?> retrieveOneWeekAppointments(String cinicId,String branchId){
//		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
//	    List<BookingResponse> finalList = new ArrayList<>();
//	   Response response = new Response();
//	    DoctorSaveDetailsDTO saveDetails = new DoctorSaveDetailsDTO();
//	    try {
//	        List<Booking> booked = repository.findByClinicIdAndBranchId(cinicId, branchId);
//	        if (booked == null || booked.isEmpty()) {
//	            res.setStatusCode(200);
//	            res.setHttpStatus(HttpStatus.OK);
//	            res.setMessage("No bookings found for customer");
//	            res.setData(finalList);
//	            return ResponseEntity.ok(res);
//	        }
//	        LocalDate today = LocalDate.now();
//	        LocalDate sixthDate = today.plusDays(6);
//	        LocalDate exp = null;
//	        DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//	        for (Booking booking : booked) {
//	            if (!"In-Progress".equalsIgnoreCase(booking.getStatus())) {
//	                continue;
//	            }
//	            // ✅ Include any In-Progress booking with serviceDate between today and sixthDate
//	            LocalDate ld = null;
//	            if(booking.getFollowupDate() != null) {
//	            	ld = LocalDate.parse(booking.getFollowupDate(), isoFormatter);
//	            }else {
//	            	if(booking.getServiceDate() != null) {
//		            	ld = LocalDate.parse(booking.getServiceDate(), isoFormatter);
//		            }
//	            }
//	            if(!ld.isAfter(sixthDate)) {
//	            // Fetch doctor save details
//	            try {
//	                response = doctorFeign.getDoctorSaveDetailsByBookingId(booking.getBookingId()).getBody();
//	               // System.out.println(response);
//	            } catch (Exception e) {
//	                System.out.println(e.getMessage());
//	            }
//
//	            if (response != null && response.getData() != null) {
//	                saveDetails = new ObjectMapper().convertValue(response.getData(), DoctorSaveDetailsDTO.class);
//	               // System.out.println(saveDetails);
//	            } else {}
//	            // ✅ Stream-based Treatments + Sittings + Next Follow-Up
//	            if (saveDetails.getTreatments() != null &&
//	                saveDetails.getTreatments().getGeneratedData() != null &&
//	                !saveDetails.getTreatments().getGeneratedData().isEmpty()) {
//
//	                // Flatten all treatment dates into sorted list
//	                List<LocalDate> allDates = saveDetails.getTreatments().getGeneratedData().values().stream()
//	                        .filter(Objects::nonNull)
//	                        .flatMap(details -> details.getDates() != null
//	                                ? details.getDates().stream()
//	                                    .map(d -> LocalDate.parse(d.getDate(), isoFormatter))
//	                                : Stream.empty())
//	                        .sorted()
//	                        .toList();
//                        //System.out.println(allDates);
//	                // Calculate totalSittings and pendingSittings
//	                int totalSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
//	                        .filter(Objects::nonNull)
//	                        .mapToInt(d -> d.getTotalSittings() != null ? d.getTotalSittings() : 0)
//	                        .sum();
//	               // System.out.println(totalSittingsSum);
//	                int pendingSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
//	                        .filter(Objects::nonNull)
//	                        .mapToInt(d -> d.getSittings() != null ? d.getSittings() : 0)
//	                        .sum();
//	                //System.out.println(pendingSittingsSum);
//	                AtomicInteger takenSittingsCount = new AtomicInteger(0);
//
//	                // Determine next follow-up date according to your rules
//	                Optional<LocalDate> nextFollowupOpt = allDates.stream()
//	                        .map(sittingDate -> {
//	                            boolean sittingBooked = false;
//	                            try {
//	                                Booking resp =
//	                                		repository.findByPatientIdAndFollowupDate(
//	                                				sd.getPatientId(),
//	                                                sittingDate.format(isoFormatter));
//
//	                                if (resp != null) {
//	                                    sittingBooked = true;
//	                                    takenSittingsCount.incrementAndGet();
//	                                }
//	                            } catch (Exception ex) {
//	                                System.err.println("Error checking booking for date "
//	                                        + sittingDate + ": " + ex.getMessage());
//	                            }
//                                if(booking.getFollowupDate() != null) {
//	                            if (LocalDate.parse(booking.getFollowupDate(), isoFormatter).isBefore(today)) {
//	                                // Past date → pick first future date if available
//	                                return allDates.stream()
//	                                        .filter(f -> f.isAfter(today))
//	                                        .findFirst()
//	                                        .orElse(null);
//	                            } else if (LocalDate.parse(booking.getFollowupDate(), isoFormatter).isEqual(today)) {
//	                                if (sittingBooked) {
//	                                    // Today booked → pick first future date if available
//	                                    return allDates.stream()
//	                                            .filter(f -> f.isAfter(today))
//	                                            .findFirst()
//	                                            .orElse(null);
//	                                } else {
//	                                    // Today not booked → keep today
//	                                    return sittingDate;
//	                                }
//	                            } else {
//	                                // Future dates → do not modify follow-up
//	                                return null;
//	                            }
//	                        }else{
//	                        	 return allDates.stream()
//	                                        .findFirst()
//	                                        .orElse(null);
//	                        }})
//	                        .filter(Objects::nonNull)
//	                        .findFirst();
//	                LocalDate lstSitting = allDates.get(allDates.size()-1);
//	                if(lstSitting.equals(LocalDate.now()) || lstSitting.isAfter(LocalDate.now())) {
//	                	exp = lstSitting.plusDays(Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", "")));
//	                	finalList = inprogressAppointmentsByConsultationExpiration(exp, booking,saveDetails);
//	                }
//	                // Update booking object
//	                booking.setTotalSittings(totalSittingsSum);
//	                booking.setPendingSittings(pendingSittingsSum);
//	                booking.setTakenSittings(takenSittingsCount.get());
//	                booking.setCurrentSitting(pendingSittingsSum);
//	                nextFollowupOpt.ifPresent(d -> booking.setFollowupDate(d.format(isoFormatter)));
//	                finalList.add(toResponse(booking));
//	                //System.out.println(nextFollowupOpt);
//	                //System.out.println(finalList);
//	            }
//	            // ✅ Existing Follow-Up section
//	            else if (saveDetails.getFollowUp() != null &&
//	                saveDetails.getFollowUp().getNextFollowUpDate() != null) {
//	                try {
//	                	 int days = 0;
//		                	if(exp == null ){
//		                    days = Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", ""));
//		                    //System.out.println(days);
//		                    LocalDate serviceDate  = LocalDate.parse(booking.getServiceDate(), isoFormatter);
//		                    exp = serviceDate.plusDays(days);}
//	                    LocalDate followDate = LocalDate.parse(saveDetails.getFollowUp().getNextFollowUpDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
//	                    if (!followDate.isBefore(today) && !followDate.isAfter(sixthDate) && !followDate.isAfter(exp)) {
//	                        Booking bkng = new Booking(booking);
//	                        bkng.setFollowupDate(followDate.format(isoFormatter));
//	                        bkng.setStatus("In-Progress");
//	                            finalList.add(toResponse(bkng));
//	                    }
//	                } catch (Exception e) {
//	                    System.out.println(e.getMessage());
//	                }
//	            }}
//	        }
//	        res.setStatusCode(200);
//	        res.setHttpStatus(HttpStatus.OK);
//	        res.setMessage(finalList.isEmpty()
//	                ? "No In-Progress or Today appointments found"
//	                : "In-Progress appointments found");
//	        res.setData(finalList);
//
//	    } catch (Exception e) {
//	        res.setStatusCode(500);
//	        res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
//	        res.setMessage("Error: " + e.getMessage());
//	    }
//
//	    return ResponseEntity.status(res.getStatusCode()).body(res);
//	}
//

//		public ResponseEntity<?> retrieveOneWeekAppointments(String clinicId, String branchId) {
//		    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
//		    List<BookingResponse> finalList = new ArrayList<>();
//		    Response response = new Response();
//		    DoctorSaveDetailsDTO saveDetails = new DoctorSaveDetailsDTO();
//
//		    try {
//		        List<Booking> booked = repository.findByClinicIdAndBranchId(clinicId, branchId);
//		        if (booked == null || booked.isEmpty()) {
//		            res.setStatusCode(200);
//		            res.setHttpStatus(HttpStatus.OK);
//		            res.setMessage("No bookings found for customer");
//		            res.setData(finalList);
//		            return ResponseEntity.ok(res);
//		        }
//
//		        LocalDate today = LocalDate.now();
//		        LocalDate sixthDate = today.plusDays(6);
//		        DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//		        for (Booking booking : booked) {
//		            // Include only In-Progress bookings
//		            if (!"In-Progress".equalsIgnoreCase(booking.getStatus())) {
//		                continue;
//		            }
//
//		            // Include both In-Clinic & Online Consultations
//		            if (!"In-Clinic Consultation".equalsIgnoreCase(booking.getConsultationType()) &&
//		                !"Services & Treatments".equalsIgnoreCase(booking.getConsultationType()) &&
//		                !"Online Consultation".equalsIgnoreCase(booking.getConsultationType())) {
//		                continue;
//		            }
//
//		            LocalDate ld = null;
//		            if (booking.getFollowupDate() != null) {
//		                ld = LocalDate.parse(booking.getFollowupDate(), isoFormatter);
//		            } else if (booking.getServiceDate() != null) {
//		                ld = LocalDate.parse(booking.getServiceDate(), isoFormatter);
//		            }
//
//		            if (ld == null || ld.isAfter(sixthDate)) {
//		                continue;
//		            }
//
//		            // fetch doctor details
//		            try {
//		                response = doctorFeign.getDoctorSaveDetailsByBookingId(booking.getBookingId()).getBody();
//		            } catch (Exception e) {
//		                System.out.println("Feign error: " + e.getMessage());
//		            }
//
//		            if (response != null && response.getData() != null) {
//		                saveDetails = new ObjectMapper().convertValue(response.getData(), DoctorSaveDetailsDTO.class);
//		            }
//
//		            // Case 1: Treatment Data
//		            if (saveDetails.getTreatments() != null &&
//		                    saveDetails.getTreatments().getGeneratedData() != null &&
//		                    !saveDetails.getTreatments().getGeneratedData().isEmpty()) {
//
//		                List<LocalDate> allDates = saveDetails.getTreatments().getGeneratedData().values().stream()
//		                        .filter(Objects::nonNull)
//		                        .flatMap(details -> details.getDates() != null
//		                                ? details.getDates().stream()
//		                                .map(d -> LocalDate.parse(d.getDate(), isoFormatter))
//		                                : Stream.empty())
//		                        .sorted()
//		                        .toList();
//
//		                int totalSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
//		                        .filter(Objects::nonNull)
//		                        .mapToInt(d -> d.getTotalSittings() != null ? d.getTotalSittings() : 0)
//		                        .sum();
//
//		                int pendingSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
//		                        .filter(Objects::nonNull)
//		                        .mapToInt(d -> d.getSittings() != null ? d.getSittings() : 0)
//		                        .sum();
//
//		                AtomicInteger takenSittingsCount = new AtomicInteger(0);
//
//		                Optional<LocalDate> nextFollowupOpt = allDates.stream().findFirst();
//
//		                LocalDate lstSitting = allDates.get(allDates.size() - 1);
//		                LocalDate exp = lstSitting.plusDays(Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", "")));
//
//		                booking.setTotalSittings(totalSittingsSum);
//		                booking.setPendingSittings(pendingSittingsSum);
//		                booking.setTakenSittings(takenSittingsCount.get());
//		                booking.setCurrentSitting(pendingSittingsSum);
//		                nextFollowupOpt.ifPresent(d -> booking.setFollowupDate(d.format(isoFormatter)));
//
//		                finalList.add(toResponse(booking));
//		            }
//
//		            // Case 2: FollowUp Data
//		            else if (saveDetails.getFollowUp() != null &&
//		                    saveDetails.getFollowUp().getNextFollowUpDate() != null) {
//
//		                try {
//		                    int days = Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", ""));
//		                    LocalDate exp = LocalDate.parse(booking.getServiceDate(), isoFormatter).plusDays(days);
//
//		                    LocalDate followDate = LocalDate.parse(saveDetails.getFollowUp().getNextFollowUpDate(),
//		                            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
//
//		                    if (!followDate.isBefore(today) && !followDate.isAfter(sixthDate) && !followDate.isAfter(exp)) {
//		                        Booking bkng = new Booking(booking);
//		                        bkng.setFollowupDate(followDate.format(isoFormatter));
//		                        bkng.setStatus("In-Progress");
//		                        finalList.add(toResponse(bkng));
//		                    }
//		                } catch (Exception e) {
//		                    System.out.println("Followup error: " + e.getMessage());
//		                }
//		            }
//
//		            // Case 3: Plain In-Clinic or Online Consultation (No Treatments/Followup)
//		            else {
//		                if (ld != null && !ld.isBefore(today) && !ld.isAfter(sixthDate)) {
//		                    finalList.add(toResponse(booking));
//		                }
//		            }
//		        }
//
//		        res.setStatusCode(200);
//		        res.setHttpStatus(HttpStatus.OK);
//		        res.setMessage(finalList.isEmpty()
//		                ? "No In-Progress or Today appointments found"
//		                : "In-Progress appointments found");
//		        res.setData(finalList);
//
//		    } catch (Exception e) {
//		        res.setStatusCode(500);
//		        res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
//		        res.setMessage("Error: " + e.getMessage());
//		    }
//
//		    return ResponseEntity.status(res.getStatusCode()).body(res);
//		}

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> retrieveOneWeekAppointments(
			String clinicId,
			String branchId,
			int page,
			int size) {

		Response response = new Response();

		try {

			// ================= CURRENT DATE =================

			LocalDate today = LocalDate.now();

			// ================= NEXT 7 DAYS =================

			LocalDate nextWeekDate = today.plusDays(7);

			// ================= FETCH BOOKINGS =================

			List<Booking> bookings =
					repository.findByClinicIdAndBranchId(
							clinicId,
							branchId);

			// ================= EMPTY CHECK =================

			if (bookings == null || bookings.isEmpty()) {

				response.setMessage("No Bookings Found");
				response.setStatus(HttpStatus.NOT_FOUND.value());
				response.setSuccess(false);
				response.setData(null);

				return new ResponseEntity<>(
						response,
						HttpStatus.NOT_FOUND);
			}

			// ================= FILTER BOOKINGS =================

			List<BookingResponse> filteredList =
					bookings.stream()

							.filter(booking -> {

								LocalDate bookingDate =
										LocalDate.parse(
												booking.getServiceDate());

								return !bookingDate.isBefore(today)
										&&
										!bookingDate.isAfter(nextWeekDate);
							})

							.map(this::toResponse)

							.collect(Collectors.toList());

			// ================= NO DATA AFTER FILTER =================

			if (filteredList.isEmpty()) {

				response.setMessage(
						"No One Week Appointments Found");
				response.setStatus(HttpStatus.NOT_FOUND.value());
				response.setSuccess(false);
				response.setData(null);

				return new ResponseEntity<>(
						response,
						HttpStatus.NOT_FOUND);
			}

			// ================= PAGINATION LOGIC =================

			int totalElements = filteredList.size();

			int start = page * size;
			int end = Math.min(start + size, totalElements);

			// Invalid page check

			if (start >= totalElements) {

				response.setMessage("Page Not Found");
				response.setStatus(HttpStatus.NOT_FOUND.value());
				response.setSuccess(false);
				response.setData(null);

				return new ResponseEntity<>(
						response,
						HttpStatus.NOT_FOUND);
			}

			List<BookingResponse> paginatedList =
					filteredList.subList(start, end);

			// ================= PAGINATION RESPONSE =================

			Map<String, Object> paginationResponse =
					new HashMap<>();

			paginationResponse.put("content", paginatedList);
			paginationResponse.put("currentPage", page);
			paginationResponse.put("pageSize", size);
			paginationResponse.put("totalElements", totalElements);
			paginationResponse.put(
					"totalPages",
					(int) Math.ceil((double) totalElements / size));

			// ================= SUCCESS RESPONSE =================

			response.setMessage(
					"One Week Appointments Retrieved Successfully");
			response.setStatus(HttpStatus.OK.value());
			response.setSuccess(true);
			response.setData(paginationResponse);

			return new ResponseEntity<>(
					response,
					HttpStatus.OK);

		} catch (Exception e) {

			response.setMessage(
					e.getMessage());
			response.setStatus(
					HttpStatus.INTERNAL_SERVER_ERROR.value());
			response.setSuccess(false);

			return new ResponseEntity<>(
					response,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

		@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> retrieveAppointments(String cinicId,String branchId,String date){
		ResponseStructure< List<BookingResponse>> res = new ResponseStructure< List<BookingResponse>>();
		try {
			List<Booking> bookings = repository.findByClinicIdAndBranchIdAndServiceDateOrderByServicetimeAsc(cinicId, branchId, date);
			// System.out.println(todayBookings);
			bookings = bookings.stream().filter(n->n.getStatus().equalsIgnoreCase("In-Progress")).toList();
			List<BookingResponse> todayBookingsDto = toResponses(bookings);
			if(todayBookingsDto!= null && !todayBookingsDto.isEmpty()) {
				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setData(todayBookingsDto);
				res.setMessage("appointments found");
			}else {
				res.setStatusCode(404);
				res.setHttpStatus(HttpStatus.NOT_FOUND);
				res.setMessage("appointments Not found with date");}
		}catch(Exception e) {
			res.setStatusCode(500);
			res.setMessage(e.getMessage());
		}
		return ResponseEntity.status(res.getStatusCode()).body(res);
	}


	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<ResponseStructure<BookingResponse>> updateAppointmentBasedOnBookingId(
			BookingResponse dto) {

		Booking updated = null;

		try {

			Booking entity = repository.findByBookingIdIgnoreCase(dto.getBookingId())
					.orElseThrow(() -> new RuntimeException("Invalid Booking Id"));

			ObjectMapper mapper = new ObjectMapper();

			// -------- BASIC --------

			if (dto.getBookingFor() != null && !dto.getBookingFor().isEmpty())
				entity.setBookingFor(dto.getBookingFor());

			if (dto.getName() != null && !dto.getName().isEmpty())
				entity.setName(dto.getName());

			if (dto.getReports() != null && !dto.getReports().isEmpty()) {
				entity.setReports(
						mapper.convertValue(dto.getReports(),
								new TypeReference<List<ReportsList>>() {}));
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

			if (dto.getFollowupStatus() != null)
				entity.setFollowupStatus(dto.getFollowupStatus());

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
				if (list == null) list = new ArrayList<>();

				ConsultationFees fee = new ConsultationFees();
				fee.setConsulationFee(dto.getConsultationFee());
				fee.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

				list.add(fee);

				entity.setConsultationFee(dto.getConsultationFee());
				entity.setListOfConsultationFee(list);
			}

			if (dto.getListOfConsultationFee() != null && !dto.getListOfConsultationFee().isEmpty()) {

				List<ConsultationFees> list = entity.getListOfConsultationFee();
				if (list == null) list = new ArrayList<>();

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
				if (statusList == null) statusList = new ArrayList<>();

				Status s = new Status();
				s.setStatus(dto.getStatus());
				s.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

				statusList.add(s);
				entity.setCurrentStatus(statusList);
			}

			if (dto.getCurrentStatus() != null && !dto.getCurrentStatus().isEmpty()) {
				entity.setCurrentStatus(
						mapper.convertValue(dto.getCurrentStatus(),
								new TypeReference<List<Status>>() {}));
			}

			if (dto.getReasonForCancel() != null && !dto.getReasonForCancel().isEmpty())
				entity.setReasonForCancel(dto.getReasonForCancel());

			// -------- FILES --------

			if (dto.getAttachments() != null && !dto.getAttachments().isEmpty())
				entity.setAttachments(dto.getAttachments());

			if (dto.getConsentFormPdf() != null && !dto.getConsentFormPdf().isEmpty())
				entity.setConsentFormPdf(dto.getConsentFormPdf());

			if (dto.getPrescriptionPdf() != null && !dto.getPrescriptionPdf().isEmpty())
				entity.setPrescriptionPdf(
						dto.getPrescriptionPdf());

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
				entity.setTheraphyAnswers(
						mapper.convertValue(dto.getTheraphyAnswers(),
								new TypeReference<Map<String, List<TheraphyAnswersEntity>>>() {}));
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

				if (!today.isAfter(expiryDate)
						&& entity.getFreeFollowUps() != null
						&& entity.getFreeFollowUps() == 0) {

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

			return new ResponseEntity<>(
					ResponseStructure.buildResponse(toResponse(updated),
							"Updated Successfully",
							HttpStatus.OK,
							HttpStatus.OK.value()),
					HttpStatus.OK);

		} catch (Exception e) {

			return new ResponseEntity<>(
					ResponseStructure.buildResponse(null,
							e.getMessage(),
							HttpStatus.INTERNAL_SERVER_ERROR,
							HttpStatus.INTERNAL_SERVER_ERROR.value()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}



		@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> getRelationsByCustomerId(String customerId) {
		ResponseStructure<Map<String, List<RelationInfoDTO>>> res = new ResponseStructure<>();
		try {
			List<Booking> bookings = repository.findByCustomerId(customerId);

			Map<String, List<RelationInfoDTO>> data = bookings.stream()
					.collect(Collectors.groupingBy(
							Booking::getRelation,
							LinkedHashMap::new,
							Collectors.collectingAndThen(
									Collectors.mapping(n -> {
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
									}, Collectors.toList()),
									list -> list.stream().distinct().collect(Collectors.toList()) // remove duplicates
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
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public BookingResponse checkBookingByDateAndTime(String date,String time,String doctorId) {
		Booking booking = repository.findByServiceDateAndServicetimeAndDoctorId(date, time, doctorId);
		if(booking != null) {
			return toResponse(booking);
		}else {
			return null;
		}

	}


	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<Response> getPatientAndPriceInfo(
			String clinicId,
			String branchId,
			Integer number,
			String startDate,
			String endDate) {

		try {

			List<Booking> bookings =
					repository.findByClinicIdAndBranchId(clinicId, branchId);

			if (bookings == null || bookings.isEmpty()) {
				return ResponseEntity.ok(
						Response.builder()
								.success(true)
								.message("No data found")
								.data(new PatientAndPriceInfo())
								.status(HttpStatus.OK.value())
								.build()
				);
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
					return ResponseEntity.badRequest().body(
							Response.builder()
									.success(false)
									.message("Invalid number value")
									.status(HttpStatus.BAD_REQUEST.value())
									.build()
					);
				}
			}

			// 🔥 Step 2: Filter + Map
			List<PatientInfo> patientList = new ArrayList<>();

			double totalConsultation = 0;
			double totalTherapy = 0;
			double totalDue = 0;

			for (Booking booking : bookings) {

				if (booking.getServiceDate() == null) continue;

				LocalDate bookingDate = LocalDate.parse(booking.getServiceDate());

				if ((bookingDate.isEqual(start) || bookingDate.isAfter(start)) &&
						(bookingDate.isEqual(end) || bookingDate.isBefore(end))) {

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
			if(number.equals(1) ) {
				Double value = clinicAdminFeign.getTodayExpenses(keyCloakTokenStore.getAccess_token(),clinicId, branchId);
				afterExpenses = grandTotal - value;
			}else if(number.equals(2) ){
				Double value = clinicAdminFeign.getWeeklyExpenses(keyCloakTokenStore.getAccess_token(),clinicId, branchId);
				afterExpenses = grandTotal - value;
			}else if(number.equals(3) ) {
				Double value = clinicAdminFeign.getMonthlyExpenses(keyCloakTokenStore.getAccess_token(),clinicId, branchId);
				afterExpenses = grandTotal - value;
			}else {
				if(!startDate.isEmpty() && !endDate.isEmpty()) {
					Double value = clinicAdminFeign.customFilter(keyCloakTokenStore.getAccess_token(),startDate, endDate);
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

			return ResponseEntity.ok(
					Response.builder()
							.success(true)
							.data(responseDto)
							.status(HttpStatus.OK.value())
							.build()
			);

		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Response.builder()
							.success(false)
							.message(e.getMessage())
							.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
							.build());
		}
	}


	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<?> getTodayBookings(
			String cId,
			String bId,
			int page,
			int size) {

		ResponseStructure<Map<String, Object>> res =
				new ResponseStructure<>();

		try {

			String today = LocalDate.now()
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.DESC, "createdAt")
			);

			Page<Booking> bookings =
					repository
							.findByClinicIdAndBranchIdAndServiceDate(
									cId,
									bId,
									today,
									pageable
							);

			if (bookings != null && !bookings.isEmpty()) {

				List<Map<String, Object>> list =
						new ArrayList<>();

				List<BookingResponse> dto =
						bookings.getContent()
								.stream()
								.map(this::toResponse)
								.toList();

				dto.stream().map(n -> {

					Map<String, Object> map =
							new LinkedHashMap<>();

					map.put("bookingId", n.getBookingId());
					map.put("serviceDate", n.getServiceDate());
					map.put("servicetime", n.getServicetime());
					map.put("name", n.getName());

					map.put(
							"mobileNumber",
							n.getPatientMobileNumber() != null
									&& !n.getPatientMobileNumber().isEmpty()
									? n.getPatientMobileNumber()
									: n.getMobileNumber()
					);

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

				Map<String, Object> responseMap =
						new LinkedHashMap<>();

				responseMap.put("content", list);
				responseMap.put("currentPage", bookings.getNumber());
				responseMap.put("totalItems", bookings.getTotalElements());
				responseMap.put("totalPages", bookings.getTotalPages());
				responseMap.put("pageSize", bookings.getSize());

				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setData(responseMap);
				res.setMessage("Today's bookings found");

				return ResponseEntity
						.status(HttpStatus.OK)
						.body(res);

			} else {

				res.setStatusCode(200);
				res.setHttpStatus(HttpStatus.OK);
				res.setMessage("Today's bookings not found");

				return ResponseEntity
						.status(HttpStatus.OK)
						.body(res);
			}

		} catch (Exception e) {

			res.setStatusCode(500);
			res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			res.setMessage(e.getMessage());

			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(res);
		}
	}

	private static final DateTimeFormatter FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd");


// ✅ API 1 → TODAY BOOKINGS
//@Override
//public ResponseEntity<Response> getTodayAllBookings(String clinicId, String branchId) {
//
//	try {
//		String today = LocalDate.now().format(FORMATTER);
//
//		// ✅ Fetch ALL bookings (no status filter)
//		List<Booking> bookings =
//				repository.findByClinicIdAndBranchIdAndServiceDate(
//						clinicId,
//						branchId,
//						today
//				);
//		List<String> followup = physioDoctorFeign.getTodayFollowUpBookingIds();
//
//		List<Booking> bkngs = repository.findByBookingIdIn(followup);
//		  List<Booking> modifiedBookings = null;
//		if (!bkngs.isEmpty()) {
//
//		      modifiedBookings = bkngs.stream().map(n -> {
//
//		        n.setStatus("follow-up");
//
//		        List<Status> statusList = n.getCurrentStatus();
//
//		        if (statusList == null || statusList.isEmpty()) {
//		            statusList = new ArrayList<>();
//		        }
//
//		        Status status = new Status();
//
//		        status.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
//		        status.setStatus("follow-up");
//
//		        statusList.add(status);
//
//		        n.setCurrentStatus(statusList);
//		        bookings.add(n);
//		        return n;
//
//		    }).toList();
//		    repository.saveAll(modifiedBookings);
//		}
//		// ✅ Convert to response DTO
//		List<BookingResponse> res = null;
//		List<BookingResponse> bookingres = null;
//		try {
//		if(!bookings.isEmpty()) {
//	    bookingres = toResponses(bookings);}
//	    if(modifiedBookings != null || !modifiedBookings.isEmpty()) {
//		res = toResponses(modifiedBookings);
//		bookingres.addAll(res);}}catch(Exception e) {}
//		//System.out.println(res.get(1));
//		// ✅ Enrich with session details (Feign call)
//		    try {
//		    if(bookingres != null) {
//			bookingres = bookingres.stream().map(n -> {
//						List<Session> lst = physioDoctorFeign
//						.getPhysioByBookingId(n.getBookingId(), n.getServiceDate())
//						.getBody();
//			 // System.out.println(n.getBookingId());
//			  // System.out.println(lst);
//                if(lst != null ) {
//				n.setSession(lst);
//				n.setVisitType("session");
//				}else {
//				n.setSession(null);}
//				return n;
//			}).toList();}
//
//		} catch (Exception e) {
//			// log error instead of silent ignore
//			System.out.println("Error while fetching session details: " + e.getMessage());
//		}
//		Map<String, Object> summary = null;
//		// ✅ Total count
//		if(bookingres != null) {
//		long totalCount = bookings.size();
//
//		// ✅ Status counts (case-insensitive + null safe)
//		long pendingCount = bookingres.stream()
//				.filter(b -> "PENDING".equalsIgnoreCase(
//						Optional.ofNullable(b.getFollowupStatus()).orElse("")
//				))
//				.count();
//
//		long confirmedCount = bookingres.stream()
//				.filter(b -> "CONFIRMED".equalsIgnoreCase(
//						Optional.ofNullable(b.getFollowupStatus()).orElse("")
//				))
//				.count();
//
//		long inProgressCount = bookingres.stream()
//				.filter(b -> "IN-PROGRESS".equalsIgnoreCase(
//						Optional.ofNullable(b.getFollowupStatus()).orElse("")
//				))
//				.count();
//
//		// ✅ Summary response
//		summary = new HashMap<>();
//		summary.put("totalAppointments", totalCount);
//		summary.put("pending", pendingCount);
//		summary.put("confirmed", confirmedCount);
//		summary.put("inProgress", inProgressCount);
//		return ResponseEntity.ok(
//				new Response(true, bookingres, summary, "Today bookings fetched", 200, null, null)
//		);}
//		else {
//			return ResponseEntity.ok(
//					new Response(true, bookingres, summary, "Today bookings not found", 200, null, null));}
//
//	} catch (Exception e) {
//		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//				.body(new Response(false, null, null,
//						"Error fetching today bookings: " + e.getMessage(),
//						500, null, null));
//	}
//}

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<Response> getTodayAllBookings(
			String clinicId,
			String branchId,
			int page,
			int size) {
		try {
			String today =
					LocalDate.now().format(FORMATTER);

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.DESC, "createdAt")
			);

			List<Map<String, Object>> list =
					new ArrayList<>();

			// ================= FETCH TODAY BOOKINGS =================

			Page<Booking> bookingPage =
					repository
							.findByClinicIdAndBranchIdAndServiceDate(
									clinicId,
									branchId,
									today,
									pageable
							);

			List<Booking> bookings =
					new ArrayList<>(bookingPage.getContent());

			// ================= FOLLOW-UP BOOKINGS =================

			List<String> followup =
					physioDoctorFeign
							.getTodayFollowUpBookingIds(keyCloakTokenStore.getAccess_token());

			List<Booking> bkngs =
					repository.findByBookingIdIn(followup);

			List<Booking> modifiedBookings =
					new ArrayList<>();

			if (bkngs != null && !bkngs.isEmpty()) {

				modifiedBookings = bkngs.stream().map(n -> {

					n.setStatus("follow-up");

					List<Status> statusList =
							n.getCurrentStatus();

					if (statusList == null || statusList.isEmpty()) {
						statusList = new ArrayList<>();
					}

					Status status = new Status();

					status.setDATE_TIME(
							LocalDateTime.now(
									ZoneId.of("Asia/Kolkata")
							)
					);

					status.setStatus("follow-up");

					statusList.add(status);

					n.setCurrentStatus(statusList);

					bookings.add(n);

					return n;

				}).toList();

				repository.saveAll(modifiedBookings);
			}

			// ================= DTO CONVERSION =================

			List<BookingResponse> bookingres =
					new ArrayList<>();

			if (!bookings.isEmpty()) {

				bookingres = toResponses(bookings);
			}

			// ================= SESSION ENRICHMENT =================

			try {

				if (bookingres != null && !bookingres.isEmpty()) {

					bookingres = bookingres.stream().map(n -> {

						List<SessionForBooking> lst =
								physioDoctorFeign
										.getPhysioByBookingId(keyCloakTokenStore.getAccess_token(),
												n.getBookingId(),
												n.getServiceDate()
										)
										;

						if (lst != null) {

							n.setSession(lst);
							n.setVisitType("session");

						} else {

							n.setSession(null);
						}

						return n;

					}).toList();
				}

			} catch (Exception e) {

				System.out.println(
						"Error while fetching session details: "
								+ e.getMessage()
				);
			}

			// ================= RESPONSE MAPPING =================

			if (bookingres != null && !bookingres.isEmpty()) {

				bookingres.stream().map(n -> {

					Map<String, Object> map =
							new LinkedHashMap<>();

					map.put("bookingId", n.getBookingId());
					map.put("serviceDate", n.getServiceDate());
					map.put("servicetime", n.getServicetime());
					map.put("name", n.getName());

					map.put(
							"mobileNumber",
							n.getPatientMobileNumber() != null
									&& !n.getPatientMobileNumber().isEmpty()
									? n.getPatientMobileNumber()
									: n.getMobileNumber()
					);

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

					list.add(map);

					return n;

				}).toList();
			}

			// ================= SUMMARY =================

			Map<String, Object> summary =
					new HashMap<>();

			long totalCount = bookingres.size();

			long pendingCount = bookingres.stream()
					.filter(b ->
							"PENDING".equalsIgnoreCase(
									Optional.ofNullable(
											b.getFollowupStatus()
									).orElse("")
							)
					)
					.count();

			long confirmedCount = bookingres.stream()
					.filter(b ->
							"CONFIRMED".equalsIgnoreCase(
									Optional.ofNullable(
											b.getFollowupStatus()
									).orElse("")
							)
					)
					.count();

			long inProgressCount = bookingres.stream()
					.filter(b ->
							"IN-PROGRESS".equalsIgnoreCase(
									Optional.ofNullable(
											b.getFollowupStatus()
									).orElse("")
							)
					)
					.count();

			summary.put("totalAppointments", totalCount);
			summary.put("pending", pendingCount);
			summary.put("confirmed", confirmedCount);
			summary.put("inProgress", inProgressCount);

			// ================= PAGINATION RESPONSE =================

			Map<String, Object> pagination =
					new LinkedHashMap<>();

			pagination.put("content", list);
			pagination.put("summary", summary);
			pagination.put("currentPage", bookingPage.getNumber());
			pagination.put("totalItems", bookingPage.getTotalElements());
			pagination.put("totalPages", bookingPage.getTotalPages());
			pagination.put("pageSize", bookingPage.getSize());

			if (!list.isEmpty()) {

				return ResponseEntity.ok(
						new Response(
								true,
								pagination,
								summary,
								"Today bookings fetched",
								200,
								null,
								null
						)
				);

			} else {

				return ResponseEntity.ok(
						new Response(
								true,
								Collections.emptyList(),
								summary,
								"Today bookings not found",
								200,
								null,
								null
						)
				);
			}

		} catch (Exception e) {

			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(
							new Response(
									false,
									null,
									null,
									"Error fetching today bookings: "
											+ e.getMessage(),
									500,
									null,
									null
							)
					);
		}
	}

	// ✅ API 2 → UPCOMING BOOKINGS (3 or 7 days)
	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<Response> getUpcomingBookings(
			String clinicId,
			String branchId,
			int option,
			int page,
			int size) {

		List<Map<String, Object>> list =
				new ArrayList<>();

		try {

			int days;

			// ================= OPTION VALIDATION =================

			if (option == 1) {

				days = 3;

			} else if (option == 2) {

				days = 7;

			} else {

				return ResponseEntity.badRequest()
						.body(
								new Response(
										false,
										null,
										null,
										"Invalid option (1=3days, 2=7days)",
										400,
										null,
										null
								)
						);
			}

			// ================= DATE RANGE =================

			LocalDate startDate =
					LocalDate.now().minusDays(1);

			LocalDate endDate =
					startDate.plusDays(days + 1);

			// ================= PAGINATION =================

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.ASC, "serviceDate")
			);

			// ================= FETCH BOOKINGS =================

			Page<Booking> bookingPage =
					repository
							.findByClinicIdAndBranchIdAndServiceDateBetween(
									clinicId,
									branchId,
									startDate.format(FORMATTER),
									endDate.format(FORMATTER),
									pageable
							);

			List<Booking> bookings =
					bookingPage.getContent();

			// ================= DTO CONVERSION =================

			List<BookingResponse> response =
					bookings.stream()
							.map(this::toResponse)
							.toList();

			// ================= SESSION ENRICHMENT =================

			try {

				response = response.stream().map(n -> {

					List<SessionForBooking> lst =
							physioDoctorFeign
									.getPhysioByBookingId(keyCloakTokenStore.getAccess_token(),
											n.getBookingId(),
											n.getServiceDate()
									)
									;

					if (lst != null) {

						n.setSession(lst);
						n.setVisitType("session");

					} else {

						n.setSession(null);
					}

					return n;

				}).toList();

			} catch (Exception e) {

				System.out.println(
						"Error while fetching session details: "
								+ e.getMessage()
				);
			}

			// ================= RESPONSE MAPPING =================

			response.stream().map(n -> {

				Map<String, Object> map =
						new LinkedHashMap<>();

				map.put("bookingId", n.getBookingId());
				map.put("serviceDate", n.getServiceDate());
				map.put("servicetime", n.getServicetime());
				map.put("name", n.getName());
				map.put(
						"mobileNumber",
						n.getPatientMobileNumber() != null
								&& !n.getPatientMobileNumber().isEmpty()
								? n.getPatientMobileNumber()
								: n.getMobileNumber()
				);

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

			// ================= SUMMARY =================

			long totalCount =
					bookingPage.getTotalElements();

			long pendingCount = bookings.stream()
					.filter(b ->
							"PENDING".equalsIgnoreCase(
									Optional.ofNullable(
											b.getFollowupStatus()
									).orElse("")
							)
					)
					.count();

			long confirmedCount = bookings.stream()
					.filter(b ->
							"CONFIRMED".equalsIgnoreCase(
									Optional.ofNullable(
											b.getFollowupStatus()
									).orElse("")
							)
					)
					.count();

			long inProgressCount = bookings.stream()
					.filter(b ->
							"IN-PROGRESS".equalsIgnoreCase(
									Optional.ofNullable(
											b.getFollowupStatus()
									).orElse("")
							)
					)
					.count();

			Map<String, Object> summary =
					new HashMap<>();
			summary.put("totalAppointments", totalCount);
			summary.put("pending", pendingCount);
			summary.put("confirmed", confirmedCount);
			summary.put("inProgress", inProgressCount);
			summary.put("startDate", startDate.toString());
			summary.put("endDate", endDate.toString());

			// ================= PAGINATION RESPONSE =================

			Map<String, Object> pagination =
					new LinkedHashMap<>();

			pagination.put("content", list);
			pagination.put("summary", summary);
			pagination.put("currentPage", bookingPage.getNumber());
			pagination.put("totalItems", bookingPage.getTotalElements());
			pagination.put("totalPages", bookingPage.getTotalPages());
			pagination.put("pageSize", bookingPage.getSize());

			return ResponseEntity.ok(
					new Response(
							true,
							pagination,
							summary,
							"Upcoming bookings fetched",
							200,
							null,
							null
					)
			);

		} catch (Exception e) {

			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(
							new Response(
									false,
									null,
									null,
									"Error fetching upcoming bookings: "
											+ e.getMessage(),
									500,
									null,
									null
							)
					);
		}
	}
	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<Response> getBookingByDate(String clinicId,
													 String branchId,
													 String date) {
		try {
			LocalDate dte = LocalDate.parse(date);

			// ✅ Fetch ALL bookings for the date (no status filter)
			List<Booking> bookings = repository.findByClinicIdAndBranchIdAndServiceDate(clinicId, branchId,
					dte.format(FORMATTER));

			List<BookingResponse> res = toResponses(bookings);

			// ✅ Enrich with session details
			try {
				res = res.stream().map(n -> {
					List<SessionForBooking> lst = physioDoctorFeign
							.getPhysioByBookingId(keyCloakTokenStore.getAccess_token(),n.getBookingId(), n.getServiceDate())
							;
					if(lst != null ) {
						n.setSession(lst);
						n.setVisitType("session");
					}else {
						n.setSession(null);}
					return n;
				}).toList();

			} catch (Exception e) {
				System.out.println("Error while fetching session details: " + e.getMessage());
			}

			// ✅ Total count
			long totalCount = bookings.size();

			// ✅ Status counts (case-insensitive + null-safe)
			long pendingCount = bookings.stream()
					.filter(b -> "PENDING".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			long confirmedCount = bookings.stream()
					.filter(b -> "CONFIRMED".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			long inProgressCount = bookings.stream()
					.filter(b -> "IN-PROGRESS".equalsIgnoreCase(Optional.ofNullable(b.getFollowupStatus()).orElse("")))
					.count();

			// ✅ Summary
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
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<Response> getBookingByCustomRange(
			String clinicId,
			String branchId,
			String start,
			String end,
			int page,
			int size) {

		List<Map<String, Object>> list =
				new ArrayList<>();

		try {

			// ================= DATE RANGE =================

			LocalDate startDate =
					LocalDate.parse(start);

			String minusDay =
					startDate.minusDays(1)
							.format(FORMATTER);

			LocalDate endDate =
					LocalDate.parse(end);

			String plusDay =
					endDate.plusDays(1)
							.format(FORMATTER);

			// ================= PAGINATION =================

			Pageable pageable = PageRequest.of(
					page,
					size,
					Sort.by(Sort.Direction.ASC, "serviceDate")
			);

			// ================= FETCH BOOKINGS =================

			Page<Booking> bookingPage =
					repository
							.findByClinicIdAndBranchIdAndServiceDateBetween(
									clinicId,
									branchId,
									minusDay,
									plusDay,
									pageable
							);

			List<Booking> bookings =
					bookingPage.getContent();

			// ================= DTO CONVERSION =================

			List<BookingResponse> response =
					bookings.stream()
							.map(this::toResponse)
							.toList();

			// ================= SESSION ENRICHMENT =================

			try {

				response = response.stream().map(n -> {

					List<SessionForBooking> lst =
							physioDoctorFeign
									.getPhysioByBookingId(keyCloakTokenStore.getAccess_token(),
											n.getBookingId(),
											n.getServiceDate()
									)
									;

					if (lst != null) {

						n.setSession(lst);
						n.setVisitType("session");

					} else {

						n.setSession(null);
					}

					return n;

				}).toList();

			} catch (Exception e) {

				System.out.println(
						"Error while fetching session details: "
								+ e.getMessage()
				);
			}

			// ================= RESPONSE MAPPING =================

			response.stream().map(n -> {

				Map<String, Object> map =
						new LinkedHashMap<>();

				map.put("bookingId", n.getBookingId());
				map.put("serviceDate", n.getServiceDate());
				map.put("servicetime", n.getServicetime());
				map.put("name", n.getName());

				map.put(
						"mobileNumber",
						n.getPatientMobileNumber() != null
								&& !n.getPatientMobileNumber().isEmpty()
								? n.getPatientMobileNumber()
								: n.getMobileNumber()
				);

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

			// ================= SUMMARY =================

			long totalCount =
					bookingPage.getTotalElements();

			long pendingCount = bookings.stream()
					.filter(b ->
							"PENDING".equalsIgnoreCase(
									Optional.ofNullable(
											b.getFollowupStatus()
									).orElse("")
							)
					)
					.count();

			long confirmedCount = bookings.stream()
					.filter(b ->
							"CONFIRMED".equalsIgnoreCase(
									Optional.ofNullable(
											b.getFollowupStatus()
									).orElse("")
							)
					)
					.count();

			long inProgressCount = bookings.stream()
					.filter(b ->
							"IN-PROGRESS".equalsIgnoreCase(
									Optional.ofNullable(
											b.getFollowupStatus()
									).orElse("")
							)
					)
					.count();

			// ================= SUMMARY RESPONSE =================

			Map<String, Object> summary =
					new HashMap<>();

			summary.put("totalAppointments", totalCount);
			summary.put("pending", pendingCount);
			summary.put("confirmed", confirmedCount);
			summary.put("inProgress", inProgressCount);
			summary.put("startDate", start);
			summary.put("endDate", end);

			// ================= PAGINATION RESPONSE =================

			Map<String, Object> pagination =
					new LinkedHashMap<>();

			pagination.put("content", list);
			pagination.put("summary", summary);
			pagination.put("currentPage", bookingPage.getNumber());
			pagination.put("totalItems", bookingPage.getTotalElements());
			pagination.put("totalPages", bookingPage.getTotalPages());
			pagination.put("pageSize", bookingPage.getSize());

			return ResponseEntity.ok(
					new Response(
							true,
							pagination,
							summary,
							"Custom range bookings fetched",
							200,
							null,
							null
					)
			);

		} catch (Exception e) {

			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(
							new Response(
									false,
									null,
									null,
									"Error fetching bookings : "
											+ e.getMessage(),
									500,
									null,
									null
							)
					);
		}
	}

	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public ResponseEntity<Response> getBookingById(String bookingId) {
		try {
			Optional<Booking> booking = repository.findByBookingIdIgnoreCase(bookingId);
			if(booking.isPresent()) {
				if(!booking.get().getFollwupBookings().isEmpty()) {
					ObjectMapper mapper = new ObjectMapper();
					mapper.registerModule(new JavaTimeModule());
					mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
					BookingResponse res = null;
					if(booking.get().getFollwupBookings().get(booking.get().getFollwupBookings().size()-1).getStatus().equalsIgnoreCase("in-progress")) {
						res = mapper.convertValue(booking.get().getFollwupBookings().get(booking.get().getFollwupBookings().size()-1), BookingResponse.class);
						List<SessionForBooking> lst = new ArrayList<>();
						try {
							lst = physioDoctorFeign.getPhysioByBookingId(keyCloakTokenStore.getAccess_token(),res.getBookingId(),res.getServiceDate());
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
			if(entity.getFollwupBookings() == null) {
				lst = new LinkedList<>();
			}else{
				lst = entity.getFollwupBookings();}
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

			if(dto.getFollowupStatus() != null ) {
				entity.setFollowupStatus(dto.getFollowupStatus());}
			// System.out.println(dto.getFollowupStatus()); }
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
				if (statusList == null) statusList = new ArrayList<>();

				Status s = new Status();
				s.setStatus(dto.getStatus());
				s.setDATE_TIME(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

				statusList.add(s);
				entity.setCurrentStatus(statusList);}

			if (dto.getServiceDate() != null && !dto.getServiceDate().isEmpty())
				entity.setServiceDate(dto.getServiceDate());

			if (dto.getServicetime() != null && !dto.getServicetime().isEmpty())
				entity.setServicetime(dto.getServicetime());

			if (dto.getConsultationType() != null && !dto.getConsultationType().isEmpty())
				entity.setConsultationType(dto.getConsultationType());
			if(dto.getConsultationFee() != null) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.registerModule(new JavaTimeModule());
				mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
			if (dto.getStatus() != null) {entity.setStatus(dto.getStatus());}

			// -------- FILES --------
			if (dto.getAttachments() != null && !dto.getAttachments().isEmpty())
				entity.setAttachments(dto.getAttachments());


			if (dto.getConsentFormPdf() != null && !dto.getConsentFormPdf().isEmpty())
				entity.setConsentFormPdf(dto.getConsentFormPdf());
			// -------- PAYMENT --------
			if( dto.getPaymentType() != null && !dto.getPaymentType().isEmpty()) {
				entity.setPaymentType(dto.getPaymentType());}

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

				entity.setTheraphyAnswers(new ObjectMapper().convertValue(dto.getTheraphyAnswers(),new TypeReference<Map<String,List<TheraphyAnswersEntity>>>() {
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
			if(dto.getFoc() != null)
				entity.setFoc(dto.getFoc());
			if (entity.getFreeFollowUps() != null && entity.getFreeFollowUps() == 0) {
				entity.setIsFollowupStatus(true);}
			int days = 0;
			try{
				if(entity.getConsultationExpiration() != null) {
					String consultationExp = entity.getConsultationExpiration(); // e.g. "8 days"
					days = Integer.parseInt(consultationExp.replaceAll("[^0-9]", ""));}

				// Parse serviceDate (assumes format: yyyy-MM-dd)
				LocalDate serviceDate = LocalDate.parse(entity.getServiceDate());

				// Add extracted days
				LocalDate expiryDate = serviceDate.plusDays(days);

				LocalDate today = LocalDate.now();

				if(!today.isAfter(expiryDate) && entity.getFreeFollowUps() != null && entity.getFreeFollowUps() == 0  ){
					entity.setIsFollowupStatus(true);
				}else if(today.isAfter(expiryDate)){
					entity.setIsFollowupStatus(true);
				}else {
					entity.setIsFollowupStatus(false);
				}} catch (Exception e) {
				// fallback safety
				entity.setIsFollowupStatus(false);}

//        if(dto.getConsultationFee() == 0.0 && dto.getIsFollowupStatus() ) {
//           	entity.setStatus("confirmed");}
//           else if(dto.getConsultationFee() == 0.0 && !dto.getIsFollowupStatus()) {
//           	entity.setStatus("pending");
//           }else {
//           if(dto.getConsultationFee() == 0.0 && dto.getPaymentType() != null) {
//           	entity.setStatus("confirmed");
//           	}}

			if(dto.getFoc() != null && dto.getPaymentType() != null) {
				if ("paid".equalsIgnoreCase(dto.getFoc())&&"not paid".equalsIgnoreCase(dto.getPaymentType())) {
					entity.setStatus("pending");
				} else if("foc".equalsIgnoreCase(dto.getFoc())&&"not paid".equalsIgnoreCase(dto.getPaymentType()))  {
					entity.setStatus("confirmed");
				}else {
					if("paid".equalsIgnoreCase(dto.getFoc()) && !dto.getPaymentType().isEmpty()){
						entity.setStatus("confirmed");}}}

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
		}catch (Exception e) {
			///System.out.println(e.getMessage());
			return null;
		}}

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN","ROLE_DOCTOR"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
	public List<Map<String, Object>> searchBookings(String clinicId, String input) {

		try {

			List<Booking> bookings = new ArrayList<>();

			// Mobile Number Search
			if (input.matches("^[6-9]\\d{9}$")) {

				bookings = repository.findByMobileNumberAndClinicId(input, clinicId);

				// If patient mobile number is stored separately
				if (bookings.isEmpty()) {
					bookings = repository.findByPatientMobileNumberAndClinicId(input, clinicId);
				}

			} else {

				// Patient Name validation
				if (input.length() < 3) {
					throw new IllegalArgumentException("Please enter at least 3 characters to search by patient name");
				}

				// Search by Patient Id first
				bookings = repository.findByPatientIdAndClinicId(input, clinicId);

				// If Patient Id not found, search by Name
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
		}}
		
		@Override
		@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "bookingApi", fallbackMethod = "rateLimitFallback")
		public ResponseEntity<Response> getTodayBookings(String clinicId, String branchId) {
			try {

				String today = LocalDate.now().format(FORMATTER);

				List<Map<String, Object>> responseList = new ArrayList<>();

				// Today's bookings for logged-in clinic & branch
				List<Booking> bookings = repository.findByClinicIdAndBranchIdAndServiceDate(clinicId, branchId, today);

				// Follow-up booking IDs
				List<String> followupIds = physioDoctorFeign.getTodayFollowUpBookingIds(keyCloakTokenStore.getAccess_token());

				List<Booking> followupBookings = new ArrayList<>();

				if (followupIds != null && !followupIds.isEmpty()) {

					// IMPORTANT:
					// Filter by clinicId & branchId also
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

				// Convert bookings
				List<BookingResponse> bookingResponses = new ArrayList<>();

				if (!bookings.isEmpty()) {
					bookingResponses.addAll(toResponses(bookings));
				}

				if (!followupBookings.isEmpty()) {
					bookingResponses.addAll(toResponses(followupBookings));
				}

				// Remove duplicate bookingIds
				bookingResponses = bookingResponses.stream().collect(Collectors.toMap(BookingResponse::getBookingId,
						Function.identity(), (oldValue, newValue) -> oldValue, LinkedHashMap::new)).values().stream()
						.toList();

				// Session details
				for (BookingResponse booking : bookingResponses) {

					try {

						List<SessionForBooking> sessions = physioDoctorFeign
								.getPhysioByBookingId(keyCloakTokenStore.getAccess_token(),booking.getBookingId(), booking.getServiceDate());

						//List<SessionForBooking> sessions = sessionResponse != null ? sessionResponse.getBody() : null;

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

				// Summary counts
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

				e.printStackTrace();

				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Response(false, null, null,
						"Error fetching today bookings : " + e.getMessage(), 500, null, null));
			}

		}
	

    // ================= RATE LIMIT FALLBACK METHODS =================

    public ResponseEntity<?> rateLimitFallback(Exception ex){
        log.warn("Rate limit exceeded", ex);
        return ResponseEntity.status(429).body("Too many requests. Please try again later.");
    }

    public BookingResponse rateLimitFallback(String bookingId, Exception ex){
        log.warn("Rate limit exceeded for bookingId={}", bookingId, ex);
        return new BookingResponse();
    }

    public Page<BookingResponse> rateLimitFallback(int page, int size, Exception ex){
        log.warn("Rate limit exceeded", ex);
        return Page.empty();
    }

    public List<Map<String,Object>> rateLimitFallback(String customerId, Exception ex, boolean dummy){
        log.warn("Rate limit exceeded for customerId={}", customerId, ex);
        return Collections.emptyList();
    }

    public List<ReportsDTO> rateLimitReportsFallback(String patientId, Exception ex){
        log.warn("Rate limit exceeded for patientId={}", patientId, ex);
        return Collections.emptyList();
    }

    public Response rateLimitResponseFallback(
            String bookingId,
            String patientId,
            String mobileNumber,
            Exception ex){
        log.warn("Rate limit exceeded", ex);
        return Response.builder()
                .success(false)
                .status(429)
                .message("Too many requests. Please try again later.")
                .build();
    }

    public void rateLimitVoidFallback(Exception ex){
        log.warn("Rate limit exceeded", ex);
    }

}

