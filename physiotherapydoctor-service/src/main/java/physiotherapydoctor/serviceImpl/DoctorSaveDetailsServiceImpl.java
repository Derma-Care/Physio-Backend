package physiotherapydoctor.serviceImpl;


import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import feign.FeignException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.DatesDTO;
import physiotherapydoctor.dto.DoctorSaveDetailsDTO;
import physiotherapydoctor.dto.FollowUpDetailsDTO;
import physiotherapydoctor.dto.MedicinesDTO;
import physiotherapydoctor.dto.PrescriptionDetailsDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.dto.SymptomDetailsDTO;
import physiotherapydoctor.dto.TestDetailsDTO;
import physiotherapydoctor.dto.TreatmentDetailsDTO;
import physiotherapydoctor.dto.TreatmentResponseDTO;
import physiotherapydoctor.entity.Dates;
import physiotherapydoctor.entity.DoctorSaveDetails;
import physiotherapydoctor.entity.FollowUpDetails;
import physiotherapydoctor.entity.Medicines;
import physiotherapydoctor.entity.PrescriptionDetails;
import physiotherapydoctor.entity.SymptomDetails;
import physiotherapydoctor.entity.TestDetails;
import physiotherapydoctor.entity.TreatmentDetails;
import physiotherapydoctor.entity.TreatmentResponse;
import physiotherapydoctor.repository.DoctorSaveDetailsRepository;
import physiotherapydoctor.service.DoctorSaveDetailsService;
import physiotherapydoctor.service.S3Service;
import physiotherapydoctor.util.AdminFeignImpl;
import physiotherapydoctor.util.BookingFeignImpl;
import physiotherapydoctor.util.ClinicAdminFeignImpl;
import physiotherapydoctor.util.VisitTypeUtil;

@Service
@Slf4j
public class DoctorSaveDetailsServiceImpl implements DoctorSaveDetailsService {

	@Autowired
	private DoctorSaveDetailsRepository repository;

	@Autowired
	private ClinicAdminFeignImpl clinicAdminServiceClient;

	@Autowired
	private BookingFeignImpl bookingFeignClient;
    
    @Autowired
    private S3Service s3Service;
    
    @Autowired
	private AdminFeignImpl adminFeignClient;

