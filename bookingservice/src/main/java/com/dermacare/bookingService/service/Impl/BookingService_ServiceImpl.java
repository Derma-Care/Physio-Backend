package com.dermacare.bookingService.service.Impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.dermacare.bookingService.dto.BookingInfoByInput;
import com.dermacare.bookingService.dto.BookingRequset;
import com.dermacare.bookingService.dto.BookingResponse;
import com.dermacare.bookingService.dto.CustomerOnbordingDTO;
import com.dermacare.bookingService.dto.DatesDTO;
import com.dermacare.bookingService.dto.DoctorSaveDetailsDTO;
import com.dermacare.bookingService.dto.PatientAndPriceInfo;
import com.dermacare.bookingService.dto.PatientInfo;
import com.dermacare.bookingService.dto.RelationInfoDTO;
import com.dermacare.bookingService.dto.TreatmentDetailsDTO;
import com.dermacare.bookingService.dto.TreatmentResponseDTO;
import com.dermacare.bookingService.entity.Booking;
import com.dermacare.bookingService.entity.ReportsList;
import com.dermacare.bookingService.entity.Status;
import com.dermacare.bookingService.entity.TheraphyAnswersEntity;
import com.dermacare.bookingService.feign.ClinicAdminFeign;
import com.dermacare.bookingService.feign.DoctorFeign;
//import com.dermacare.bookingService.producer.KafkaProducer;
import com.dermacare.bookingService.repository.BookingServiceRepository;
import com.dermacare.bookingService.service.BookingService_Service;
import com.dermacare.bookingService.util.Response;
import com.dermacare.bookingService.util.ResponseStructure;
import com.dermacare.bookingService.util.SequenceGeneratorService;
import com.dermacare.bookingService.util.geneateIds;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class BookingService_ServiceImpl implements BookingService_Service {

	@Autowired
	private BookingServiceRepository repository;
	
//	@Autowired
//	private KafkaProducer kafkaProducer;
	
//	// @Autowired
	//private NotificationFeign notificationFeign;
	
	@Autowired
	private DoctorFeign doctorFeign;
	
	@Autowired
	private ClinicAdminFeign clinicAdminFeign;
	@Autowired
	private geneateIds sequenceGeneratorService;
	
	public DoctorSaveDetailsDTO saveDetails = new DoctorSaveDetailsDTO();
	public DoctorSaveDetailsDTO sDetails = new DoctorSaveDetailsDTO();
	public DoctorSaveDetailsDTO sd = new DoctorSaveDetailsDTO();
	 LocalDate exp = null;

	 
	 @Override
	 public ResponseEntity<?> addService(BookingRequset request) {
	     ResponseStructure<BookingResponse> response = new ResponseStructure<>();
	     Booking entity = toEntity(request);

	     try {
	         // 🩺 Validate visitType
	         if (request.getVisitType() == null || request.getVisitType().trim().isEmpty()) {
	             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                     .body(ResponseStructure.buildResponse(
	                             null,
	                             "Visit type is required.",
	                             HttpStatus.BAD_REQUEST,
	                             HttpStatus.BAD_REQUEST.value()
	                     ));
	         }

	         // =========================
	         // FOLLOW-UP BOOKING
	         // =========================
	         if ("follow-up".equalsIgnoreCase(request.getVisitType())) {

	             Booking b = repository.findByMobileNumberAndPatientIdAndBookingId(
	                     request.getMobileNumber(), request.getPatientId(), request.getBookingId());

	             if (b == null) {
	                 return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                         .body(ResponseStructure.buildResponse(
	                                 null,
	                                 "No appointment found.",
	                                 HttpStatus.NOT_FOUND,
	                                 HttpStatus.NOT_FOUND.value()));
	             }

	             if (!"In-Progress".equalsIgnoreCase(b.getStatus())) {
	                 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                         .body(ResponseStructure.buildResponse(
	                                 null,
	                                 "No In-Progress appointments found for the provided booking ID.",
	                                 HttpStatus.BAD_REQUEST,
	                                 HttpStatus.BAD_REQUEST.value()));
	             }

	             boolean sittingBooked = false;
	             DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	             if (b.getTreatments() != null && b.getTreatments().getGeneratedData() != null) {
	                 for (Map.Entry<String, TreatmentDetailsDTO> entry : b.getTreatments().getGeneratedData().entrySet()) {
	                     String treatmentName = entry.getKey();
	                     TreatmentDetailsDTO treatment = entry.getValue();

	                     // Filter by subServiceName (only the treatment being booked)
	                     if (request.getSubServiceName() != null &&
	                             !request.getSubServiceName().equalsIgnoreCase(treatmentName)) {
	                         continue;
	                     }

	                     // 🔹 Find the first pending sitting (next to confirm)
	                     Optional<DatesDTO> nextPendingOpt = treatment.getDates().stream()
	                             .filter(d -> "Pending".equalsIgnoreCase(d.getStatus()))
	                             .sorted(Comparator.comparingInt(DatesDTO::getSitting))
	                             .findFirst();

	                     if (nextPendingOpt.isEmpty()) continue;

	                     DatesDTO nextSitting = nextPendingOpt.get();

	                     // ✅ Mark this sitting as Confirmed
	                     nextSitting.setStatus("Confirmed");
	                     nextSitting.setFollowupStatus(null);

	                     // ✅ Update booking follow-up date & time
	                     b.setFollowupDate(nextSitting.getDate());
	                     b.setServicetime(request.getServicetime());
	                     b.setVisitType(request.getVisitType());
	                     b.setSubServiceName(treatmentName);

	                     // 🧮 Update treatment-level stats
	                     treatment.setTakenSittings(treatment.getTakenSittings() + 1);
	                     treatment.setPendingSittings(Math.max(0, treatment.getPendingSittings() - 1));
	                     treatment.setCurrentSitting(treatment.getTakenSittings() + 1);
	                     treatment.setStatus(treatment.getPendingSittings() == 0 ? "Confirmed" : "In-Progress");

	                     // 💸 Decrement free follow-ups if available
	                     if (b.getFreeFollowUpsLeft() != null && b.getFreeFollowUpsLeft() > 0) {
	                         b.setFreeFollowUpsLeft(b.getFreeFollowUpsLeft() - 1);
	                     }

	                     sittingBooked = true;
	                     break; // Only one sitting booked per request
	                 }
	             }

	             if (!sittingBooked) {
	                 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                         .body(ResponseStructure.buildResponse(
	                                 null,
	                                 "No pending sittings available for the selected treatment.",
	                                 HttpStatus.BAD_REQUEST,
	                                 HttpStatus.BAD_REQUEST.value()));
	             }

	             // 🔹 Update booking-level status dynamically
	             boolean anyInProgress = b.getTreatments().getGeneratedData().values().stream()
	                     .anyMatch(t -> "In-Progress".equalsIgnoreCase(t.getStatus()));
	             boolean allConfirmed = b.getTreatments().getGeneratedData().values().stream()
	                     .allMatch(t -> "Confirmed".equalsIgnoreCase(t.getStatus()));

	             if (allConfirmed) {
	                 b.setStatus("Confirmed");
	             } else if (anyInProgress) {
	                 b.setStatus("In-Progress");
	             } else {
	                 b.setStatus("Pending");
	             }

	             Booking updatedBooking = repository.save(b);
	             nullifyLargeFields(updatedBooking);

	             // 🔔 Publish to Kafka
//	             try {
//	            	 updatedBooking.setAttachments(null); 
//	            	 updatedBooking.setPartImage(null);
//	            	 updatedBooking.setConsentFormPdf(null);
//	            	 updatedBooking.setPrescriptionPdf(null);
//	                 kafkaProducer.publishBooking(updatedBooking);
//	             } catch (Exception e) {
//	                 System.err.println("⚠️ Kafka publish failed: " + e.getMessage());
//	             }

	             BookingResponse res = toResponse(updatedBooking);

	             // 🩵 Keep only the booked treatment in the response
	             if (res.getTreatments() != null && res.getTreatments().getGeneratedData() != null) {
	                 Map<String, TreatmentDetailsDTO> onlySelected = new HashMap<>();
	                 TreatmentDetailsDTO selectedTreatment = res.getTreatments().getGeneratedData().get(request.getSubServiceName());
	                 if (selectedTreatment != null) {
	                     onlySelected.put(request.getSubServiceName(), selectedTreatment);
	                     res.getTreatments().setGeneratedData(onlySelected);
	                 }
	             }

	             // Optional: explicitly include which treatment was updated
	             res.setUpdatedTreatment(request.getSubServiceName());

	             response = ResponseStructure.buildResponse(
	                     res,
	                     "Next sitting booked successfully for " + request.getSubServiceName(),
	                     HttpStatus.OK,
	                     HttpStatus.OK.value());

	             return ResponseEntity.ok(response);
	         }

	         // =========================
	         // NEW BOOKING
	         // =========================
	         if (entity.getTreatments() != null && entity.getTreatments().getGeneratedData() != null) {
	             entity.getTreatments().getGeneratedData().forEach((name, t) -> {
	                 if (t.getPendingSittings() != null && t.getPendingSittings() > 0) {
	                     t.setStatus("In-Progress");
	                 } else {
	                     t.setStatus("Confirmed");
	                 }
	             });

	             boolean allConfirmed = entity.getTreatments().getGeneratedData().values().stream()
	                     .allMatch(t -> "Confirmed".equalsIgnoreCase(t.getStatus()));
	             entity.setStatus(allConfirmed ? "Confirmed" : "In-Progress");
	         } else {
	             entity.setStatus("Confirmed");
	         }

	         Booking savedBooking = repository.save(entity);
	         nullifyLargeFields(savedBooking);

//	         try {
//	        	 savedBooking.setConsentFormPdf(null);
//	        	 savedBooking.setConsentFormPdf(null);
//	        	 savedBooking.setPrescriptionPdf(null); 
//	        	 savedBooking.setPartImage(null);
//	             kafkaProducer.publishBooking(savedBooking);
//	         } catch (Exception e) {
//	             System.err.println("⚠️ Kafka publish failed: " + e.getMessage());
//	         }

	         BookingResponse bRes = toResponse(savedBooking);
	         response = ResponseStructure.buildResponse(
	                 bRes,
	                 "Service booked successfully",
	                 HttpStatus.CREATED,
	                 HttpStatus.CREATED.value());
	         return ResponseEntity.status(HttpStatus.CREATED).body(response);

	     } catch (Exception e) {
	         e.printStackTrace();
	         response = ResponseStructure.buildResponse(
	                 null,
	                 "Internal Server Error: " + e.getMessage(),
	                 HttpStatus.INTERNAL_SERVER_ERROR,
	                 HttpStatus.INTERNAL_SERVER_ERROR.value());
	         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	     }
	 }

	 
	 // =========================
	 // Support Methods
	 // =========================
	 private void nullifyLargeFields(Booking booking) {
	     if (booking == null) return;
	     booking.setReports(null);
	     booking.setNotes(null);
	     booking.setAttachments(null);
	     booking.setConsentFormPdf(null);
	     booking.setPrescriptionPdf(null);
	 }

	 private Booking toEntity(BookingRequset request) {
		 Booking entity = null;
            try {
		    entity = new ObjectMapper().convertValue(request, Booking.class);
		    entity.setPatientId(generatePatientId(request.getBranchId()));	  
		    ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
            if(request.getTotalFee() != 0.0) {
		    double due = request.getTotalFee() - request.getPartAmount();
		    entity.setDueAmount(due);
		    entity.setBookedAt(istTime.format(formatter));}
		    entity.setFreeFollowUpsLeft(request.getFreeFollowUps());

		    // ✅ Generate Custom Booking ID
		    String bookingId = sequenceGeneratorService.generateBookingId(request.getClinicName().substring(0, 3),request.getBranchname().substring(0, 3));
		    entity.setBookingId(bookingId);
            
		    // Channel ID logic
		    if (request.getConsultationType() != null &&
		            (request.getConsultationType().equalsIgnoreCase("video consultation") ||
		             request.getConsultationType().equalsIgnoreCase("online consultation"))) {
		        entity.setChannelId(randomNumber());
		    }
            if(request.getPaymentType() != null) {
		    if ("paid".equalsIgnoreCase(request.getPaymentType())) {
		        entity.setStatus("confirmed");
		    } else {
		        entity.setStatus("pending");
		    }}
            	List<Status> status = new LinkedList<>();
            	Status s = new Status();
            	ZoneId zone = ZoneId.of("Asia/Kolkata");
            	LocalDateTime dateTime = LocalDateTime.now(zone);
            	s.setDATE_TIME(dateTime);
            	s.setStatus(entity.getStatus());
            	status.add(s);
            	 Collections.reverse(status);
            	 entity.setCurrentStatus(status);                                      
		}catch (Exception e) {
			System.out.println(e.getMessage()); 
		}return entity;}
	 

	 private BookingResponse toResponse(Booking entity) {
	     BookingResponse response = new ObjectMapper().convertValue(entity, BookingResponse.class);

	     // Attach prescription PDF if exists
	     DoctorSaveDetailsDTO dto = getPrescriptionpdf(response.getBookingId());
	     if (dto != null) {
	         response.setPrescriptionPdf(dto.getPrescriptionPdf());
	     }

	     response.setBookingId(String.valueOf(entity.getBookingId()));

	     // Update each treatment status
	     if (entity.getTreatments() != null && entity.getTreatments().getGeneratedData() != null) {
	         entity.getTreatments().getGeneratedData().forEach((name, t) -> {
	             if (t.getPendingSittings() != null && t.getPendingSittings() > 0) {
	                 t.setStatus("In-Progress");
	             } else {
	                 t.setStatus("Confirmed");
	             }
	         });
	     }

	     return response;
	 }

	 
	 private DoctorSaveDetailsDTO getPrescriptionpdf(String bid) {
	     try {
	         ObjectMapper mapper = new ObjectMapper();
	         mapper.registerModule(new JavaTimeModule());
	         mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	         Response res = doctorFeign.getDoctorSaveDetailsByBookingId(bid).getBody();
	         return mapper.convertValue(res.getData(), DoctorSaveDetailsDTO.class);
	     } catch (Exception e) {
	         System.out.println(e.getMessage());
	         return null;
	     }
	 }

	 
	  private static String generatePatientId(String id) {	       
	        String uuid = UUID.randomUUID().toString();
	        String randomPart = uuid.replaceAll("-", "").substring(0, 6).toUpperCase();
	        return id+"_"+"PT_" + randomPart;
	    }
	
	
	private static String randomNumber() {
        Random random = new Random();    
        int sixDigitNumber = 100000 + random.nextInt(900000); // Generates number from 100000 to 999999
        return String.valueOf(sixDigitNumber);
    }

	
	private List<BookingResponse> toResponses(List<Booking> bookings) {		
		List<BookingResponse> res = new ObjectMapper().convertValue(bookings,new TypeReference<List<BookingResponse>>(){});
		for(BookingResponse bres : res) {
			//System.out.println(bres.getBookingId());
			DoctorSaveDetailsDTO dto = getPrescriptionpdf(bres.getBookingId());
			//System.out.println(dto);
			if(dto != null ) {
			bres.setPrescriptionPdf(dto.getPrescriptionPdf());}}
		return res;
	}	
	
	
	
	
//	 public ResponseEntity<?> physioAppointment() {
//	     ResponseStructure<BookingResponse> response = new ResponseStructure<>();
//	     List<Booking> existingBooking = repository.findByClinicIdAndBranchId(cId, bId);
//	     LocalDa
//	}
	
	
	@Override
	 public ResponseEntity<?> physioAppointment(BookingRequset request) {
		 Response res = new Response();
		 ResponseEntity<?> repnse = null;
		 try {
	     Booking entity = toEntity(request);
	     //System.out.println(entity);
	     Booking updatedBooking = repository.save(entity);
	     if(updatedBooking != null) {
//	    	  try {
//	            	 updatedBooking.setAttachments(null); 
//	            	 updatedBooking.setPartImage(null);
//	            	 updatedBooking.setConsentFormPdf(null);
//	            	 updatedBooking.setPrescriptionPdf(null);
//	                 kafkaProducer.publishBooking(updatedBooking);
//	             } catch (Exception e) {	            	 
//	    	    	  res.setMessage(e.getMessage());
//	    	    	  res.setStatus(500);
//	    	    	  res.setSuccess(false);
//	    	    	  repnse = ResponseEntity.status(res.getStatus()).body(res);}
	    	  ObjectMapper mapper = new ObjectMapper();
		         mapper.registerModule(new JavaTimeModule());
		         mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	    	  res.setData(mapper.convertValue(updatedBooking,BookingResponse.class ) );
	    	  res.setMessage("Booked Successfully");
	    	  res.setStatus(200);
	    	  res.setSuccess(true);
	    	  repnse = ResponseEntity.status(res.getStatus()).body(res);
	    	  }}catch (Exception e) {
	    		  res.setMessage(e.getMessage());
    	    	  res.setStatus(500);
    	    	  res.setSuccess(false);
    	    	  repnse = ResponseEntity.status(res.getStatus()).body(res);
			}
		 return repnse;
	     }
	
	     
	public ResponseEntity<?> getAppointsByPatientId(String patientId) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
		    try {
			List<Booking> existingBooking = repository.findByPatientId(patientId);
			if(existingBooking != null && !existingBooking.isEmpty() ) {
			List<BookingResponse> respnse = new ObjectMapper().convertValue(existingBooking, new TypeReference<List<BookingResponse>>() {});
			res.setStatusCode(200);
			res.setData(respnse);
			res.setMessage("Appointments Are Found");
			return ResponseEntity.status(200).body(res); 
			}else{
				res.setStatusCode(200);
				res.setMessage("Appointments Are Not Found");
				return ResponseEntity.status(200).body(res);}
		    }catch(Exception e) {
			res.setStatusCode(500);
			res.setMessage(e.getMessage());
			return ResponseEntity.status(500).body(res);
		}
	}

	
	
	public ResponseEntity<?> getAppointsByInput(String input) {
		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
		try {
			List<Booking> existingBooking = repository.findByNameIgnoreCaseOrBookingIdOrPatientId(input);
			if(existingBooking != null && !existingBooking.isEmpty() ) {
			List<BookingResponse> respnse = new ObjectMapper().convertValue(existingBooking, new TypeReference<List<BookingResponse>>() {});
			res.setStatusCode(200);
			res.setData(respnse);
			res.setMessage("Appointments Are Found");
			return ResponseEntity.status(200).body(res); 
			}else {
				res.setStatusCode(200);
				res.setMessage("Appointments Are Not Found");
				return ResponseEntity.status(200).body(res);
			}}catch(Exception e) {
			res.setStatusCode(500);
			res.setMessage(e.getMessage());
			return ResponseEntity.status(500).body(res);
		}
	}
	
	

	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(String hospitalId, String doctorId) {
	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
	    List<BookingResponse> responseList = new ArrayList<>();

	    try {
	        List<Booking> existingBookings = repository.findByClinicIdAndDoctorId(hospitalId, doctorId);
	        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	        LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));

	        if (existingBookings != null && !existingBookings.isEmpty()) {

	            for (Booking b : existingBookings) {
	                boolean added = false;

	                // 🩺 Case A: First-time booking (no treatments)
	                if (b.getTreatments() == null || b.getTreatments().getGeneratedData() == null) {
	                    if (b.getServiceDate() != null) {
	                        LocalDate bookingDate = LocalDate.parse(b.getServiceDate(), dateFormatter);

	                        if (bookingDate.equals(currentDate) &&
	                                (b.getStatus().equalsIgnoreCase("Confirmed") ||
	                                 b.getStatus().equalsIgnoreCase("In-Progress"))) {

	                            BookingResponse temp = toResponse(b);
	                            temp.setServiceDate(b.getServiceDate());
	                            temp.setServicetime(b.getServicetime());
	                            temp.setStatus(b.getStatus());
	                            temp.setSubServiceName(b.getSubServiceName());
	                            responseList.add(temp);
	                            added = true;
	                        }
	                    }
	                }

	                // 💊 Case B: Has treatment sittings
	                if (!added && b.getTreatments() != null && b.getTreatments().getGeneratedData() != null) {
	                    for (Map.Entry<String, TreatmentDetailsDTO> entry : b.getTreatments().getGeneratedData().entrySet()) {
	                        String treatmentName = entry.getKey();
	                        TreatmentDetailsDTO t = entry.getValue();

	                        if (t.getDates() != null) {
	                            for (DatesDTO d : t.getDates()) {
	                                LocalDate sittingDate = LocalDate.parse(d.getDate(), dateFormatter);

	                                if (sittingDate.equals(currentDate) &&
	                                        (d.getStatus().equalsIgnoreCase("Confirmed") ||
	                                         d.getStatus().equalsIgnoreCase("In-Progress"))) {

	                                    BookingResponse temp = toResponse(b);
	                                    temp.setSubServiceName(treatmentName);
	                                    temp.setServiceDate(d.getDate());
	                                    temp.setServicetime(b.getServicetime());
	                                    temp.setStatus(d.getStatus());

	                                    // keep only this treatment info
	                                    Map<String, TreatmentDetailsDTO> oneTreatment = new HashMap<>();
	                                    oneTreatment.put(treatmentName, t);
	                                    temp.getTreatments().setGeneratedData(oneTreatment);

	                                    responseList.add(temp);
	                                    break;
	                                }
	                            }
	                        }
	                    }
	                }
	            }

	            if (!responseList.isEmpty()) {
	                res.setStatusCode(200);
	                res.setHttpStatus(HttpStatus.OK);
	                res.setData(responseList);
	                res.setMessage("Today's Appointments Found");
	            } else {
	                res.setStatusCode(200);
	                res.setHttpStatus(HttpStatus.OK);
	                res.setData(responseList);
	                res.setMessage("No Appointments for Today");
	            }
	        } else {
	            res.setStatusCode(200);
	            res.setHttpStatus(HttpStatus.OK);
	            res.setData(responseList);
	            res.setMessage("Appointments Not Found");
	        }

	    } catch (Exception e) {
	        res.setStatusCode(500);
	        res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
	        res.setMessage("Error occurred: " + e.getMessage());
	    }

	    return ResponseEntity.status(res.getStatusCode()).body(res);
	}



	public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(
	        String hospitalId,
	        String doctorId,
	        String number) {

	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
	    List<BookingResponse> responses = new ArrayList<>();

	    try {
	        List<Booking> bookings =
	                repository.findByClinicIdAndDoctorId(hospitalId, doctorId);

	        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

	        if (bookings == null || bookings.isEmpty()) {
	            res.setStatusCode(200);
	            res.setData(responses);
	            res.setMessage("Appointments Are Not Found");
	            return ResponseEntity.ok(res);
	        }

	        for (Booking b : bookings) {

	            /* -------------------------------
	             * CASE A: TREATMENT BOOKINGS
	             * ------------------------------- */
	            if (b.getTreatments() != null
	                    && b.getTreatments().getGeneratedData() != null) {

	                for (Map.Entry<String, TreatmentDetailsDTO> entry
	                        : b.getTreatments().getGeneratedData().entrySet()) {

	                    String treatmentName = entry.getKey();
	                    TreatmentDetailsDTO treatment = entry.getValue();

	                    if (treatment.getDates() == null) continue;

	                    for (DatesDTO d : treatment.getDates()) {

	                        LocalDate appointmentDate =
	                                LocalDate.parse(d.getDate());

	                        boolean add = false;

	                        switch (number) {

	                            // 1️⃣ UPCOMING (Future confirmed sittings)
	                            case "1":
	                                if ("Confirmed".equalsIgnoreCase(d.getStatus())
	                                        && appointmentDate.isAfter(today)) {
	                                    add = true;
	                                }
	                                break;

	                            // 2️⃣ UPCOMING ONLINE
	                            case "2":
	                                if ("Online Consultation".equalsIgnoreCase(b.getConsultationType())
	                                        && "Confirmed".equalsIgnoreCase(d.getStatus())
	                                        && appointmentDate.isAfter(today)) {
	                                    add = true;
	                                }
	                                break;

	                            // 3️⃣ COMPLETED
	                            case "3":
	                                if ("Completed".equalsIgnoreCase(d.getStatus())) {
	                                    add = true;
	                                }
	                                break;

	                            // 4️⃣ ACTIVE / IN-PROGRESS
	                            case "4": // Active (ONLY next pending sitting)

	                                if (!"In-Progress".equalsIgnoreCase(b.getStatus())) {
	                                    break;
	                                }

	                                // Find earliest pending sitting
	                                DatesDTO nextPending = null;

	                                for (DatesDTO date : treatment.getDates()) {
	                                    if ("Pending".equalsIgnoreCase(date.getStatus())) {
	                                        if (nextPending == null ||
	                                            LocalDate.parse(date.getDate())
	                                                     .isBefore(LocalDate.parse(nextPending.getDate()))) {
	                                            nextPending = date;
	                                        }
	                                    }
	                                }

	                                // Add ONLY the next pending sitting
	                                if (nextPending != null && d == nextPending) {
	                                    add = true;
	                                }

	                                break;

	                        }

	                        if (add) {
	                            BookingResponse temp = toResponse(b);
	                            temp.setSubServiceName(treatmentName);
	                            temp.setServiceDate(d.getDate());
	                            temp.setServicetime(b.getServicetime());
	                            temp.setStatus(d.getStatus());
	                            responses.add(temp);
	                        }
	                    }
	                }
	            }

	            /* -------------------------------
	             * CASE B: NORMAL BOOKINGS
	             * ------------------------------- */
	            else if (b.getServiceDate() != null) {

	                LocalDate appointmentDate =
	                        LocalDate.parse(b.getServiceDate());

	                boolean add = false;

	                switch (number) {

	                    // 1️⃣ UPCOMING
	                    case "1":
	                        if ("Confirmed".equalsIgnoreCase(b.getStatus())
	                                && appointmentDate.isAfter(today)) {
	                            add = true;
	                        }
	                        break;

	                    // 2️⃣ UPCOMING ONLINE
	                    case "2":
	                        if ("Online Consultation".equalsIgnoreCase(b.getConsultationType())
	                                && "Confirmed".equalsIgnoreCase(b.getStatus())
	                                && appointmentDate.isAfter(today)) {
	                            add = true;
	                        }
	                        break;

	                    // 3️⃣ COMPLETED
	                    case "3":
	                        if ("Completed".equalsIgnoreCase(b.getStatus())) {
	                            add = true;
	                        }
	                        break;

	                    // 4️⃣ ACTIVE / IN-PROGRESS
	                    case "4":
	                        if ("In-Progress".equalsIgnoreCase(b.getStatus())) {
	                            add = true;
	                        }
	                        break;
	                }

	                if (add) {
	                    BookingResponse temp = toResponse(b);
	                    responses.add(temp);
	                }
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
	
	
		
	public BookingResponse getBookedService(String id) {
		Booking entity = repository.findByBookingId(id)
				.orElseThrow(() -> new RuntimeException("Invalid Booking Id Please provide Valid Id"));
		return toResponse(entity);
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
		for(int i = bookings.size()-1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if (bookings == null  || bookings.isEmpty()) {
			return null;
		}
		return toResponses(reversedBookings);
	}
	
	@Override
	public List<BookingResponse> getAllBookedServices() {
		List<Booking> bookings = repository.findAll();
		List<Booking> reversedBookings = new ArrayList<>();
		for(int i = bookings.size()-1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if (bookings == null  || bookings.isEmpty()) {
			return null;
		}
		return toResponses(reversedBookings);
	}

	@Override
	public List<BookingResponse> bookingByDoctorId(String doctorId) {
		List<Booking> bookings = repository.findByDoctorId(doctorId);
		List<Booking> reversedBookings = new ArrayList<>();
		for(int i = bookings.size()-1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if (bookings == null  || bookings.isEmpty()) {
			return null;
		}
		return toResponses(reversedBookings);
	}

	@Override
	public List<BookingResponse> bookingByServiceId(String serviceId) {
		List<Booking> bookings = repository.findBySubServiceId(serviceId);
		List<Booking> reversedBookings = new ArrayList<>();
		for(int i = bookings.size()-1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if (bookings == null  || bookings.isEmpty()) {
			return null;
		}
		return toResponses(reversedBookings);
	}
	
	
	@Override
	public List<BookingResponse> bookingByCustomerId(String customerId) {
		List<Booking> bookings = repository.findByCustomerId(customerId);
		List<Booking> reversedBookings = new ArrayList<>();
		for(int i = bookings.size()-1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if (bookings == null  || bookings.isEmpty()) {
			return null;
		}
		return toResponses(reversedBookings);
	}
	
	
	@Override
	public List<BookingResponse> bookingByPatientId(String patientId) {
		List<Booking> bookings = repository.findByPatientId(patientId);
		List<Booking> reversedBookings = new ArrayList<>();
		for(int i = bookings.size()-1; i >= 0; i--) {
			if(bookings.get(i).getStatus().equalsIgnoreCase("In-Progress")) {
			reversedBookings.add(bookings.get(i));
		}}
		if (bookings == null  || bookings.isEmpty()) {
			return null;
		}
		return toResponses(reversedBookings);
	}
		
	@Override
	public BookingInfoByInput bookingByInput(String input,String clinicId) {
		   List<BookingInfoByInput> outpt = new ArrayList<>();
	       try {
	    	 List<Booking> bookings = repository.findByMobileNumberAndClinicId(input,clinicId);
		        // If still not found, try customerId
	    	// System.out.println(bookings);
		    	 if(bookings == null || bookings.isEmpty()) {
		            bookings = repository.findByCustomerIdAndClinicId(input,clinicId);
		            //System.out.println(bookings);
		    	} 
	        	 if(bookings == null || bookings.isEmpty()) {
		            bookings = repository.findByNameContainingIgnoreCaseAndClinicId(input,clinicId);
		          // System.out.println(bookings);
		        }
	        	if(bookings != null && !bookings.isEmpty()) {
		        for(Booking b : bookings) {
		        	//System.out.println(b);
		        BookingInfoByInput bkng = new BookingInfoByInput() ;	
		        bkng.setAge(b.getAge());
		        bkng.setClinicId(b.getClinicId());
		        bkng.setCustomerId(b.getCustomerId());
		        bkng.setGender(b.getGender());
		        bkng.setMobileNumber(b.getMobileNumber());
		        bkng.setName(b.getName());
		        bkng.setPatientAddress(b.getPatientAddress());
		        bkng.setPatientId(b.getPatientId());
		        bkng.setPatientMobileNumber(b.getPatientMobileNumber());
		        bkng.setRelation(b.getRelation());
		        outpt.add(bkng);}}
	        if(input.contains("_")){
		    List<Booking> bookgs = repository.findByPatientIdAndClinicId(input,clinicId);
	    	if( bookgs != null && !bookgs.isEmpty()) {
		        Booking b = bookgs.get(0);	
		        BookingInfoByInput bkng = new BookingInfoByInput() ;	
		        bkng.setAge(b.getAge());
		        bkng.setClinicId(b.getClinicId());
		        bkng.setCustomerId(b.getCustomerId());
		        bkng.setGender(b.getGender());
		        bkng.setMobileNumber(b.getMobileNumber());
		        bkng.setName(b.getName());
		        bkng.setPatientAddress(b.getPatientAddress());
		        bkng.setPatientId(b.getPatientId());
		        bkng.setPatientMobileNumber(b.getPatientMobileNumber());
		        bkng.setRelation(b.getRelation());
		        outpt.add(bkng); 
		        }else{
		      Response res = clinicAdminFeign.getCustomerByPatientId(input,clinicId).getBody();
		     // System.out.println(res);
		      CustomerOnbordingDTO bg = new ObjectMapper().convertValue(res.getData(), CustomerOnbordingDTO.class);
		      //System.out.println(bg);
		      BookingInfoByInput bkng = new BookingInfoByInput() ;	
		        bkng.setAge(bg.getAge());
		        bkng.setClinicId(null);
		        bkng.setCustomerId(bg.getCustomerId());
		        bkng.setGender(bg.getGender());
		        bkng.setMobileNumber(bg.getMobileNumber());
		        bkng.setName(bg.getFullName());
		        bkng.setPatientAddress(bg.getAddress().toString());
		        bkng.setPatientId(bg.getPatientId());
		        bkng.setPatientMobileNumber(bg.getMobileNumber());
		        bkng.setRelation(null);
		        outpt.add(bkng);}} 
	       }catch (Exception e) {
	        //System.err.println("Error fetching bookings: " + e.getMessage());
	        System.out.println(e.getMessage());; // safe fallback
	    }
	    return outpt.get(0);
	}

		
	@Override
	public List<BookingResponse> bookingByClinicId(String clinicId) {
		List<Booking> bookings = repository.findByClinicId(clinicId);
		List<Booking> reversedBookings = new ArrayList<>();
		for(int i = bookings.size()-1; i >= 0; i--) {
			reversedBookings.add(bookings.get(i));
		}
		if(bookings==null || bookings.isEmpty()) {
		 return null;
		}
		return toResponses(bookings);
	}
	
	


	@Override
	public ResponseEntity<?> updateAppointment(BookingResponse bookingResponse) {
	    try {
	        // --- Fetch booking from DB ---
	        Booking entity = repository.findByBookingId(bookingResponse.getBookingId())
	                .orElseThrow(() -> new RuntimeException("Invalid Booking Id. Please provide a valid Id."));

	        // --- Update fields from bookingResponse to entity ---
	        if (bookingResponse.getAge() != null) entity.setAge(bookingResponse.getAge());
	        if (bookingResponse.getBookedAt() != null) entity.setBookedAt(bookingResponse.getBookedAt());
	        if (bookingResponse.getBookingFor() != null) entity.setBookingFor(bookingResponse.getBookingFor());
	        if (bookingResponse.getClinicId() != null) entity.setClinicId(bookingResponse.getClinicId());
	        if (bookingResponse.getConsultationFee() != 0) entity.setConsultationFee(bookingResponse.getConsultationFee());
	        if (bookingResponse.getConsultationType() != null) entity.setConsultationType(bookingResponse.getConsultationType());
	        if (bookingResponse.getDoctorId() != null) entity.setDoctorId(bookingResponse.getDoctorId());
	        if (bookingResponse.getGender() != null) entity.setGender(bookingResponse.getGender());
	        if (bookingResponse.getMobileNumber() != null) entity.setMobileNumber(bookingResponse.getMobileNumber());
	        if (bookingResponse.getName() != null) entity.setName(bookingResponse.getName());
	        if (bookingResponse.getProblem() != null) entity.setProblem(bookingResponse.getProblem());
	        if (bookingResponse.getServiceDate() != null) entity.setServiceDate(bookingResponse.getServiceDate());
	        if (bookingResponse.getServicetime() != null) entity.setServicetime(bookingResponse.getServicetime());
	        if (bookingResponse.getStatus() != null) {entity.setStatus(bookingResponse.getStatus());
	        List<Status> status = entity.getCurrentStatus();
        	Status s = new Status();
        	ZoneId zone = ZoneId.of("Asia/Kolkata");
        	LocalDateTime dateTime = LocalDateTime.now(zone);
        	s.setDATE_TIME(dateTime);
        	s.setStatus(bookingResponse.getStatus());
        	status.add(s);
        	 Collections.reverse(status);
        	 entity.setCurrentStatus(status); 
	        }
	        if (bookingResponse.getNotes() != null) entity.setNotes(bookingResponse.getNotes());
	        if (bookingResponse.getReports() != null) {
	            entity.setReports(new ObjectMapper().convertValue(
	                    bookingResponse.getReports(),
	                    new TypeReference<List<ReportsList>>() {}));
	        }
	        if( bookingResponse.getPaymentType() != null && bookingResponse.getPaymentType().equalsIgnoreCase("paid")) {
	        	entity.setStatus("confirmed");
	        	List<Status> status = new LinkedList<>();
            	Status s = new Status();
            	ZoneId zone = ZoneId.of("Asia/Kolkata");
            	LocalDateTime dateTime = LocalDateTime.now(zone);
            	s.setDATE_TIME(dateTime);
            	s.setStatus(entity.getStatus());
            	status.add(s);
            	 Collections.reverse(status);
            	 entity.setCurrentStatus(status);}
	        if (bookingResponse.getSubServiceId() != null) entity.setSubServiceId(bookingResponse.getSubServiceId());
	        if (bookingResponse.getSubServiceName() != null) entity.setSubServiceName(bookingResponse.getSubServiceName());
	        if (bookingResponse.getReasonForCancel() != null) entity.setReasonForCancel(bookingResponse.getReasonForCancel());
	        if (bookingResponse.getTotalFee() != 0) entity.setTotalFee(bookingResponse.getTotalFee());
	        if (bookingResponse.getFreeFollowUpsLeft() != null) entity.setFreeFollowUpsLeft(bookingResponse.getFreeFollowUpsLeft());
	        if (bookingResponse.getFreeFollowUps() != null) entity.setFreeFollowUps(bookingResponse.getFreeFollowUps());
	        if (bookingResponse.getVisitCount() != null) entity.setVisitCount(bookingResponse.getVisitCount());
	        if (bookingResponse.getFollowupDate() != null) entity.setFollowupDate(bookingResponse.getFollowupDate());

	        // --- Update sitting summary ---
	        if (bookingResponse.getTotalSittings() != null) entity.setTotalSittings(bookingResponse.getTotalSittings());
	        if (bookingResponse.getTakenSittings() != null) entity.setTakenSittings(bookingResponse.getTakenSittings());
	        if (bookingResponse.getPendingSittings() != null) entity.setPendingSittings(bookingResponse.getPendingSittings());
	        if (bookingResponse.getCurrentSitting() != null) entity.setCurrentSitting(bookingResponse.getCurrentSitting());

	        // --- Update treatments (map + sitting summary inside) ---
	        if (bookingResponse.getTreatments() != null) {
	            entity.setTreatments(bookingResponse.getTreatments());
	        }

	        // --- Save updated booking ---
	        Booking updatedBooking = repository.save(entity);

	        // --- Build response DTO ---
	        BookingResponse responseDTO = new BookingResponse();
	        responseDTO.setBookingId(updatedBooking.getBookingId());
	        responseDTO.setBookingFor(updatedBooking.getBookingFor());
	        responseDTO.setName(updatedBooking.getName());
	        responseDTO.setRelation(updatedBooking.getRelation());
	        responseDTO.setPatientMobileNumber(updatedBooking.getPatientMobileNumber());
	        responseDTO.setPatientId(updatedBooking.getPatientId());
	        responseDTO.setVisitType(updatedBooking.getVisitType());
	        responseDTO.setFreeFollowUpsLeft(updatedBooking.getFreeFollowUpsLeft());
	        responseDTO.setFreeFollowUps(updatedBooking.getFreeFollowUps());
	        responseDTO.setPatientAddress(updatedBooking.getPatientAddress());
	        responseDTO.setAge(updatedBooking.getAge());
	        responseDTO.setGender(updatedBooking.getGender());
	        responseDTO.setMobileNumber(updatedBooking.getMobileNumber());
	        responseDTO.setCustomerId(updatedBooking.getCustomerId());
	        responseDTO.setConsultationExpiration(updatedBooking.getConsultationExpiration());
	        responseDTO.setCustomerDeviceId(updatedBooking.getCustomerDeviceId());
	        responseDTO.setProblem(updatedBooking.getProblem());
	        responseDTO.setSymptomsDuration(updatedBooking.getSymptomsDuration());
	        responseDTO.setClinicId(updatedBooking.getClinicId());
	        responseDTO.setClinicName(updatedBooking.getClinicName());
	        responseDTO.setBranchId(updatedBooking.getBranchId());
	        responseDTO.setBranchname(updatedBooking.getBranchname());
	        responseDTO.setDoctorId(updatedBooking.getDoctorId());
	        responseDTO.setDoctorName(updatedBooking.getDoctorName());
	        responseDTO.setSubServiceId(updatedBooking.getSubServiceId());
	        responseDTO.setSubServiceName(updatedBooking.getSubServiceName());
	        responseDTO.setServiceDate(updatedBooking.getServiceDate());
	        responseDTO.setServicetime(updatedBooking.getServicetime());
	        responseDTO.setConsultationType(updatedBooking.getConsultationType());
	        responseDTO.setConsultationFee(updatedBooking.getConsultationFee());
	        responseDTO.setStatus(updatedBooking.getStatus());
	        responseDTO.setTotalFee(updatedBooking.getTotalFee());

	        // ✅ Sitting summary
	        responseDTO.setTotalSittings(updatedBooking.getTotalSittings());
	        responseDTO.setPendingSittings(updatedBooking.getPendingSittings());
	        responseDTO.setTakenSittings(updatedBooking.getTakenSittings());
	        responseDTO.setCurrentSitting(updatedBooking.getCurrentSitting());

	        responseDTO.setBookedAt(updatedBooking.getBookedAt());

	        // ✅ Treatments (with dates, sittings, status)
	        responseDTO.setTreatments(updatedBooking.getTreatments());

	        // --- Return wrapped response ---
	        return new ResponseEntity<>(
	                ResponseStructure.buildResponse(
	                        responseDTO,
	                        "Booking updated successfully",
	                        HttpStatus.OK,
	                        HttpStatus.OK.value()
	                ),
	                HttpStatus.OK
	        );

	    } catch (Exception e) {
	        return new ResponseEntity<>(
	                ResponseStructure.buildResponse(
	                        null,
	                        e.getMessage(),
	                        HttpStatus.INTERNAL_SERVER_ERROR,
	                        HttpStatus.INTERNAL_SERVER_ERROR.value()
	                ),
	                HttpStatus.INTERNAL_SERVER_ERROR
	        );
	    }
	}

	
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


		public ResponseEntity<?> getInProgressAppointments(String number){
			ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
			   try{
				List<Booking> booked=repository.findByMobileNumber(number);
				List<BookingResponse> response=new ArrayList<>();
				if(booked!=null && !booked.isEmpty()){
					for(Booking b:booked){
						if(b.getStatus().equalsIgnoreCase("In-Progress")){
							response.add(toResponse(b));}}
					if(response!=null && !response.isEmpty()){
						res.setStatusCode(200);
						res.setHttpStatus(HttpStatus.OK);
						res.setData(response);
						res.setMessage("In-Progress appointments found");
					}else{
						res.setStatusCode(200);
						res.setHttpStatus(HttpStatus.OK);
						res.setData(response);
						res.setMessage("In-Progress appointments not found");}}}
			catch(Exception e){
				res.setStatusCode(500);
				res.setMessage(e.getMessage());}
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
		
		

		
		@Override
		public ResponseEntity<?> getInProgressAppointmentsByCustomerId(String customerId) {
		    ResponseStructure<List<BookingResponse>> response = new ResponseStructure<>();

		    try {
		        List<Booking> bookings = repository.findByCustomerIdAndStatusIgnoreCase(customerId, "In-Progress");

		        if (bookings == null || bookings.isEmpty()) {
		            return ResponseEntity.status(HttpStatus.NOT_FOUND)
		                    .body(ResponseStructure.buildResponse(
		                            null,
		                            "No in-progress bookings found for this customer",
		                            HttpStatus.NOT_FOUND,
		                            404
		                    ));
		        }

		        List<BookingResponse> bookingResponses = new ArrayList<>();
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		        for (Booking booking : bookings) {
		            nullifyLargeFields(booking);

		            if (booking.getTreatments() != null && booking.getTreatments().getGeneratedData() != null) {

		                booking.getTreatments().getGeneratedData().forEach((treatmentName, treatment) -> {
		                    if (treatment.getDates() != null && !treatment.getDates().isEmpty()) {

		                        // Sort sittings ascending by date
		                        treatment.getDates().sort(Comparator.comparing(
		                                d -> LocalDate.parse(d.getDate(), formatter)
		                        ));

		                        // ✅ Mark the next pending sitting as "Next Sitting"
		                        boolean nextMarked = false;
		                        for (DatesDTO date : treatment.getDates()) {
		                            if (!nextMarked && "Pending".equalsIgnoreCase(date.getStatus())) {
		                                date.setFollowupStatus("Next Sitting");
		                                nextMarked = true;
		                            } else {
		                                date.setFollowupStatus(null);
		                            }
		                        }

		                        // 🧮 Compute counts
		                        long completed = treatment.getDates().stream()
		                                .filter(d -> "Completed".equalsIgnoreCase(d.getStatus()))
		                                .count();

		                        long pending = treatment.getDates().stream()
		                                .filter(d -> "Pending".equalsIgnoreCase(d.getStatus()))
		                                .count();

		                        long confirmed = treatment.getDates().stream()
		                                .filter(d -> "Confirmed".equalsIgnoreCase(d.getStatus()))
		                                .count();

		                        treatment.setTakenSittings((int) completed);
		                        treatment.setPendingSittings((int) pending);
		                        treatment.setCurrentSitting((int) (completed + confirmed + 1));

		                        // Set overall treatment status
		                        if (pending == 0 && confirmed == 0) {
		                            treatment.setStatus("Completed");
		                        } else if (pending == 0 && confirmed > 0) {
		                            treatment.setStatus("Confirmed");
		                        } else if (completed > 0) {
		                            treatment.setStatus("In-Progress");
		                        } else {
		                            treatment.setStatus("Pending");
		                        }
		                    }
		                });

		                // ✅ Aggregate totals at parent level
		                int totalSittings = booking.getTreatments().getGeneratedData().values().stream()
		                        .mapToInt(TreatmentDetailsDTO::getTotalSittings).sum();

		                int totalTaken = booking.getTreatments().getGeneratedData().values().stream()
		                        .mapToInt(TreatmentDetailsDTO::getTakenSittings).sum();

		                int totalPending = booking.getTreatments().getGeneratedData().values().stream()
		                        .mapToInt(TreatmentDetailsDTO::getPendingSittings).sum();

		                booking.getTreatments().setTotalSittings(totalSittings);
		                booking.getTreatments().setTakenSittings(totalTaken);
		                booking.getTreatments().setPendingSittings(totalPending);
		                booking.getTreatments().setCurrentSitting(totalTaken + 1);
		            }

		            // Convert booking to base response
		            BookingResponse baseResponse = new ObjectMapper().convertValue(booking, BookingResponse.class);

		            // Attach prescription PDF if available
		            DoctorSaveDetailsDTO dto = getPrescriptionpdf(booking.getBookingId());
		            if (dto != null) {
		                baseResponse.setPrescriptionPdf(dto.getPrescriptionPdf());
		            }

		            // 🧩 Create separate responses per treatment
		            if (booking.getTreatments() != null && booking.getTreatments().getGeneratedData() != null) {
		                for (Map.Entry<String, TreatmentDetailsDTO> entry : booking.getTreatments().getGeneratedData().entrySet()) {
		                    String treatmentName = entry.getKey();
		                    TreatmentDetailsDTO treatment = entry.getValue();

		                    // Clone base booking response
		                    BookingResponse treatmentResponse = new ObjectMapper().convertValue(baseResponse, BookingResponse.class);

		                    // Attach only this treatment
		                    TreatmentResponseDTO singleTreatment = new TreatmentResponseDTO();
		                    singleTreatment.setGeneratedData(Map.of(treatmentName, treatment));
		                    singleTreatment.setTotalSittings(treatment.getTotalSittings());
		                    singleTreatment.setTakenSittings(treatment.getTakenSittings());
		                    singleTreatment.setPendingSittings(treatment.getPendingSittings());
		                    singleTreatment.setCurrentSitting(treatment.getCurrentSitting());

		                    treatmentResponse.setTreatments(singleTreatment);
		                    treatmentResponse.setSubServiceName(treatmentName); // Make subServiceName reflect this treatment

		                    bookingResponses.add(treatmentResponse);
		                }
		            } else {
		                // If no treatments
		                bookingResponses.add(baseResponse);
		            }
		        }

		        response = ResponseStructure.buildResponse(
		                bookingResponses,
		                "In-Progress appointments found",
		                HttpStatus.OK,
		                HttpStatus.OK.value()
		        );

		        return ResponseEntity.ok(response);

		    } catch (Exception e) {
		        e.printStackTrace();
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
		public ResponseEntity<?> getInProgressAppointmentsByPatientId(String patientId,String clinicId) {
		    ResponseStructure<List<BookingResponse>> response = new ResponseStructure<>();

		    try {
		        // Fetch all bookings for this patient
		        List<Booking> bookings = repository.findByPatientIdAndClinicId(patientId,clinicId);

		        if (bookings == null || bookings.isEmpty()) {
		            return ResponseEntity.status(HttpStatus.NOT_FOUND)
		                    .body(ResponseStructure.buildResponse(
		                            null,
		                            "No bookings found for this patient",
		                            HttpStatus.NOT_FOUND,
		                            404
		                    ));
		        }

		        List<BookingResponse> bookingResponses = new ArrayList<>();
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		        for (Booking booking : bookings) {
		            // Only process In-Progress bookings
		            if (!"In-Progress".equalsIgnoreCase(booking.getStatus())) {
		                continue;
		            }

		            nullifyLargeFields(booking);

		            if (booking.getTreatments() != null && booking.getTreatments().getGeneratedData() != null) {

		                booking.getTreatments().getGeneratedData().forEach((treatmentName, treatment) -> {

		                    if (treatment.getDates() != null && !treatment.getDates().isEmpty()) {

		                        // Sort sittings ascending by date
		                        treatment.getDates().sort(Comparator.comparing(
		                                d -> LocalDate.parse(d.getDate(), formatter)
		                        ));

		                        // Build stream of sittings that are not completed or confirmed
		                        Stream<DatesDTO> dateStream = treatment.getDates().stream()
		                                .filter(d -> !"Completed".equalsIgnoreCase(d.getStatus()) &&
		                                             !"Confirmed".equalsIgnoreCase(d.getStatus()));

		                        // If booking is follow-up, skip sittings on or before the follow-up date
		                        if ("follow-up".equalsIgnoreCase(booking.getVisitType()) && booking.getFollowupDate() != null) {
		                            try {
		                                LocalDate followupDate = LocalDate.parse(booking.getFollowupDate(), formatter);
		                                dateStream = dateStream.filter(d -> {
		                                    LocalDate sittingDate = LocalDate.parse(d.getDate(), formatter);
		                                    return sittingDate.isAfter(followupDate);
		                                });
		                            } catch (Exception e) {
		                                System.err.println("⚠️ Error parsing follow-up date: " + e.getMessage());
		                            }
		                        }

		                        // Get the next pending sitting after filtering
		                        Optional<DatesDTO> nextSittingOpt = dateStream.findFirst();

		                        if (nextSittingOpt.isPresent()) {
		                            treatment.setDates(Collections.singletonList(nextSittingOpt.get()));
		                        } else {
		                            // All sittings completed or confirmed
		                            treatment.setDates(Collections.emptyList());
		                            treatment.setStatus("Confirmed");
		                        }
		                    }

		                    // 🧮 Update treatment-level summary
		                    long completed = treatment.getDates().stream()
		                            .filter(d -> "Completed".equalsIgnoreCase(d.getStatus()))
		                            .count();

		                    treatment.setTakenSittings((int) completed);
		                    treatment.setPendingSittings(Math.max(0, treatment.getTotalSittings() - (int) completed));
		                    treatment.setCurrentSitting((int) (completed + 1));

		                    if (treatment.getPendingSittings() == 0) {
		                        treatment.setStatus("Confirmed");
		                    } else if (treatment.getTakenSittings() > 0) {
		                        treatment.setStatus("In-Progress");
		                    } else {
		                        treatment.setStatus("Pending");
		                    }

		                    // 🧾 Build booking response
		                    BookingResponse bookingResponse = new ObjectMapper().convertValue(booking, BookingResponse.class);

		                    Map<String, TreatmentDetailsDTO> singleTreatment = new HashMap<>();
		                    singleTreatment.put(treatmentName, treatment);
		                    bookingResponse.getTreatments().setGeneratedData(singleTreatment);

		                    bookingResponse.getTreatments().setTotalSittings(treatment.getTotalSittings());
		                    bookingResponse.getTreatments().setTakenSittings(treatment.getTakenSittings());
		                    bookingResponse.getTreatments().setPendingSittings(treatment.getPendingSittings());
		                    bookingResponse.getTreatments().setCurrentSitting(treatment.getCurrentSitting());

		                    // Attach prescription PDF if exists
		                    DoctorSaveDetailsDTO dto = getPrescriptionpdf(booking.getBookingId());
		                    if (dto != null) {
		                        bookingResponse.setPrescriptionPdf(dto.getPrescriptionPdf());
		                    }

		                    bookingResponses.add(bookingResponse);
		                });
		            }
		        }

		        if (bookingResponses.isEmpty()) {
		            return ResponseEntity.status(HttpStatus.NOT_FOUND)
		                    .body(ResponseStructure.buildResponse(
		                            null,
		                            "No In-Progress appointments found for this patient",
		                            HttpStatus.NOT_FOUND,
		                            404
		                    ));
		        }

		        response = ResponseStructure.buildResponse(
		                bookingResponses,
		                "In-Progress appointments found",
		                HttpStatus.OK,
		                HttpStatus.OK.value()
		        );

		        return ResponseEntity.ok(response);

		    } catch (Exception e) {
		        e.printStackTrace();
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
		
	
		public ResponseEntity<?> getDoctorFutureAppointments(String doctorId){
			ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
			   try{
				List<Booking> booked=repository.findByDoctorId(doctorId);
				List<BookingResponse> response=new ArrayList<>();
				if(booked!=null && !booked.isEmpty()){
					for(Booking b:booked){
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					LocalDate serviceDate = LocalDate.parse(b.getServiceDate(), formatter);
					LocalDate currentDate = LocalDate.now();
					LocalDate plus = currentDate.plusDays(15);
						if(!serviceDate.isBefore(currentDate) && !serviceDate.isAfter(plus)){
							response.add(toResponse(b));}}
					if(response!=null && !response.isEmpty()){
						res.setStatusCode(200);
						res.setHttpStatus(HttpStatus.OK);
						res.setData(response);
						res.setMessage("appointments found");
					}else{
						res.setStatusCode(200);
						res.setHttpStatus(HttpStatus.OK);
						res.setData(response);
						res.setMessage("appointments not found");}}}
			catch(Exception e){
				res.setStatusCode(500);
				res.setMessage(e.getMessage());}
			return ResponseEntity.status(res.getStatusCode()).body(res);
		}
		
		
		
		@Override
		public List<BookingResponse> bookingByBranchId(String branchId) {
			List<Booking> bookings = repository.findByBranchId(branchId);
			List<Booking> reversedBookings = new ArrayList<>();
			for(int i = bookings.size()-1; i >= 0; i--) {
				reversedBookings.add(bookings.get(i));
			}
			if (bookings == null  || bookings.isEmpty()) {
				return null;
			}
			return toResponses(reversedBookings);
		}
		
		
		
		@Override
		public List<BookingResponse> getBookedServicesByClinicIdWithBranchId(String clinicId, String branchId) {
		    List<Booking> bookings = repository.findByClinicIdAndBranchId(clinicId, branchId);
		    List<Booking> reversedBookings = new ArrayList<>();
		    for (int i = bookings.size() - 1; i >= 0; i--) {
		        reversedBookings.add(bookings.get(i));
		    }
		    if (bookings == null || bookings.isEmpty()) {
		        return null;
		    }
		    return toResponses(reversedBookings);
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
		public ResponseEntity<?> retrieveOneWeekAppointments(String clinicId, String branchId) {
		    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
		    List<BookingResponse> finalList = new ArrayList<>();
		    Response response;
		    DoctorSaveDetailsDTO saveDetails;

		    try {
		        List<Booking> booked = repository.findByClinicIdAndBranchId(clinicId, branchId);
		        if (booked == null || booked.isEmpty()) {
		            res.setStatusCode(200);
		            res.setHttpStatus(HttpStatus.OK);
		            res.setMessage("No bookings found for customer");
		            res.setData(finalList);
		            return ResponseEntity.ok(res);
		        }

		        LocalDate today = LocalDate.now();
		        LocalDate sixthDate = today.plusDays(6);
		        DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		        for (Booking booking : booked) {
		            // Include only In-Progress bookings
		            if (!"In-Progress".equalsIgnoreCase(booking.getStatus())) continue;

		            // Include relevant consultation types
		            String type = booking.getConsultationType() != null ? booking.getConsultationType().trim() : "";
		            if (!type.equalsIgnoreCase("In-Clinic Consultation") &&
		                !type.equalsIgnoreCase("Services & Treatments") &&
		                !type.equalsIgnoreCase("Online Consultation")) {
		                continue;
		            }

		            LocalDate ld = null;
		            if (booking.getFollowupDate() != null) {
		                ld = LocalDate.parse(booking.getFollowupDate(), isoFormatter);
		            } else if (booking.getServiceDate() != null) {
		                ld = LocalDate.parse(booking.getServiceDate(), isoFormatter);
		            }

		            if (ld == null || ld.isAfter(sixthDate)) continue;

		            // fetch doctor details
		            saveDetails = new DoctorSaveDetailsDTO();
		            try {
		                response = doctorFeign.getDoctorSaveDetailsByBookingId(booking.getBookingId()).getBody();
		                if (response != null && response.getData() != null) {
		                    saveDetails = new ObjectMapper().convertValue(response.getData(), DoctorSaveDetailsDTO.class);
		                }
		            } catch (Exception e) {
		                System.out.println("Feign error: " + e.getMessage());
		            }

		            boolean bookingAdded = false;

		            // Case 1: Treatment Data
		            if (saveDetails.getTreatments() != null &&
		                saveDetails.getTreatments().getGeneratedData() != null &&
		                !saveDetails.getTreatments().getGeneratedData().isEmpty()) {

		                List<LocalDate> allDates = saveDetails.getTreatments().getGeneratedData().values().stream()
		                        .filter(Objects::nonNull)
		                        .flatMap(details -> details.getDates() != null
		                                ? details.getDates().stream().map(d -> LocalDate.parse(d.getDate(), isoFormatter))
		                                : Stream.empty())
		                        .sorted()
		                        .toList();

		                int totalSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
		                        .mapToInt(d -> d.getTotalSittings() != null ? d.getTotalSittings() : 0)
		                        .sum();

		                int pendingSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
		                        .mapToInt(d -> d.getSittings() != null ? d.getSittings() : 0)
		                        .sum();

		                AtomicInteger takenSittingsCount = new AtomicInteger(0);

		                LocalDate lastSitting = allDates.get(allDates.size() - 1);
		                LocalDate exp = lastSitting.plusDays(Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", "")));

		                booking.setTotalSittings(totalSittingsSum);
		                booking.setPendingSittings(pendingSittingsSum);
		                booking.setTakenSittings(takenSittingsCount.get());
		                booking.setCurrentSitting(pendingSittingsSum);
		                booking.setFollowupDate(allDates.get(0).format(isoFormatter));

		                if (!exp.isBefore(today)) {
		                    finalList.add(toResponse(booking));
		                    bookingAdded = true;
		                }
		            }

		            // Case 2: FollowUp Data
		            if (!bookingAdded && saveDetails.getFollowUp() != null &&
		                saveDetails.getFollowUp().getNextFollowUpDate() != null) {
		                try {
		                    LocalDate followDate = LocalDate.parse(
		                            saveDetails.getFollowUp().getNextFollowUpDate(),
		                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
		                    );

		                    LocalDate exp = LocalDate.parse(booking.getServiceDate(), isoFormatter)
		                            .plusDays(Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", "")));

		                    if (!followDate.isBefore(today) && !followDate.isAfter(sixthDate) && !followDate.isAfter(exp)) {
		                        Booking bkng = new Booking(booking);
		                        bkng.setFollowupDate(followDate.format(isoFormatter));
		                        bkng.setStatus("In-Progress");
		                        finalList.add(toResponse(bkng));
		                        bookingAdded = true;
		                    }
		                } catch (Exception e) {
		                    System.out.println("Followup error: " + e.getMessage());
		                }
		            }

		            // Case 3: Plain In-Clinic or Online Consultation (No Treatments/Followup)
		            if (!bookingAdded) {
		                if (ld != null && !ld.isBefore(today) && !ld.isAfter(sixthDate)) {
		                    finalList.add(toResponse(booking));
		                }
		            }
		        }

		        res.setStatusCode(200);
		        res.setHttpStatus(HttpStatus.OK);
		        res.setMessage(finalList.isEmpty()
		                ? "No In-Progress or Today appointments found"
		                : "In-Progress appointments found");
		        res.setData(finalList);

		    } catch (Exception e) {
		        res.setStatusCode(500);
		        res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		        res.setMessage("Error: " + e.getMessage());
		    }

		    return ResponseEntity.status(res.getStatusCode()).body(res);
		}

		
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
		
		
			public ResponseEntity<?> updateAppointmentBasedOnBookingId(BookingResponse dto) {
			    try {
			        Booking entity = repository.findByBookingId(dto.getBookingId())
			                .orElseThrow(() -> new RuntimeException("Invalid Booking Id"));

			        // -------- BASIC --------
			        if (dto.getBookingFor() != null && !dto.getBookingFor().isEmpty())
			            entity.setBookingFor(dto.getBookingFor());

			        if (dto.getName() != null && !dto.getName().isEmpty())
			            entity.setName(dto.getName());

			        if (dto.getRelation() != null && !dto.getRelation().isEmpty())
			            entity.setRelation(dto.getRelation());

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

			        if(dto.getFollowupStatus() != null && dto.getFollowupStatus().equalsIgnoreCase("no-followup")) {
				        entity.setStatus("Completed");
				    }else {
				    	try {
				    		//System.out.println(dto);
				    		if(entity.getTreatments().getGeneratedData().containsKey(dto.getTreatmentName())) {
				    		TreatmentDetailsDTO	treatmentDetailsDTO = entity.getTreatments().getGeneratedData().get(dto.getTreatmentName());
				    		//System.out.println(treatmentDetailsDTO);
				    		if(treatmentDetailsDTO != null) {
				    			for(DatesDTO datesDTO : treatmentDetailsDTO.getDates()) {
				    				//System.out.println(datesDTO);
				    				if(datesDTO.getDate().equals(dto.getTreatmentDate())){
				    					datesDTO.setFollowupStatus(dto.getFollowupStatus());}
				    				TreatmentResponseDTO treatmentResponseDTO = entity.getTreatments();
				    				//System.out.println(treatmentResponseDTO);
				    					entity.setTreatments(treatmentResponseDTO);		
				    			}}}}catch(Exception e) {System.out.println(e.getMessage());}}

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
			        if (dto.getSubServiceId() != null && !dto.getSubServiceId().isEmpty())
			            entity.setSubServiceId(dto.getSubServiceId());

			        if (dto.getSubServiceName() != null && !dto.getSubServiceName().isEmpty())
			            entity.setSubServiceName(dto.getSubServiceName());

			        if (dto.getServiceDate() != null && !dto.getServiceDate().isEmpty())
			            entity.setServiceDate(dto.getServiceDate());

			        if (dto.getServicetime() != null && !dto.getServicetime().isEmpty())
			            entity.setServicetime(dto.getServicetime());

			        if (dto.getConsultationType() != null && !dto.getConsultationType().isEmpty())
			            entity.setConsultationType(dto.getConsultationType());

			        if (dto.getConsultationFee() > 0)
			            entity.setConsultationFee(dto.getConsultationFee());

			        if (dto.getConsultationExpiration() != null && !dto.getConsultationExpiration().isEmpty())
			            entity.setConsultationExpiration(dto.getConsultationExpiration());

			        // -------- STATUS --------
			       if (dto.getStatus() != null) {entity.setStatus(dto.getStatus());
				        List<Status> status = entity.getCurrentStatus();
			        	Status s = new Status();
			        	ZoneId zone = ZoneId.of("Asia/Kolkata");
			        	LocalDateTime dateTime = LocalDateTime.now(zone);
			        	s.setDATE_TIME(dateTime);
			        	s.setStatus(dto.getStatus());
			        	status.add(s);
			        	 Collections.reverse(status);
			        	 entity.setCurrentStatus(status); 
				        }

			        if (dto.getCurrentStatus() != null && !dto.getCurrentStatus().isEmpty())
			            entity.setCurrentStatus(
			                new ObjectMapper().convertValue(dto.getCurrentStatus(),
			                        new TypeReference<List<Status>>() {})
			            );

			        if (dto.getReasonForCancel() != null && !dto.getReasonForCancel().isEmpty())
			            entity.setReasonForCancel(dto.getReasonForCancel());

			        if (dto.getNotes() != null && !dto.getNotes().isEmpty())
			            entity.setNotes(dto.getNotes());

			        // -------- FILES --------
			        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty())
			            entity.setAttachments(
			                new ObjectMapper().convertValue(dto.getAttachments(),
			                        new TypeReference<List<byte[]>>() {})
			            );

			        if (dto.getConsentFormPdf() != null && !dto.getConsentFormPdf().isEmpty())
			            entity.setConsentFormPdf(Base64.getDecoder().decode(dto.getConsentFormPdf()));

			        if (dto.getPrescriptionPdf() != null && !dto.getPrescriptionPdf().isEmpty())
			            entity.setPrescriptionPdf(
			                new ObjectMapper().convertValue(dto.getPrescriptionPdf(),
			                        new TypeReference<List<byte[]>>() {})
			            );

			        // -------- PAYMENT --------
			         if( dto.getPaymentType() != null && dto.getPaymentType().equalsIgnoreCase("paid")) {
				        	entity.setStatus("confirmed");
				        	List<Status> status = new LinkedList<>();
			            	Status s = new Status();
			            	ZoneId zone = ZoneId.of("Asia/Kolkata");
			            	LocalDateTime dateTime = LocalDateTime.now(zone);
			            	s.setDATE_TIME(dateTime);
			            	s.setStatus(entity.getStatus());
			            	status.add(s);
			            	 Collections.reverse(status);
			            	 entity.setCurrentStatus(status);}
			        if (dto.getPaymentStatus() != null && !dto.getPaymentStatus().isEmpty())
			            entity.setPaymentStatus(dto.getPaymentStatus());

			        if (dto.getTotalFee() > 0)
			            entity.setTotalFee(dto.getTotalFee());

			        if (dto.getDoctorRefCode() != null && !dto.getDoctorRefCode().isEmpty())
			            entity.setDoctorRefCode(dto.getDoctorRefCode());

			        // -------- SITTINGS --------
			        if (dto.getTotalSittings() != null)
			            entity.setTotalSittings(dto.getTotalSittings());

			        if (dto.getPendingSittings() != null)
			            entity.setPendingSittings(dto.getPendingSittings());

			        if (dto.getTakenSittings() != null)
			            entity.setTakenSittings(dto.getTakenSittings());

			        if (dto.getCurrentSitting() != null)
			            entity.setCurrentSitting(dto.getCurrentSitting());

			        // -------- BODY PART --------
			        if (dto.getBodyPartId() != null && !dto.getBodyPartId().isEmpty())
			            entity.setBodyPartId(dto.getBodyPartId());

			        if (dto.getBodyPartName() != null && !dto.getBodyPartName().isEmpty())
			            entity.setBodyPartName(dto.getBodyPartName());

			        if (dto.getPartImage() != null && !dto.getPartImage().isEmpty())
			            entity.setPartImage(Base64.getDecoder().decode(dto.getPartImage()));
			        if (dto.getReports() != null) {
				            entity.setReports(new ObjectMapper().convertValue(
				                    dto.getReports(),
				                    new TypeReference<List<ReportsList>>() {}));
				        }

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
			        if (dto.getTreatments() != null)
			            entity.setTreatments(dto.getTreatments());

			        Booking updated = repository.save(entity);

			        return new ResponseEntity<>(
			                ResponseStructure.buildResponse(updated, "Updated Successfully",
			                        HttpStatus.OK, HttpStatus.OK.value()),
			                HttpStatus.OK
			        );

			    } catch (Exception e) {
			        return new ResponseEntity<>(
			                ResponseStructure.buildResponse(null, e.getMessage(),
			                        HttpStatus.INTERNAL_SERVER_ERROR,
			                        HttpStatus.INTERNAL_SERVER_ERROR.value()),
			                HttpStatus.INTERNAL_SERVER_ERROR
			        );
			    }
			}	
			
			
		
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
		public BookingResponse checkBookingByDateAndTime(String date,String time,String doctorId) {
			Booking booking = repository.findByServiceDateAndServicetimeAndDoctorId(date, time, doctorId);
			if(booking != null) {
			return toResponse(booking);
			}else {
				return null;
			}
			
		}
		
		
		@Override
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
		                totalConsultation += booking.getConsultationFee();
		                totalTherapy += booking.getTotalFee();
		                totalDue += booking.getDueAmount();
		            }
		        }

		        // 🔥 Step 3: Final Calculation
		        double grandTotal = totalConsultation + totalTherapy + totalDue;
		        double afterExpenses = 0.0;
		        if(number.equals(1) ) {
		        	Double value = clinicAdminFeign.getTodayExpenses(clinicId, branchId);
		        	 afterExpenses = grandTotal - value;
		        }else if(number.equals(2) ){
		        	Double value = clinicAdminFeign.getWeeklyExpenses(clinicId, branchId);
		        	 afterExpenses = grandTotal - value;	       	
		        }else if(number.equals(3) ) {
		        	Double value = clinicAdminFeign.getMonthlyExpenses(clinicId, branchId);
		        	 afterExpenses = grandTotal - value;		       
		        }else {
		        	if(!startDate.isEmpty() && !endDate.isEmpty()) {
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



public List<BookingResponse> getTodayBookings(String cId,String bId) {
    String today = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    // Disable timestamp format
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    List<Booking> b  = repository.findByClinicIdAndBranchIdAndServiceDate(cId,bId,today);
    if(b != null || !b.isEmpty()) {
    	List<BookingResponse> dto = mapper.convertValue(b, new TypeReference<List<BookingResponse>>() {
		});
    	return dto;
    }else {
    	return Collections.emptyList();
    }
}}
























//package com.dermacare.bookingService.service.Impl;
//
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Base64;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.Random;
//import java.util.Set;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import org.bson.types.ObjectId;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import com.dermacare.bookingService.dto.BookingInfoByInput;
//import com.dermacare.bookingService.dto.BookingRequset;
//import com.dermacare.bookingService.dto.BookingResponse;
//import com.dermacare.bookingService.dto.CustomerOnbordingDTO;
//import com.dermacare.bookingService.dto.DatesDTO;
//import com.dermacare.bookingService.dto.DoctorSaveDetailsDTO;
//import com.dermacare.bookingService.dto.RelationInfoDTO;
//import com.dermacare.bookingService.dto.TreatmentDetailsDTO;
//import com.dermacare.bookingService.dto.TreatmentResponseDTO;
//import com.dermacare.bookingService.entity.Booking;
//import com.dermacare.bookingService.entity.ReportsList;
//import com.dermacare.bookingService.feign.ClinicAdminFeign;
//import com.dermacare.bookingService.feign.DoctorFeign;
//import com.dermacare.bookingService.producer.KafkaProducer;
//import com.dermacare.bookingService.repository.BookingServiceRepository;
//import com.dermacare.bookingService.service.BookingService_Service;
//import com.dermacare.bookingService.util.Response;
//import com.dermacare.bookingService.util.ResponseStructure;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//
//@Service
//public class BookingService_ServiceImpl implements BookingService_Service {
//
//	@Autowired
//	private BookingServiceRepository repository;
//	
//	@Autowired
//	private KafkaProducer kafkaProducer;
//	
//	// @Autowired
//	//private NotificationFeign notificationFeign;
//	
//	@Autowired
//	private DoctorFeign doctorFeign;
//	
//	@Autowired
//	private ClinicAdminFeign clinicAdminFeign;
//	
//	public DoctorSaveDetailsDTO saveDetails = new DoctorSaveDetailsDTO();
//	public DoctorSaveDetailsDTO sDetails = new DoctorSaveDetailsDTO();
//	public DoctorSaveDetailsDTO sd = new DoctorSaveDetailsDTO();
//	 LocalDate exp = null;
//
//	 
//	 @Override
//	 public ResponseEntity<?> addService(BookingRequset request) {
//	     ResponseStructure<BookingResponse> response = new ResponseStructure<>();
//	     Booking entity = toEntity(request);
//
//	     try {
//	         // 🩺 Validate visitType
//	         if (request.getVisitType() == null || request.getVisitType().trim().isEmpty()) {
//	             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//	                     .body(ResponseStructure.buildResponse(
//	                             null,
//	                             "Visit type is required.",
//	                             HttpStatus.BAD_REQUEST,
//	                             HttpStatus.BAD_REQUEST.value()
//	                     ));
//	         }
//
//	         // =========================
//	         // FOLLOW-UP BOOKING
//	         // =========================
//	         if ("follow-up".equalsIgnoreCase(request.getVisitType())) {
//
//	             Booking b = repository.findByMobileNumberAndPatientIdAndBookingId(
//	                     request.getMobileNumber(), request.getPatientId(), request.getBookingId());
//
//	             if (b == null) {
//	                 return ResponseEntity.status(HttpStatus.NOT_FOUND)
//	                         .body(ResponseStructure.buildResponse(
//	                                 null,
//	                                 "No appointment found.",
//	                                 HttpStatus.NOT_FOUND,
//	                                 HttpStatus.NOT_FOUND.value()));
//	             }
//
//	             if (!"In-Progress".equalsIgnoreCase(b.getStatus())) {
//	                 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//	                         .body(ResponseStructure.buildResponse(
//	                                 null,
//	                                 "No In-Progress appointments found for the provided booking ID.",
//	                                 HttpStatus.BAD_REQUEST,
//	                                 HttpStatus.BAD_REQUEST.value()));
//	             }
//
//	             boolean sittingBooked = false;
//	             DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//	             if (b.getTreatments() != null && b.getTreatments().getGeneratedData() != null) {
//	                 for (Map.Entry<String, TreatmentDetailsDTO> entry : b.getTreatments().getGeneratedData().entrySet()) {
//	                     String treatmentName = entry.getKey();
//	                     TreatmentDetailsDTO treatment = entry.getValue();
//
//	                     // Filter by subServiceName (only the treatment being booked)
//	                     if (request.getSubServiceName() != null &&
//	                             !request.getSubServiceName().equalsIgnoreCase(treatmentName)) {
//	                         continue;
//	                     }
//
//	                     // 🔹 Find the first pending sitting (next to confirm)
//	                     Optional<DatesDTO> nextPendingOpt = treatment.getDates().stream()
//	                             .filter(d -> "Pending".equalsIgnoreCase(d.getStatus()))
//	                             .sorted(Comparator.comparingInt(DatesDTO::getSitting))
//	                             .findFirst();
//
//	                     if (nextPendingOpt.isEmpty()) continue;
//
//	                     DatesDTO nextSitting = nextPendingOpt.get();
//
//	                     // ✅ Mark this sitting as Confirmed
//	                     nextSitting.setStatus("Confirmed");
//	                     nextSitting.setFollowupStatus(null);
//
//	                     // ✅ Update booking follow-up date & time
//	                     b.setFollowupDate(nextSitting.getDate());
//	                     b.setServicetime(request.getServicetime());
//	                     b.setVisitType(request.getVisitType());
//	                     b.setSubServiceName(treatmentName);
//
//	                     // 🧮 Update treatment-level stats
//	                     treatment.setTakenSittings(treatment.getTakenSittings() + 1);
//	                     treatment.setPendingSittings(Math.max(0, treatment.getPendingSittings() - 1));
//	                     treatment.setCurrentSitting(treatment.getTakenSittings() + 1);
//	                     treatment.setStatus(treatment.getPendingSittings() == 0 ? "Confirmed" : "In-Progress");
//
//	                     // 💸 Decrement free follow-ups if available
//	                     if (b.getFreeFollowUpsLeft() != null && b.getFreeFollowUpsLeft() > 0) {
//	                         b.setFreeFollowUpsLeft(b.getFreeFollowUpsLeft() - 1);
//	                     }
//
//	                     sittingBooked = true;
//	                     break; // Only one sitting booked per request
//	                 }
//	             }
//
//	             if (!sittingBooked) {
//	                 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//	                         .body(ResponseStructure.buildResponse(
//	                                 null,
//	                                 "No pending sittings available for the selected treatment.",
//	                                 HttpStatus.BAD_REQUEST,
//	                                 HttpStatus.BAD_REQUEST.value()));
//	             }
//
//	             // 🔹 Update booking-level status dynamically
//	             boolean anyInProgress = b.getTreatments().getGeneratedData().values().stream()
//	                     .anyMatch(t -> "In-Progress".equalsIgnoreCase(t.getStatus()));
//	             boolean allConfirmed = b.getTreatments().getGeneratedData().values().stream()
//	                     .allMatch(t -> "Confirmed".equalsIgnoreCase(t.getStatus()));
//
//	             if (allConfirmed) {
//	                 b.setStatus("Confirmed");
//	             } else if (anyInProgress) {
//	                 b.setStatus("In-Progress");
//	             } else {
//	                 b.setStatus("Pending");
//	             }
//
//	             Booking updatedBooking = repository.save(b);
//	             nullifyLargeFields(updatedBooking);
//
//	             // 🔔 Publish to Kafka
//	             try {
//	                 kafkaProducer.publishBooking(updatedBooking);
//	             } catch (Exception e) {
//	                 System.err.println("⚠️ Kafka publish failed: " + e.getMessage());
//	             }
//
//	             BookingResponse res = toResponse(updatedBooking);
//
//	             // 🩵 Keep only the booked treatment in the response
//	             if (res.getTreatments() != null && res.getTreatments().getGeneratedData() != null) {
//	                 Map<String, TreatmentDetailsDTO> onlySelected = new HashMap<>();
//	                 TreatmentDetailsDTO selectedTreatment = res.getTreatments().getGeneratedData().get(request.getSubServiceName());
//	                 if (selectedTreatment != null) {
//	                     onlySelected.put(request.getSubServiceName(), selectedTreatment);
//	                     res.getTreatments().setGeneratedData(onlySelected);
//	                 }
//	             }
//
//	             // Optional: explicitly include which treatment was updated
//	             res.setUpdatedTreatment(request.getSubServiceName());
//
//	             response = ResponseStructure.buildResponse(
//	                     res,
//	                     "Next sitting booked successfully for " + request.getSubServiceName(),
//	                     HttpStatus.OK,
//	                     HttpStatus.OK.value());
//
//	             return ResponseEntity.ok(response);
//	         }
//
//	         // =========================
//	         // NEW BOOKING
//	         // =========================
//	         if (entity.getTreatments() != null && entity.getTreatments().getGeneratedData() != null) {
//	             entity.getTreatments().getGeneratedData().forEach((name, t) -> {
//	                 if (t.getPendingSittings() != null && t.getPendingSittings() > 0) {
//	                     t.setStatus("In-Progress");
//	                 } else {
//	                     t.setStatus("Confirmed");
//	                 }
//	             });
//
//	             boolean allConfirmed = entity.getTreatments().getGeneratedData().values().stream()
//	                     .allMatch(t -> "Confirmed".equalsIgnoreCase(t.getStatus()));
//	             entity.setStatus(allConfirmed ? "Confirmed" : "In-Progress");
//	         } else {
//	             entity.setStatus("Confirmed");
//	         }
//
//	         Booking savedBooking = repository.save(entity);
//	         nullifyLargeFields(savedBooking);
//
//	         try {
//	             kafkaProducer.publishBooking(savedBooking);
//	         } catch (Exception e) {
//	             System.err.println("⚠️ Kafka publish failed: " + e.getMessage());
//	         }
//
//	         BookingResponse bRes = toResponse(savedBooking);
//	         response = ResponseStructure.buildResponse(
//	                 bRes,
//	                 "Service booked successfully",
//	                 HttpStatus.CREATED,
//	                 HttpStatus.CREATED.value());
//	         return ResponseEntity.status(HttpStatus.CREATED).body(response);
//
//	     } catch (Exception e) {
//	         e.printStackTrace();
//	         response = ResponseStructure.buildResponse(
//	                 null,
//	                 "Internal Server Error: " + e.getMessage(),
//	                 HttpStatus.INTERNAL_SERVER_ERROR,
//	                 HttpStatus.INTERNAL_SERVER_ERROR.value());
//	         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//	     }
//	 }
//
//	 
//	 // =========================
//	 // Support Methods
//	 // =========================
//	 private void nullifyLargeFields(Booking booking) {
//	     if (booking == null) return;
//	     booking.setReports(null);
//	     booking.setNotes(null);
//	     booking.setAttachments(null);
//	     booking.setConsentFormPdf(null);
//	     booking.setPrescriptionPdf(null);
//	 }
//
//	 private Booking toEntity(BookingRequset request) {
//	     Booking entity = new ObjectMapper().convertValue(request, Booking.class);
//	     ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
//	     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
//	     entity.setBookedAt(istTime.format(formatter));
//	     entity.setFreeFollowUpsLeft(request.getFreeFollowUps());
//
//	     // Channel ID for online/video consultations
//	     if (request.getConsultationType() != null &&
//	             (request.getConsultationType().equalsIgnoreCase("video consultation") ||
//	                     request.getConsultationType().equalsIgnoreCase("online consultation"))) {
//	         entity.setChannelId(randomNumber());
//	     }
//
//	     // Patient ID logic
//	     if (request.getBookingFor() != null) {
//	         if ("Someone".equalsIgnoreCase(request.getBookingFor())) {
//	             if (request.getRelation() != null &&
//	                     (request.getPatientId() == null || request.getPatientId().trim().isEmpty())) {
//
//	                 List<Booking> existingBooking = repository.findByRelationIgnoreCaseAndCustomerIdAndNameIgnoreCase(
//	                         request.getRelation(), request.getCustomerId(), request.getName());
//
//	                 if (existingBooking != null && !existingBooking.isEmpty()) {
//	                     entity.setPatientId(existingBooking.get(0).getPatientId());
//	                 } else {
//	                     entity.setPatientId(generatePatientId(request));
//	                 }
//	             } else {
//	                 entity.setPatientId(request.getPatientId());
//	             }
//	         } else {
//	             if (request.getPatientId() == null || request.getPatientId().trim().isEmpty()) {
//	                 entity.setPatientId(generatePatientId(request));
//	             } else {
//	                 entity.setPatientId(request.getPatientId());
//	             }
//	         }
//	     }
//
//	     return entity;
//	 }
//
//	 private BookingResponse toResponse(Booking entity) {
//	     BookingResponse response = new ObjectMapper().convertValue(entity, BookingResponse.class);
//
//	     // Attach prescription PDF if exists
//	     DoctorSaveDetailsDTO dto = getPrescriptionpdf(response.getBookingId());
//	     if (dto != null) {
//	         response.setPrescriptionPdf(dto.getPrescriptionPdf());
//	     }
//
//	     response.setBookingId(String.valueOf(entity.getBookingId()));
//
//	     // Update each treatment status
//	     if (entity.getTreatments() != null && entity.getTreatments().getGeneratedData() != null) {
//	         entity.getTreatments().getGeneratedData().forEach((name, t) -> {
//	             if (t.getPendingSittings() != null && t.getPendingSittings() > 0) {
//	                 t.setStatus("In-Progress");
//	             } else {
//	                 t.setStatus("Confirmed");
//	             }
//	         });
//	     }
//
//	     return response;
//	 }
//
//	 
//	 private DoctorSaveDetailsDTO getPrescriptionpdf(String bid) {
//	     try {
//	         ObjectMapper mapper = new ObjectMapper();
//	         mapper.registerModule(new JavaTimeModule());
//	         mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//	         Response res = doctorFeign.getDoctorSaveDetailsByBookingId(bid).getBody();
//	         return mapper.convertValue(res.getData(), DoctorSaveDetailsDTO.class);
//	     } catch (Exception e) {
//	         System.out.println(e.getMessage());
//	         return null;
//	     }
//	 }
//
//	 
//	  private static String generatePatientId(BookingRequset request) {	       
//	        String uuid = UUID.randomUUID().toString();
//	        String randomPart = uuid.replaceAll("-", "").substring(0, 6).toUpperCase();
//	        return request.getBranchId()+"_"+"PT_" + randomPart;
//	    }
//	
//	
//	private static String randomNumber() {
//        Random random = new Random();    
//        int sixDigitNumber = 100000 + random.nextInt(900000); // Generates number from 100000 to 999999
//        return String.valueOf(sixDigitNumber);
//    }
//
//	
//	private List<BookingResponse> toResponses(List<Booking> bookings) {		
//		List<BookingResponse> res = new ObjectMapper().convertValue(bookings,new TypeReference<List<BookingResponse>>(){});
//		for(BookingResponse bres : res) {
//			//System.out.println(bres.getBookingId());
//			DoctorSaveDetailsDTO dto = getPrescriptionpdf(bres.getBookingId());
//			//System.out.println(dto);
//			if(dto != null ) {
//			bres.setPrescriptionPdf(dto.getPrescriptionPdf());}}
//		return res;
//	}	
//	
//	public ResponseEntity<?> getAppointsByPatientId(String patientId) {
//		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
//		    try {
//			List<Booking> existingBooking = repository.findByPatientId(patientId);
//			if(existingBooking != null && !existingBooking.isEmpty() ) {
//			List<BookingResponse> respnse = new ObjectMapper().convertValue(existingBooking, new TypeReference<List<BookingResponse>>() {});
//			res.setStatusCode(200);
//			res.setData(respnse);
//			res.setMessage("Appointments Are Found");
//			return ResponseEntity.status(200).body(res); 
//			}else{
//				res.setStatusCode(200);
//				res.setMessage("Appointments Are Not Found");
//				return ResponseEntity.status(200).body(res);}
//		    }catch(Exception e) {
//			res.setStatusCode(500);
//			res.setMessage(e.getMessage());
//			return ResponseEntity.status(500).body(res);
//		}
//	}
//
//	
//	
//	public ResponseEntity<?> getAppointsByInput(String input) {
//		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
//		try {
//			List<Booking> existingBooking = repository.findByNameIgnoreCaseOrBookingIdOrPatientId(input);
//			if(existingBooking != null && !existingBooking.isEmpty() ) {
//			List<BookingResponse> respnse = new ObjectMapper().convertValue(existingBooking, new TypeReference<List<BookingResponse>>() {});
//			res.setStatusCode(200);
//			res.setData(respnse);
//			res.setMessage("Appointments Are Found");
//			return ResponseEntity.status(200).body(res); 
//			}else {
//				res.setStatusCode(200);
//				res.setMessage("Appointments Are Not Found");
//				return ResponseEntity.status(200).body(res);
//			}}catch(Exception e) {
//			res.setStatusCode(500);
//			res.setMessage(e.getMessage());
//			return ResponseEntity.status(500).body(res);
//		}
//	}
//	
//	
//
//	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(String hospitalId, String doctorId) {
//	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
//	    List<BookingResponse> responseList = new ArrayList<>();
//
//	    try {
//	        List<Booking> existingBookings = repository.findByClinicIdAndDoctorId(hospitalId, doctorId);
//	        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//	        LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
//
//	        if (existingBookings != null && !existingBookings.isEmpty()) {
//
//	            for (Booking b : existingBookings) {
//	                boolean added = false;
//
//	                // 🩺 Case A: First-time booking (no treatments)
//	                if (b.getTreatments() == null || b.getTreatments().getGeneratedData() == null) {
//	                    if (b.getServiceDate() != null) {
//	                        LocalDate bookingDate = LocalDate.parse(b.getServiceDate(), dateFormatter);
//
//	                        if (bookingDate.equals(currentDate) &&
//	                                (b.getStatus().equalsIgnoreCase("Confirmed") ||
//	                                 b.getStatus().equalsIgnoreCase("In-Progress"))) {
//
//	                            BookingResponse temp = toResponse(b);
//	                            temp.setServiceDate(b.getServiceDate());
//	                            temp.setServicetime(b.getServicetime());
//	                            temp.setStatus(b.getStatus());
//	                            temp.setSubServiceName(b.getSubServiceName());
//	                            responseList.add(temp);
//	                            added = true;
//	                        }
//	                    }
//	                }
//
//	                // 💊 Case B: Has treatment sittings
//	                if (!added && b.getTreatments() != null && b.getTreatments().getGeneratedData() != null) {
//	                    for (Map.Entry<String, TreatmentDetailsDTO> entry : b.getTreatments().getGeneratedData().entrySet()) {
//	                        String treatmentName = entry.getKey();
//	                        TreatmentDetailsDTO t = entry.getValue();
//
//	                        if (t.getDates() != null) {
//	                            for (DatesDTO d : t.getDates()) {
//	                                LocalDate sittingDate = LocalDate.parse(d.getDate(), dateFormatter);
//
//	                                if (sittingDate.equals(currentDate) &&
//	                                        (d.getStatus().equalsIgnoreCase("Confirmed") ||
//	                                         d.getStatus().equalsIgnoreCase("In-Progress"))) {
//
//	                                    BookingResponse temp = toResponse(b);
//	                                    temp.setSubServiceName(treatmentName);
//	                                    temp.setServiceDate(d.getDate());
//	                                    temp.setServicetime(b.getServicetime());
//	                                    temp.setStatus(d.getStatus());
//
//	                                    // keep only this treatment info
//	                                    Map<String, TreatmentDetailsDTO> oneTreatment = new HashMap<>();
//	                                    oneTreatment.put(treatmentName, t);
//	                                    temp.getTreatments().setGeneratedData(oneTreatment);
//
//	                                    responseList.add(temp);
//	                                    break;
//	                                }
//	                            }
//	                        }
//	                    }
//	                }
//	            }
//
//	            if (!responseList.isEmpty()) {
//	                res.setStatusCode(200);
//	                res.setHttpStatus(HttpStatus.OK);
//	                res.setData(responseList);
//	                res.setMessage("Today's Appointments Found");
//	            } else {
//	                res.setStatusCode(200);
//	                res.setHttpStatus(HttpStatus.OK);
//	                res.setData(responseList);
//	                res.setMessage("No Appointments for Today");
//	            }
//	        } else {
//	            res.setStatusCode(200);
//	            res.setHttpStatus(HttpStatus.OK);
//	            res.setData(responseList);
//	            res.setMessage("Appointments Not Found");
//	        }
//
//	    } catch (Exception e) {
//	        res.setStatusCode(500);
//	        res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
//	        res.setMessage("Error occurred: " + e.getMessage());
//	    }
//
//	    return ResponseEntity.status(res.getStatusCode()).body(res);
//	}
//
//
//
//	public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(String hospitalId, String doctorId, String number) {
//	    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
//	    List<BookingResponse> responses = new ArrayList<>();
//	    try {
//	        List<Booking> existingBooking = repository.findByClinicIdAndDoctorId(hospitalId, doctorId);
//	        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//	        String currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(dateFormatter);
//
//	        if (existingBooking != null && !existingBooking.isEmpty()) {
//	            for (Booking b : existingBooking) {
//	                if (b.getTreatments() != null && b.getTreatments().getGeneratedData() != null) {
//	                    for (Map.Entry<String, TreatmentDetailsDTO> entry : b.getTreatments().getGeneratedData().entrySet()) {
//	                        String treatmentName = entry.getKey();
//	                        TreatmentDetailsDTO t = entry.getValue();
//
//	                        if (t.getDates() != null) {
//	                            for (DatesDTO dateEntry : t.getDates()) {
//	                                // Create a clone of the booking per sitting date
//	                                BookingResponse temp = toResponse(b);
//
//	                                // Keep only the current treatment in generatedData
//	                                Map<String, TreatmentDetailsDTO> oneTreatment = new HashMap<>();
//	                                oneTreatment.put(treatmentName, t);
//	                                temp.getTreatments().setGeneratedData(oneTreatment);
//	                                temp.setSubServiceName(treatmentName);
//
//	                                // Set service date and time per sitting date
//	                                temp.setServiceDate(dateEntry.getDate());
//	                                // If time is specific per sitting, set here; otherwise, fallback to booking time
//	                                temp.setServicetime(b.getServicetime());
//
//	                                // Filtering logic based on the 'number' parameter and sitting date status
//	                                boolean add = false;
//	                                switch (number) {
//	                                    case "1":
//	                                        if ((b.getConsultationType().equalsIgnoreCase("Services & Treatments")
//	                                                || b.getConsultationType().equalsIgnoreCase("In-Clinic Consultation")
//	                                                || b.getConsultationType().equalsIgnoreCase("Online Consultation"))
//	                                                && "Confirmed".equalsIgnoreCase(dateEntry.getStatus())
//	                                                && !dateEntry.getDate().equals(currentDate)) {
//	                                            add = true;
//	                                        }
//	                                        break;
//
//	                                    case "2":
//	                                        if (b.getConsultationType().equalsIgnoreCase("Online Consultation")
//	                                                && "Confirmed".equalsIgnoreCase(dateEntry.getStatus())
//	                                                && !dateEntry.getDate().equals(currentDate)) {
//	                                            add = true;
//	                                        }
//	                                        break;
//
//	                                    case "3":
//	                                        if ("Completed".equalsIgnoreCase(dateEntry.getStatus())) {
//	                                            add = true;
//	                                        }
//	                                        break;
//
//	                                    case "4":
//	                                        if ("In-Progress".equalsIgnoreCase(dateEntry.getStatus())) {
//	                                            add = true;
//	                                        }
//	                                        break;
//	                                }
//
//	                                if (add) {
//	                                    responses.add(temp);
//	                                }
//	                            }
//	                        }
//	                    }
//	                }
//	            }
//
//	            if (!responses.isEmpty()) {
//	                res.setStatusCode(200);
//	                res.setData(responses);
//	                res.setMessage("Appointments Are Found");
//	            } else {
//	                res.setStatusCode(200);
//	                res.setData(responses);
//	                res.setMessage("Appointments Are Not Found");
//	            }
//	        } else {
//	            res.setStatusCode(200);
//	            res.setData(responses);
//	            res.setMessage("Appointments Are Not Found");
//	        }
//	    } catch (Exception e) {
//	        responses = null;
//	        res.setStatusCode(500);
//	        res.setData(responses);
//	        res.setMessage(e.getMessage());
//	    }
//	    return ResponseEntity.status(res.getStatusCode()).body(res);
//	}
//
//		
//	public ResponseEntity<?> getCompletedApntsByDoctorId(String hospitalId,String doctorId) {
//		    Map<String,Object> m = new LinkedHashMap<>();
//		try {
//			List<Booking> existingBooking = repository.findByClinicIdAndDoctorId(hospitalId, doctorId);
//			List<BookingResponse> res = new ArrayList<>();
//			if(existingBooking != null) {
//			for(Booking b : existingBooking) {
//			if(b.getStatus().equalsIgnoreCase("Completed")) {
//			res.add(toResponse(b));}}
//			 m.put("completedAppointmentsCount",res.size());
//			 m.put("status",200);
//			 return ResponseEntity.status(200).body(m);
//			}else {
//				 m.put("Message","No Appointsments Found");
//				 m.put("status",200);
//				return ResponseEntity.status(200).body(m);
//			}
//		}catch(Exception e) {
//			 m.put("Message",e.getMessage());
//			 m.put("status",500);
//			return ResponseEntity.status(500).body(m);}
//	}
//	
//	
//	public ResponseEntity<?> getSizeOfConsultationTypesByDoctorId(String hospitalId,String doctorId) {
//	    Map<String,Object> m = new LinkedHashMap<>();
//	try {
//		List<Booking> existingBooking = repository.findByClinicIdAndDoctorId(hospitalId, doctorId);
//		List<BookingResponse> servicesAndConsul = new ArrayList<>();
//		List<BookingResponse> inClinic = new ArrayList<>();
//		List<BookingResponse> online = new ArrayList<>();
//		if(existingBooking != null) {
//		for(Booking b : existingBooking) {
//		if(b.getStatus().equalsIgnoreCase("Completed")) {
//		if(b.getConsultationType().equalsIgnoreCase("Services & Treatments")) {
//		servicesAndConsul.add(toResponse(b));}
//		if(b.getConsultationType().equalsIgnoreCase("In-Clinic Consultation")){
//			inClinic.add(toResponse(b));}
//		if(b.getConsultationType().equalsIgnoreCase("Online Consultation")){
//			online.add(toResponse(b));}
//		}}
//		 m.put("services & Treatments",servicesAndConsul.size());
//		 m.put("in-Clinic Consultation",inClinic.size());
//		 m.put("online Consultation",online.size());
//		 m.put("status",200);
//		 return ResponseEntity.status(200).body(m);
//		}else {
//			 m.put("Message","No Appointsments Found");
//			 m.put("status",200);
//			return ResponseEntity.status(200).body(m);}
//	}catch(Exception e) {
//		 m.put("Message",e.getMessage());
//		 m.put("status",500);
//		return ResponseEntity.status(500).body(m);}
//}
//	
//	
//		
//	public BookingResponse getBookedService(String id) {
//		Booking entity = repository.findByBookingId(id)
//				.orElseThrow(() -> new RuntimeException("Invalid Booking Id Please provide Valid Id"));
//		return toResponse(entity);
//	}
//
//	@Override
//	public BookingResponse deleteService(String id) {
//		Booking entity = repository.findByBookingId(id)
//				.orElseThrow(() -> new RuntimeException("Invalid Booking Id Please provide Valid Id"));
//		repository.deleteById(id);
//		return toResponse(entity);
//	}
//
//	@Override
//	public List<BookingResponse> getBookedServices(String mobileNumber) {
//		List<Booking> bookings = repository.findByMobileNumber(mobileNumber);
//		List<Booking> reversedBookings = new ArrayList<>();
//		for(int i = bookings.size()-1; i >= 0; i--) {
//			reversedBookings.add(bookings.get(i));
//		}
//		if (bookings == null  || bookings.isEmpty()) {
//			return null;
//		}
//		return toResponses(reversedBookings);
//	}
//	
//	@Override
//	public List<BookingResponse> getAllBookedServices() {
//		List<Booking> bookings = repository.findAll();
//		List<Booking> reversedBookings = new ArrayList<>();
//		for(int i = bookings.size()-1; i >= 0; i--) {
//			reversedBookings.add(bookings.get(i));
//		}
//		if (bookings == null  || bookings.isEmpty()) {
//			return null;
//		}
//		return toResponses(reversedBookings);
//	}
//
//	@Override
//	public List<BookingResponse> bookingByDoctorId(String doctorId) {
//		List<Booking> bookings = repository.findByDoctorId(doctorId);
//		List<Booking> reversedBookings = new ArrayList<>();
//		for(int i = bookings.size()-1; i >= 0; i--) {
//			reversedBookings.add(bookings.get(i));
//		}
//		if (bookings == null  || bookings.isEmpty()) {
//			return null;
//		}
//		return toResponses(reversedBookings);
//	}
//
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
//	
//	
//	@Override
//	public List<BookingResponse> bookingByCustomerId(String customerId) {
//		List<Booking> bookings = repository.findByCustomerId(customerId);
//		List<Booking> reversedBookings = new ArrayList<>();
//		for(int i = bookings.size()-1; i >= 0; i--) {
//			reversedBookings.add(bookings.get(i));
//		}
//		if (bookings == null  || bookings.isEmpty()) {
//			return null;
//		}
//		return toResponses(reversedBookings);
//	}
//	
//	
//	@Override
//	public List<BookingResponse> bookingByPatientId(String patientId) {
//		List<Booking> bookings = repository.findByPatientId(patientId);
//		List<Booking> reversedBookings = new ArrayList<>();
//		for(int i = bookings.size()-1; i >= 0; i--) {
//			if(bookings.get(i).getStatus().equalsIgnoreCase("In-Progress")) {
//			reversedBookings.add(bookings.get(i));
//		}}
//		if (bookings == null  || bookings.isEmpty()) {
//			return null;
//		}
//		return toResponses(reversedBookings);
//	}
//		
//	@Override
//	public List<BookingInfoByInput> bookingByInput(String input,String clinicId) {
//		   List<BookingInfoByInput> outpt = new ArrayList<>();
//	       try {
//	    	 List<Booking> bookings = repository.findByMobileNumberAndClinicId(input,clinicId);
//		        // If still not found, try customerId
//	    	// System.out.println(bookings);
//		    	 if(bookings == null || bookings.isEmpty()) {
//		            bookings = repository.findByCustomerIdAndClinicId(input,clinicId);
//		            //System.out.println(bookings);
//		    	} 
//	        	 if(bookings == null || bookings.isEmpty()) {
//		            bookings = repository.findByNameContainingIgnoreCaseAndClinicId(input,clinicId);
//		          // System.out.println(bookings);
//		        }
//	        	if(bookings != null && !bookings.isEmpty()) {
//		        for(Booking b : bookings) {
//		        	//System.out.println(b);
//		        BookingInfoByInput bkng = new BookingInfoByInput() ;	
//		        bkng.setAge(b.getAge());
//		        bkng.setClinicId(b.getClinicId());
//		        bkng.setCustomerId(b.getCustomerId());
//		        bkng.setGender(b.getGender());
//		        bkng.setMobileNumber(b.getMobileNumber());
//		        bkng.setName(b.getName());
//		        bkng.setPatientAddress(b.getPatientAddress());
//		        bkng.setPatientId(b.getPatientId());
//		        bkng.setPatientMobileNumber(b.getPatientMobileNumber());
//		        bkng.setRelation(b.getRelation());
//		        outpt.add(bkng);}}
//	        if(input.contains("_")){
//		    List<Booking> bookgs = repository.findByPatientIdAndClinicId(input,clinicId);
//	    	if( bookgs != null && !bookgs.isEmpty()) {
//		        Booking b = bookgs.get(0);	
//		        BookingInfoByInput bkng = new BookingInfoByInput() ;	
//		        bkng.setAge(b.getAge());
//		        bkng.setClinicId(b.getClinicId());
//		        bkng.setCustomerId(b.getCustomerId());
//		        bkng.setGender(b.getGender());
//		        bkng.setMobileNumber(b.getMobileNumber());
//		        bkng.setName(b.getName());
//		        bkng.setPatientAddress(b.getPatientAddress());
//		        bkng.setPatientId(b.getPatientId());
//		        bkng.setPatientMobileNumber(b.getPatientMobileNumber());
//		        bkng.setRelation(b.getRelation());
//		        outpt.add(bkng); 
//		        }else{
//		      Response res = clinicAdminFeign.getCustomerByPatientId(input,clinicId).getBody();
//		     // System.out.println(res);
//		      CustomerOnbordingDTO bg = new ObjectMapper().convertValue(res.getData(), CustomerOnbordingDTO.class);
//		      //System.out.println(bg);
//		      BookingInfoByInput bkng = new BookingInfoByInput() ;	
//		        bkng.setAge(bg.getAge());
//		        bkng.setClinicId(null);
//		        bkng.setCustomerId(bg.getCustomerId());
//		        bkng.setGender(bg.getGender());
//		        bkng.setMobileNumber(bg.getMobileNumber());
//		        bkng.setName(bg.getFullName());
//		        bkng.setPatientAddress(bg.getAddress().toString());
//		        bkng.setPatientId(bg.getPatientId());
//		        bkng.setPatientMobileNumber(bg.getMobileNumber());
//		        bkng.setRelation(null);
//		        outpt.add(bkng);}} 
//	       }catch (Exception e) {
//	        //System.err.println("Error fetching bookings: " + e.getMessage());
//	        System.out.println(e.getMessage());; // safe fallback
//	    }
//	    return outpt;
//	}
//
//		
//	@Override
//	public List<BookingResponse> bookingByClinicId(String clinicId) {
//		List<Booking> bookings = repository.findByClinicId(clinicId);
//		List<Booking> reversedBookings = new ArrayList<>();
//		for(int i = bookings.size()-1; i >= 0; i--) {
//			reversedBookings.add(bookings.get(i));
//		}
//		if(bookings==null || bookings.isEmpty()) {
//		 return null;
//		}
//		return toResponses(bookings);
//	}
//	
//	
//
//
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
//	        if (bookingResponse.getStatus() != null) entity.setStatus(bookingResponse.getStatus());
//	        if (bookingResponse.getNotes() != null) entity.setNotes(bookingResponse.getNotes());
//	        if (bookingResponse.getReports() != null) {
//	            entity.setReports(new ObjectMapper().convertValue(
//	                    bookingResponse.getReports(),
//	                    new TypeReference<List<ReportsList>>() {}));
//	        }
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
////	@Scheduled(cron = "0 01 0 * * ?")
////	////@Scheduled(fixedRate = 20000)
////	private void changingStatusFromConfirmedToCompleted() {
////	    try {
////	        List<Booking> bookings = repository.findAll();
////	        for (Booking b : bookings) {
////	            if (b.getStatus().equalsIgnoreCase("In-Progress")) {
////	            	
////	                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
////	                LocalDateTime bookedDateTime = LocalDateTime.parse(b.getBookedAt(), inputFormatter);
////                   // System.out.println(bookedDateTime);
////                    
////	                ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
////	                LocalDate todayDate = istTime.toLocalDate(); // only date part
////	                LocalDate bookedDate = bookedDateTime.toLocalDate(); // only date part
////                   // System.out.println(bookedDate);
////	                long gap = ChronoUnit.DAYS.between(bookedDate, todayDate);
////                   // System.out.println(gap);	               
////	                int expirationDays = Integer.parseInt(Character.toString(b.getConsultationExpiration().charAt(0)) + 
////	            			Character.toString(b.getConsultationExpiration().charAt(1)));
////                   // System.out.println(expirationDays);
////	               
////	                if (gap > expirationDays) {
////	                    b.setStatus("Completed");
////	                    repository.save(b);
////
////	                    NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
////	                    n.getData().setStatus("Completed");
////	                    notificationFeign.updateNotification(n);
////	                    //System.out.println("Updated to Completed for bookingId: " + b.getBookingId());
////	                    }}}}catch (Exception e) {}}
////
////	
////	
////	@Scheduled(cron = "0 30 0 * * ?")
////	////@Scheduled(fixedRate = 20000)
////	private void secondTimeChangingStatusFromConfirmedToCompleted() {
////	    try {
////	        List<Booking> bookings = repository.findAll();
////	        for (Booking b : bookings) {
////	            if (b.getStatus().equalsIgnoreCase("In-Progress")) {
////	              
////	                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
////	                LocalDateTime bookedDateTime = LocalDateTime.parse(b.getBookedAt(), inputFormatter);
////	               // System.out.println(bookedDateTime);
////	                ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
////	                LocalDate todayDate = istTime.toLocalDate(); // only date part
////	                LocalDate bookedDate = bookedDateTime.toLocalDate(); // only date part
////	               // System.out.println(bookedDate);
////	                long gap = ChronoUnit.DAYS.between(bookedDate, todayDate);
////	                int expirationDays = Integer.parseInt(Character.toString(b.getConsultationExpiration().charAt(0)) + 
////	            			Character.toString(b.getConsultationExpiration().charAt(1)));
////	                //System.out.println(gap);
////	               // System.out.println(expirationDays);
////	                if (gap > expirationDays) {
////	                    b.setStatus("Completed");
////	                    repository.save(b);
////
////	                    NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
////	                    n.getData().setStatus("Completed");
////	                    notificationFeign.updateNotification(n);
////
////	                    //System.out.println("Updated to Completed for bookingId: " + b.getBookingId());
////	                }}}}catch (Exception e) {}}
////
////	
//	
//	@Scheduled(fixedRate = 60 * 60 * 1000)
//	public void autoCalculatePatientCompletedAppointments() { 
//		Map<String,Integer> map = new LinkedHashMap<>();
//		Set<String> ids = new LinkedHashSet<>();
//		    try {
//			List<Booking> existingBooking = repository.findAll();
//			//System.out.println("existingBooking");
//			if(existingBooking != null && !existingBooking.isEmpty()){
//				//System.out.println("not null");
//			for(Booking b:existingBooking) {
//			if(b.getStatus().equalsIgnoreCase("Completed")) {
//				//System.out.println("find complted");
//				if(!ids.contains(b.getPatientId())){
//			List<Booking> bookings = repository.findByPatientId(b.getPatientId());
//			ids.add(b.getPatientId());
//			//System.out.println("got obj by patient id");
//			for(Booking c:bookings) {
//			if(c.getStatus().equalsIgnoreCase("Completed")) {
//				//System.out.println("patient id with cmplted");
//			if(map.containsKey(b.getPatientId())){
//				//System.out.println("adding to map");
//				Integer value = map.get(b.getPatientId());
//				int vlue = value.intValue();
//				vlue += 1;
//				value = Integer.valueOf(vlue);
//			map.put(b.getPatientId(),value);
//			}else{
//				map.put(b.getPatientId(),1);
//			}
//			for(String key : map.keySet()) {
//				List<Booking> bkings = repository.findByPatientId(key);
//				//System.out.println("got obj with key in map");
//				for(Booking bkng : bkings ) {
//					bkng.setVisitCount(map.get(key));
//					repository.save(bkng);}
//			}}}}}}}}catch(Exception e) {}
//	   }
//	
//	
//	
//	//---------------------------to get patientdetails by bookingId,pateintId,mobileNumber---------------------------
//		@Override
//		public Response getPatientDetailsForConsetForm(String bookingId, String patientId, String mobileNumber) {
//		    try {
//			Optional<Booking> optionalBooking = repository.findByBookingIdAndPatientIdAndMobileNumber(bookingId, patientId, mobileNumber);
//		    if (optionalBooking.isPresent()) {
//		        Booking booking = optionalBooking.get();
//		        if(booking.getStatus().equalsIgnoreCase("Confirmed") || booking.getStatus().equalsIgnoreCase("Completed")){
//		        BookingResponse response = new ObjectMapper().convertValue(booking, BookingResponse.class);
//		        return Response.builder()
//		                .success(true)
//		                .status(200)
//		                .message("Booking details fetched successfully.")
//		                .data(response)
//		                .build();
//		         }else{
//		    	 return Response.builder()
//			                .success(false)
//			                .status(404)
//			                .message("No booking found with the given details.")
//			                .build();	
//		        }}else{
//		        return Response.builder()
//		                .success(false)
//		                .status(404)
//		                .message("No booking found with the given details.")
//		                .build();}
//		        }catch(Exception e){
//			    return Response.builder()
//	                .success(false)
//	                .status(500)
//	                .message(e.getMessage())
//	                .build();}}	    
//
//
//		public ResponseEntity<?> getInProgressAppointments(String number){
//			ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
//			   try{
//				List<Booking> booked=repository.findByMobileNumber(number);
//				List<BookingResponse> response=new ArrayList<>();
//				if(booked!=null && !booked.isEmpty()){
//					for(Booking b:booked){
//						if(b.getStatus().equalsIgnoreCase("In-Progress")){
//							response.add(toResponse(b));}}
//					if(response!=null && !response.isEmpty()){
//						res.setStatusCode(200);
//						res.setHttpStatus(HttpStatus.OK);
//						res.setData(response);
//						res.setMessage("In-Progress appointments found");
//					}else{
//						res.setStatusCode(200);
//						res.setHttpStatus(HttpStatus.OK);
//						res.setData(response);
//						res.setMessage("In-Progress appointments not found");}}}
//			catch(Exception e){
//				res.setStatusCode(500);
//				res.setMessage(e.getMessage());}
//			return ResponseEntity.status(res.getStatusCode()).body(res);
//		}
//		
//		
//
//		
//		@Override
//		public ResponseEntity<?> getInProgressAppointmentsByCustomerId(String customerId) {
//		    ResponseStructure<List<BookingResponse>> response = new ResponseStructure<>();
//
//		    try {
//		        List<Booking> bookings = repository.findByCustomerIdAndStatusIgnoreCase(customerId, "In-Progress");
//
//		        if (bookings == null || bookings.isEmpty()) {
//		            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//		                    .body(ResponseStructure.buildResponse(
//		                            null,
//		                            "No in-progress bookings found for this customer",
//		                            HttpStatus.NOT_FOUND,
//		                            404
//		                    ));
//		        }
//
//		        List<BookingResponse> bookingResponses = new ArrayList<>();
//		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//		        for (Booking booking : bookings) {
//		            nullifyLargeFields(booking);
//
//		            if (booking.getTreatments() != null && booking.getTreatments().getGeneratedData() != null) {
//
//		                booking.getTreatments().getGeneratedData().forEach((treatmentName, treatment) -> {
//		                    if (treatment.getDates() != null && !treatment.getDates().isEmpty()) {
//
//		                        // Sort sittings ascending by date
//		                        treatment.getDates().sort(Comparator.comparing(
//		                                d -> LocalDate.parse(d.getDate(), formatter)
//		                        ));
//
//		                        // ✅ Mark the next pending sitting as "Next Sitting"
//		                        boolean nextMarked = false;
//		                        for (DatesDTO date : treatment.getDates()) {
//		                            if (!nextMarked && "Pending".equalsIgnoreCase(date.getStatus())) {
//		                                date.setFollowupStatus("Next Sitting");
//		                                nextMarked = true;
//		                            } else {
//		                                date.setFollowupStatus(null);
//		                            }
//		                        }
//
//		                        // 🧮 Compute counts
//		                        long completed = treatment.getDates().stream()
//		                                .filter(d -> "Completed".equalsIgnoreCase(d.getStatus()))
//		                                .count();
//
//		                        long pending = treatment.getDates().stream()
//		                                .filter(d -> "Pending".equalsIgnoreCase(d.getStatus()))
//		                                .count();
//
//		                        long confirmed = treatment.getDates().stream()
//		                                .filter(d -> "Confirmed".equalsIgnoreCase(d.getStatus()))
//		                                .count();
//
//		                        treatment.setTakenSittings((int) completed);
//		                        treatment.setPendingSittings((int) pending);
//		                        treatment.setCurrentSitting((int) (completed + confirmed + 1));
//
//		                        // Set overall treatment status
//		                        if (pending == 0 && confirmed == 0) {
//		                            treatment.setStatus("Completed");
//		                        } else if (pending == 0 && confirmed > 0) {
//		                            treatment.setStatus("Confirmed");
//		                        } else if (completed > 0) {
//		                            treatment.setStatus("In-Progress");
//		                        } else {
//		                            treatment.setStatus("Pending");
//		                        }
//		                    }
//		                });
//
//		                // ✅ Aggregate totals at parent level
//		                int totalSittings = booking.getTreatments().getGeneratedData().values().stream()
//		                        .mapToInt(TreatmentDetailsDTO::getTotalSittings).sum();
//
//		                int totalTaken = booking.getTreatments().getGeneratedData().values().stream()
//		                        .mapToInt(TreatmentDetailsDTO::getTakenSittings).sum();
//
//		                int totalPending = booking.getTreatments().getGeneratedData().values().stream()
//		                        .mapToInt(TreatmentDetailsDTO::getPendingSittings).sum();
//
//		                booking.getTreatments().setTotalSittings(totalSittings);
//		                booking.getTreatments().setTakenSittings(totalTaken);
//		                booking.getTreatments().setPendingSittings(totalPending);
//		                booking.getTreatments().setCurrentSitting(totalTaken + 1);
//		            }
//
//		            // Convert booking to base response
//		            BookingResponse baseResponse = new ObjectMapper().convertValue(booking, BookingResponse.class);
//
//		            // Attach prescription PDF if available
//		            DoctorSaveDetailsDTO dto = getPrescriptionpdf(booking.getBookingId());
//		            if (dto != null) {
//		                baseResponse.setPrescriptionPdf(dto.getPrescriptionPdf());
//		            }
//
//		            // 🧩 Create separate responses per treatment
//		            if (booking.getTreatments() != null && booking.getTreatments().getGeneratedData() != null) {
//		                for (Map.Entry<String, TreatmentDetailsDTO> entry : booking.getTreatments().getGeneratedData().entrySet()) {
//		                    String treatmentName = entry.getKey();
//		                    TreatmentDetailsDTO treatment = entry.getValue();
//
//		                    // Clone base booking response
//		                    BookingResponse treatmentResponse = new ObjectMapper().convertValue(baseResponse, BookingResponse.class);
//
//		                    // Attach only this treatment
//		                    TreatmentResponseDTO singleTreatment = new TreatmentResponseDTO();
//		                    singleTreatment.setGeneratedData(Map.of(treatmentName, treatment));
//		                    singleTreatment.setTotalSittings(treatment.getTotalSittings());
//		                    singleTreatment.setTakenSittings(treatment.getTakenSittings());
//		                    singleTreatment.setPendingSittings(treatment.getPendingSittings());
//		                    singleTreatment.setCurrentSitting(treatment.getCurrentSitting());
//
//		                    treatmentResponse.setTreatments(singleTreatment);
//		                    treatmentResponse.setSubServiceName(treatmentName); // Make subServiceName reflect this treatment
//
//		                    bookingResponses.add(treatmentResponse);
//		                }
//		            } else {
//		                // If no treatments
//		                bookingResponses.add(baseResponse);
//		            }
//		        }
//
//		        response = ResponseStructure.buildResponse(
//		                bookingResponses,
//		                "In-Progress appointments found",
//		                HttpStatus.OK,
//		                HttpStatus.OK.value()
//		        );
//
//		        return ResponseEntity.ok(response);
//
//		    } catch (Exception e) {
//		        e.printStackTrace();
//		        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//		                .body(ResponseStructure.buildResponse(
//		                        null,
//		                        "Internal server error: " + e.getMessage(),
//		                        HttpStatus.INTERNAL_SERVER_ERROR,
//		                        500
//		                ));
//		    }
//		}
//
//
//		@Override
//		public ResponseEntity<?> getInProgressAppointmentsByPatientId(String patientId,String clinicId) {
//		    ResponseStructure<List<BookingResponse>> response = new ResponseStructure<>();
//
//		    try {
//		        // Fetch all bookings for this patient
//		        List<Booking> bookings = repository.findByPatientIdAndClinicId(patientId,clinicId);
//
//		        if (bookings == null || bookings.isEmpty()) {
//		            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//		                    .body(ResponseStructure.buildResponse(
//		                            null,
//		                            "No bookings found for this patient",
//		                            HttpStatus.NOT_FOUND,
//		                            404
//		                    ));
//		        }
//
//		        List<BookingResponse> bookingResponses = new ArrayList<>();
//		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//		        for (Booking booking : bookings) {
//		            // Only process In-Progress bookings
//		            if (!"In-Progress".equalsIgnoreCase(booking.getStatus())) {
//		                continue;
//		            }
//
//		            nullifyLargeFields(booking);
//
//		            if (booking.getTreatments() != null && booking.getTreatments().getGeneratedData() != null) {
//
//		                booking.getTreatments().getGeneratedData().forEach((treatmentName, treatment) -> {
//
//		                    if (treatment.getDates() != null && !treatment.getDates().isEmpty()) {
//
//		                        // Sort sittings ascending by date
//		                        treatment.getDates().sort(Comparator.comparing(
//		                                d -> LocalDate.parse(d.getDate(), formatter)
//		                        ));
//
//		                        // Build stream of sittings that are not completed or confirmed
//		                        Stream<DatesDTO> dateStream = treatment.getDates().stream()
//		                                .filter(d -> !"Completed".equalsIgnoreCase(d.getStatus()) &&
//		                                             !"Confirmed".equalsIgnoreCase(d.getStatus()));
//
//		                        // If booking is follow-up, skip sittings on or before the follow-up date
//		                        if ("follow-up".equalsIgnoreCase(booking.getVisitType()) && booking.getFollowupDate() != null) {
//		                            try {
//		                                LocalDate followupDate = LocalDate.parse(booking.getFollowupDate(), formatter);
//		                                dateStream = dateStream.filter(d -> {
//		                                    LocalDate sittingDate = LocalDate.parse(d.getDate(), formatter);
//		                                    return sittingDate.isAfter(followupDate);
//		                                });
//		                            } catch (Exception e) {
//		                                System.err.println("⚠️ Error parsing follow-up date: " + e.getMessage());
//		                            }
//		                        }
//
//		                        // Get the next pending sitting after filtering
//		                        Optional<DatesDTO> nextSittingOpt = dateStream.findFirst();
//
//		                        if (nextSittingOpt.isPresent()) {
//		                            treatment.setDates(Collections.singletonList(nextSittingOpt.get()));
//		                        } else {
//		                            // All sittings completed or confirmed
//		                            treatment.setDates(Collections.emptyList());
//		                            treatment.setStatus("Confirmed");
//		                        }
//		                    }
//
//		                    // 🧮 Update treatment-level summary
//		                    long completed = treatment.getDates().stream()
//		                            .filter(d -> "Completed".equalsIgnoreCase(d.getStatus()))
//		                            .count();
//
//		                    treatment.setTakenSittings((int) completed);
//		                    treatment.setPendingSittings(Math.max(0, treatment.getTotalSittings() - (int) completed));
//		                    treatment.setCurrentSitting((int) (completed + 1));
//
//		                    if (treatment.getPendingSittings() == 0) {
//		                        treatment.setStatus("Confirmed");
//		                    } else if (treatment.getTakenSittings() > 0) {
//		                        treatment.setStatus("In-Progress");
//		                    } else {
//		                        treatment.setStatus("Pending");
//		                    }
//
//		                    // 🧾 Build booking response
//		                    BookingResponse bookingResponse = new ObjectMapper().convertValue(booking, BookingResponse.class);
//
//		                    Map<String, TreatmentDetailsDTO> singleTreatment = new HashMap<>();
//		                    singleTreatment.put(treatmentName, treatment);
//		                    bookingResponse.getTreatments().setGeneratedData(singleTreatment);
//
//		                    bookingResponse.getTreatments().setTotalSittings(treatment.getTotalSittings());
//		                    bookingResponse.getTreatments().setTakenSittings(treatment.getTakenSittings());
//		                    bookingResponse.getTreatments().setPendingSittings(treatment.getPendingSittings());
//		                    bookingResponse.getTreatments().setCurrentSitting(treatment.getCurrentSitting());
//
//		                    // Attach prescription PDF if exists
//		                    DoctorSaveDetailsDTO dto = getPrescriptionpdf(booking.getBookingId());
//		                    if (dto != null) {
//		                        bookingResponse.setPrescriptionPdf(dto.getPrescriptionPdf());
//		                    }
//
//		                    bookingResponses.add(bookingResponse);
//		                });
//		            }
//		        }
//
//		        if (bookingResponses.isEmpty()) {
//		            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//		                    .body(ResponseStructure.buildResponse(
//		                            null,
//		                            "No In-Progress appointments found for this patient",
//		                            HttpStatus.NOT_FOUND,
//		                            404
//		                    ));
//		        }
//
//		        response = ResponseStructure.buildResponse(
//		                bookingResponses,
//		                "In-Progress appointments found",
//		                HttpStatus.OK,
//		                HttpStatus.OK.value()
//		        );
//
//		        return ResponseEntity.ok(response);
//
//		    } catch (Exception e) {
//		        e.printStackTrace();
//		        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//		                .body(ResponseStructure.buildResponse(
//		                        null,
//		                        "Internal server error: " + e.getMessage(),
//		                        HttpStatus.INTERNAL_SERVER_ERROR,
//		                        500
//		                ));
//		    }
//		}
//
//		
//		/**
//		 * ✅ Utility: Parse both yyyy-MM-dd and dd-MM-yyyy formats
//		 */
//		private LocalDate parseDate(String dateStr) {
//		    if (dateStr == null) return null;
//		    List<DateTimeFormatter> formatters = Arrays.asList(
//		            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
//		            DateTimeFormatter.ofPattern("dd-MM-yyyy")
//		    );
//		    for (DateTimeFormatter fmt : formatters) {
//		        try {
//		            return LocalDate.parse(dateStr, fmt);
//		        } catch (Exception ignored) {}
//		    }
//		    return null;
//		}
//
//		
//		public List<BookingResponse> inprogressAppointmentsByConsultationExpiration(LocalDate exp,Booking booking, DoctorSaveDetailsDTO saveDetails ) {
//			List<BookingResponse> finalList = new ArrayList<>();
//			try {
//				LocalDate today = LocalDate.now();
// 		        LocalDate sixthDate = today.plusDays(6);
// 		        DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
// 		     	 if(saveDetails.getFollowUp() != null &&
//	                saveDetails.getFollowUp().getNextFollowUpDate() != null) {
//	                  try{
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
//	                           finalList.add(toResponse(bkng));		                        
//	                    }
//	                } catch (Exception e) {
//	                    System.out.println(e.getMessage());
//	                }
//	            }else {        
//	            // ✅ Consultation expiration fallback
//	            if (booking.getConsultationExpiration() != null) {
//	                    try {
//	                	  int days = 0;
//	                	if(exp == null ){
//	                    days = Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", ""));
//	                   // System.out.println(days);
//	                    LocalDate serviceDate  = LocalDate.parse(booking.getServiceDate(), isoFormatter);
//	                    exp = serviceDate.plusDays(days);}
//	                   // System.out.println(expDate);
//	                    for (int i = 0; i <= 6; i++) {
//	                        LocalDate date = today.plusDays(i);
//	                        //System.out.println(date);
//	                       // System.out.println(sixthDate);
//	                        if ((!date.isAfter(sixthDate)) && (date.isBefore(exp) || date.equals(exp))) {
//	                            //System.out.println("hii");
//	                        	Booking bkng = new Booking(booking);
//	                        	//bkng.setConsentFormPdf(null);
//	                        	//bkng.setAttachments(null);
//	                        	//bkng.setReports(null);
//	                           // System.out.println(bkng);
//	                            bkng.setFollowupDate(date.format(isoFormatter));
//	                            bkng.setStatus("In-Progress");	                            
//	                            finalList.add(toResponse(bkng));
//	                        }}	                   
//	                }catch (Exception e){
//	                	 System.out.println(e.getMessage());
//	               }}}}catch(Exception e) {
//	            	System.out.println(e.getMessage());
//	            	return null;
//	            }
//			return finalList;
//			}
//		
//	
//		public ResponseEntity<?> getDoctorFutureAppointments(String doctorId){
//			ResponseStructure<List<BookingResponse>> res = new ResponseStructure<List<BookingResponse>>();
//			   try{
//				List<Booking> booked=repository.findByDoctorId(doctorId);
//				List<BookingResponse> response=new ArrayList<>();
//				if(booked!=null && !booked.isEmpty()){
//					for(Booking b:booked){
//					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//					LocalDate serviceDate = LocalDate.parse(b.getServiceDate(), formatter);
//					LocalDate currentDate = LocalDate.now();
//					LocalDate plus = currentDate.plusDays(15);
//						if(!serviceDate.isBefore(currentDate) && !serviceDate.isAfter(plus)){
//							response.add(toResponse(b));}}
//					if(response!=null && !response.isEmpty()){
//						res.setStatusCode(200);
//						res.setHttpStatus(HttpStatus.OK);
//						res.setData(response);
//						res.setMessage("appointments found");
//					}else{
//						res.setStatusCode(200);
//						res.setHttpStatus(HttpStatus.OK);
//						res.setData(response);
//						res.setMessage("appointments not found");}}}
//			catch(Exception e){
//				res.setStatusCode(500);
//				res.setMessage(e.getMessage());}
//			return ResponseEntity.status(res.getStatusCode()).body(res);
//		}
//		
//		
//		
//		@Override
//		public List<BookingResponse> bookingByBranchId(String branchId) {
//			List<Booking> bookings = repository.findByBranchId(branchId);
//			List<Booking> reversedBookings = new ArrayList<>();
//			for(int i = bookings.size()-1; i >= 0; i--) {
//				reversedBookings.add(bookings.get(i));
//			}
//			if (bookings == null  || bookings.isEmpty()) {
//				return null;
//			}
//			return toResponses(reversedBookings);
//		}
//		
//		
//		
//		@Override
//		public List<BookingResponse> getBookedServicesByClinicIdWithBranchId(String clinicId, String branchId) {
//		    List<Booking> bookings = repository.findByClinicIdAndBranchId(clinicId, branchId);
//		    List<Booking> reversedBookings = new ArrayList<>();
//		    for (int i = bookings.size() - 1; i >= 0; i--) {
//		        reversedBookings.add(bookings.get(i));
//		    }
//		    if (bookings == null || bookings.isEmpty()) {
//		        return null;
//		    }
//		    return toResponses(reversedBookings);
//		}
//
//		
//		
////		@Scheduled(cron = "0 02 00 * * ?")
////		////@Scheduled(fixedRate = 20000)
////		private void changingStatusFromInprogressToCompletedForSittings() {
////			 try {
////		        List<Booking> bookings = repository.findAll();
////			        for (Booking b : bookings) {
////			            if (b.getStatus().equalsIgnoreCase("In-Progress")) {
////			            	Response res = doctorFeign.getDoctorSaveDetailsByBookingId(b.getBookingId()).getBody();
////			            	//System.out.println(b.getBookingId());
////			            	DoctorSaveDetailsDTO dto = new ObjectMapper().convertValue(res.getData(),DoctorSaveDetailsDTO.class);
////			            	//System.out.println(dto);
////			            	if(dto != null) {
////			            	if(dto.getTreatments() != null) {
////			            	for(Map.Entry<String,TreatmentDetailsDTO> mp : dto.getTreatments().getGeneratedData().entrySet()){
////			            	TreatmentDetailsDTO treatments = mp.getValue();
////			            	//System.out.println(treatments);
////			            	if(treatments != null) {
////			            	 List<DatesDTO> dates =	treatments.getDates();
////			            	 //System.out.println(dates.size());
////			            	 int lastIndex = dates.size()-1;
////			            	 DatesDTO datesDTO = dates.get(lastIndex);
////			            	// System.out.println("last index"+datesDTO );
////			            	 String date = datesDTO.getDate();
////			            	 //System.out.println(date);
////			                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////			                LocalDate lastSitting = LocalDate.parse(date, inputFormatter);
////		                    //System.out.println(lastSitting);	                    
////			                LocalDate todayDate = LocalDate.now(); 
////		                  // System.out.println(todayDate);
////		                   // System.out.println(gap);	               
////			                int expirationDays = Integer.parseInt(Character.toString(b.getConsultationExpiration().charAt(0)) + 
////			            			Character.toString(b.getConsultationExpiration().charAt(1)));
////		                    //System.out.println(expirationDays);
////			                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////			                LocalDate serviceDate = LocalDate.parse(b.getServiceDate(),formatter);
////			                LocalDate plusedTime = serviceDate.plusDays(expirationDays);
////			                if(!lastSitting.isBefore(plusedTime)) {  /// if it is before plustime they its status should be in inprogress
////			                	  if(!lastSitting.isBefore(serviceDate) && lastSitting.isBefore(todayDate)) {
////			                		b.setStatus("Completed");
////			                		//System.out.println("status changed");
////			                		  repository.save(b);
////			                		  try {
////			                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
////					                    n.getData().setStatus("Completed");
////					                    notificationFeign.updateNotification(n);
////			                		  }catch(Exception e) {
////			                			  System.out.println(e.getMessage());
////			                		  }
////			                	}}else{
////			                		if(todayDate.isAfter(plusedTime)) {
////				                		b.setStatus("Completed");
////				                		//System.out.println("status changed");
////				                		  repository.save(b);
////				                		  try {
////					                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
////							                    n.getData().setStatus("Completed");
////							                    notificationFeign.updateNotification(n);
////					                		  }catch(Exception e) {
////					                			  System.out.println(e.getMessage());
////					                		  }}}}}}else{
////				                	DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////						            LocalDate followUpDate = LocalDate.parse(dto.getFollowUp().getNextFollowUpDate(), inputFormatter);
////					                    //System.out.println(followUpDate);
////						             DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////						             LocalDate serviceDate = LocalDate.parse(b.getServiceDate(),formatter);
////						             LocalDate todayDate = LocalDate.now();
////						             if(!followUpDate.isBefore(serviceDate) && followUpDate.isBefore(todayDate)) {
////						                	b.setStatus("Completed");
////						                	//System.out.println("status changed");
////					                		  repository.save(b);
////					                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
////							                    n.getData().setStatus("Completed");
////							                    notificationFeign.updateNotification(n);
////						               }}}}}}catch (Exception e) {
////						            	   System.out.println(e.getMessage());
////						               }}
////		
////		
////		
////		
////		@Scheduled(cron = "0 30 00 * * ?")
////		private void SecondTimeChangingStatusFromInprogressToCompletedForSittings() {
////		    try {
////		        List<Booking> bookings = repository.findAll();
////		        for (Booking b : bookings) {
////		            if (b.getStatus().equalsIgnoreCase("In-Progress")) {
////		            	Response res = doctorFeign.getDoctorSaveDetailsByBookingId(b.getBookingId()).getBody();
////		            	//System.out.println(b.getBookingId());
////		            	DoctorSaveDetailsDTO dto = new ObjectMapper().convertValue(res.getData(),DoctorSaveDetailsDTO.class);
////		            	//System.out.println(dto);
////		            	if(dto != null) {
////		            	if(dto.getTreatments() != null) {
////		            	for(Map.Entry<String,TreatmentDetailsDTO> mp : dto.getTreatments().getGeneratedData().entrySet()){
////		            	TreatmentDetailsDTO treatments = mp.getValue();
////		            	//System.out.println(treatments);
////		            	if(treatments != null) {
////		            	 List<DatesDTO> dates =	treatments.getDates();
////		            	 //System.out.println(dates.size());
////		            	 int lastIndex = dates.size()-1;
////		            	 DatesDTO datesDTO = dates.get(lastIndex);
////		            	// System.out.println("last index"+datesDTO );
////		            	 String date = datesDTO.getDate();
////		            	 //System.out.println(date);
////		                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////		                LocalDate lastSitting = LocalDate.parse(date, inputFormatter);
////	                    //System.out.println(lastSitting);	                    
////		                LocalDate todayDate = LocalDate.now(); 
////	                  // System.out.println(todayDate);
////	                   // System.out.println(gap);	               
////		                int expirationDays = Integer.parseInt(Character.toString(b.getConsultationExpiration().charAt(0)) + 
////		            			Character.toString(b.getConsultationExpiration().charAt(1)));
////	                    //System.out.println(expirationDays);
////		                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////		                LocalDate serviceDate = LocalDate.parse(b.getServiceDate(),formatter);
////		                LocalDate plusedTime = serviceDate.plusDays(expirationDays);
////		                if(!lastSitting.isBefore(plusedTime)) {  /// if it is before plustime they its status should be in inprogress
////		                	  if(!lastSitting.isBefore(serviceDate) && lastSitting.isBefore(todayDate)) {
////		                		b.setStatus("Completed");
////		                		//System.out.println("status changed");
////		                		  repository.save(b);
////		                		  try {
////		                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
////				                    n.getData().setStatus("Completed");
////				                    notificationFeign.updateNotification(n);
////		                		  }catch(Exception e) {
////		                			  System.out.println(e.getMessage());
////		                		  }
////		                	}}else{
////		                		if(todayDate.isAfter(plusedTime)) {
////			                		b.setStatus("Completed");
////			                		//System.out.println("status changed");
////			                		  repository.save(b);
////			                		  try {
////				                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
////						                    n.getData().setStatus("Completed");
////						                    notificationFeign.updateNotification(n);
////				                		  }catch(Exception e) {
////				                			  System.out.println(e.getMessage());
////				                		  }}}}}}else{
////			                	DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////					            LocalDate followUpDate = LocalDate.parse(dto.getFollowUp().getNextFollowUpDate(), inputFormatter);
////				                    //System.out.println(followUpDate);
////					             DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////					             LocalDate serviceDate = LocalDate.parse(b.getServiceDate(),formatter);
////					             LocalDate todayDate = LocalDate.now();
////					             if(!followUpDate.isBefore(serviceDate) && followUpDate.isBefore(todayDate)) {
////					                	b.setStatus("Completed");
////					                	//System.out.println("status changed");
////				                		  repository.save(b);
////				                		  NotificationDTO n = notificationFeign.getNotificationByBookingId(b.getBookingId());
////						                    n.getData().setStatus("Completed");
////						                    notificationFeign.updateNotification(n);
////					               }}}}}}catch (Exception e) {
////					            	   System.out.println(e.getMessage());
////					               }}
////		
//		
////    public ResponseEntity<?> retrieveOneWeekAppointments(String cinicId,String branchId){						
////		ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
////	    List<BookingResponse> finalList = new ArrayList<>();
////	   Response response = new Response();
////	    DoctorSaveDetailsDTO saveDetails = new DoctorSaveDetailsDTO();
////	    try {
////	        List<Booking> booked = repository.findByClinicIdAndBranchId(cinicId, branchId);
////	        if (booked == null || booked.isEmpty()) {
////	            res.setStatusCode(200);
////	            res.setHttpStatus(HttpStatus.OK);
////	            res.setMessage("No bookings found for customer");
////	            res.setData(finalList);
////	            return ResponseEntity.ok(res);
////	        }
////	        LocalDate today = LocalDate.now();
////	        LocalDate sixthDate = today.plusDays(6);
////	        LocalDate exp = null;
////	        DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////	        for (Booking booking : booked) {
////	            if (!"In-Progress".equalsIgnoreCase(booking.getStatus())) {
////	                continue;
////	            }
////	            // ✅ Include any In-Progress booking with serviceDate between today and sixthDate
////	            LocalDate ld = null;
////	            if(booking.getFollowupDate() != null) {
////	            	ld = LocalDate.parse(booking.getFollowupDate(), isoFormatter);
////	            }else {
////	            	if(booking.getServiceDate() != null) {
////		            	ld = LocalDate.parse(booking.getServiceDate(), isoFormatter);
////		            }	
////	            }
////	            if(!ld.isAfter(sixthDate)) {
////	            // Fetch doctor save details
////	            try {
////	                response = doctorFeign.getDoctorSaveDetailsByBookingId(booking.getBookingId()).getBody();
////	               // System.out.println(response);
////	            } catch (Exception e) {
////	                System.out.println(e.getMessage());
////	            }
////
////	            if (response != null && response.getData() != null) {
////	                saveDetails = new ObjectMapper().convertValue(response.getData(), DoctorSaveDetailsDTO.class);
////	               // System.out.println(saveDetails);
////	            } else {}
////	            // ✅ Stream-based Treatments + Sittings + Next Follow-Up
////	            if (saveDetails.getTreatments() != null &&
////	                saveDetails.getTreatments().getGeneratedData() != null &&
////	                !saveDetails.getTreatments().getGeneratedData().isEmpty()) {
////
////	                // Flatten all treatment dates into sorted list
////	                List<LocalDate> allDates = saveDetails.getTreatments().getGeneratedData().values().stream()
////	                        .filter(Objects::nonNull)
////	                        .flatMap(details -> details.getDates() != null
////	                                ? details.getDates().stream()
////	                                    .map(d -> LocalDate.parse(d.getDate(), isoFormatter))
////	                                : Stream.empty())
////	                        .sorted()
////	                        .toList();
////                        //System.out.println(allDates);
////	                // Calculate totalSittings and pendingSittings
////	                int totalSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
////	                        .filter(Objects::nonNull)
////	                        .mapToInt(d -> d.getTotalSittings() != null ? d.getTotalSittings() : 0)
////	                        .sum();
////	               // System.out.println(totalSittingsSum);
////	                int pendingSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
////	                        .filter(Objects::nonNull)
////	                        .mapToInt(d -> d.getSittings() != null ? d.getSittings() : 0)
////	                        .sum();
////	                //System.out.println(pendingSittingsSum);
////	                AtomicInteger takenSittingsCount = new AtomicInteger(0);
////
////	                // Determine next follow-up date according to your rules
////	                Optional<LocalDate> nextFollowupOpt = allDates.stream()
////	                        .map(sittingDate -> {
////	                            boolean sittingBooked = false;
////	                            try {
////	                                Booking resp =
////	                                		repository.findByPatientIdAndFollowupDate(
////	                                				sd.getPatientId(),
////	                                                sittingDate.format(isoFormatter));
////
////	                                if (resp != null) {
////	                                    sittingBooked = true;
////	                                    takenSittingsCount.incrementAndGet();
////	                                }
////	                            } catch (Exception ex) {
////	                                System.err.println("Error checking booking for date "
////	                                        + sittingDate + ": " + ex.getMessage());
////	                            }
////                                if(booking.getFollowupDate() != null) {
////	                            if (LocalDate.parse(booking.getFollowupDate(), isoFormatter).isBefore(today)) {
////	                                // Past date → pick first future date if available
////	                                return allDates.stream()
////	                                        .filter(f -> f.isAfter(today))
////	                                        .findFirst()
////	                                        .orElse(null);
////	                            } else if (LocalDate.parse(booking.getFollowupDate(), isoFormatter).isEqual(today)) {
////	                                if (sittingBooked) {
////	                                    // Today booked → pick first future date if available
////	                                    return allDates.stream()
////	                                            .filter(f -> f.isAfter(today))
////	                                            .findFirst()
////	                                            .orElse(null);
////	                                } else {
////	                                    // Today not booked → keep today
////	                                    return sittingDate;
////	                                }
////	                            } else {
////	                                // Future dates → do not modify follow-up
////	                                return null;
////	                            }
////	                        }else{
////	                        	 return allDates.stream()                                        
////	                                        .findFirst()
////	                                        .orElse(null);
////	                        }})
////	                        .filter(Objects::nonNull)
////	                        .findFirst();
////	                LocalDate lstSitting = allDates.get(allDates.size()-1);
////	                if(lstSitting.equals(LocalDate.now()) || lstSitting.isAfter(LocalDate.now())) {
////	                	exp = lstSitting.plusDays(Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", "")));
////	                	finalList = inprogressAppointmentsByConsultationExpiration(exp, booking,saveDetails); 
////	                }
////	                // Update booking object
////	                booking.setTotalSittings(totalSittingsSum);
////	                booking.setPendingSittings(pendingSittingsSum);
////	                booking.setTakenSittings(takenSittingsCount.get());
////	                booking.setCurrentSitting(pendingSittingsSum);
////	                nextFollowupOpt.ifPresent(d -> booking.setFollowupDate(d.format(isoFormatter)));
////	                finalList.add(toResponse(booking));
////	                //System.out.println(nextFollowupOpt);
////	                //System.out.println(finalList);
////	            }
////	            // ✅ Existing Follow-Up section
////	            else if (saveDetails.getFollowUp() != null &&
////	                saveDetails.getFollowUp().getNextFollowUpDate() != null) {
////	                try {
////	                	 int days = 0;
////		                	if(exp == null ){
////		                    days = Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", ""));
////		                    //System.out.println(days);
////		                    LocalDate serviceDate  = LocalDate.parse(booking.getServiceDate(), isoFormatter);
////		                    exp = serviceDate.plusDays(days);}
////	                    LocalDate followDate = LocalDate.parse(saveDetails.getFollowUp().getNextFollowUpDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
////	                    if (!followDate.isBefore(today) && !followDate.isAfter(sixthDate) && !followDate.isAfter(exp)) {
////	                        Booking bkng = new Booking(booking);
////	                        bkng.setFollowupDate(followDate.format(isoFormatter));
////	                        bkng.setStatus("In-Progress");
////	                            finalList.add(toResponse(bkng));		                        
////	                    }
////	                } catch (Exception e) {
////	                    System.out.println(e.getMessage());
////	                }
////	            }}
////	        }
////	        res.setStatusCode(200);
////	        res.setHttpStatus(HttpStatus.OK);
////	        res.setMessage(finalList.isEmpty()
////	                ? "No In-Progress or Today appointments found"
////	                : "In-Progress appointments found");
////	        res.setData(finalList);
////
////	    } catch (Exception e) {
////	        res.setStatusCode(500);
////	        res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
////	        res.setMessage("Error: " + e.getMessage());
////	    }
////
////	    return ResponseEntity.status(res.getStatusCode()).body(res);
////	}
////
//
////		public ResponseEntity<?> retrieveOneWeekAppointments(String clinicId, String branchId) {
////		    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
////		    List<BookingResponse> finalList = new ArrayList<>();
////		    Response response = new Response();
////		    DoctorSaveDetailsDTO saveDetails = new DoctorSaveDetailsDTO();
////
////		    try {
////		        List<Booking> booked = repository.findByClinicIdAndBranchId(clinicId, branchId);
////		        if (booked == null || booked.isEmpty()) {
////		            res.setStatusCode(200);
////		            res.setHttpStatus(HttpStatus.OK);
////		            res.setMessage("No bookings found for customer");
////		            res.setData(finalList);
////		            return ResponseEntity.ok(res);
////		        }
////
////		        LocalDate today = LocalDate.now();
////		        LocalDate sixthDate = today.plusDays(6);
////		        DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////
////		        for (Booking booking : booked) {
////		            // Include only In-Progress bookings
////		            if (!"In-Progress".equalsIgnoreCase(booking.getStatus())) {
////		                continue;
////		            }
////
////		            // Include both In-Clinic & Online Consultations
////		            if (!"In-Clinic Consultation".equalsIgnoreCase(booking.getConsultationType()) &&
////		                !"Services & Treatments".equalsIgnoreCase(booking.getConsultationType()) &&
////		                !"Online Consultation".equalsIgnoreCase(booking.getConsultationType())) {
////		                continue;
////		            }
////
////		            LocalDate ld = null;
////		            if (booking.getFollowupDate() != null) {
////		                ld = LocalDate.parse(booking.getFollowupDate(), isoFormatter);
////		            } else if (booking.getServiceDate() != null) {
////		                ld = LocalDate.parse(booking.getServiceDate(), isoFormatter);
////		            }
////
////		            if (ld == null || ld.isAfter(sixthDate)) {
////		                continue;
////		            }
////
////		            // fetch doctor details
////		            try {
////		                response = doctorFeign.getDoctorSaveDetailsByBookingId(booking.getBookingId()).getBody();
////		            } catch (Exception e) {
////		                System.out.println("Feign error: " + e.getMessage());
////		            }
////
////		            if (response != null && response.getData() != null) {
////		                saveDetails = new ObjectMapper().convertValue(response.getData(), DoctorSaveDetailsDTO.class);
////		            }
////
////		            // Case 1: Treatment Data
////		            if (saveDetails.getTreatments() != null &&
////		                    saveDetails.getTreatments().getGeneratedData() != null &&
////		                    !saveDetails.getTreatments().getGeneratedData().isEmpty()) {
////
////		                List<LocalDate> allDates = saveDetails.getTreatments().getGeneratedData().values().stream()
////		                        .filter(Objects::nonNull)
////		                        .flatMap(details -> details.getDates() != null
////		                                ? details.getDates().stream()
////		                                .map(d -> LocalDate.parse(d.getDate(), isoFormatter))
////		                                : Stream.empty())
////		                        .sorted()
////		                        .toList();
////
////		                int totalSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
////		                        .filter(Objects::nonNull)
////		                        .mapToInt(d -> d.getTotalSittings() != null ? d.getTotalSittings() : 0)
////		                        .sum();
////
////		                int pendingSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
////		                        .filter(Objects::nonNull)
////		                        .mapToInt(d -> d.getSittings() != null ? d.getSittings() : 0)
////		                        .sum();
////
////		                AtomicInteger takenSittingsCount = new AtomicInteger(0);
////
////		                Optional<LocalDate> nextFollowupOpt = allDates.stream().findFirst();
////
////		                LocalDate lstSitting = allDates.get(allDates.size() - 1);
////		                LocalDate exp = lstSitting.plusDays(Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", "")));
////
////		                booking.setTotalSittings(totalSittingsSum);
////		                booking.setPendingSittings(pendingSittingsSum);
////		                booking.setTakenSittings(takenSittingsCount.get());
////		                booking.setCurrentSitting(pendingSittingsSum);
////		                nextFollowupOpt.ifPresent(d -> booking.setFollowupDate(d.format(isoFormatter)));
////
////		                finalList.add(toResponse(booking));
////		            }
////
////		            // Case 2: FollowUp Data
////		            else if (saveDetails.getFollowUp() != null &&
////		                    saveDetails.getFollowUp().getNextFollowUpDate() != null) {
////
////		                try {
////		                    int days = Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", ""));
////		                    LocalDate exp = LocalDate.parse(booking.getServiceDate(), isoFormatter).plusDays(days);
////
////		                    LocalDate followDate = LocalDate.parse(saveDetails.getFollowUp().getNextFollowUpDate(),
////		                            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
////
////		                    if (!followDate.isBefore(today) && !followDate.isAfter(sixthDate) && !followDate.isAfter(exp)) {
////		                        Booking bkng = new Booking(booking);
////		                        bkng.setFollowupDate(followDate.format(isoFormatter));
////		                        bkng.setStatus("In-Progress");
////		                        finalList.add(toResponse(bkng));
////		                    }
////		                } catch (Exception e) {
////		                    System.out.println("Followup error: " + e.getMessage());
////		                }
////		            }
////
////		            // Case 3: Plain In-Clinic or Online Consultation (No Treatments/Followup)
////		            else {
////		                if (ld != null && !ld.isBefore(today) && !ld.isAfter(sixthDate)) {
////		                    finalList.add(toResponse(booking));
////		                }
////		            }
////		        }
////
////		        res.setStatusCode(200);
////		        res.setHttpStatus(HttpStatus.OK);
////		        res.setMessage(finalList.isEmpty()
////		                ? "No In-Progress or Today appointments found"
////		                : "In-Progress appointments found");
////		        res.setData(finalList);
////
////		    } catch (Exception e) {
////		        res.setStatusCode(500);
////		        res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
////		        res.setMessage("Error: " + e.getMessage());
////		    }
////
////		    return ResponseEntity.status(res.getStatusCode()).body(res);
////		}
//
//
//		@Override
//		public ResponseEntity<?> retrieveOneWeekAppointments(String clinicId, String branchId) {
//		    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();
//		    List<BookingResponse> finalList = new ArrayList<>();
//		    Response response;
//		    DoctorSaveDetailsDTO saveDetails;
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
//		            if (!"In-Progress".equalsIgnoreCase(booking.getStatus())) continue;
//
//		            // Include relevant consultation types
//		            String type = booking.getConsultationType() != null ? booking.getConsultationType().trim() : "";
//		            if (!type.equalsIgnoreCase("In-Clinic Consultation") &&
//		                !type.equalsIgnoreCase("Services & Treatments") &&
//		                !type.equalsIgnoreCase("Online Consultation")) {
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
//		            if (ld == null || ld.isAfter(sixthDate)) continue;
//
//		            // fetch doctor details
//		            saveDetails = new DoctorSaveDetailsDTO();
//		            try {
//		                response = doctorFeign.getDoctorSaveDetailsByBookingId(booking.getBookingId()).getBody();
//		                if (response != null && response.getData() != null) {
//		                    saveDetails = new ObjectMapper().convertValue(response.getData(), DoctorSaveDetailsDTO.class);
//		                }
//		            } catch (Exception e) {
//		                System.out.println("Feign error: " + e.getMessage());
//		            }
//
//		            boolean bookingAdded = false;
//
//		            // Case 1: Treatment Data
//		            if (saveDetails.getTreatments() != null &&
//		                saveDetails.getTreatments().getGeneratedData() != null &&
//		                !saveDetails.getTreatments().getGeneratedData().isEmpty()) {
//
//		                List<LocalDate> allDates = saveDetails.getTreatments().getGeneratedData().values().stream()
//		                        .filter(Objects::nonNull)
//		                        .flatMap(details -> details.getDates() != null
//		                                ? details.getDates().stream().map(d -> LocalDate.parse(d.getDate(), isoFormatter))
//		                                : Stream.empty())
//		                        .sorted()
//		                        .toList();
//
//		                int totalSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
//		                        .mapToInt(d -> d.getTotalSittings() != null ? d.getTotalSittings() : 0)
//		                        .sum();
//
//		                int pendingSittingsSum = saveDetails.getTreatments().getGeneratedData().values().stream()
//		                        .mapToInt(d -> d.getSittings() != null ? d.getSittings() : 0)
//		                        .sum();
//
//		                AtomicInteger takenSittingsCount = new AtomicInteger(0);
//
//		                LocalDate lastSitting = allDates.get(allDates.size() - 1);
//		                LocalDate exp = lastSitting.plusDays(Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", "")));
//
//		                booking.setTotalSittings(totalSittingsSum);
//		                booking.setPendingSittings(pendingSittingsSum);
//		                booking.setTakenSittings(takenSittingsCount.get());
//		                booking.setCurrentSitting(pendingSittingsSum);
//		                booking.setFollowupDate(allDates.get(0).format(isoFormatter));
//
//		                if (!exp.isBefore(today)) {
//		                    finalList.add(toResponse(booking));
//		                    bookingAdded = true;
//		                }
//		            }
//
//		            // Case 2: FollowUp Data
//		            if (!bookingAdded && saveDetails.getFollowUp() != null &&
//		                saveDetails.getFollowUp().getNextFollowUpDate() != null) {
//		                try {
//		                    LocalDate followDate = LocalDate.parse(
//		                            saveDetails.getFollowUp().getNextFollowUpDate(),
//		                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
//		                    );
//
//		                    LocalDate exp = LocalDate.parse(booking.getServiceDate(), isoFormatter)
//		                            .plusDays(Integer.parseInt(booking.getConsultationExpiration().replaceAll("\\D+", "")));
//
//		                    if (!followDate.isBefore(today) && !followDate.isAfter(sixthDate) && !followDate.isAfter(exp)) {
//		                        Booking bkng = new Booking(booking);
//		                        bkng.setFollowupDate(followDate.format(isoFormatter));
//		                        bkng.setStatus("In-Progress");
//		                        finalList.add(toResponse(bkng));
//		                        bookingAdded = true;
//		                    }
//		                } catch (Exception e) {
//		                    System.out.println("Followup error: " + e.getMessage());
//		                }
//		            }
//
//		            // Case 3: Plain In-Clinic or Online Consultation (No Treatments/Followup)
//		            if (!bookingAdded) {
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
//
//		
//		public ResponseEntity<?> retrieveAppointments(String cinicId,String branchId,String date){			
//			ResponseStructure< List<BookingResponse>> res = new ResponseStructure< List<BookingResponse>>();		  
//			try {	
//				 List<Booking> bookings = repository.findByClinicIdAndBranchIdAndServiceDateOrderByServicetimeAsc(cinicId, branchId, date);
//				// System.out.println(todayBookings);
//				 bookings = bookings.stream().filter(n->n.getStatus().equalsIgnoreCase("In-Progress")).toList();
//				 List<BookingResponse> todayBookingsDto = toResponses(bookings);
//				 if(todayBookingsDto!= null && !todayBookingsDto.isEmpty()) {
//				 res.setStatusCode(200);
//					res.setHttpStatus(HttpStatus.OK);
//					res.setData(todayBookingsDto);
//					res.setMessage("appointments found");
//				 }else {
//					 res.setStatusCode(404);
//						res.setHttpStatus(HttpStatus.NOT_FOUND);
//						res.setMessage("appointments Not found with date");}
//			}catch(Exception e) {
//				res.setStatusCode(500);
//				res.setMessage(e.getMessage());
//			}			
//			return ResponseEntity.status(res.getStatusCode()).body(res);	
//}
//		
//		
//		public ResponseEntity<?> updateAppointmentBasedOnBookingId(BookingResponse dto){
//			try {
//				//System.out.println(dto);
//				Booking entity = repository.findByBookingId(dto.getBookingId())
//						.orElseThrow(() -> new RuntimeException("Invalid Booking Id Please provide Valid Id"));
//                //System.out.println(entity);
//			    if (dto.getBookingId() != null && !dto.getBookingId().isEmpty()) {
//			        entity.setBookingId(dto.getBookingId());
//			    }
//
//			    if (dto.getBookingFor() != null && !dto.getBookingFor().isEmpty()) {
//			        entity.setBookingFor(dto.getBookingFor());
//			    }
//
//			    if (dto.getRelation() != null && !dto.getRelation().isEmpty()) {
//			        entity.setRelation(dto.getRelation());
//			    }
//
//			    if (dto.getPatientMobileNumber() != null && !dto.getPatientMobileNumber().isEmpty()) {
//			        entity.setPatientMobileNumber(dto.getPatientMobileNumber());
//			    }
//
//			    if (dto.getPatientAddress() != null && !dto.getPatientAddress().isEmpty()) {
//			        entity.setPatientAddress(dto.getPatientAddress());
//			    }
//
//			    if (dto.getPatientId() != null && !dto.getPatientId().isEmpty()) {
//			        entity.setPatientId(dto.getPatientId());
//			    }
//
//			    if (dto.getFreeFollowUpsLeft() != null) {
//			        entity.setFreeFollowUpsLeft(dto.getFreeFollowUpsLeft());
//			    }
//
//			    if (dto.getFreeFollowUps() != null) {
//			        entity.setFreeFollowUps(dto.getFreeFollowUps());
//			    }
//
//			    if (dto.getVisitType() != null && !dto.getVisitType().isEmpty()) {
//			        entity.setVisitType(dto.getVisitType());
//			    }
//
//			    if (dto.getName() != null && !dto.getName().isEmpty()) {
//			        entity.setName(dto.getName());
//			    }
//
//			    if (dto.getAge() != null && !dto.getAge().isEmpty()) {
//			        entity.setAge(dto.getAge());
//			    }
//
//			    if (dto.getGender() != null && !dto.getGender().isEmpty()) {
//			        entity.setGender(dto.getGender());
//			    }
//
//			    if (dto.getMobileNumber() != null && !dto.getMobileNumber().isEmpty()) {
//			        entity.setMobileNumber(dto.getMobileNumber());
//			    }
//
//			    if (dto.getCustomerId() != null && !dto.getCustomerId().isEmpty()) {
//			        entity.setCustomerId(dto.getCustomerId());
//			    }
//
//			    if (dto.getCustomerDeviceId() != null && !dto.getCustomerDeviceId().isEmpty()) {
//			        entity.setCustomerDeviceId(dto.getCustomerDeviceId());
//			    }
//
//			    if (dto.getProblem() != null && !dto.getProblem().isEmpty()) {
//			        entity.setProblem(dto.getProblem());
//			    }
//
//			    if (dto.getSymptomsDuration() != null && !dto.getSymptomsDuration().isEmpty()) {
//			        entity.setSymptomsDuration(dto.getSymptomsDuration());
//			    }
//
//			    if (dto.getClinicId() != null && !dto.getClinicId().isEmpty()) {
//			        entity.setClinicId(dto.getClinicId());
//			    }
//
//			    if (dto.getClinicName() != null && !dto.getClinicName().isEmpty()) {
//			        entity.setClinicName(dto.getClinicName());
//			    }
//
//			    if (dto.getBranchId() != null && !dto.getBranchId().isEmpty()) {
//			        entity.setBranchId(dto.getBranchId());
//			    }
//
//			    if (dto.getBranchname() != null && !dto.getBranchname().isEmpty()) {
//			        entity.setBranchname(dto.getBranchname());
//			    }
//
//			    if (dto.getClinicDeviceId() != null && !dto.getClinicDeviceId().isEmpty()) {
//			        entity.setClinicDeviceId(dto.getClinicDeviceId());
//			    }
//
//			    if (dto.getDoctorId() != null && !dto.getDoctorId().isEmpty()) {
//			        entity.setDoctorId(dto.getDoctorId());
//			    }
//
//			    if (dto.getDoctorName() != null && !dto.getDoctorName().isEmpty()) {
//			        entity.setDoctorName(dto.getDoctorName());
//			    }
//
//			    // Mapping: doctorMobileDeviceId → doctorDeviceId (different field names)
//			    if (dto.getDoctorMobileDeviceId() != null && !dto.getDoctorMobileDeviceId().isEmpty()) {
//			        entity.setDoctorDeviceId(dto.getDoctorMobileDeviceId());
//			    }
//
//			    if (dto.getDoctorWebDeviceId() != null && !dto.getDoctorWebDeviceId().isEmpty()) {
//			        entity.setDoctorWebDeviceId(dto.getDoctorWebDeviceId());
//			    }
//
//			    if (dto.getSubServiceId() != null && !dto.getSubServiceId().isEmpty()) {
//			        entity.setSubServiceId(dto.getSubServiceId());
//			    }
//
//			    if (dto.getSubServiceName() != null && !dto.getSubServiceName().isEmpty()) {
//			        entity.setSubServiceName(dto.getSubServiceName());
//			    }
//
//			    if (dto.getServiceDate() != null && !dto.getServiceDate().isEmpty()) {
//			        entity.setServiceDate(dto.getServiceDate());
//			    }
//
//			    if (dto.getServicetime() != null && !dto.getServicetime().isEmpty()) {
//			        entity.setServicetime(dto.getServicetime());
//			    }
//
//			    if (dto.getConsultationType() != null && !dto.getConsultationType().isEmpty()) {
//			        entity.setConsultationType(dto.getConsultationType());
//			    }
//
//			    if (dto.getConsultationFee() > 0) {
//			        entity.setConsultationFee(dto.getConsultationFee());
//			    }
//
//			    if (dto.getReasonForCancel() != null && !dto.getReasonForCancel().isEmpty()) {
//			        entity.setReasonForCancel(dto.getReasonForCancel());
//			    }
//
//			    if (dto.getNotes() != null && !dto.getNotes().isEmpty()) {
//			        entity.setNotes(dto.getNotes());
//			    }
//
//			    // reports DTO list → entity list (need manual conversion if types differ)
//			    if (dto.getReports() != null && !dto.getReports().isEmpty()) {
//			    	List<ReportsList> lst = new ObjectMapper().convertValue(dto.getReports(),new TypeReference<List<ReportsList>>(){});
//			    	entity.setReports(lst);
//			    }
//
//			    if (dto.getChannelId() != null && !dto.getChannelId().isEmpty()) {
//			        entity.setChannelId(dto.getChannelId());
//			    }
//
//			    if (dto.getBookedAt() != null && !dto.getBookedAt().isEmpty()) {
//			        entity.setBookedAt(dto.getBookedAt());
//			    }
//				if(dto.getFollowupStatus() != null && dto.getFollowupStatus().equalsIgnoreCase("no-followup")) {
//			        entity.setStatus("Completed");
//			    }else {
//			    	try {
//			    		//System.out.println(dto);
//			    		if(entity.getTreatments().getGeneratedData().containsKey(dto.getTreatmentName())) {
//			    		TreatmentDetailsDTO	treatmentDetailsDTO = entity.getTreatments().getGeneratedData().get(dto.getTreatmentName());
//			    		//System.out.println(treatmentDetailsDTO);
//			    		if(treatmentDetailsDTO != null) {
//			    			for(DatesDTO datesDTO : treatmentDetailsDTO.getDates()) {
//			    				//System.out.println(datesDTO);
//			    				if(datesDTO.getDate().equals(dto.getTreatmentDate())){
//			    					datesDTO.setFollowupStatus(dto.getFollowupStatus());}
//			    				TreatmentResponseDTO treatmentResponseDTO = entity.getTreatments();
//			    				//System.out.println(treatmentResponseDTO);
//			    					entity.setTreatments(treatmentResponseDTO);		
//			    			}}}}catch(Exception e) {System.out.println(e.getMessage());}}
//			    if (dto.getVisitCount() != null) {
//			        entity.setVisitCount(dto.getVisitCount());
//			    }
//			    if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
//			    	List<byte[]> bte = new ObjectMapper().convertValue(dto.getAttachments(), new TypeReference<List<byte[]>>(){});
//			    	entity.setAttachments(bte);
//			    }
//			    if (dto.getConsentFormPdf() != null && !dto.getConsentFormPdf().isEmpty()) {
//			        entity.setConsentFormPdf(Base64.getDecoder().decode(dto.getConsentFormPdf()));
//			    }
//
//			    if (dto.getPrescriptionPdf() != null && !dto.getPrescriptionPdf().isEmpty()) {
//			    	List<byte[]> bte = new ObjectMapper().convertValue(dto.getPrescriptionPdf(), new TypeReference<List<byte[]>>(){});
//			    	entity.setPrescriptionPdf(bte);
//			    }
//
//			    if (dto.getTotalFee() > 0) {
//			        entity.setTotalFee(dto.getTotalFee());
//			    }
//
//			    if (dto.getDoctorRefCode() != null && !dto.getDoctorRefCode().isEmpty()) {
//			        entity.setDoctorRefCode(dto.getDoctorRefCode());
//			    }
//
//			    if (dto.getConsultationExpiration() != null && !dto.getConsultationExpiration().isEmpty()) {
//			        entity.setConsultationExpiration(dto.getConsultationExpiration());
//			    }
//			    
////			    if (dto.getFollowupStatus() != null) {
////			        entity.setFollowupStatus(dto.getFollowupStatus());
////			    }
//			    Booking e = repository.save(entity);			
//				if(e != null){	
//				return new ResponseEntity<>(ResponseStructure.buildResponse(e,
//						"Booking updated sucessfully",HttpStatus.OK, HttpStatus.OK.value()),
//						HttpStatus.OK);			
//				}else {
//					return new ResponseEntity<>(ResponseStructure.buildResponse(null,
//							"Booking Not Updated", HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value()),
//							HttpStatus.NOT_FOUND);
//				}}catch(Exception e) {
//					return new ResponseEntity<>(ResponseStructure.buildResponse(null,
//							e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value()),
//							HttpStatus.INTERNAL_SERVER_ERROR);
//				}}
//			
//		
//		public ResponseEntity<?> getRelationsByCustomerId(String customerId) {
//		    ResponseStructure<Map<String, List<RelationInfoDTO>>> res = new ResponseStructure<>();
//
//		    try {
//		        List<Booking> bookings = repository.findByCustomerId(customerId);
//
//		        Map<String, List<RelationInfoDTO>> data = bookings.stream()
//		                .collect(Collectors.groupingBy(
//		                        Booking::getRelation,
//		                        LinkedHashMap::new,
//		                        Collectors.collectingAndThen(
//		                                Collectors.mapping(n -> {
//		                                    RelationInfoDTO dto = new RelationInfoDTO();
//		                                    dto.setAddress(n.getPatientAddress());
//		                                    dto.setAge(n.getAge());
//		                                    dto.setFullname(n.getName());
//		                                    dto.setMobileNumber(n.getMobileNumber());
//		                                    dto.setRelation(n.getRelation());
//		                                    dto.setGender(n.getGender());
//		                                    dto.setCustomerId(n.getCustomerId());
//		                                    dto.setPatientId(n.getPatientId());
//		                                    return dto;
//		                                }, Collectors.toList()),
//		                                list -> list.stream().distinct().collect(Collectors.toList()) // remove duplicates
//		                        )));
//		        res.setStatusCode(200);
//		        res.setHttpStatus(HttpStatus.OK);
//		        res.setData(data);
//		        res.setMessage("Relations found successfully");
//		    } catch (Exception e) {
//		        res.setStatusCode(500);
//		        res.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
//		        res.setMessage("Error: " + e.getMessage());
//		    }
//
//		    return ResponseEntity.status(res.getStatusCode()).body(res);
//		}
//		
//		
//		@Override
//		public BookingResponse checkBookingByDateAndTime(String date,String time,String doctorId) {
//			Booking booking = repository.findByServiceDateAndServicetimeAndDoctorId(date, time, doctorId);
//			if(booking != null) {
//			return toResponse(booking);
//			}else {
//				return null;
//			}
//			
//		}
//
//
//}