	@Autowired
	private ObjectMapper objectMapper;

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "saveDoctorDetailsFallback")
    @Secured("ROLE_DOCTOR")
    public Response saveDoctorDetails(DoctorSaveDetailsDTO dto) {

        long startTime = System.currentTimeMillis();

        log.info("Entered saveDoctorDetails() with bookingId : {}, doctorId : {}, patientId : {}",
                dto.getBookingId(),
                dto.getDoctorId(),
                dto.getPatientId());

        try {

            // ----------------------- Step 0: Validate Booking ID -----------------------
            log.debug("Validating bookingId");

            if (dto.getBookingId() == null || dto.getBookingId().isBlank()) {

                log.warn("Booking ID is null or empty");

                return buildResponse(
                        false,
                        null,
                        "Booking ID must not be null or empty",
                        HttpStatus.BAD_REQUEST.value());
            }

            // ----------------------- Step 1: Fetch Booking -----------------------
            log.info("Fetching booking details for bookingId : {}", dto.getBookingId());

            ResponseEntity<ResponseStructure<BookingResponse>> bookingEntity =
                    bookingFeignClient.getBookedService(dto.getBookingId());

            if (bookingEntity == null || bookingEntity.getBody() == null) {

                log.error("Booking service returned null response for bookingId : {}",
                        dto.getBookingId());

                return buildResponse(
                        false,
                        null,
                        "Unable to fetch booking details. Booking service returned null.",
                        HttpStatus.BAD_GATEWAY.value());
            }

            BookingResponse bookingData = bookingEntity.getBody().getData();

            if (bookingData == null) {

                log.warn("Booking not found with bookingId : {}", dto.getBookingId());

                return buildResponse(
                        false,
                        null,
                        "Booking not found with ID: " + dto.getBookingId(),
                        HttpStatus.NOT_FOUND.value());
            }

            log.info("Successfully fetched booking details for bookingId : {}",
                    dto.getBookingId());

            // ----------------------- Step 2: Fetch Doctor -----------------------
            log.info("Fetching doctor details for doctorId : {}", dto.getDoctorId());

            Response doctorResponse =
                    clinicAdminServiceClient.getDoctorById(dto.getDoctorId()).getBody();

            if (doctorResponse == null
                    || !doctorResponse.isSuccess()
                    || doctorResponse.getData() == null) {

                log.warn("Doctor not found with doctorId : {}", dto.getDoctorId());

                return buildResponse(
                        false,
                        null,
                        "Doctor not found with ID: " + dto.getDoctorId(),
                        HttpStatus.NOT_FOUND.value());
            }

            Map<String, Object> doctorData =
                    objectMapper.convertValue(doctorResponse.getData(), Map.class);

            dto.setDoctorName((String) doctorData.get("doctorName"));

            log.info("Doctor fetched successfully. Doctor Name : {}",
                    dto.getDoctorName());

            // ----------------------- Step 3: Setup Clinic Info -----------------------
            dto.setClinicId(Optional.ofNullable(dto.getClinicId()).orElse(""));
            dto.setClinicName(Optional.ofNullable(dto.getClinicName()).orElse(""));

            log.debug("Clinic information set. ClinicId : {}, ClinicName : {}",
                    dto.getClinicId(),
                    dto.getClinicName());

            // ----------------------- Step 4: Calculate Visit Count & Type -----------------------
            log.info("Calculating visit count for doctorId : {}, patientId : {}, subServiceId : {}",
                    dto.getDoctorId(),
                    dto.getPatientId(),
                    dto.getSubServiceId());

            List<DoctorSaveDetails> previousVisits =
                    repository.findByDoctorIdAndPatientIdAndSubServiceId(
                            dto.getDoctorId(),
                            dto.getPatientId(),
                            dto.getSubServiceId());

            int visitCount =
                    (previousVisits != null && !previousVisits.isEmpty())
                            ? previousVisits.size() + 1
                            : 1;

            dto.setVisitCount(visitCount);
            dto.setVisitType(VisitTypeUtil.getVisitTypeFromCount(visitCount));
            dto.setVisitDateTime(LocalDateTime.now());

            log.info("Visit count calculated : {}, Visit type : {}",
                    visitCount,
                    dto.getVisitType());

            // ----------------------- Step 5: Save Visit -----------------------
            log.info("Saving doctor visit details");

            DoctorSaveDetails entity = convertToEntity(dto);
            entity.setVisitCount(visitCount);

            DoctorSaveDetails savedVisit = repository.save(entity);

            log.info("Doctor visit details saved successfully with id : {}",
                    savedVisit.getId());

            // ----------------------- Step 6: Fetch Clinic -----------------------
            log.info("Fetching clinic details for clinicId : {}",
                    dto.getClinicId());

            Response clinicResponse =
                    adminFeignClient.getClinicById(dto.getClinicId()).getBody();

            int expirationDays = 0;
            String consultationExpirationStr = "";

            if (clinicResponse != null
                    && clinicResponse.isSuccess()
                    && clinicResponse.getData() != null) {

                Map<String, Object> clinicData =
                        objectMapper.convertValue(clinicResponse.getData(), Map.class);

                if (clinicData.containsKey("consultationExpiration")
                        && clinicData.get("consultationExpiration") != null) {

                    consultationExpirationStr =
                            clinicData.get("consultationExpiration").toString();

                    expirationDays =
                            parseExpirationDays(consultationExpirationStr);

                    log.info("Consultation expiration configured as : {} days",
                            expirationDays);
                }
            } else {

                log.warn("Unable to fetch clinic consultation expiration details for clinicId : {}",
                        dto.getClinicId());
            }

            // ----------------------- Step 11: Update Booking Service -----------------------
            log.info("Updating booking status for bookingId : {}",
                    dto.getBookingId());

            if (bookingData.getFreeFollowUpsLeft() != 0) {

                bookingData.setFreeFollowUpsLeft(
                        bookingData.getFreeFollowUpsLeft() - 1);

                log.info("Free follow-ups remaining : {}",
                        bookingData.getFreeFollowUpsLeft());

                if (bookingData.getFreeFollowUpsLeft() == 0) {

                    bookingData.setStatus("completed");

                    log.info("No free follow-ups remaining. Status set to completed");

                } else {

                    bookingData.setStatus("In-Progress");

                    log.info("Booking status set to In-Progress");
                }

            } else {

                bookingData.setStatus("completed");

                log.info("Booking status set to completed");
            }

            bookingFeignClient.updateAppointmentBasedOnBookingId(bookingData);

            log.info("Booking service updated successfully for bookingId : {}",
                    dto.getBookingId());

            // ----------------------- Step 12: Build Response -----------------------
            DoctorSaveDetailsDTO savedDto = convertToDto(savedVisit);

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("saveDoctorDetails() completed successfully in {} ms",
                    executionTime);

            return buildResponse(
                    true,
                    savedDto,
                    "Doctor details saved successfully",
                    HttpStatus.CREATED.value());

        } catch (FeignException e) {

            log.error("Feign exception occurred while communicating with external services. Error : {}",
                    e.getMessage(),
                    e);

            return buildResponse(
                    false,
                    null,
                    "Error fetching doctor/booking/clinic details: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY.value());

        } catch (Exception e) {

            log.error("Unexpected exception occurred in saveDoctorDetails(). Error : {}",
                    e.getMessage(),
                    e);

            return buildResponse(
                    false,
                    null,
                    "Unexpected error: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    /** Utility to parse "7 days" -> 7 */
    private int parseExpirationDays(String expirationStr) {
        if (expirationStr == null || expirationStr.isBlank()) return 0;
        expirationStr = expirationStr.toLowerCase().trim();
        try {
            if (expirationStr.contains("day")) {
                return Integer.parseInt(expirationStr.replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return 0;
    }

    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorDetailsByIdFallback")
    @Secured("ROLE_DOCTOR")
    public Response getDoctorDetailsById(String id) {

        long startTime = System.currentTimeMillis();

        log.info("Entered getDoctorDetailsById() with id : {}", id);

        try {

            log.debug("Fetching doctor details from repository for id : {}", id);

            Optional<DoctorSaveDetails> optional = repository.findById(id);

            if (optional.isPresent()) {

                log.info("Doctor details found for id : {}", id);

                DoctorSaveDetailsDTO dto =
                        objectMapper.convertValue(optional.get(), DoctorSaveDetailsDTO.class);

                long executionTime = System.currentTimeMillis() - startTime;

                log.info("getDoctorDetailsById() completed successfully in {} ms",
                        executionTime);

                return buildResponse(
                        true,
                        dto,
                        "Doctor details found",
                        HttpStatus.OK.value());
            }

            log.warn("Doctor details not found for id : {}", id);

            return buildResponse(
                    false,
                    null,
                    "Doctor details not found",
                    HttpStatus.NOT_FOUND.value());

        } catch (Exception e) {

            log.error("Exception occurred while fetching doctor details for id : {}. Error : {}",
                    id,
                    e.getMessage(),
                    e);

            return buildResponse(
                    false,
                    null,
                    "Failed to fetch doctor details : " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateDoctorDetailsFallback")
    @Secured("ROLE_DOCTOR")
    public Response updateDoctorDetails(String id, DoctorSaveDetailsDTO dto) {

        long startTime = System.currentTimeMillis();

        log.info("Entered updateDoctorDetails() with id : {}", id);

        try {

            log.debug("Fetching existing doctor details for id : {}", id);

            Optional<DoctorSaveDetails> optional = repository.findById(id);

            if (optional.isEmpty()) {

                log.warn("Doctor details not found for id : {}", id);

                return buildResponse(
                        false,
                        null,
                        "Doctor details not found",
                        HttpStatus.NOT_FOUND.value());
            }

            DoctorSaveDetails existing = optional.get();

            log.info("Doctor details found. Updating record for id : {}", id);

            // Map DTO -> Entity
            DoctorSaveDetails updated =
                    objectMapper.convertValue(dto, DoctorSaveDetails.class);

            // Preserve existing DB id
            updated.setId(existing.getId());

            log.debug("Preserved existing entity id : {}", existing.getId());

            // Preserve prescription if not provided
            if (updated.getPrescription() == null) {

                log.debug("Prescription not provided in request. Preserving existing prescription.");

                updated.setPrescription(existing.getPrescription());
            }

            // Ensure medicine IDs exist
            log.debug("Fixing medicine IDs before save");

            fixMedicineIds(updated, existing);

            log.debug("Saving updated doctor details for id : {}", id);

            DoctorSaveDetails saved = repository.save(updated);

            log.info("Doctor details updated successfully for id : {}", saved.getId());

            DoctorSaveDetailsDTO savedDto = convertToDto(saved);

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("updateDoctorDetails() completed successfully in {} ms",
                    executionTime);

            return buildResponse(
                    true,
                    savedDto,
                    "Doctor details updated successfully",
                    HttpStatus.OK.value());

        } catch (Exception e) {

            log.error("Exception occurred while updating doctor details for id : {}. Error : {}",
                    id,
                    e.getMessage(),
                    e);

            return buildResponse(
                    false,
                    null,
                    "Failed to update doctor details : " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    
    /**
     * Copies old medicine IDs where possible and generates new IDs for any medicines with null id.
     */
    private void fixMedicineIds(DoctorSaveDetails updated, DoctorSaveDetails existing) {
		if (updated.getPrescription() == null || updated.getPrescription().getMedicines() == null) {
			return;
		}

		List<Medicines> updatedMeds = updated.getPrescription().getMedicines();

		List<Medicines> existingMeds = null;
		if (existing.getPrescription() != null) {
			existingMeds = existing.getPrescription().getMedicines();
		}

		// 1) Try to reuse IDs from existing list (by index)
		if (existingMeds != null && !existingMeds.isEmpty()) {
			int size = Math.min(existingMeds.size(), updatedMeds.size());
			for (int i = 0; i < size; i++) {
				Medicines u = updatedMeds.get(i);
				Medicines e = existingMeds.get(i);

				if (u.getId() == null && e.getId() != null) {
					u.setId(e.getId());
				}
			}
		}

		// 2) For any remaining null IDs, generate new UUIDs
		for (Medicines med : updatedMeds) {
			if (med.getId() == null) {
				med.setId(UUID.randomUUID());
			}
		}
	}

	
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateDoctorDetailsByBookingIdFallback")
    @Secured("ROLE_DOCTOR")
    public Response updateDoctorDetailsByBookingId(String id, DoctorSaveDetailsDTO dto) {

        long startTime = System.currentTimeMillis();

        log.info("Entered updateDoctorDetailsByBookingId() with bookingId : {}", id);

        try {

            log.debug("Fetching doctor details using bookingId : {}", id);

            DoctorSaveDetails existing = repository.findByBookingId(id);

            if (existing != null) {

                log.info("Doctor details found for bookingId : {}", id);

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                DoctorSaveDetails updated =
                        mapper.convertValue(dto, DoctorSaveDetails.class);

                updated.setId(existing.getId());

                log.debug("Saving updated doctor details for bookingId : {}", id);

                DoctorSaveDetails saved = repository.save(updated);

                log.info("Doctor details updated successfully. Record Id : {}",
                        saved.getId());

                DoctorSaveDetailsDTO savedDto = convertToDto(saved);

                long executionTime = System.currentTimeMillis() - startTime;

                log.info("updateDoctorDetailsByBookingId() completed successfully in {} ms",
                        executionTime);

                return buildResponse(
                        true,
                        savedDto,
                        "Doctor details updated successfully",
                        HttpStatus.OK.value());
            }

            log.warn("Doctor details not found for bookingId : {}", id);

            return buildResponse(
                    false,
                    null,
                    "Doctor details not found",
                    HttpStatus.NOT_FOUND.value());

        } catch (Exception e) {

            log.error("Exception occurred while updating doctor details using bookingId : {}. Error : {}",
                    id,
                    e.getMessage(),
                    e);

            return buildResponse(
                    false,
                    null,
                    "Failed to update doctor details : " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "deleteDoctorDetailsFallback")
    @Secured("ROLE_DOCTOR")
    public Response deleteDoctorDetails(String id) {

        long startTime = System.currentTimeMillis();

        log.info("Entered deleteDoctorDetails() with id : {}", id);

        try {

            Optional<DoctorSaveDetails> optional = repository.findById(id);

            if (optional.isPresent()) {

                log.info("Doctor details found. Deleting record with id : {}", id);

                repository.deleteById(id);

                long executionTime = System.currentTimeMillis() - startTime;

                log.info("Doctor details deleted successfully. Execution time : {} ms",
                        executionTime);

                return buildResponse(
                        true,
                        null,
                        "Doctor details deleted successfully",
                        HttpStatus.OK.value());
            }

            log.warn("Doctor details not found for id : {}", id);

            return buildResponse(
                    false,
                    null,
                    "Doctor details not found",
                    HttpStatus.NOT_FOUND.value());

        } catch (Exception e) {

            log.error("Exception occurred while deleting doctor details for id : {}. Error : {}",
                    id,
                    e.getMessage(),
                    e);

            return buildResponse(
                    false,
                    null,
                    "Failed to delete doctor details : " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAllDoctorDetailsFallback")
    @Secured("ROLE_DOCTOR")
    public Response getAllDoctorDetails() {

        long startTime = System.currentTimeMillis();

        log.info("Entered getAllDoctorDetails()");

        try {

            log.debug("Fetching all doctor details from repository");

            List<DoctorSaveDetails> list = repository.findAll();

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("Fetched {} doctor records successfully in {} ms",
                    list.size(),
                    executionTime);

            return buildResponse(
                    true,
                    list,
                    "All doctor details fetched",
                    HttpStatus.OK.value());

        } catch (Exception e) {

            log.error("Exception occurred while fetching all doctor details. Error : {}",
                    e.getMessage(),
                    e);

            return buildResponse(
                    false,
                    null,
                    "Failed to fetch doctor details : " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getVisitHistoryByPatientAndBookingFallback")
    @Secured("ROLE_DOCTOR")
    public Response getVisitHistoryByPatientAndBooking(String patientId, String bookingId) {

        long startTime = System.currentTimeMillis();

        log.info("Entered getVisitHistoryByPatientAndBooking() with patientId : {}, bookingId : {}",
                patientId,
                bookingId);

        try {

            List<DoctorSaveDetails> visits =
                    repository.findByPatientIdAndBookingId(patientId, bookingId);

            if (visits.isEmpty()) {

                log.warn("No visit history found for patientId : {} and bookingId : {}",
                        patientId,
                        bookingId);

                return buildResponse(
                        false,
                        null,
                        "No visit history found for the given patient and booking ID",
                        HttpStatus.NOT_FOUND.value());
            }

            visits.sort((v1, v2) ->
                    v1.getVisitDateTime().compareTo(v2.getVisitDateTime()));

            log.info("Found {} visits for patientId : {} and bookingId : {}",
                    visits.size(),
                    patientId,
                    bookingId);

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("getVisitHistoryByPatientAndBooking() completed in {} ms",
                    executionTime);

            return buildResponse(
                    true,
                    Map.of(
                            "patientId", patientId,
                            "bookingId", bookingId,
                            "visitCount", visits.size(),
                            "visits", visits),
                    "Visit history fetched successfully",
                    HttpStatus.OK.value());

        } catch (Exception e) {

            log.error("Exception occurred while fetching visit history. patientId : {}, bookingId : {}, Error : {}",
                    patientId,
                    bookingId,
                    e.getMessage(),
                    e);

            return buildResponse(
                    false,
                    null,
                    "Error fetching visit history: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getVisitHistoryByPatientFallback")
    @Secured("ROLE_DOCTOR")
    public Response getVisitHistoryByPatient(String patientId) {

        log.info("Fetching visit history for patientId: {}", patientId);

        try {
            List<DoctorSaveDetails> visits = repository.findByPatientId(patientId);

            log.info("Retrieved {} visit records for patientId: {}", visits.size(), patientId);

            if (visits.isEmpty()) {
                log.warn("No visit history found for patientId: {}", patientId);

                return buildResponse(
                        false,
                        null,
                        "No visit history found for the patient ID",
                        HttpStatus.NOT_FOUND.value());
            }

            log.debug("Sorting visit history records for patientId: {}", patientId);

            visits.sort((v1, v2) -> {
                LocalDateTime dt1 = v1.getVisitDateTime();
                LocalDateTime dt2 = v2.getVisitDateTime();

                if (dt1 == null && dt2 == null)
                    return 0;
                if (dt1 == null)
                    return 1;
                if (dt2 == null)
                    return -1;

                return dt1.compareTo(dt2);
            });

            log.info(
                    "Successfully fetched and sorted {} visit records for patientId: {}",
                    visits.size(),
                    patientId);

            return buildResponse(
                    true,
                    Map.of(
                            "patientId", patientId,
                            "totalVisits", visits.size(),
                            "visitHistory", visits),
                    "All visit history fetched successfully",
                    HttpStatus.OK.value());

        } catch (Exception e) {

            log.error(
                    "Error occurred while fetching visit history for patientId: {}. Error: {}",
                    patientId,
                    e.getMessage(),
                    e);

            return buildResponse(
                    false,
                    null,
                    "Error fetching visit history: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

	private DoctorSaveDetails convertToEntity(DoctorSaveDetailsDTO dto) {
		if (dto == null)
			return null;

		return DoctorSaveDetails.builder().id(dto.getId()).patientId(dto.getPatientId()).doctorId(dto.getDoctorId())
				.doctorName(dto.getDoctorName()).clinicId(dto.getClinicId()).clinicName(dto.getClinicName())
				.customerId(dto.getCustomerId()).bookingId(dto.getBookingId()).subServiceId(dto.getSubServiceId()) // ✅
																													// include
																													// subServiceId
																													// always
				// Symptoms
				.symptoms(dto.getSymptoms() != null ? SymptomDetails.builder()
						.symptomDetails(dto.getSymptoms().getSymptomDetails())
						.doctorObs(dto.getSymptoms().getDoctorObs()).diagnosis(dto.getSymptoms().getDiagnosis())
						.duration(dto.getSymptoms().getDuration())
						.attachments(dto.getSymptoms().getAttachments() != null ? dto.getSymptoms().getAttachments()
								.stream().map(this::decodeIfBase64).collect(Collectors.toList()) : null)
						.build() : null)
				.prescriptionPdf(dto.getPrescriptionPdf() != null
						? dto.getPrescriptionPdf().stream().map(this::decodeIfBase64).collect(Collectors.toList())
						: null)
				// Tests
				.tests(dto.getTests() != null ? TestDetails.builder().selectedTests(dto.getTests().getSelectedTests())
						.testReason(dto.getTests().getTestReason()).build() : null)
				// Treatments
				.treatments(dto.getTreatments() != null && dto.getTreatments().getGeneratedData() != null
						? TreatmentResponse.builder()
								.selectedTestTreatment(dto.getTreatments().getSelectedTestTreatment())
								.generatedData(dto.getTreatments().getGeneratedData().entrySet().stream()
										.collect(Collectors.toMap(Map.Entry::getKey, e -> {
											TreatmentDetailsDTO tDto = e.getValue();
											// build Dates list
											List<Dates> dates = tDto.getDates() != null ? tDto.getDates().stream()
													.map(d -> Dates.builder().date(d.getDate()).sitting(d.getSitting())
															.status(d.getStatus()).build())
													.collect(Collectors.toList()) : null;

											Integer totalSittings = tDto.getTotalSittings();
											if (totalSittings == null) {
												totalSittings = dates != null ? dates.size() : 0;
											}

											return TreatmentDetails.builder().dates(dates).reason(tDto.getReason())
													.frequency(tDto.getFrequency()).sittings(tDto.getSittings())
													.startDate(tDto.getStartDate()).totalSittings(totalSittings)
													.build();
										})))
								.build()
						: null)
				// Follow up
				.followUp(
						dto.getFollowUp() != null
								? FollowUpDetails.builder().durationValue(dto.getFollowUp().getDurationValue())
										.durationUnit(dto.getFollowUp().getDurationUnit())
										.nextFollowUpDate(dto.getFollowUp().getNextFollowUpDate())
										.followUpNote(dto.getFollowUp().getFollowUpNote()).build()
								: null)
				// Prescription object
				.prescription(
						dto.getPrescription() != null ? PrescriptionDetails.builder()
								.medicines(dto.getPrescription().getMedicines() != null ? dto.getPrescription()
										.getMedicines().stream()
										.map(med -> Medicines.builder().id(UUID.randomUUID()).name(med.getName())
												.dose(med.getDose()).duration(med.getDuration())
												.durationUnit(med.getDurationUnit()).food(med.getFood())
												.medicineType(med.getMedicineType()).note(med.getNote())
												.remindWhen(med.getRemindWhen()).times(med.getTimes())
												.others(med.getOthers()).build())
										.collect(Collectors.toList()) : null)
								.build() : null)
				// Visit meta
				.visitType(dto.getVisitType()).visitDateTime(dto.getVisitDateTime()).visitCount(dto.getVisitCount())
				// Consultation dates (if present in DTO)
				.consultationStartDate(dto.getConsultationStartDate())
				.consultationExpiryDate(dto.getConsultationExpiryDate()).build();
	}

	private String decodeIfBase64(String base64String) {
		if (base64String == null || base64String.trim().isEmpty()) {
			return base64String; // return as is if null or empty
		}
		try {
			// Try decoding just to validate format
			Base64.getDecoder().decode(base64String);
			return base64String; // It's valid Base64, return as is without converting to text
		} catch (IllegalArgumentException e) {
			// Not valid Base64, return origina
			return base64String;
		}
	}

	private DoctorSaveDetailsDTO convertToDto(DoctorSaveDetails entity) {
		return DoctorSaveDetailsDTO.builder().id(entity.getId()).patientId(entity.getPatientId())
				.doctorId(entity.getDoctorId()).doctorName(entity.getDoctorName()).clinicId(entity.getClinicId())
				.clinicName(entity.getClinicName())
//                .subServiceId(entity.getSubServiceId())
				.customerId(
						entity.getCustomerId())
				.bookingId(entity.getBookingId()).subServiceId(entity.getSubServiceId())
				.symptoms(
						entity.getSymptoms() != null
								? SymptomDetailsDTO.builder().symptomDetails(entity.getSymptoms().getSymptomDetails())
										.doctorObs(entity.getSymptoms().getDoctorObs())
										.diagnosis(entity.getSymptoms().getDiagnosis())
										.duration(entity.getSymptoms().getDuration())
										.attachments(entity.getSymptoms().getAttachments() != null
												? entity.getSymptoms().getAttachments().stream()
														.map(this::encodeIfNotBase64).collect(Collectors.toList())
												: null)
										.build()
								: null)

				.prescriptionPdf(
						entity.getPrescriptionPdf() != null
								? entity.getPrescriptionPdf().stream().map(key -> s3Service.generateSignedUrl(key))
										.collect(Collectors.toList())
								: null)

				.tests(entity.getTests() != null
						? TestDetailsDTO.builder().selectedTests(entity.getTests().getSelectedTests())
								.testReason(entity.getTests().getTestReason()).build()
						: null)

				.treatments(entity.getTreatments() != null && entity.getTreatments().getGeneratedData() != null
						? TreatmentResponseDTO.builder()
								.selectedTestTreatment(entity.getTreatments().getSelectedTestTreatment())
								.generatedData(entity.getTreatments().getGeneratedData().entrySet().stream()
										.collect(Collectors.toMap(Map.Entry::getKey, e -> TreatmentDetailsDTO.builder()
												.dates(e.getValue().getDates() != null ? e.getValue().getDates()
														.stream()
														.map(d -> DatesDTO.builder().date(d.getDate())
																.sitting(d.getSitting()).status(d.getStatus()).build())
														.collect(Collectors.toList()) : null)
												.reason(e.getValue().getReason()).frequency(e.getValue().getFrequency())
												.sittings(e.getValue().getSittings())
												.startDate(e.getValue().getStartDate())
												.totalSittings(e.getValue().getTotalSittings() != null
														? e.getValue().getTotalSittings()
														: (e.getValue().getDates() != null
																? e.getValue().getDates().size()
																: 0))

												.build())))
								.build()
						: null)

				.followUp(
						entity.getFollowUp() != null
								? FollowUpDetailsDTO.builder().durationValue(entity.getFollowUp().getDurationValue())
										.durationUnit(entity.getFollowUp().getDurationUnit())
										.nextFollowUpDate(entity.getFollowUp().getNextFollowUpDate())
										.followUpNote(entity.getFollowUp().getFollowUpNote()).build()
								: null)

				.prescription(
						entity.getPrescription() != null ? PrescriptionDetailsDTO.builder()
								.medicines(entity.getPrescription().getMedicines() != null ? entity.getPrescription()
										.getMedicines().stream()
										.map(med -> MedicinesDTO.builder().id(med.getId().toString())
												.name(med.getName()).dose(med.getDose()).duration(med.getDuration())
												.durationUnit(med.getDurationUnit()).food(med.getFood())
												.medicineType(med.getMedicineType()).note(med.getNote())
												.remindWhen(med.getRemindWhen()).times(med.getTimes())
												.others(med.getOthers()).build())
										.collect(Collectors.toList()) : null)
								.build() : null)

				.visitType(entity.getVisitType()).visitDateTime(entity.getVisitDateTime())
				.visitCount(entity.getVisitCount()).consultationExpiryDate(entity.getConsultationExpiryDate())
				.consultationStartDate(entity.getConsultationStartDate()).build();
	}

	private String encodeIfNotBase64(String input) {
		if (input == null || input.isBlank()) {
			return input;
		}

		String base64Pattern = "^[A-Za-z0-9+/]*={0,2}$";

		if (input.matches(base64Pattern) && (input.length() % 4 == 0)) {
			try {
				Base64.getDecoder().decode(input); // Validate decoding works
				return input; // Already Base64
			} catch (IllegalArgumentException e) {
				// Not valid Base64 despite matching pattern — will encode below
			}
		}

		// Encode if not valid Base64
		return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
	}

	// Builds standard Response object
	private Response buildResponse(boolean success, Object data, String message, int status) {
		return Response.builder().success(success).data(data).message(message).status(status).build();
	}

	
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getVisitHistoryByPatientAndDoctorFallback")
    @Secured("ROLE_DOCTOR")
    public Response getVisitHistoryByPatientAndDoctor(String patientId, String doctorId) {

        long startTime = System.currentTimeMillis();

        log.info("Entered getVisitHistoryByPatientAndDoctor() with patientId : {}, doctorId : {}",
                patientId, doctorId);

        try {

            log.debug("Fetching visit history for patientId : {}", patientId);

            List<DoctorSaveDetails> visits = repository.findByPatientId(patientId);

            if (visits.isEmpty()) {

                log.warn("No visit history found for patientId : {}", patientId);

                return buildResponse(
                        true,
                        null,
                        "No visit history found for the patient ID",
                        HttpStatus.OK.value());
            }

            if (doctorId != null && !doctorId.isBlank()) {

                log.debug("Filtering visit history by doctorId : {}", doctorId);

                visits = visits.stream()
                        .filter(v -> doctorId.equals(v.getDoctorId()))
                        .collect(Collectors.toList());

                if (visits.isEmpty()) {

                    log.warn("No visit history found for patientId : {} and doctorId : {}",
                            patientId, doctorId);

                    return buildResponse(
                            true,
                            null,
                            "No visit history found for the patient with the specified doctor ID",
                            HttpStatus.OK.value());
                }
            }

            visits.sort((v1, v2) -> {
                LocalDateTime dt1 = v1.getVisitDateTime();
                LocalDateTime dt2 = v2.getVisitDateTime();

                if (dt1 == null && dt2 == null) return 0;
                if (dt1 == null) return 1;
                if (dt2 == null) return -1;

                return dt2.compareTo(dt1);
            });

            List<DoctorSaveDetailsDTO> visitDtos = visits.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("Fetched {} visit records successfully in {} ms",
                    visitDtos.size(),
                    executionTime);

            return buildResponse(
                    true,
                    Map.of(
                            "patientId", patientId,
                            "doctorId", doctorId,
                            "totalVisits", visitDtos.size(),
                            "visitHistory", visitDtos),
                    "Visit history fetched successfully",
                    HttpStatus.OK.value());

        } catch (Exception e) {

            log.error("Exception occurred while fetching visit history for patientId : {}, doctorId : {}. Error : {}",
                    patientId,
                    doctorId,
                    e.getMessage(),
                    e);

            return buildResponse(
                    false,
                    null,
                    "Error fetching visit history: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    @Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getInProgressDetailsFallback")
	@Secured("ROLE_DOCTOR")
	public Response getInProgressDetails(String patientId, String bookingId) {
		try {
			// 1. Fetch booking from Booking Service
			ResponseEntity<ResponseStructure<BookingResponse>> bookingResponseEntity = bookingFeignClient
					.getBookedService(bookingId);

			if (bookingResponseEntity == null || bookingResponseEntity.getBody() == null) {
				return buildResponse(false, null, "Booking not found for ID " + bookingId,
						HttpStatus.NOT_FOUND.value());
			}

			BookingResponse booking = bookingResponseEntity.getBody().getData();

			// 2. Validate status
			if (!"In-Progress".equalsIgnoreCase(booking.getStatus())) {
				return buildResponse(false, null, "Booking is not In-Progress. Current status: " + booking.getStatus(),
						HttpStatus.BAD_REQUEST.value());
			}

			// 3. Fetch doctor details saved in your DB
			List<DoctorSaveDetails> visits = repository.findByPatientIdAndBookingId(patientId, bookingId);

			if (visits.isEmpty()) {
				return buildResponse(false, null,
						"No doctor details found for patient " + patientId + " and booking " + bookingId,
						HttpStatus.NOT_FOUND.value());
			}

			List<DoctorSaveDetailsDTO> dtos = visits.stream().map(this::convertToDto).collect(Collectors.toList());

			// 4. Build success response
			return buildResponse(true,
					Map.of("patientId", patientId, "bookingId", bookingId, "status", booking.getStatus(),
							"savedDetails", dtos),
					"In-Progress doctor details fetched successfully", HttpStatus.OK.value());

		} catch (Exception e) {
			return buildResponse(false, null, "Error fetching in-progress details: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorDetailsByBookingIdFallback")
    @Secured("ROLE_DOCTOR")
    public Response getDoctorDetailsByBookingId(String bookingId) {

        long startTime = System.currentTimeMillis();

        log.info("Entered getDoctorDetailsByBookingId() with bookingId : {}", bookingId);

        try {

            DoctorSaveDetails doctorDetails =
                    repository.findByBookingIdIgnoreCase(bookingId);

            if (doctorDetails != null) {

                log.info("Doctor details found for bookingId : {}", bookingId);

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                DoctorSaveDetailsDTO dto =
                        mapper.convertValue(doctorDetails, DoctorSaveDetailsDTO.class);

                long executionTime = System.currentTimeMillis() - startTime;

                log.info("getDoctorDetailsByBookingId() completed successfully in {} ms",
                        executionTime);

                return new Response(
                        true,
                        dto,
                        "prescription details found",
                        HttpStatus.OK.value());
            }

            log.warn("Doctor details not found for bookingId : {}", bookingId);

            return new Response(
                    false,
                    null,
                    "prescription details Not found",
                    HttpStatus.NOT_FOUND.value());

        } catch (Exception e) {

            log.error("Exception occurred while fetching doctor details by bookingId : {}. Error : {}",
                    bookingId,
                    e.getMessage(),
                    e);

            return new Response(
                    false,
                    null,
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorDetailsByCustomerIdFallback")
    @Secured({"ROLE_DOCTOR", "ROLE_CUSTOMER"})
    public Response getDoctorDetailsByCustomerId(String customerId) {

        long startTime = System.currentTimeMillis();

        log.info("Entered getDoctorDetailsByCustomerId() with customerId : {}", customerId);

        try {

            List<DoctorSaveDetails> doctorDetails =
                    repository.findByCustomerId(customerId);

            if (doctorDetails != null && !doctorDetails.isEmpty()) {

                log.info("Found {} doctor records for customerId : {}",
                        doctorDetails.size(),
                        customerId);

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                List<DoctorSaveDetailsDTO> dtos =
                        mapper.convertValue(
                                doctorDetails,
                                new TypeReference<List<DoctorSaveDetailsDTO>>() {
                                });

                long executionTime = System.currentTimeMillis() - startTime;

                log.info("getDoctorDetailsByCustomerId() completed successfully in {} ms",
                        executionTime);

                return new Response(
                        true,
                        dtos,
                        "prescription details found",
                        HttpStatus.OK.value());
            }

            log.warn("No doctor details found for customerId : {}", customerId);

            return new Response(
                    false,
                    null,
                    "prescription details Not found",
                    HttpStatus.NOT_FOUND.value());

        } catch (Exception e) {

            log.error("Exception occurred while fetching doctor details for customerId : {}. Error : {}",
                    customerId,
                    e.getMessage(),
                    e);

            return new Response(
                    false,
                    null,
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    private String extractErrorMessage(FeignException e) {
        try {
            String body = e.contentUTF8();
            if (body != null && !body.isEmpty()) {
                // Parse Booking Service JSON: {"timestamp":"...","status":500,"error":"Internal Server Error","message":"Invalid Booking Id Please provide Valid Id"}
                Map<String, Object> errorMap = objectMapper.readValue(body, Map.class);
                Object msg = errorMap.get("message");
                return msg != null ? msg.toString() : "Unknown booking service error";
            }
        } catch (Exception ex) {
            // Ignore parsing errors
        }
        return "Booking Service unreachable or internal error";
    }
    
    
    @Override
    @Secured("ROLE_DOCTOR")
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorLatestDetailsByCustomerIdFallback")
    public DoctorSaveDetailsDTO getDoctorLatestDetailsByCustomerId(String customerId) {

        long startTime = System.currentTimeMillis();

        log.info("Entered getDoctorLatestDetailsByCustomerId() with customerId : {}", customerId);

        try {

            log.debug("Fetching doctor details for customerId : {}", customerId);

            List<DoctorSaveDetails> doctorDetailsList =
                    repository.findByCustomerId(customerId);

            if (doctorDetailsList != null && !doctorDetailsList.isEmpty()) {

                log.info("Found {} doctor records for customerId : {}",
                        doctorDetailsList.size(),
                        customerId);

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                DoctorSaveDetails latestDoctorDetails =
                        doctorDetailsList.get(doctorDetailsList.size() - 1);

                log.debug("Latest doctor details record selected with id : {}",
                        latestDoctorDetails.getId());

                DoctorSaveDetailsDTO doctorSaveDetailsDTO =
                        mapper.convertValue(
                                latestDoctorDetails,
                                DoctorSaveDetailsDTO.class);

                long executionTime = System.currentTimeMillis() - startTime;

                log.info("getDoctorLatestDetailsByCustomerId() completed successfully in {} ms",
                        executionTime);

                return doctorSaveDetailsDTO;
            }

            log.warn("No doctor details found for customerId : {}", customerId);

            return null;

        } catch (Exception e) {

            log.error("Exception occurred while fetching latest doctor details for customerId : {}. Error : {}",
                    customerId,
                    e.getMessage(),
                    e);

            return null;
        }
    }
    
    private Response buildRateLimitResponse(Exception ex) {
        return new Response(false, null,
                "Rate limit exceeded. Please try again later.",
                429);
    }

	public Response getDoctorDetailsByIdFallback(String id, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response updateDoctorDetailsFallback(String id, DoctorSaveDetailsDTO dto, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response updateDoctorDetailsByBookingIdFallback(String id, DoctorSaveDetailsDTO dto, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response deleteDoctorDetailsFallback(String id, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response getAllDoctorDetailsFallback(Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response getVisitHistoryByPatientAndBookingFallback(String patientId, String bookingId, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response getVisitHistoryByPatientFallback(String patientId, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response getVisitHistoryByPatientAndDoctorFallback(String patientId, String doctorId, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response getInProgressDetailsFallback(String patientId, String bookingId, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response getDoctorDetailsByBookingIdFallback(String bookingId, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response getDoctorDetailsByCustomerIdFallback(String customerId, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response getDoctorLatestDetailsByCustomerIdFallback(String customerId, Exception ex) {
		return buildRateLimitResponse(ex);
	}

}