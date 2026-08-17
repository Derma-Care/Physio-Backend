package com.chiselon.clinicadmin.service.impl;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.chiselon.clinicadmin.dto.BankAccountDetails;
import com.chiselon.clinicadmin.dto.BookingResponse;
import com.chiselon.clinicadmin.dto.Branch;
import com.chiselon.clinicadmin.dto.ChangeDoctorPasswordDTO;
import com.chiselon.clinicadmin.dto.ClinicDTO;
import com.chiselon.clinicadmin.dto.ClinicWithDoctorsDTO;
import com.chiselon.clinicadmin.dto.ConsultationTypeDTO;
import com.chiselon.clinicadmin.dto.DoctorAvailabilityStatusDTO;
import com.chiselon.clinicadmin.dto.DoctorAvailableSlotDTO;
import com.chiselon.clinicadmin.dto.DoctorSlotDTO;
import com.chiselon.clinicadmin.dto.DoctorsDTO;
import com.chiselon.clinicadmin.dto.ResBody;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.dto.TempBlockingSlot;
import com.chiselon.clinicadmin.entity.DoctorCounter;
import com.chiselon.clinicadmin.entity.DoctorLoginCredentials;
import com.chiselon.clinicadmin.entity.DoctorSlot;
import com.chiselon.clinicadmin.entity.Doctors;
//import com.clinicadmin.feignclient.ServiceFeignClient;
import com.chiselon.clinicadmin.repository.DoctorLoginCredentialsRepository;
import com.chiselon.clinicadmin.repository.DoctorSlotRepository;
import com.chiselon.clinicadmin.repository.DoctorsRepository;
import com.chiselon.clinicadmin.service.DoctorService;
import com.chiselon.clinicadmin.service.EmailService;
import com.chiselon.clinicadmin.service.S3Service;
import com.chiselon.clinicadmin.utils.DoctorMapper;
import com.chiselon.clinicadmin.utils.DoctorSlotMapper;
import com.chiselon.clinicadmin.utils.ExtractFeignMessage;
import com.chiselon.clinicadmin.utils.FeignImpl;
import com.chiselon.clinicadmin.utils.KeyCloakTokenStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DoctorServiceImpl implements DoctorService {
	
	@Autowired
	private DoctorsRepository doctorsRepository;

	@Autowired
	private DoctorLoginCredentialsRepository credentialsRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private DoctorSlotRepository slotRepository;

	@Autowired
	private FeignImpl adminServiceClient;

	@Autowired
	private FeignImpl notificationFeign;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MongoOperations mongoOperations;

	@Autowired
	private FeignImpl bookingFeign;
	
	 @Autowired
	 private KeyCloakTokenStore keyCloakTokenStore;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private S3Service s3Service;

	private List<TempBlockingSlot> slots = new CopyOnWriteArrayList<>();

	BookingResponse bkng = new BookingResponse();

	public DoctorServiceImpl(DoctorsRepository doctorsRepository,
			DoctorLoginCredentialsRepository credentialsRepository, PasswordEncoder passwordEncoder,
			DoctorSlotRepository slotRepository) {
		this.doctorsRepository = doctorsRepository;
		this.credentialsRepository = credentialsRepository;
		this.passwordEncoder = passwordEncoder;
		this.slotRepository = slotRepository;
//		this.serviceFeignClient = serviceFeignClient;

	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "addDoctorFallback")
	public Response addDoctor(DoctorsDTO dto) {
		log.info("Add Doctor reqest received. moblie={}, hospitalId ={}, brancId ={}", dto.getDoctorMobileNumber(),
				dto.getHospitalId(), dto.getBranchId());
		Response response = new Response();
		try {
			dto.trimAllDoctorFields();
			log.debug("Doctor DTO fields trimmed successfully");

			// -------------------- Check duplicate mobile --------------------
			if (doctorsRepository.existsByDoctorMobileNumber(dto.getDoctorMobileNumber())) {
				log.warn("Duplicate doctor mobile number detected, mobileNumber = {}", dto.getDoctorMobileNumber());
				response.setSuccess(false);
				response.setMessage("Doctor with this mobile number already exists");
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return response;
			}
			if (credentialsRepository.existsByUsername(dto.getDoctorMobileNumber())) {
				log.warn("Login credentials already exist for this mobile number :{}", dto.getDoctorMobileNumber());
				response.setSuccess(false);
				response.setMessage("Login credentials already exist for this mobile number");
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return response;
			}

			// -------------------- Validate clinic --------------------
			log.debug("Validating clinicId :{}", dto.getHospitalId());
			Response clinicRes;
			try {
				clinicRes = adminServiceClient.getClinicById(keyCloakTokenStore.getAccess_token(),dto.getHospitalId());
			} catch (FeignException fe) {
				log.error("Clinic not found via admin service. clinicId={}", dto.getHospitalId());
				response.setSuccess(false);
				response.setMessage("Clinic not found with ID: " + dto.getHospitalId());
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}

			if (clinicRes == null || !clinicRes.isSuccess()) {
				log.warn("Clinic validation failed. clinicId ={}", dto.getHospitalId());
				response.setSuccess(false);
				response.setMessage("Clinic not found with ID: " + dto.getHospitalId());
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}

			ClinicDTO clinicDTO = objectMapper.convertValue(clinicRes.getData(), ClinicDTO.class);
			log.info("Clinic validated successfully. clinicId={}, clinicName={}", dto.getHospitalId(),
					dto.getHospitalName());

			// -------------------- Validate branch --------------------
			if (dto.getBranchId() == null || dto.getBranchId().isBlank()) {
				log.warn("Branch Id is missing for clinicId = {}", dto.getHospitalId());
				response.setSuccess(false);
				response.setMessage("Branch ID is required");
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return response;
			}
			log.debug("Validationg branchId={}, clinicId={}", dto.getBranchId(), dto.getHospitalId());
			Response branchRes;
			try {
				branchRes = adminServiceClient.getBranchByClinicAndBranchId(keyCloakTokenStore.getAccess_token(),dto.getHospitalId(), dto.getBranchId());
			} catch (FeignException fe) {
				log.error("Branch not found via Admin Service. clinicId={}, brancId ={}", dto.getHospitalId(),
						dto.getBranchId());
				response.setSuccess(false);
				response.setMessage("Branch not found for clinicId: " + dto.getHospitalId() + " and branchId: "
						+ dto.getBranchId());
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}

			if (branchRes == null || !branchRes.isSuccess()) {
				log.warn("Branch Validation failed. clinicId={}, branchId={}", dto.getHospitalId(), dto.getBranchId());
				response.setSuccess(false);
				response.setMessage("Branch not found for clinicId: " + dto.getHospitalId() + " and branchId: "
						+ dto.getBranchId());
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}

			Branch branchDTO = objectMapper.convertValue(branchRes.getData(), Branch.class);
			log.debug("After Mapping branch details branchName={}, branchId={}, clinicId={}", branchDTO.getBranchName(),
					branchDTO.getBranchId(), branchDTO.getClinicId());

			// -------------------- Generate doctorId --------------------
			log.debug("Generating DoctorId for clinicId={}, branchId={}", dto.getHospitalId(), dto.getBranchId());
			String clinicSeq = String.format("%04d", Integer.parseInt(dto.getHospitalId()));
			String branchSeq = branchDTO.getBranchId().substring(clinicSeq.length());

			String counterKey = "doctor_" + dto.getHospitalId() + "_" + branchDTO.getBranchId();
			Query query = Query.query(Criteria.where("_id").is(counterKey));
			Update update = new Update().inc("seq", 1);
			FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);

			DoctorCounter counter = mongoOperations.findAndModify(query, update, options, DoctorCounter.class);
			long nextDoctorSeq = (counter != null) ? counter.getSeq() : 1L;
			String doctorSeq = String.format("%02d", nextDoctorSeq);

			String doctorId = clinicSeq + branchSeq + doctorSeq;
			log.info("Generated doctorId={}", doctorId);
			dto.setDoctorId(doctorId);

			// -------------------- Map DTO -> Entity --------------------
			Doctors doctor = DoctorMapper.mapDoctorDTOtoDoctorEntity(dto);

			doctor.setDoctorId(doctorId);
			doctor.setHospitalName(clinicDTO.getName());

			// ⚡ Strict branch assignment: Only use the branch provided in payload
			doctor.setBranchId(dto.getBranchId());

			// -------------------- Save doctor --------------------
			Doctors savedDoctor = doctorsRepository.save(doctor);
			log.info("Doctor saved successfully. doctorId={}", savedDoctor.getDoctorId());

			// -------------------- Create login credentials --------------------
			String username = savedDoctor.getDoctorMobileNumber();
			String rawPassword = generateStructuredPassword();
			String encodedPassword = passwordEncoder.encode(rawPassword);

			DoctorLoginCredentials credentials = DoctorLoginCredentials.builder().staffId(savedDoctor.getDoctorId())
					.staffName(savedDoctor.getDoctorName()).hospitalId(savedDoctor.getHospitalId())
					.hospitalName(savedDoctor.getHospitalName()).branchId(savedDoctor.getBranchId()).username(username)
					.password(encodedPassword).role("ROLE_DOCTOR").emailId(savedDoctor.getDoctorEmail()).permissions(savedDoctor.getPermissions()).build();

			credentialsRepository.save(credentials);
			log.info("Logib credentials created successfully for doctorId={}", savedDoctor.getDoctorId());
	
			// -------------------- Send Email to Doctor --------------------

						try {
						    Map<String, String> mailData = new HashMap<>();
						    mailData.put("subject", "Doctor Onboarding Successful");
						    mailData.put("message",
						            "Welcome to CCMS Kinetix!\n\n" +
						            "Your account has been created successfully.\n" +
						            "Please use the below credentials to login.\n\n" +
						            "Doctor ID: " + savedDoctor.getDoctorId()
						    );


			    mailData.put("username", username);
			    mailData.put("password", rawPassword);
			    mailData.put("role", dto.getRole());   // ✅ ADD THIS

			    emailService.sendEmail(savedDoctor.getDoctorEmail(), mailData);

			    log.info("Doctor onboarding email sent to {}", savedDoctor.getDoctorEmail());

			} catch (Exception e) {
			    log.error("Failed to send doctor onboarding email: {}", e.getMessage());
			}

			DoctorsDTO toDTO = DoctorMapper.mapDoctorEntityToDoctorDTO(savedDoctor,s3Service);
			Map<String, Object> data = new HashMap<>();
			data.put("doctor", toDTO);
			data.put("username", username);
			data.put("temporaryPassword", rawPassword);
			data.put("generatedDoctorId", doctorId);

			response.setSuccess(true);
			response.setData(data);
			response.setMessage("Doctor added successfully with login credentials");
			response.setStatus(HttpStatus.CREATED.value());

		} catch (Exception e) {
			log.error("Exception occured while adding doctor. mobile={}, hospitalId= {} ,exception={}",
					dto.getDoctorMobileNumber(), dto.getHospitalId(), e.getMessage());
			response.setSuccess(false);
			response.setMessage("Error occurred while adding doctor: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		log.info("Add Doctor request completed. status={}", response.getStatus());
		return response;
	}
	
//	@Override
//	public Response startVerificationProcess(String doctorId) {
//
//	    Response response = new Response();
//
//	    try {
//	        Optional<Doctors> optionalDoctor = doctorsRepository.findByDoctorId(doctorId);
//
//	        if (optionalDoctor.isEmpty()) {
//	            response.setSuccess(false);
//	            response.setStatus(404);
//	            response.setMessage("Doctor not found");
//	            return response;
//	        }
//
//	        Doctors doctor = optionalDoctor.get();
//
//	        if (!"PENDING".equals(doctor.getStatus())) {
//	            response.setSuccess(false);
//	            response.setStatus(400);
//	            response.setMessage("Doctor is not in PENDING state");
//	            return response;
//	        }
//
//	        // ✅ Update status
//	        doctor.setStatus("VERIFICATION_IN_PROGRESS");
//	        doctorsRepository.save(doctor);
//
//	        // 📧 Email
//	        Map<String, String> mailData = new HashMap<>();
//	        mailData.put("subject", "Doctor Verification Started");
//	        mailData.put("message",
//	                "Your verification process has started.\n" +
//	                "Our team is reviewing your details.");
//
//	        emailService.sendEmail(doctor.getDoctorEmail(), mailData);
//
//	        // ✅ Response
//	        response.setSuccess(true);
//	        response.setStatus(200);
//	        response.setMessage("Verification started successfully");
//	        response.setData(doctorId);
//
//	    } catch (Exception e) {
//	        response.setSuccess(false);
//	        response.setStatus(500);
//	        response.setMessage("Failed to start verification: " + e.getMessage());
//	    }
//
//	    return response;
//	}
//
//	    @Override
//	    public Response verifyDoctor(String doctorId) {
//
//	        Response response = new Response();
//
//	        try {
//	            Optional<Doctors> optionalDoctor = doctorsRepository.findByDoctorId(doctorId);
//
//	            if (optionalDoctor.isEmpty()) {
//	                response.setSuccess(false);
//	                response.setStatus(404);
//	                response.setMessage("Doctor not found");
//	                return response;
//	            }
//
//	            Doctors doctor = optionalDoctor.get();
//
//	            if (!"VERIFICATION_IN_PROGRESS".equals(doctor.getStatus())) {
//	                response.setSuccess(false);
//	                response.setStatus(400);
//	                response.setMessage("Doctor is not under verification");
//	                return response;
//	            }
//
//	            // ✅ Update status
//	            doctor.setStatus("VERIFIED");
//	            doctorsRepository.save(doctor);
//
//	            // 📧 Email
//	            Map<String, String> mailData = new HashMap<>();
//	            mailData.put("subject", "Doctor Verified Successfully");
//	            mailData.put("message",
//	                    "Congratulations! Your profile has been verified successfully.");
//
//	            emailService.sendEmail(doctor.getDoctorEmail(), mailData);
//
//	            response.setSuccess(true);
//	            response.setStatus(200);
//	            response.setMessage("Doctor verified successfully");
//	            response.setData(doctorId);
//
//	            return response;
//
//	        } catch (Exception e) {
//	            response.setSuccess(false);
//	            response.setStatus(500);
//	            response.setMessage("Failed to verify doctor: " + e.getMessage());
//	            return response;
//	        }
//	    }
	    
//	    @Override
//	    public Response rejectDoctor(String doctorId, String reason) {
//
//	        Response response = new Response();
//
//	        try {
//	            Optional<Doctors> optionalDoctor = doctorsRepository.findByDoctorId(doctorId);
//
//	            if (optionalDoctor.isEmpty()) {
//	                response.setSuccess(false);
//	                response.setStatus(404);
//	                response.setMessage("Doctor not found");
//	                return response;
//	            }
//
//	            Doctors doctor = optionalDoctor.get();
//
//	            if ("VERIFIED".equals(doctor.getStatus())) {
//	                response.setSuccess(false);
//	                response.setStatus(400);
//	                response.setMessage("Verified doctor cannot be rejected");
//	                return response;
//	            }
//
//	            doctor.setStatus("REJECTED");
//	            doctorsRepository.save(doctor);
//
//	            // 📧 Email
//	            Map<String, String> mailData = new HashMap<>();
//	            mailData.put("subject", "Doctor Registration Rejected");
//	            mailData.put("message",
//	                    "Unfortunately, your registration has been rejected.");
//	            mailData.put("reason", reason);
//
//	            emailService.sendEmail(doctor.getDoctorEmail(), mailData);
//
//	            response.setSuccess(true);
//	            response.setStatus(200);
//	            response.setMessage("Doctor rejected successfully");
//	            response.setData(doctorId);
//
//	            return response;
//
//	        } catch (Exception e) {
//	            response.setSuccess(false);
//	            response.setStatus(500);
//	            response.setMessage("Failed to reject doctor: " + e.getMessage());
//	            return response;
//	        }
//	    }

	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllDoctorsFallback")
	public Response getAllDoctors() {
		log.info("Get All Doctors request received");
		Response response = new Response();
		try {
			log.debug("Fetching doctors data from database");
			List<Doctors> doctors = doctorsRepository.findAll();

			if (doctors.isEmpty()) {
				log.warn("No doctors found in the system");
				response.setSuccess(true);
				response.setData(Collections.emptyList());
				response.setMessage("No doctor data available");
				response.setStatus(HttpStatus.OK.value());
			} else {
				log.info("Number of doctors found :{}", doctors.size());
				List<DoctorsDTO> toDTO = doctors.stream().map(DoctorMapper::mapDoctorEntityToDoctorDTO)
						.collect(Collectors.toList());
				response.setSuccess(true);
				response.setData(toDTO);
				response.setMessage("Doctor data retrieved successfully");
				response.setStatus(HttpStatus.OK.value());
			}

		} catch (Exception e) {
			log.error("Exception occured while fitching all doctors :{}", e.getMessage());
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Error while fetching doctors: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		log.info("Get all doctors request completed. status={}", response.getStatus());
		return response;
	}

	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getDoctorsByClinicIdFallback")
	public Response getDoctorsByClinicId(String hospitalId) {
		log.info("Get doctors by clinicId request received . hospitalId={}", hospitalId);
		Response response = new Response();
		try {
			log.debug("Fetching doctors data from database. hospitalId={}", hospitalId);
			List<Doctors> doctorList = doctorsRepository.findByHospitalId(hospitalId);
			if (!doctorList.isEmpty()) {
				log.info("Doctors found hospitalId={}, count={}", hospitalId, doctorList.size());
				List<DoctorsDTO> dtos = doctorList.stream()
				        .map(doc -> DoctorMapper.mapDoctorEntityToDoctorDTO(doc, s3Service))
				        .collect(Collectors.toList());
				response.setSuccess(true);
				response.setData(dtos);
				response.setMessage("Doctors fetched successfully");
				response.setStatus(200);
			} else {
				log.warn("No doctors found in hospitalId={}", hospitalId);
				response.setSuccess(true);
				response.setData(Collections.emptyList()); // Return an empty list
				response.setMessage("No doctors found for hospitalId: " + hospitalId);
				response.setStatus(200);

			}
		} catch (Exception e) {
			log.error("Exception occured while fetching doctors using hospitalId={},Exception={}", hospitalId,
					e.getMessage());
			response.setSuccess(false);
			response.setMessage("An error occurred while fetching doctors for hospitalId: " + hospitalId);
			response.setStatus(500);
		}
		log.info("Get doctors by clinicId request completed. hospitalId={}, status={}", hospitalId,
				response.getStatus());
		return response;
	}

	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getDoctorByIdFallback")
	public Response getDoctorById(String id) {
		log.info("Get Doctor by id request received :{}", id);
		Response response = new Response();
		try {
			log.debug("Fetching doctor data from database. doctorId={}", id);
			Optional<Doctors> doctorOptional = doctorsRepository.findByDoctorId(id);

			if (doctorOptional.isPresent()) {
				Doctors dataFromDB = doctorOptional.get();
				DoctorsDTO toDTO = DoctorMapper.mapDoctorEntityToDoctorDTO(dataFromDB,s3Service);
				log.info("Doctor found. doctorId={}, doctorName={}", toDTO.getDoctorId(), toDTO.getDoctorName());
				response.setSuccess(true);
				response.setData(toDTO);
				response.setMessage("Doctor retrive successfully");
				response.setStatus(HttpStatus.OK.value());
			} else {
				log.warn("Doctor not found with this doctorId={}", id);
				response.setSuccess(false);
				response.setData(null);
				response.setMessage("Doctor not found with ID: " + id);
				response.setStatus(HttpStatus.NOT_FOUND.value());
			}
		} catch (Exception e) {
			log.error("Exception occured while fetching doctor by ID :{}", e.getMessage());
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Error fetching doctor by ID: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return response;
	}

	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "upDateDoctorByIdFallback")
	public Response upDateDoctorById(String doctorId, DoctorsDTO dto) {
		log.info("Update doctor request received for doctorId={}", doctorId);

		Response response = new Response();
		try {
			log.debug("Fetching doctor from database for doctorId={}", doctorId);
			Optional<Doctors> doctorOptional = doctorsRepository.findByDoctorId(doctorId);

			if (doctorOptional.isEmpty()) {
				log.warn("Doctor not found with doctorId={}", doctorId);
				response.setSuccess(false);
				response.setData(null);
				response.setMessage("Doctor not found with ID: " + doctorId);
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}

			Doctors doctor = doctorOptional.get();
			log.debug("Doctor found. Updating fields for doctorId={}", doctorId);

			/* ---------- FIELD UPDATES ---------- */
			if (dto.getDoctorPicture() != null && !dto.getDoctorPicture().isBlank())
			    doctor.setDoctorPicture(dto.getDoctorPicture()); // S3 key stored as-is
			if (dto.getHospitalId() != null)
				doctor.setHospitalId(dto.getHospitalId());
			if (dto.getDoctorEmail() != null)
				doctor.setDoctorEmail(dto.getDoctorEmail());
			if (dto.getDoctorLicence() != null)
				doctor.setDoctorLicence(dto.getDoctorLicence());
			if (dto.getDoctorMobileNumber() != null)
				doctor.setDoctorMobileNumber(dto.getDoctorMobileNumber());
			if (dto.getDoctorName() != null)
				doctor.setDoctorName(dto.getDoctorName());
			if (dto.getSpecialization() != null)
				doctor.setSpecialization(dto.getSpecialization());
			if (dto.getGender() != null)
				doctor.setGender(dto.getGender());
			if (dto.getExperience() != null)
				doctor.setExperience(dto.getExperience());
			if (dto.getQualification() != null)
				doctor.setQualification(dto.getQualification());
			if (dto.getAvailableDays() != null)
				doctor.setAvailableDays(dto.getAvailableDays());
			if (dto.getAvailableTimes() != null)
				doctor.setAvailableTimes(dto.getAvailableTimes());
			if (dto.getProfileDescription() != null)
				doctor.setProfileDescription(dto.getProfileDescription());
			if (dto.getFocusAreas() != null)
				doctor.setFocusAreas(dto.getFocusAreas());
			if (dto.getDoctorAverageRating() != 0.0)
				doctor.setDoctorAverageRating(dto.getDoctorAverageRating());
			if (dto.getLanguages() != null)
				doctor.setLanguages(dto.getLanguages());
			if (dto.getHighlights() != null)
				doctor.setHighlights(dto.getHighlights());
			if (dto.getDateofJoining() != null)
				doctor.setDateofJoining(dto.getDateofJoining());
			if (dto.getDoctorSignature() != null && !dto.getDoctorSignature().isBlank())
			    doctor.setDoctorSignature(dto.getDoctorSignature()); // S3 key stored as-is
			if (dto.getDoctorFees() != null)
				doctor.setDoctorFees(DoctorMapper.mapDoctorFeeDTOtoEntity(dto.getDoctorFees()));
			if (dto.getBankAccountDetails() != null) {

				BankAccountDetails bankDetails = doctor.getBankAccountDetails();

				if (dto.getBankAccountDetails() != null) {

				    if (dto.getBankAccountDetails().getAccountHolderName() != null) {
				        bankDetails.setAccountHolderName(
				                dto.getBankAccountDetails().getAccountHolderName());
				    }

				    if (dto.getBankAccountDetails().getAccountNumber() != null) {
				        bankDetails.setAccountNumber(
				                dto.getBankAccountDetails().getAccountNumber());
				    }

				    if (dto.getBankAccountDetails().getBankName() != null) {
				        bankDetails.setBankName(
				                dto.getBankAccountDetails().getBankName());
				    }

				    if (dto.getBankAccountDetails().getBranchName() != null) {
				        bankDetails.setBranchName(
				                dto.getBankAccountDetails().getBranchName());
				    }

				    if (dto.getBankAccountDetails().getIfscCode() != null) {
				        bankDetails.setIfscCode(
				                dto.getBankAccountDetails().getIfscCode());
				    }
				    
				    if (dto.getBankAccountDetails().getPanCardNumber()  != null) {
				        bankDetails.setPanCardNumber(
				                dto.getBankAccountDetails().getPanCardNumber());
				    }
				}

				doctor.setBankAccountDetails(bankDetails);
			}


			if (dto.getDoctorAvailabilityStatus() != null) {
			    doctor.setDoctorAvailabilityStatus(dto.getDoctorAvailabilityStatus());
			}

			if (dto.isRecommendation() != doctor.isRecommendation()) {
			    doctor.setRecommendation(dto.isRecommendation());
			}

			if (dto.isAssociatedWithIADVC() != doctor.isAssociatedWithIADVC()) {
			    doctor.setAssociatedWithIADVC(dto.isAssociatedWithIADVC());
			}

			if (dto.getAssociationsOrMemberships() != null 
			        && !dto.getAssociationsOrMemberships().isEmpty()) {
			    doctor.setAssociationsOrMemberships(dto.getAssociationsOrMemberships());
			}

			if (dto.getBranches() != null 
			        && !dto.getBranches().isEmpty()) {
			    doctor.setBranches(dto.getBranches());
			}

			log.info("Saving updated doctor data for doctorId={}", doctorId);
			Doctors updatedDoctor = doctorsRepository.save(doctor);

			DoctorsDTO toDTO = DoctorMapper.mapDoctorEntityToDoctorDTO(updatedDoctor,s3Service);

			response.setSuccess(true);
			response.setData(toDTO);
			response.setMessage("Doctor updated successfully");
			response.setStatus(HttpStatus.OK.value());

			log.info("Doctor updated successfully for doctorId={}", doctorId);

		} catch (Exception e) {
			log.error("Exception occurred while updating doctorId={}", doctorId, e);
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Error updating doctor: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		return response;
	}

	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getDoctorsByClinicIdAndDoctorIdFallback")
	public Response getDoctorsByClinicIdAndDoctorId(String clinicId, String doctorId) {

		log.info("Get Doctor request received. clinicId={}, doctorId={}", clinicId, doctorId);

		Response response = new Response();

		try {
			// Validate clinicId
			if (clinicId == null || clinicId.trim().isEmpty()) {
				log.warn("ClinicId is missing or empty");
				response.setSuccess(false);
				response.setData(Collections.emptyList());
				response.setMessage("Clinic ID (hospitalId) is required.");
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return response;
			}

			// Validate doctorId
			if (doctorId == null || doctorId.trim().isEmpty()) {
				log.warn("DoctorId is missing or empty for clinicId={}", clinicId);
				response.setSuccess(false);
				response.setData(Collections.emptyList());
				response.setMessage("Doctor ID is required.");
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return response;
			}

			log.debug("Fetching doctor from DB for clinicId={}, doctorId={}", clinicId, doctorId);

			// Fetch doctor by clinicId and doctorId
			Optional<Doctors> doctorOptional = doctorsRepository.findByHospitalIdAndDoctorId(clinicId, doctorId);

			if (doctorOptional.isPresent()) {

				log.info("Doctor found. clinicId={}, doctorId={}", clinicId, doctorId);

				Doctors dbData = doctorOptional.get();
				DoctorsDTO toDTO = DoctorMapper.mapDoctorEntityToDoctorDTO(dbData,s3Service);

				response.setSuccess(true);
				response.setData(toDTO);
				response.setMessage("Doctor retrieved successfully");
				response.setStatus(HttpStatus.OK.value());

			} else {
				log.warn("Doctor not found. clinicId={}, doctorId={}", clinicId, doctorId);

				response.setSuccess(false);
				response.setData(Collections.emptyList());
				response.setMessage("Doctor not found with ID: " + doctorId + " in Clinic: " + clinicId);
				response.setStatus(HttpStatus.NOT_FOUND.value());
			}

		} catch (Exception e) {
			log.error("Exception while fetching doctor. clinicId={}, doctorId={}", clinicId, doctorId, e);

			response.setSuccess(false);
			response.setData(Collections.emptyList());
			response.setMessage("Error fetching doctor: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		log.info("Get Doctor request completed. clinicId={}, doctorId={}, status={}", clinicId, doctorId,
				response.getStatus());

		return response;
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteDoctorByIdFallback")
	public Response deleteDoctorById(String doctorId) {

		log.info("Delete doctor request received for doctorId={}", doctorId);
		Response response = new Response();

		try {
			log.debug("Fetching doctor from DB for doctorId={}", doctorId);
			Optional<Doctors> optionalDoctor = doctorsRepository.findByDoctorId(doctorId);

			if (optionalDoctor.isPresent()) {

				log.info("Doctor found. Deleting doctor record for doctorId={}", doctorId);
				doctorsRepository.deleteById(optionalDoctor.get().getId());

				log.debug("Checking login credentials for doctorId={}", doctorId);
				Optional<DoctorLoginCredentials> optionalCredentials = credentialsRepository.findByStaffId(doctorId);

				optionalCredentials.ifPresent(credentials -> {
					log.info("Deleting login credentials for doctorId={}", doctorId);
					credentialsRepository.delete(credentials);
				});

				response.setSuccess(true);
				response.setStatus(HttpStatus.OK.value());
				response.setMessage("Doctor and credentials deleted successfully.");

				log.info("Doctor and credentials deleted successfully for doctorId={}", doctorId);

			} else {
				log.warn("Doctor not found for deletion, doctorId={}", doctorId);
				response.setSuccess(false);
				response.setStatus(HttpStatus.NOT_FOUND.value());
				response.setMessage("Doctor not found with ID: " + doctorId);
			}

		} catch (Exception e) {
			log.error("Exception while deleting doctorId={}", doctorId, e);
			response.setSuccess(false);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			response.setMessage("Error deleting doctor: " + e.getMessage());
		}

		return response;
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteDoctorFromBranchFallback")
	public Response deleteDoctorFromBranch(String doctorId, String branchId) {

		log.info("Delete doctor from branch request received. doctorId={}, branchId={}", doctorId, branchId);
		Response response = new Response();

		Optional<Doctors> optionalDoctor = doctorsRepository.findByDoctorId(doctorId);

		if (optionalDoctor.isEmpty()) {
			log.warn("Doctor not found for doctorId={}", doctorId);
			response.setSuccess(false);
			response.setStatus(HttpStatus.NOT_FOUND.value());
			response.setMessage("Doctor not found with ID: " + doctorId);
			return response;
		}

		Doctors doctor = optionalDoctor.get();

		if (doctor.getBranches() == null || doctor.getBranches().isEmpty()) {
			log.warn("Doctor has no branches assigned. doctorId={}", doctorId);
			response.setSuccess(false);
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			response.setMessage("Doctor has no branches assigned");
			return response;
		}

		log.debug("Attempting to remove branchId={} from doctorId={}", branchId, doctorId);
		Boolean removed = doctor.getBranches().removeIf(b -> b.getBranchId().equals(branchId));

		if (!removed) {
			log.warn("Doctor not assigned to branchId={} for doctorId={}", branchId, doctorId);
			response.setSuccess(false);
			response.setStatus(HttpStatus.NOT_FOUND.value());
			response.setMessage("Doctor not assigned to branch: " + branchId);
			return response;
		}

		if (doctor.getBranches().isEmpty()) {
			log.info("No branches left. Deleting doctor entirely for doctorId={}", doctorId);

			doctorsRepository.deleteById(doctor.getId());

			Optional<DoctorLoginCredentials> optionalCredentials = credentialsRepository.findByStaffId(doctorId);

			optionalCredentials.ifPresent(credentials -> {
				log.info("Deleting credentials for doctorId={}", doctorId);
				credentialsRepository.delete(credentials);
			});

			response.setSuccess(true);
			response.setStatus(HttpStatus.OK.value());
			response.setMessage("Doctor deleted entirely as no branches left");

		} else {
			log.info("Updating doctor after branch removal. doctorId={}", doctorId);
			doctorsRepository.save(doctor);

			response.setSuccess(true);
			response.setStatus(HttpStatus.OK.value());
			response.setMessage("Doctor removed from branch successfully");
		}

		return response;
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteDoctorsByClinicFallback")
	public Response deleteDoctorsByClinic(String hospitalId) {

		log.info("Delete doctors by clinic request received. hospitalId={}", hospitalId);
		Response response = new Response();

		try {
			List<Doctors> doctors = doctorsRepository.findByHospitalId(hospitalId);

			if (!doctors.isEmpty()) {

				log.info("Found {} doctors for hospitalId={}", doctors.size(), hospitalId);

				for (Doctors doctor : doctors) {

					log.debug("Deleting credentials for doctorId={}", doctor.getDoctorId());
					Optional<DoctorLoginCredentials> optionalCredentials = credentialsRepository
							.findByStaffId(doctor.getDoctorId());
					optionalCredentials.ifPresent(credentialsRepository::delete);

					log.debug("Deleting doctor record for doctorId={}", doctor.getDoctorId());
					doctorsRepository.deleteById(doctor.getId());
				}

				response.setSuccess(true);
				response.setStatus(HttpStatus.OK.value());
				response.setMessage(
						"All doctors and their credentials linked to clinic ID " + hospitalId + " have been deleted.");

				log.info("All doctors deleted successfully for hospitalId={}", hospitalId);

			} else {
				log.warn("No doctors found for hospitalId={}", hospitalId);
				response.setSuccess(false);
				response.setStatus(HttpStatus.NOT_FOUND.value());
				response.setMessage("No doctors found for clinic ID: " + hospitalId);
			}

		} catch (Exception e) {
			log.error("Exception while deleting doctors for hospitalId={}", hospitalId, e);
			response.setSuccess(false);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			response.setMessage("Error deleting doctors linked to clinic ID " + hospitalId + ": " + e.getMessage());
		}

		return response;
	}

	// -------------------------------DOCTOR can Change
	// password-------------------------------------------------------------
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "changePasswordFallback")
	public Response changePassword(ChangeDoctorPasswordDTO updateDTO) {

		log.info("Change password request received for username={}", updateDTO.getUserName());

		Response responseDTO = new Response();

		/* ---------- PASSWORD MATCH VALIDATION ---------- */
		if (!updateDTO.getNewPassword().equals(updateDTO.getConfirmPassword())) {
			log.warn("Change password failed: new and confirm password mismatch for username={}",
					updateDTO.getUserName());

			responseDTO.setSuccess(false);
			responseDTO.setStatus(HttpStatus.BAD_REQUEST.value());
			responseDTO.setMessage("New password and confirm password do not match");
			responseDTO.setData(null);
			return responseDTO;
		}

		/* ---------- FETCH CREDENTIALS ---------- */
		log.debug("Fetching credentials for username={}", updateDTO.getUserName());
		Optional<DoctorLoginCredentials> optionalCredentials = credentialsRepository
				.findByUsername(updateDTO.getUserName());

		if (optionalCredentials.isPresent()) {

			DoctorLoginCredentials credentials = optionalCredentials.get();
			log.debug("Credentials found for username={}, staffId={}", credentials.getUsername(),
					credentials.getStaffId());

			/* ---------- CURRENT PASSWORD VALIDATION ---------- */
			log.debug("Validating current password for username={}", updateDTO.getUserName());
			if (passwordEncoder.matches(updateDTO.getCurrentPassword(), credentials.getPassword())) {

				log.info("Current password verified. Updating password for username={}", updateDTO.getUserName());

				credentials.setPassword(passwordEncoder.encode(updateDTO.getNewPassword()));
				credentialsRepository.save(credentials);

				responseDTO.setSuccess(true);
				responseDTO.setStatus(HttpStatus.OK.value());
				responseDTO.setMessage("Password updated successfully");
				responseDTO.setData(null);

				log.info("Password updated successfully for username={}", updateDTO.getUserName());

			} else {
				log.warn("Change password failed: incorrect current password for username={}", updateDTO.getUserName());

				responseDTO.setSuccess(false);
				responseDTO.setStatus(HttpStatus.UNAUTHORIZED.value());
				responseDTO.setMessage("Old password is incorrect");
				responseDTO.setData(null);
			}

		} else {
			log.warn("Change password failed: doctor not found for username={}", updateDTO.getUserName());

			responseDTO.setSuccess(false);
			responseDTO.setStatus(HttpStatus.NOT_FOUND.value());
			responseDTO.setMessage("Doctor not found");
			responseDTO.setData(null);
		}

		return responseDTO;
	}

//    ---------------------Get DoctorsAll By hospitalId---------------------------------------
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getDoctorsByClinicIdAndBranchIdFallback")
	public Response getDoctorsByClinicIdAndBranchId(String hospitalId, String branchId) {

		log.info("Get doctors request received for hospitalId={}, branchId={}", hospitalId, branchId);

		Response response = new Response();

		try {
			log.debug("Fetching doctors from DB for hospitalId={} and branchId={}", hospitalId, branchId);
			List<Doctors> doctorList = doctorsRepository.findByHospitalIdAndBranchId(hospitalId, branchId);

			if (!doctorList.isEmpty()) {

				log.info("Found {} doctors for hospitalId={} and branchId={}", doctorList.size(), hospitalId, branchId);

				List<DoctorsDTO> dtos = doctorList.stream()
				        .map(doc -> DoctorMapper.mapDoctorEntityToDoctorDTO(doc, s3Service))
				        .collect(Collectors.toList());

				response.setSuccess(true);
				response.setData(dtos);
				response.setMessage("Doctors fetched successfully");
				response.setStatus(HttpStatus.OK.value());

			} else {

				log.warn("No doctors found for hospitalId={} and branchId={}", hospitalId, branchId);

				response.setSuccess(true);
				response.setData(Collections.emptyList());
				response.setMessage("No doctors found for hospitalId: " + hospitalId + " and branchId: " + branchId);
				response.setStatus(HttpStatus.OK.value());
			}

		} catch (Exception e) {

			log.error("Exception while fetching doctors for hospitalId={} and branchId={}", hospitalId, branchId, e);

			response.setSuccess(false);
			response.setMessage("An error occurred while fetching doctors for hospitalId: " + hospitalId
					+ " and branchId: " + branchId);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		return response;
	}
	// ----------------- Helper Methods ------------------------

//	private String generateDoctorId() {
//		String doctorId = "DC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//		// Get the current count of doctors
//		return doctorId;
//	}

	private String generateStructuredPassword() {
		String[] words = { "doctor" };
		String specialChars = "@#$%&*!?";
		String digits = "0123456789";
		SecureRandom random = new SecureRandom();
		// Choose a random word and capitalize the first letter
		String word = words[random.nextInt(words.length)];
		String capitalizedWord = word.substring(0, 1).toUpperCase() + word.substring(1);

		// Choose one special character
		char specialChar = specialChars.charAt(random.nextInt(specialChars.length()));

		// Generate a 3-digit number
		StringBuilder numberPart = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			numberPart.append(digits.charAt(random.nextInt(digits.length())));
		}

		// Combine all parts to form the password
		return capitalizedWord + specialChar + numberPart;
	}

//-------------------------------Doctor AvailabilityStatus--------------------------------------------------------------------------------
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "availabilityStatusFallback")
	public Response availabilityStatus(String doctorId, DoctorAvailabilityStatusDTO status) {

		log.info("Update availability status request received for doctorId={}", doctorId);

		Response response = new Response();

		try {
			log.debug("Fetching doctor from DB for doctorId={}", doctorId);
			Optional<Doctors> doctor = doctorsRepository.findByDoctorId(doctorId);

			if (doctor.isPresent()) {

				Doctors getDoctor = doctor.get();
				boolean availability = status.isDoctorAvailabilityStatus();

				log.debug("Updating availability status to {} for doctorId={}", availability, doctorId);

				getDoctor.setDoctorAvailabilityStatus(availability);
				doctorsRepository.save(getDoctor);

				response.setSuccess(true);
				String message = availability ? "Doctor is now available" : "Doctor is now unavailable";

				response.setMessage(message);
				response.setStatus(HttpStatus.OK.value());

				log.info("Availability status updated successfully for doctorId={}, status={}", doctorId, availability);

			} else {

				log.warn("Doctor not found while updating availability status, doctorId={}", doctorId);

				response.setSuccess(false);
				response.setMessage("Doctor Not found with this id :" + doctorId);
				response.setStatus(HttpStatus.NOT_FOUND.value());
			}

		} catch (Exception e) {

			log.error("Exception while updating availability status for doctorId={}", doctorId, e);

			response.setSuccess(false);
			response.setMessage("Error while updating doctor availability status");
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		return response;
	}

	// -------------------------------------Adding
	// Slots---------------------------------------------------------------------------------------
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "saveDoctorSlotFallback")
	public Response saveDoctorSlot(String hospitalId, String doctorId, DoctorSlotDTO dto) {
		log.info("Save doctor slot request received hospitalId={}, doctorId={}", hospitalId, doctorId);
		Response response = new Response();

		try {
			if (dto == null || dto.getAvailableSlots() == null || dto.getAvailableSlots().isEmpty()) {
				log.warn("Invalid slot details provided doctorId=={}", doctorId);
				throw new IllegalArgumentException("Invalid slot details provided");
			}
			log.debug("Checking doctor existence doctorId={}", doctorId);
			Optional<Doctors> getDoctor = doctorsRepository.findByDoctorId(doctorId);
			if (getDoctor.isEmpty()) {
				log.warn("Doctor not found, doctorId={}", doctorId);
				response.setSuccess(false);
				response.setMessage("Doctor not found with ID: " + doctorId);
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}

			// Corrected line: assuming repository returns Optional<DoctorSlot>
			log.debug("Checking existing slot for doctorId={} on date={}", doctorId, dto.getDate());
			DoctorSlot existingSlot = slotRepository.findByDoctorIdAndDate(doctorId, dto.getDate());
			DoctorSlot savedSlot;
			if (existingSlot != null) {
				log.info("Existing slot found , doctorId={}, date={}", doctorId, dto.getDate());
				List<DoctorAvailableSlotDTO> currentSlots = existingSlot.getAvailableSlots();

				// Filter incoming slots to avoid duplicates
				List<DoctorAvailableSlotDTO> newUniqueSlots = dto.getAvailableSlots().stream()
						.filter(incoming -> currentSlots.stream()
								.noneMatch(existing -> existing.getSlot().equals(incoming.getSlot())))
						.toList();
				log.debug("New unique slots count={}, doctorId={}", newUniqueSlots.size(), doctorId);
				currentSlots.addAll(newUniqueSlots); // Add only new unique slots
				existingSlot.setAvailableSlots(currentSlots);

				savedSlot = slotRepository.save(existingSlot);
				log.info("Slots updated successfully, doctorId={}, totalSlots={}", doctorId, currentSlots.size());
			} else {
				log.info("No existing slot fount. Creating new slot, doctorId={}, date={}", doctorId, dto.getDate());
				DoctorSlot newSlot = DoctorSlotMapper.doctorSlotDTOtoEntity(dto);
				newSlot.setDoctorId(doctorId);
				newSlot.setHospitalId(hospitalId);
				savedSlot = slotRepository.save(newSlot);
				log.info("New slot created successfully doctorId={}, slotCount={}", doctorId,
						dto.getAvailableSlots().size());
			}

			response.setSuccess(true);
			response.setData(savedSlot);
			response.setMessage("Slot(s) saved successfully");
			response.setStatus(HttpStatus.CREATED.value());
			log.info("Save doctor slot completed successfully, doctorId={}", doctorId);

		} catch (IllegalArgumentException e) {
			log.error("Validation error while saving slots doctorId={}, message={}", doctorId, e.getMessage());
			response.setSuccess(false);
			response.setMessage("Validation Error: " + e.getMessage());
			response.setStatus(HttpStatus.BAD_REQUEST.value());

		} catch (Exception e) {
			log.error("Exception occured while saving slots, doctorId={}", doctorId, e);
			response.setSuccess(false);
			response.setMessage("An error occurred while saving slots: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		log.info("Save doctor slot request completed", response.getStatus());
		return response;
	}

//		-------------------------Get Slots by Doctors -------------------------------------------
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getDoctorSlotsFallback")
	public Response getDoctorSlots(String hospitalId, String doctorId) {
		log.info("Get doctor slot request received, hospitalId={}, doctorId={}", hospitalId, doctorId);
		Response response = new Response();
		try {
			log.debug("Fetching slots from database, hospitalId={}, doctorId={}", hospitalId, doctorId);
			List<DoctorSlot> slots = slotRepository.findByHospitalIdAndDoctorId(hospitalId, doctorId);

			if (slots == null || slots.isEmpty()) {
				log.warn("No slots found, hospitalId={}, doctorId={}", hospitalId, doctorId);
				response.setSuccess(true);
				response.setData(null);
				response.setMessage("Slots Not Found");
				response.setStatus(HttpStatus.OK.value());

			}
			log.info("Slots fetched successfully, count={},doctorId={}", slots.size(), doctorId);
			response.setSuccess(true);
			response.setData(slots);
			response.setMessage("Slots fetched successfully");
			response.setStatus(HttpStatus.OK.value());

		} catch (Exception e) {
			log.error("Error while fetching slots | hospitalId={} | doctorId={}", hospitalId, doctorId, e);

			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Internal server error occurred");
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return response;
	}

	// --------------------------- detele slot by time and date using
	// doctorId-----------------------------------------
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteDoctorSlotFallback")
	public Response deleteDoctorSlot(String doctorId, String branchId, String date, String slotToDelete) {
		log.info("Delete doctor slot request received , doctorId={}, branchId={}, date={}, slot={}", doctorId, branchId,
				date, slotToDelete);
		Response response = new Response();
		try {
			// Fetch slot by doctorId, branchId, and date
			log.debug("Fetcing doctor slot for deletion, doctorId, branchId={}, date={}", doctorId, branchId, date);
			DoctorSlot doctorSlot = slotRepository.findByDoctorIdAndBranchIdAndDate(doctorId, branchId, date);

			if (doctorSlot == null) {
				log.warn("No slot found for givrn details , doctorId={}, branchId={}, date={}", doctorId, branchId,
						date);
				response.setSuccess(false);
				response.setData(null);
				response.setMessage("No slot found for the doctor in this branch on the given date");
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}
			log.debug("Checking slot availability, slot={}, doctorId={}", slotToDelete, doctorId);
			boolean slotExists = doctorSlot.getAvailableSlots().stream()
					.anyMatch(s -> slotToDelete.equals(s.getSlot()) && !s.isSlotbooked());

			if (!slotExists) {
				log.warn("Slot not found or already booked,slot={}, doctorId={}", slotToDelete, doctorId);
				response.setSuccess(false);
				response.setData(null);
				response.setMessage("Slot not found or already booked");
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return response;
			}

			List<DoctorAvailableSlotDTO> updatedSlots = doctorSlot.getAvailableSlots().stream()
					.filter(s -> !(slotToDelete.equals(s.getSlot()) && !s.isSlotbooked())).collect(Collectors.toList());
			log.debug("Slot removed successfully, remaingSlots={}, doctorId={}", updatedSlots.size(), doctorId);
			doctorSlot.setAvailableSlots(updatedSlots);
			slotRepository.save(doctorSlot);

			DoctorSlotDTO dto = new DoctorSlotDTO();
			dto.setDoctorId(doctorSlot.getDoctorId());
			dto.setHospitalId(doctorSlot.getHospitalId());
			dto.setBranchId(doctorSlot.getBranchId());
			dto.setBranchName(doctorSlot.getBranchName());
			dto.setDate(doctorSlot.getDate());
			dto.setAvailableSlots(updatedSlots);

			response.setSuccess(true);
			response.setData(dto);
			response.setMessage("Slot deleted successfully for the given branch");
			response.setStatus(HttpStatus.OK.value());
			log.info("Slot deleted successfully, doctorId={}, branchId={}, date={},slot={}", doctorId, branchId, date,
					slotToDelete);
		} catch (Exception e) {
			log.error("Exception occured while deleting slots, doctorId={}, brachId={}, date={}", doctorId, branchId,
					date, e);
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Internal server error occurred: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return response;
	}

	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteDoctorSlotFallback")
	public Response deleteDoctorSlot(String doctorId, String date, String slotToDelete) {
		log.info("Delete doctor slot request received , doctorId={}, date={}, slot={}", doctorId, date, slotToDelete);
		Response response = new Response();
		try {
			log.debug("Fetching doctor slot | doctorId={} | date={}", doctorId, date);
			DoctorSlot doctorSlot = slotRepository.findByDoctorIdAndDate(doctorId, date);

			if (doctorSlot == null) {
				log.warn("No slot found | doctorId={} | date={}", doctorId, date);
				response.setSuccess(false);
				response.setData(null);
				response.setMessage("No slot found for the doctor on the given date");
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}
			log.debug("Validating slot availability | slot={} | doctorId={}", slotToDelete, doctorId);
			boolean slotExists = doctorSlot.getAvailableSlots().stream()
					.anyMatch(s -> slotToDelete.equals(s.getSlot()) && !s.isSlotbooked());

			if (!slotExists) {
				log.warn("Slot not found or already booked | slot={} | doctorId={}", slotToDelete, doctorId);
				response.setSuccess(false);
				response.setData(null);
				response.setMessage("Slot not found or already booked");
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return response;
			}

			List<DoctorAvailableSlotDTO> updatedSlots = doctorSlot.getAvailableSlots().stream()
					.filter(s -> !(slotToDelete.equals(s.getSlot()) && !s.isSlotbooked())).collect(Collectors.toList());
			log.debug("Slot removed | remainingSlots={} | doctorId={}", updatedSlots.size(), doctorId);

			doctorSlot.setAvailableSlots(updatedSlots);
			slotRepository.save(doctorSlot);

			DoctorSlotDTO dto = new DoctorSlotDTO();
			dto.setDoctorId(doctorSlot.getDoctorId());
			dto.setHospitalId(doctorSlot.getHospitalId());
			dto.setBranchId(doctorSlot.getBranchId());
			dto.setBranchName(doctorSlot.getBranchName());
			dto.setDate(doctorSlot.getDate());
			dto.setAvailableSlots(updatedSlots);

			response.setSuccess(true);
			response.setData(dto);
			response.setMessage("Slot deleted successfully");
			response.setStatus(HttpStatus.OK.value());
			log.info("Slot deleted successfully | doctorId={} | date={} | slot={}", doctorId, date, slotToDelete);
		} catch (Exception e) {
			log.error("Error while deleting slot | doctorId={} | date={}", doctorId, date, e);
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Internal server error occurred: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return response;
	}

	// -----------------------------update
	// Slot---------------------------------------------------------------------------
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateDoctorSlotFallback")
	public Response updateDoctorSlot(String doctorId, String date, String oldSlot, String newSlot) {
		log.info("Update doctor slot request received, doctorId={}, date={}, oldSlot={}, newSlot={}", doctorId, date,
				oldSlot, newSlot);
		try {
			log.debug("Fetching doctor slot details from database");
			DoctorSlot doctorSlot = slotRepository.findByDoctorIdAndDate(doctorId, date);
			if (doctorSlot == null) {
				log.warn("No slot found for doctorId={} on date={}", doctorId, date);
				Response response = new Response();
				response.setSuccess(false);
				response.setData(null);
				response.setMessage("No slot found for the doctor on the given date");
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}
			List<DoctorAvailableSlotDTO> slots = doctorSlot.getAvailableSlots();
			log.debug("Total available slots found: {}", slots.size());
			boolean slotUpdated = false;

			for (DoctorAvailableSlotDTO slot : slots) {
				log.debug("Checking slot={}, booked={}", slot.getSlot(), slot.isSlotbooked());
				if (slot.getSlot().equals(oldSlot) && !slot.isSlotbooked()) {
					slot.setSlot(newSlot);
					slotUpdated = true;
					log.info("slot updated successfully, oldSlot={}-> newSlot={}", oldSlot, newSlot);
					break;
				}
			}

			if (!slotUpdated) {
				log.warn("Slot updated failed, oldSlot={} not found or alredy booked", oldSlot);
				Response response = new Response();
				response.setSuccess(false);
				response.setData(null);
				response.setMessage("Old slot not found or already booked");
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return response;

			}

			doctorSlot.setAvailableSlots(slots);
			slotRepository.save(doctorSlot);
			log.info("Doctor slot successfully, doctorId={}, date={}", doctorId, date);
			Response response = new Response();
			response.setSuccess(true);
			response.setData(doctorSlot);
			response.setMessage("Slot updated successfully");
			response.setStatus(HttpStatus.OK.value());
			return response;

		} catch (Exception e) {
			log.error("Exception occured while updating doctor slot, doctorId={}, date={}", doctorId, date,
					e.getMessage());
			Response response = new Response();
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("An error occurred: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return response;

		}

	}

	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteDoctorSlotbyDateFallback")
	public Response deleteDoctorSlotbyDate(String doctorId, String date) {
		log.info("Delete doctor slots by date request received, doctorId={}, date={}", doctorId, date);
		try {
			DoctorSlot doctorSlot = slotRepository.findByDoctorIdAndDate(doctorId, date);

			if (doctorSlot == null) {
				log.warn("No slots found to delete | doctorId={}, date={}", doctorId, date);
				Response response = new Response();
				response.setSuccess(false);
				response.setData(null);
				response.setMessage("No slots found for doctor on this date");
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}

			slotRepository.delete(doctorSlot);
			log.info("Slots deleted successfully | doctorId={}, date={}", doctorId, date);
			Response response = new Response();
			response.setSuccess(true);
			response.setData(null);
			response.setMessage("All slots deleted successfully for date " + date);
			response.setStatus(HttpStatus.OK.value());
			return response;
		} catch (Exception e) {
			log.error("Exception occurred while deleting slots | doctorId={}, date={}", doctorId, date, e);

			Response response = new Response();
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("An error occurred while deleting slots");
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return response;
		}
	}

	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteDoctorSlotbyDateFallback")
	public Response deleteDoctorSlotbyDate(String doctorId, String branchId, String date) {
		log.info("Delete doctor slot by branch and date request received | doctorId={}, branchId={}, date={}", doctorId,
				branchId, date);
		Response response = new Response();

		try {
			// Fetch doctor slot by doctorId, branchId, and date
			log.debug("fetching doctor slots from database | doctorId={}, branchId={}, date={}", doctorId, branchId,
					date);
			DoctorSlot doctorSlot = slotRepository.findByDoctorIdAndBranchIdAndDate(doctorId, branchId, date);

			if (doctorSlot == null) {
				log.warn("No slots found for doctorId={}, branchId={}, date={}", doctorId, branchId, date);
				response.setSuccess(false);
				response.setData(null);
				response.setMessage("No slots found for the doctor in this branch on the given date");
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}

			List<DoctorAvailableSlotDTO> allSlots = doctorSlot.getAvailableSlots();
			log.debug("Total slots found: {}", allSlots.size());
			// Keep only booked slots
			List<DoctorAvailableSlotDTO> bookedSlots = allSlots.stream().filter(DoctorAvailableSlotDTO::isSlotbooked) // retain
																														// only
																														// booked
																														// ones
					.collect(Collectors.toList());
			log.debug("Total booked slots count={}, Unbooked slots count={}", bookedSlots.size(),
					allSlots.size() - bookedSlots.size());

			if (bookedSlots.isEmpty()) {
				// If no booked slots exist, delete the entire slot document
				log.info("No booked slots found, deleting entire document | doctorId={}, branchId={}, date={}",
						doctorId, branchId, date);
				slotRepository.delete(doctorSlot);
				response.setSuccess(true);
				response.setData(null);
				response.setMessage("All unbooked slots deleted successfully (no booked slots found).");
				response.setStatus(HttpStatus.OK.value());
				return response;
			}

			// Update the document to retain only booked slots
			log.info("Deleting unbooked slots and retains booked slots | doctorId={}, branchId={}, date={}", doctorId,
					branchId, date);
			doctorSlot.setAvailableSlots(bookedSlots);
			slotRepository.save(doctorSlot);

			response.setSuccess(true);
			response.setData(bookedSlots);
			response.setMessage("Unbooked slots deleted successfully, booked slots retained.");
			response.setStatus(HttpStatus.OK.value());
		} catch (Exception e) {
			log.error("Exception occured while delete doctor slot by branch and date Exception={}", e.getMessage());
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Internal server error occurred: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		log.info("Delete doctor slot by branch and date request completed | status={}", response.getStatus());
		return response;
	}
	
	 @Secured({"ROLE_CLINICADMIN","ROLE_CUSTOMER"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateSlotFallback")
	public boolean updateSlot(String doctorId, String branchId, String date, String time) {
		log.info("Update slot request | doctorId={}, branchId={}, date={}, time={}", doctorId, branchId, date, time);
		if (doctorId == null || date == null || time == null) {
			log.warn("Invalid input received while updating slot");
			return false;
		}
		try {
			// Fetch doctor slots from repository
			log.debug("Fetching doctor slots from DB");
			DoctorSlot doctorSlots = slotRepository.findByDoctorIdAndDateAndBranchId(doctorId, date, branchId);

			if (doctorSlots == null || doctorSlots.getAvailableSlots() == null
					|| doctorSlots.getAvailableSlots().isEmpty()) {
				log.warn("No slots found for doctorId={}, branchId={}, date={}", doctorId, branchId, date);
				return false;
			}
			// Find the slot that matches the time
			Optional<DoctorAvailableSlotDTO> matchingSlotOpt = doctorSlots.getAvailableSlots().stream()
					.filter(slot -> time.equalsIgnoreCase(slot.getSlot())).findFirst();
			if (matchingSlotOpt.isPresent()) {
				DoctorAvailableSlotDTO matchingSlot = matchingSlotOpt.get();

				// Check if slot already booked
				if (matchingSlot.isSlotbooked()) {
					log.warn("Slot already booked | doctorId={}, date={}, time={}", doctorId, date, time);
					return false;
				}
				// Mark the slot as booked
				matchingSlot.setSlotbooked(true);
				slotRepository.save(doctorSlots);
				log.info("Slot successfully booked | doctorId={}, branchId={}, date={}, time={}", doctorId, branchId,
						date, time);
				return true;
			} else {
				log.warn("Requested slot not found | doctorId={}, date={}, time={}", doctorId, date, time);
				return false;
			}
		} catch (Exception e) {
			log.error("Exception while booking slot | doctorId={}, branchId={}, date={}, time={}", doctorId, branchId,
					date, time, e);
			return false;
		}
	}
	
	 @Secured({"ROLE_CLINICADMIN","ROLE_CUSTOMER"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "makingFalseDoctorSlotFallback")
	public boolean makingFalseDoctorSlot(String doctorId, String branchId, String date, String time) {
		log.info("Unbook slot request | doctorId={}, branchId={}, date={}, time={}", doctorId, branchId, date, time);
		if (doctorId == null || date == null || time == null) {
			log.warn("Invalid input received while unbooking slot");
			return false;
		}

		try {
			log.debug("Fetching doctor slots from DB");
			DoctorSlot doctorSlots = slotRepository.findByDoctorIdAndDateAndBranchId(doctorId, date, branchId);

			if (doctorSlots == null || doctorSlots.getAvailableSlots() == null
					|| doctorSlots.getAvailableSlots().isEmpty()) {
				log.warn("No slots found to unbook | doctorId={}, branchId={}, date={}", doctorId, branchId, date);
				return false;
			}

			Optional<DoctorAvailableSlotDTO> matchingSlot = doctorSlots.getAvailableSlots().stream()
					.filter(slot -> time.equalsIgnoreCase(slot.getSlot())).findFirst();

			if (matchingSlot.isPresent()) {
				DoctorAvailableSlotDTO slot = matchingSlot.get();
				if (slot.isSlotbooked()) {
					slot.setSlotbooked(false);
					slotRepository.save(doctorSlots);
					log.info("Slot successfully unbooked | doctorId={}, branchId={}, date={}, time={}", doctorId,
							branchId, date, time);
				}
				return true;
			}
			log.warn("Requested slot not found for unbooking | doctorId={}, date={}, time={}", doctorId, date, time);
			return false;

		} catch (Exception e) {
			log.error("Exception occured while unbooking slot | doctorId={}, branchId={}, date={}, time={}", doctorId,
					branchId, date, time, e);
			return false;
		}
	}
	 
	 
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "saveDoctorSlotFallback")
	public Response saveDoctorSlot(String hospitalId, String branchId, String doctorId, DoctorSlotDTO dto) {
		log.info("Saved doctor slot called, hospitalId={}, branchId={}, date={}", hospitalId, branchId, doctorId);
		Response response = new Response();

		try {
			if (dto == null || dto.getAvailableSlots() == null || dto.getAvailableSlots().isEmpty()) {
				log.warn("Invalid slot details, doctorId={}, dto={}", doctorId, dto);
				throw new IllegalArgumentException("Invalid slot details provided");
			}

			Optional<Doctors> getDoctor = doctorsRepository.findByDoctorId(doctorId);
			if (getDoctor.isEmpty()) {
				log.warn("Doctor not found with ID: " + doctorId);
				response.setSuccess(false);
				response.setMessage("Doctor not found with ID: " + doctorId);
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return response;
			}
			log.debug("Doctor found | doctorId={}", doctorId);
			// ✅ Fetch ALL slots of doctor on the same date (across all branches)
			List<DoctorSlot> doctorSlotsOnDate = slotRepository.findAllByDoctorIdAndDate(doctorId, dto.getDate());
			log.debug("Existing slots found for date {}:{}", dto.getDate(), doctorSlotsOnDate.size());
			// ✅ Prepare slots with availability info
			List<DoctorAvailableSlotDTO> slotsWithAvailability = dto.getAvailableSlots().stream().map(incomingSlot -> {
				Optional<DoctorSlot> conflictingSlot = doctorSlotsOnDate.stream().filter(slot -> slot
						.getAvailableSlots().stream().anyMatch(s -> s.getSlot().equals(incomingSlot.getSlot())))
						.findFirst();

				if (conflictingSlot.isPresent()) {
					String existingBranchName = conflictingSlot.get().getBranchName();
					incomingSlot.setAvailable(false);
					incomingSlot.setReason("Already exists in " + existingBranchName + " Branch");
					log.info("Slot conflict | doctorId={}, slot={}, branch={}", doctorId, incomingSlot.getSlot(),
							existingBranchName);
				} else {
					incomingSlot.setAvailable(true);
					incomingSlot.setReason(null);
					log.debug("Slot available | doctorId={}, slot={}", doctorId, incomingSlot.getSlot());
				}

				return incomingSlot;
			})
					// ✅ Sort after mapping
					.sorted(Comparator.comparing(slot -> {
						DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive()
								.appendPattern("h:mm a").toFormatter(Locale.ENGLISH);

						return LocalTime.parse(normalizeTime(slot.getSlot()), formatter);
					})).toList();

			// ✅ Filter only slots that are available to save in this branch
			List<DoctorAvailableSlotDTO> slotsToSave = slotsWithAvailability.stream()
					.filter(DoctorAvailableSlotDTO::isAvailable).toList();
			log.info("Slots requested={}, slots eligible for save={}", slotsWithAvailability.size(),
					slotsToSave.size());

			DoctorSlot savedSlot = null;

			if (!slotsToSave.isEmpty()) {
				// Check if doctor already has slots in this branch for the same date
				DoctorSlot existingSlot = slotRepository.findByDoctorIdAndBranchIdAndDate(doctorId, branchId,
						dto.getDate());
				if (existingSlot != null) {
					log.info("Updating existing slots | doctorId={}, branchId={}, date={}", doctorId, branchId,
							dto.getDate());
					List<DoctorAvailableSlotDTO> currentSlots = existingSlot.getAvailableSlots();

					// Add only new unique slots
					List<DoctorAvailableSlotDTO> newUniqueSlots = slotsToSave.stream().filter(incoming -> currentSlots
							.stream().noneMatch(existing -> existing.getSlot().equals(incoming.getSlot()))).toList();
					log.debug("New unique slots count={}", newUniqueSlots.size());
					currentSlots.addAll(newUniqueSlots);
					existingSlot.setAvailableSlots(currentSlots);
					savedSlot = slotRepository.save(existingSlot);
				} else {
					log.info("Creating new slot entry | doctorId={}, branchId={}, date={}", doctorId, branchId,
							dto.getDate());
					DoctorSlot newSlot = DoctorSlotMapper.doctorSlotDTOtoEntity(dto);

					// ✅ Fetch branch details for saving (only once)
					Response branchResponse = adminServiceClient.getBranchById(keyCloakTokenStore.getAccess_token(),branchId);
					Branch branchDetails = objectMapper.convertValue(branchResponse.getData(), Branch.class);

					newSlot.setDoctorId(doctorId);
					newSlot.setHospitalId(hospitalId);
					newSlot.setBranchId(branchId);
					if (branchDetails != null) {
						newSlot.setBranchName(branchDetails.getBranchName()); // ✅ Store branch name in DB
					}
					newSlot.setAvailableSlots(slotsToSave);
					savedSlot = slotRepository.save(newSlot);
				}
				log.info("Slots saved successfully | slotId={}", savedSlot != null ? savedSlot.getId() : null);
			}

			response.setSuccess(true);
			response.setData(slotsWithAvailability);
			response.setMessage("Slots processed successfully. Unavailable slots are flagged with branch info.");
			response.setStatus(HttpStatus.OK.value());

		} catch (IllegalArgumentException e) {
			log.error("Validation error | doctorId={} | message={}", doctorId, e.getMessage());
			response.setSuccess(false);
			response.setMessage("Validation Error: " + e.getMessage());
			response.setStatus(HttpStatus.BAD_REQUEST.value());

		} catch (Exception e) {
			log.error("Exception while saving slots | doctorId={}, branchId={}, error={}", doctorId, branchId,
					e.getMessage(), e);
			response.setSuccess(false);
			response.setMessage("An error occurred while saving slots: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		log.info("SaveDoctorSlot completed | doctorId={}, branchId={}", doctorId, branchId);
		return response;
	}

	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "generateDoctorSlotsFallback")
	public Response generateDoctorSlots(String doctorId, String branchId, String date, int intervalMinutes,
			String openingTime, String closingTime) {

		Response response = new Response();

		try {
			// Normalize times
			openingTime = normalizeTime(URLDecoder.decode(openingTime, StandardCharsets.UTF_8));
			closingTime = normalizeTime(URLDecoder.decode(closingTime, StandardCharsets.UTF_8));

			// Detect local timezone (default to Asia/Kolkata)
			ZoneId zoneId = ZoneId.of("Asia/Kolkata"); // <-- change if your clinic is elsewhere
			ZonedDateTime nowZoned = ZonedDateTime.now(zoneId);
			LocalDate today = nowZoned.toLocalDate();
			LocalTime now = nowZoned.toLocalTime();

			// Generate slots
			List<DoctorAvailableSlotDTO> generatedSlots = generateSlots(openingTime, closingTime, intervalMinutes, date,
					zoneId);

			// Fetch existing slots
			List<DoctorSlot> doctorSlotsOnDate = slotRepository.findAllByDoctorIdAndDate(doctorId, date);

			// Flatten existing slots
			List<DoctorAvailableSlotDTO> existingSlots = doctorSlotsOnDate.stream()
					.flatMap(ds -> ds.getAvailableSlots().stream().map(s -> {
						DoctorAvailableSlotDTO dto = new DoctorAvailableSlotDTO();
						dto.setSlot(normalizeTime(s.getSlot()));
						dto.setAvailable(s.isAvailable());
						dto.setReason(ds.getBranchName());
						return dto;
					})).toList();

			DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mm a")
					.toFormatter(Locale.ENGLISH);

			LocalDate slotDate = LocalDate.parse(date);
			List<DoctorAvailableSlotDTO> finalSlots = new ArrayList<>();

			for (DoctorAvailableSlotDTO slot : generatedSlots) {
				LocalTime slotTime = LocalTime.parse(normalizeTime(slot.getSlot()), formatter);
				boolean available = slot.isAvailable();
				String reason = slot.getReason();

				// 🔹 Branch overlap
				if (available) {
					DoctorAvailableSlotDTO conflictSlot = existingSlots.stream()
							.filter(existing -> isOverlapping(slot.getSlot(), intervalMinutes, List.of(existing), 30))
							.findFirst().orElse(null);

					if (conflictSlot != null) {
						available = false;
						reason = "Already exists in " + conflictSlot.getReason() + " Branch";
					}
				}

				// 🔹 Date/time checks (timezone aware)
				if (slotDate.isBefore(today)) {
					available = false;
					reason = "Date already passed";
				} else if (slotDate.equals(today) && slotTime.isBefore(now)) {
					available = false;
					reason = "Time already passed";
				}

				slot.setAvailable(available);
				slot.setReason(reason);
				finalSlots.add(slot);
			}

			// ✅ Logging
			System.out.println("Final generated slots:");
			finalSlots.forEach(s -> System.out
					.println(s.getSlot() + " | Available: " + s.isAvailable() + " | Reason: " + s.getReason()));

			long unavailableCount = finalSlots.stream().filter(s -> !s.isAvailable()).count();

			response.setSuccess(true);
			response.setData(finalSlots);
			response.setMessage("Slots generated successfully. " + unavailableCount
					+ " slot(s) are unavailable due to branch conflicts or past time.");
			response.setStatus(200);

		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error generating slots: " + e.getMessage());
			response.setStatus(500);
		}

		return response;
	}

	// ---------------- Helper Methods ----------------

	private List<DoctorAvailableSlotDTO> generateSlots(String openingTime, String closingTime, int intervalMinutes,
			String date, ZoneId zoneId) {
		DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mm a")
				.toFormatter(Locale.ENGLISH);

		LocalTime start;
		LocalTime end;

		try {
			start = LocalTime.parse(openingTime.trim().toUpperCase(), formatter);
			end = LocalTime.parse(closingTime.trim().toUpperCase(), formatter);
		} catch (DateTimeParseException e) {
			throw new RuntimeException("Failed to parse time: " + e.getParsedString(), e);
		}

		List<DoctorAvailableSlotDTO> slots = new ArrayList<>();
		LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();
		LocalTime now = ZonedDateTime.now(zoneId).toLocalTime();
		LocalDate slotDate = LocalDate.parse(date);

		// If selected date is before today — no slots at all
		if (slotDate.isBefore(today)) {
			return slots;
		}

		while (!start.isAfter(end.minusMinutes(intervalMinutes))) {

			// ⏰ Skip past slots for today's date
			if (slotDate.equals(today) && start.isBefore(now)) {
				start = start.plusMinutes(intervalMinutes);
				continue;
			}

			DoctorAvailableSlotDTO slot = new DoctorAvailableSlotDTO();
			slot.setSlot(start.format(formatter));
			slot.setSlotbooked(false);
			slot.setAvailable(true);
			slot.setReason(null);

			slots.add(slot);
			start = start.plusMinutes(intervalMinutes);
		}

		return slots;
	}

	private String normalizeTime(String time) {
		time = time.trim().replaceAll("\\s+", " ").toUpperCase();
		time = time.replaceAll("(?<=\\d)(AM|PM)", " $1");
		return time;
	}

	private boolean isOverlapping(String newSlot, int newInterval, List<DoctorAvailableSlotDTO> existingSlots,
			int existingInterval) {
		DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mm a")
				.toFormatter(Locale.ENGLISH);

		newSlot = normalizeTime(newSlot);
		LocalTime newStart = LocalTime.parse(newSlot, formatter);
		LocalTime newEnd = newStart.plusMinutes(newInterval);

		for (DoctorAvailableSlotDTO existing : existingSlots) {
			if (existing.getSlot() == null)
				continue;

			String existingSlotStr = normalizeTime(existing.getSlot());
			LocalTime existStart = LocalTime.parse(existingSlotStr, formatter);

			int effectiveInterval = existingInterval > 0 ? existingInterval : newInterval;
			LocalTime existEnd = existStart.plusMinutes(effectiveInterval);

			boolean overlaps = newStart.isBefore(existEnd) && newEnd.isAfter(existStart);
			if (overlaps) {
				return true;
			}
		}
		return false;
	}

	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR","ROLE_CUSTOMER"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getDoctorSlotsFallback")
	public Response getDoctorSlots(String hospitalId, String branchId, String doctorId) {
		List<DoctorSlot> slots = slotRepository.findByHospitalIdAndBranchIdAndDoctorId(hospitalId, branchId, doctorId);

		Response response = new Response();
		if (slots == null || slots.isEmpty()) {
			response.setSuccess(true);
			response.setData(null);
			response.setMessage("Slots Not Found");
			response.setStatus(HttpStatus.OK.value());
			return response;
		}

		// 🔹 Time processing
		ZoneId zoneId = ZoneId.of("Asia/Kolkata");
		LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();
		LocalTime now = ZonedDateTime.now(zoneId).toLocalTime();

		DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mm a")
				.toFormatter(Locale.ENGLISH);

		// Update availability based on time and booking status
		for (DoctorSlot slotEntity : slots) {
			LocalDate slotDate = LocalDate.parse(slotEntity.getDate());
			for (DoctorAvailableSlotDTO slot : slotEntity.getAvailableSlots()) {
				LocalTime slotTime = LocalTime.parse(normalizeTime(slot.getSlot()), formatter);

				if (slot.isSlotbooked()) {
					slot.setAvailable(false);
					slot.setReason("Already booked");
				} else if (slotDate.isBefore(today)) {
					slot.setAvailable(false);
					slot.setReason("Date already passed");
				} else if (slotDate.equals(today) && slotTime.isBefore(now)) {
					slot.setAvailable(false);
					slot.setReason("Time already passed");
				} else {
					slot.setAvailable(true);
					slot.setReason(null);
				}
			}
		}

		response.setSuccess(true);
		response.setData(slots);
		response.setMessage("Slots fetched successfully");
		response.setStatus(HttpStatus.OK.value());
		return response;
	}

	// -------------------Simplified
	// Mapper----------------------------------------------
	private ClinicWithDoctorsDTO mapToClinicWithDoctorsDTO(ClinicDTO clinic, List<Doctors> doctorList) {
		// ✅ Directly copy clinic fields
		ClinicWithDoctorsDTO dto = objectMapper.convertValue(clinic, ClinicWithDoctorsDTO.class);

		// ✅ Convert doctors with consultation mapping
		List<DoctorsDTO> doctorDTOs = doctorList.stream().map(doc -> {
			DoctorsDTO doctorDTO = DoctorMapper.mapDoctorEntityToDoctorDTO(doc,s3Service);

//			if (doc.getConsultation() != null) {
//				ConsultationType consultation = doc.getConsultation();
//				ConsultationTypeDTO consultationDTO = new ConsultationTypeDTO();
//				consultationDTO.setServiceAndTreatments(consultation.getServiceAndTreatments());
//				consultationDTO.setInClinic(consultation.getInClinic());
//				consultationDTO.setVideoOrOnline(consultation.getVideoOrOnline());
//				doctorDTO.setConsultation(consultationDTO);
//			} else {
//				doctorDTO.setConsultation(null);
//			}

			return doctorDTO;
		}).collect(Collectors.toList());

		dto.setDoctors(doctorDTOs);
		return dto;
	}
	

	/// NOTIFICATIONOFDOCTOR
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "notificationToClinicFallback")
	public ResponseEntity<?> notificationToClinic(String hospitalId) {
		try {
			return notificationFeign.sendNotificationToClinic(keyCloakTokenStore.getAccess_token(),hospitalId);
		} catch (FeignException e) {
			ResBody<List<String>> res = new ResBody<List<String>>(ExtractFeignMessage.clearMessage(e), e.status(),
					null);
			return ResponseEntity.status(e.status()).body(res);
		}
	}

	// -----------------------------GET CLINICS AND DOCTORS BY RECOMMENDATION ==
	// TRUE---------------------------------
	@Override
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getRecommendedClinicsAndDoctorsFallback")
	public Response getRecommendedClinicsAndDoctors() {
		Response finalResponse = new Response();

		try {
			Response responseEntity = adminServiceClient.getHospitalUsingRecommendentaion(keyCloakTokenStore.getAccess_token());
			Response responseBody = responseEntity;

			List<ClinicWithDoctorsDTO> result = new ArrayList<>();

			if (responseBody != null && responseBody.isSuccess()) {
				Object rawData = responseBody.getData();

				// Convert raw JSON -> List<ClinicDTO>
				List<ClinicDTO> clinics = objectMapper.convertValue(rawData, new TypeReference<List<ClinicDTO>>() {
				});

				for (ClinicDTO clinicDTO : clinics) {
					// Map ClinicDTO -> ClinicWithDoctorsDTO
					ClinicWithDoctorsDTO clinic = objectMapper.convertValue(clinicDTO, ClinicWithDoctorsDTO.class);

					// Fetch doctors from DB
					List<Doctors> doctorEntities = doctorsRepository.findByHospitalId(clinic.getHospitalId());

					// Convert doctors
					List<DoctorsDTO> doctors = doctorEntities.stream().map(doc -> {
						DoctorsDTO dto = DoctorMapper.mapDoctorEntityToDoctorDTO(doc,s3Service);

						// doctorFees mapping
						if (doc.getDoctorFees() != null) {
							dto.setDoctorFees(DoctorMapper.mapDoctorFeeEntityToDTO(doc.getDoctorFees()));
						}

						if (doc.getDoctorSignature() != null && !doc.getDoctorSignature().isBlank())
						    dto.setDoctorSignature(doc.getDoctorSignature()); // S3 key passed through as-is

						return dto;
					}).collect(Collectors.toList());

					clinic.setDoctors(doctors);
					result.add(clinic);
				}
			}

			finalResponse.setSuccess(true);
			finalResponse.setStatus(HttpStatus.OK.value());
			finalResponse.setMessage("Recommended clinics with doctors retrieved successfully.");
			finalResponse.setData(result);

		} catch (Exception e) {
			log.error("Error fetching recommended clinics and doctors: {}", e.getMessage(), e);
			finalResponse.setSuccess(false);
			finalResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			finalResponse.setMessage("Internal error: " + e.getMessage());
		}

		return finalResponse;
	}


	@Override
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getRecommendedClinicsAndOneDoctorsFallback")
	public Response getRecommendedClinicsAndOneDoctors(List<String> keyPointsFromUser) {
		Logger log = LoggerFactory.getLogger(getClass());

		Response responseEntity = adminServiceClient.getHospitalUsingRecommendentaion(keyCloakTokenStore.getAccess_token());
		Response responseBody = responseEntity;

		List<ClinicWithDoctorsDTO> result = new ArrayList<>();
		boolean anyDoctorMatched = false;

		if (responseBody != null && responseBody.isSuccess()) {
			Object rawData = responseBody.getData();
			log.info("Raw clinic data from Feign: {}", rawData);

			List<ClinicWithDoctorsDTO> clinics = new ObjectMapper().convertValue(rawData,
					new TypeReference<List<ClinicWithDoctorsDTO>>() {
					});

			log.info("Converted clinic list size: {}", clinics.size());

			for (ClinicWithDoctorsDTO clinic : clinics) {
				log.info("Processing clinic: {} | ID: {}", clinic.getName(), clinic.getHospitalId());

				if (clinic.getHospitalId() == null) {
					log.warn("Clinic missing hospitalId, skipping...");
					continue;
				}

				List<Doctors> doctorEntities = doctorsRepository.findByHospitalId(clinic.getHospitalId());
				log.info("Doctors found for clinic {}: {}", clinic.getHospitalId(), doctorEntities.size());

				List<DoctorsDTO> matchedDoctors = new ArrayList<>();

				for (Doctors doctor : doctorEntities) {
					DoctorsDTO dto = DoctorMapper.mapDoctorEntityToDoctorDTO(doctor,s3Service);
					boolean relevant = isDoctorRelevant(dto, keyPointsFromUser);
					log.info("Doctor: {} | Relevant: {}", dto.getDoctorName(), relevant);

					if (relevant) {
						matchedDoctors.add(dto);
						anyDoctorMatched = true;
					}
				}

				clinic.setDoctors(matchedDoctors);
				result.add(clinic);
			}

			// Step 3: If no doctor matched, return all clinics with all doctors
			if (!anyDoctorMatched) {
				log.info("No doctor matched. Returning all clinics and doctors.");

				result.clear(); // Reset result

				for (ClinicWithDoctorsDTO clinic : clinics) {
					if (clinic.getHospitalId() == null)
						continue;

					List<Doctors> doctorEntities = doctorsRepository.findByHospitalId(clinic.getHospitalId());
					List<DoctorsDTO> allDoctors = doctorEntities.stream()
					        .map(doc -> DoctorMapper.mapDoctorEntityToDoctorDTO(doc, s3Service))
					        .toList();
					clinic.setDoctors(allDoctors);
					result.add(clinic);
				}
			}

		} else {
			log.warn("Feign response unsuccessful or null");
		}

		return Response.builder().success(true).status(HttpStatus.OK.value()).data(result)
				.message("Matched clinics and doctors").build();
	}

	private boolean isDoctorRelevant(DoctorsDTO doctor, List<String> keyPoints) {
		Logger logger = LoggerFactory.getLogger(getClass());
		if (keyPoints == null || keyPoints.isEmpty())
			return false;

		for (String key : keyPoints) {
			String lowerKey = key.toLowerCase();

			// Specialization
			if (doctor.getSpecialization() != null && doctor.getSpecialization().toLowerCase().contains(lowerKey)) {
				logger.debug("Matched specialization: {} with keyword: {}", doctor.getSpecialization(), key);
				return true;
			}
		}

		return true;
	}

//---------------- get All doctors with respective their clinics --------------------------
	@Override
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllDoctorsWithRespectiveClinicFallback")
	public Response getAllDoctorsWithRespectiveClinic() {
		Response response = new Response();

		try {
			// 1. Get list of clinics (recommended ones)
			Response clinicsResponse = adminServiceClient.firstRecommendedTureClincs();
			Object clinicObj = clinicsResponse.getData();

			// Convert to list of ClinicDTO
			List<ClinicDTO> clinics = objectMapper.convertValue(clinicObj, new TypeReference<List<ClinicDTO>>() {
			});

			List<ClinicWithDoctorsDTO> clinicsWithDoctors = clinics.stream().map(clinicDTO -> {
				// Fetch doctors by hospitalId
				List<Doctors> doctorsDbData = doctorsRepository.findByHospitalId(clinicDTO.getHospitalId());

				List<DoctorsDTO> doctorDTOs = doctorsDbData.stream()
				        .map(doc -> DoctorMapper.mapDoctorEntityToDoctorDTO(doc, s3Service))
				        .collect(Collectors.toList());

				// Map ClinicDTO -> ClinicWithDoctorsDTO
				ClinicWithDoctorsDTO clDTO = objectMapper.convertValue(clinicDTO, ClinicWithDoctorsDTO.class);

				// Set doctors list
				clDTO.setDoctors(doctorDTOs);

				return clDTO;
			}).collect(Collectors.toList());

			// 3. Wrap response
			response.setSuccess(true);
			response.setData(clinicsWithDoctors);
			response.setMessage("Fetched clinics with respective doctors");
			response.setStatus(HttpStatus.OK.value());

		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error fetching clinics and doctors: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		return response;
	}

	@Override
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllDoctorsWithRespectiveClinicFallback")
	public Response getAllDoctorsWithRespectiveClinic(int consultationType) {
		Response response = new Response();

		try {
			// 1. Get list of recommended clinics
			Response clinicsResponse = adminServiceClient.firstRecommendedTureClincs();
			Object clinicObj = clinicsResponse.getData();

			// Convert to list of ClinicDTO
			List<ClinicDTO> clinics = objectMapper.convertValue(clinicObj, new TypeReference<List<ClinicDTO>>() {
			});

			// 2. Map each clinic to its respective doctors filtered by consultation type
			List<ClinicWithDoctorsDTO> clinicsWithDoctors = clinics.stream().map(clinicDTO -> {
				// Fetch doctors by hospitalId
				List<Doctors> doctorsDbData = doctorsRepository.findByHospitalId(clinicDTO.getHospitalId());

//				// Convert to DTOs and filter based on consultation type
//				List<DoctorsDTO> doctorDTOs = doctorsDbData.stream().map(DoctorMapper::mapDoctorEntityToDoctorDTO)
//						.filter(dto -> {
//							ConsultationTypeDTO consultation = dto.getConsultation();
//							if (consultation == null)
//								return false;
//
//							switch (consultationType) {
//							case 1:
//								return consultation.getInClinic() == 1;
//							case 2:
//								return consultation.getVideoOrOnline() == 2;
//							case 3:
//								return consultation.getServiceAndTreatments() == 3;
//							default:
//								return false;
//							}
//						}).collect(Collectors.toList());

				// Map ClinicDTO to ClinicWithDoctorsDTO
				ClinicWithDoctorsDTO clDTO = objectMapper.convertValue(clinicDTO, ClinicWithDoctorsDTO.class);
//				clDTO.setDoctors(doctorDTOs);

				return clDTO;
			}).collect(Collectors.toList());

			// 3. Wrap response
			response.setSuccess(true);
			response.setData(clinicsWithDoctors);
			response.setMessage("Fetched clinics with respective doctors filtered by consultation type");
			response.setStatus(HttpStatus.OK.value());

		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error fetching clinics and doctors: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		return response;
	}

	@Override
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllDoctorsWithRespectiveClinicFallback")
	public Response getAllDoctorsWithRespectiveClinic(String hospitalId, int consultationType) {
		Response response = new Response();

		try {
			// 1. Get list of recommended clinics
			Response clinicsResponse = adminServiceClient.firstRecommendedTureClincs();
			Object clinicObj = clinicsResponse.getData();

			// Convert to list of ClinicDTO
			List<ClinicDTO> clinics = objectMapper.convertValue(clinicObj, new TypeReference<List<ClinicDTO>>() {
			});

			// 2. Filter clinics by hospitalId
			List<ClinicDTO> filteredClinics = clinics.stream()
					.filter(clinic -> clinic.getHospitalId().equals(hospitalId)).collect(Collectors.toList());

			// 3. Map each clinic to its respective doctors filtered by consultation type
			List<ClinicWithDoctorsDTO> clinicsWithDoctors = filteredClinics.stream().map(clinicDTO -> {
				// Fetch doctors by hospitalId
				List<Doctors> doctorsDbData = doctorsRepository.findByHospitalId(clinicDTO.getHospitalId());

//				// Convert to DTOs and filter based on consultation type
//				List<DoctorsDTO> doctorDTOs = doctorsDbData.stream().map(DoctorMapper::mapDoctorEntityToDoctorDTO)
//						.filter(dto -> {
//							ConsultationTypeDTO consultation = dto.getConsultation();
//							if (consultation == null)
//								return false;
//
//							switch (consultationType) {
//							case 1:
//								return consultation.getInClinic() == 1;
//							case 2:
//								return consultation.getVideoOrOnline() == 2;
//							case 3:
//								return consultation.getServiceAndTreatments() == 3;
//							default:
//								return false;
//							}
//						}).collect(Collectors.toList());

				// Map ClinicDTO to ClinicWithDoctorsDTO
				ClinicWithDoctorsDTO clDTO = objectMapper.convertValue(clinicDTO, ClinicWithDoctorsDTO.class);
//				clDTO.setDoctors(doctorDTOs);

				return clDTO;
			}).collect(Collectors.toList());

			// 4. Wrap response
			response.setSuccess(true);
			response.setData(clinicsWithDoctors);
			response.setMessage("Fetched clinics with respective doctors filtered by hospitalId and consultation type");
			response.setStatus(HttpStatus.OK.value());

		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error fetching clinics and doctors: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

		return response;
	}

	
	@Override
	public String getByTherapistDeviceId(String therapistId) {
		try {
			Optional<DoctorLoginCredentials> credentialsOpt = credentialsRepository.findByUsername(therapistId);

			if (credentialsOpt.isEmpty()) {				
				return null;
			}else {
				return credentialsOpt.get().getDeviceId();
			}
		}catch(Exception e) {return null;}
	}


//-----------------------best one doctor using key word-------------------------------------------
	@Override
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getRecommendedClinicsAndDoctorsFallback")
	public Response getRecommendedClinicsAndDoctors(List<String> keyPointsFromUser) {
		Logger log = LoggerFactory.getLogger(getClass());

		Response responseEntity = adminServiceClient.getHospitalUsingRecommendentaion(keyCloakTokenStore.getAccess_token());
		Response responseBody = responseEntity;

		ClinicWithDoctorsDTO bestClinic = null;
		DoctorsDTO bestDoctor = null;
		int bestScore = 0;

		if (responseBody != null && responseBody.isSuccess()) {
			Object rawData = responseBody.getData();
			List<ClinicWithDoctorsDTO> clinics = new ObjectMapper().convertValue(rawData,
					new TypeReference<List<ClinicWithDoctorsDTO>>() {
					});

			for (ClinicWithDoctorsDTO clinic : clinics) {
				if (clinic.getHospitalId() == null)
					continue;

				List<Doctors> doctorEntities = doctorsRepository.findByHospitalId(clinic.getHospitalId());

				for (Doctors doctor : doctorEntities) {
					DoctorsDTO dto = DoctorMapper.mapDoctorEntityToDoctorDTO(doctor,s3Service);
					int score = calculateDoctorScore(dto, keyPointsFromUser);

					log.info("Doctor: {} | Score: {}", dto.getDoctorName(), score);

					if (score > bestScore) {
						bestScore = score;
						bestDoctor = dto;
						bestClinic = clinic;
					}
				}
			}
		}

		if (bestDoctor != null && bestClinic != null) {
			bestClinic.setDoctors(List.of(bestDoctor));
			return Response.builder().success(true).status(HttpStatus.OK.value()).data(bestClinic)
					.message("Best doctor recommendation based on keywords, ratings, experience, and qualifications")
					.build();
		}

		return Response.builder().success(false).status(HttpStatus.NOT_FOUND.value())
				.message("No matching doctor found").build();
	}

	private int calculateDoctorScore(DoctorsDTO doctor, List<String> keyPoints) {
		int score = 0;

		// Keyword match score
		if (keyPoints != null && !keyPoints.isEmpty()) {
			for (String key : keyPoints) {
				String lowerKey = key.toLowerCase();

//				if (doctor.getSubServices() != null) {
//					for (DoctorSubServiceDTO sub : doctor.getSubServices()) {
//						if (sub != null && sub.getSubServiceName() != null
//								&& sub.getSubServiceName().toLowerCase().contains(lowerKey)) {
//							score += 5; // weight for subService match
//						}
//					}
//				}

//				if (doctor.getService() != null) {
//					for (DoctorServicesDTO service : doctor.getService()) {
//						if (service != null && service.getServiceName() != null
//								&& service.getServiceName().toLowerCase().contains(lowerKey)) {
//							score += 4; // weight for service match
//						}
//					}
//				}
//
//				if (doctor.getCategory() != null) {
//					for (DoctorCategoryDTO category : doctor.getCategory()) {
//						if (category != null && category.getCategoryName() != null
//								&& category.getCategoryName().toLowerCase().contains(lowerKey)) {
//							score += 3; // weight for category match
//						}
//					}
//				}

				if (doctor.getSpecialization() != null && doctor.getSpecialization().toLowerCase().contains(lowerKey)) {
					score += 6; // specialization match gets higher weight
				}
			}
		}

		// 2️⃣ Rating (scale 0–5 → multiply by weight)
		score += (int) (doctor.getDoctorAverageRating() * 10);

		// 3️⃣ Experience (convert years string to int if possible)
		try {
			int years = Integer.parseInt(doctor.getExperience().replaceAll("[^0-9]", ""));
			score += years * 2; // each year of experience adds 2 points
		} catch (Exception e) {
			// ignore if parsing fails
		}

		// 4️⃣ Qualification priority
		if (doctor.getQualification() != null) {
			String q = doctor.getQualification().toLowerCase();
			if (q.contains("dm"))
				score += 30;
			else if (q.contains("md"))
				score += 20;
			else if (q.contains("ms"))
				score += 15;
			else if (q.contains("mbbs"))
				score += 10;
		}

		return score;
	}

	@Override
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getRecommendedClinicsAndDoctorsFallback")
	public Response getRecommendedClinicsAndDoctors(List<String> keyPointsFromUser, int consultationType) {
		Logger log = LoggerFactory.getLogger(getClass());

		Response responseEntity = adminServiceClient.getHospitalUsingRecommendentaion(keyCloakTokenStore.getAccess_token());
		Response responseBody = responseEntity;

		ClinicWithDoctorsDTO bestClinic = null;
		DoctorsDTO bestDoctor = null;
		int bestScore = 0;

		if (responseBody != null && responseBody.isSuccess()) {
			Object rawData = responseBody.getData();
			List<ClinicWithDoctorsDTO> clinics = new ObjectMapper().convertValue(rawData,
					new TypeReference<List<ClinicWithDoctorsDTO>>() {
					});

			for (ClinicWithDoctorsDTO clinic : clinics) {
				if (clinic.getHospitalId() == null)
					continue;

				List<Doctors> doctorEntities = doctorsRepository.findByHospitalId(clinic.getHospitalId());

				for (Doctors doctor : doctorEntities) {
					DoctorsDTO dto = DoctorMapper.mapDoctorEntityToDoctorDTO(doctor,s3Service);

//					// 🧠 Step 1: Filter based on consultation type (numeric)
//					if (!matchesConsultationType(dto.getConsultation(), consultationType)) {
//						continue;
//					}

					// 🧠 Step 2: Calculate doctor score
					int score = calculateDoctorScore(dto, keyPointsFromUser);
					log.info("Doctor: {} | Score: {}", dto.getDoctorName(), score);

					if (score > bestScore) {
						bestScore = score;
						bestDoctor = dto;
						bestClinic = clinic;
					}
				}
			}
		}

		if (bestDoctor != null && bestClinic != null) {
			bestClinic.setDoctors(List.of(bestDoctor));
			return Response.builder().success(true).status(HttpStatus.OK.value()).data(bestClinic).message(
					"Best doctor recommendation based on consultation type, keywords, ratings, and qualifications")
					.build();
		}

		return Response.builder().success(false).status(HttpStatus.NOT_FOUND.value())
				.message("No matching doctor found for the given consultation type").build();
	}

	/**
	 * ✅ Helper method to check numeric consultation type
	 */
	private boolean matchesConsultationType(ConsultationTypeDTO doctorConsultation, int consultationType) {
		if (doctorConsultation == null)
			return false;

		switch (consultationType) {
		case 1:
			return doctorConsultation.getInClinic() == 1;
		case 2:
			return doctorConsultation.getVideoOrOnline() == 2;
		case 3:
			return doctorConsultation.getServiceAndTreatments() == 3;
		default:
			return false;
		}
	}

	@Override
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getRecommendedClinicsAndDoctorsFallback")
	public Response getRecommendedClinicsAndDoctors(String hospitalId, List<String> keyPointsFromUser,
			int consultationType) {
		Logger log = LoggerFactory.getLogger(getClass());

		Response responseEntity = adminServiceClient.getHospitalUsingRecommendentaion(keyCloakTokenStore.getAccess_token());
		Response responseBody = responseEntity;

		ClinicWithDoctorsDTO bestClinic = null;
		DoctorsDTO bestDoctor = null;
		int bestScore = 0;

		if (responseBody != null && responseBody.isSuccess()) {
			Object rawData = responseBody.getData();
			List<ClinicWithDoctorsDTO> clinics = new ObjectMapper().convertValue(rawData,
					new TypeReference<List<ClinicWithDoctorsDTO>>() {
					});

			// 🔍 Filter only clinics that match the given hospitalId
			for (ClinicWithDoctorsDTO clinic : clinics) {
				if (clinic.getHospitalId() == null || !clinic.getHospitalId().equals(hospitalId))
					continue;

				// Fetch doctors only for this hospital
				List<Doctors> doctorEntities = doctorsRepository.findByHospitalId(hospitalId);

				for (Doctors doctor : doctorEntities) {
					DoctorsDTO dto = DoctorMapper.mapDoctorEntityToDoctorDTO(doctor,s3Service);

//					// ✅ Step 1: Filter by consultation type
//					if (!matchesConsultationType(dto.getConsultation(), consultationType)) {
//						continue;
//					}

					// ✅ Step 2: Score based on key points
					int score = calculateDoctorScore(dto, keyPointsFromUser);
					log.info("Doctor: {} | Score: {}", dto.getDoctorName(), score);

					if (score > bestScore) {
						bestScore = score;
						bestDoctor = dto;
						bestClinic = clinic;
					}
				}
			}
		}

		if (bestDoctor != null && bestClinic != null) {
			bestClinic.setDoctors(List.of(bestDoctor));
			return Response.builder().success(true).status(HttpStatus.OK.value()).data(bestClinic)
					.message("Best doctor recommendation for given hospital, consultation type, and keywords.").build();
		}

		return Response.builder().success(false).status(HttpStatus.NOT_FOUND.value())
				.message("No matching doctor found for the given hospital and consultation type.").build();
	}

	@Override
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getDoctorsByHospitalIdAndBranchIdFallback")
	public Response getDoctorsByHospitalIdAndBranchId(String hospitalId, String branchId) {
		Response response = new Response();
		try {
			// ✅ Fetch doctors assigned to this branch in their branches list
			List<Doctors> doctorList = doctorsRepository.findByHospitalIdAndBranchIdIncludingBranches(hospitalId,
					branchId);

			// Filter out doctors that are not actually assigned to the branch
			doctorList = doctorList.stream().filter(doc -> doc.getBranches() != null
					&& doc.getBranches().stream().anyMatch(b -> branchId.equals(b.getBranchId()))).toList();

			if (!doctorList.isEmpty()) {
				List<DoctorsDTO> dtos = doctorList.stream()
				        .map(doc -> DoctorMapper.mapDoctorEntityToDoctorDTO(doc, s3Service))
				        .toList();
				response.setSuccess(true);
				response.setData(dtos);
				response.setMessage(
						"Doctors fetched successfully for hospitalId: " + hospitalId + " and branchId: " + branchId);
				response.setStatus(HttpStatus.OK.value());
			} else {
				response.setSuccess(true);
				response.setData(Collections.emptyList());
				response.setMessage("No doctors found for hospitalId: " + hospitalId + " and branchId: " + branchId);
				response.setStatus(HttpStatus.OK.value());
			}
		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error fetching doctors: " + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return response;
	}
	
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "blockingSlotFallback")
	public boolean blockingSlot(TempBlockingSlot tempBlockingSlot) {
		// Validate input
		if (tempBlockingSlot == null || tempBlockingSlot.getDoctorId() == null
				|| tempBlockingSlot.getServiceDate() == null || tempBlockingSlot.getServicetime() == null) {
			return false;
		}
		try {
			// Fetch doctor slots for that date
			DoctorSlot doctorSlots = slotRepository.findByDoctorIdAndDateAndBranchId(tempBlockingSlot.getDoctorId(),
					tempBlockingSlot.getServiceDate(), tempBlockingSlot.getBranchId());
			if (doctorSlots == null || doctorSlots.getAvailableSlots() == null
					|| doctorSlots.getAvailableSlots().isEmpty()) {
				return false;
			}
			// Find matching slot by time
			Optional<DoctorAvailableSlotDTO> matchingSlotOpt = doctorSlots.getAvailableSlots().stream()
					.filter(slot -> tempBlockingSlot.getServicetime().equalsIgnoreCase(slot.getSlot())).findFirst();
			if (matchingSlotOpt.isPresent()) {
				DoctorAvailableSlotDTO matchingSlot = matchingSlotOpt.get();
				// Check if slot already booked
				if (matchingSlot.isSlotbooked()) {
					return true;
				} else {
					// Mark slot as booked
					matchingSlot.setSlotbooked(true);
					slotRepository.save(doctorSlots);
					tempBlockingSlot.setTimeInMillis(System.currentTimeMillis());
					slots.add(tempBlockingSlot);
					return true;
				} // Successfully blocked
			} else {
				return false;
			} // No matching slot found
		} catch (Exception e) {
			// Log error for debugging (important for production)
			System.err.println("Error while blocking slot: " + e.getMessage());
			return false;
		} finally {
			// Optional cleanup or logging
			System.out.println("Slot blocking process completed for doctor: " + tempBlockingSlot.getDoctorId());
		}
	}

	@Scheduled(fixedRate = 30000)
    //@RateLimiter(name = "doctorApi", fallbackMethod = "checkingSlotsFallback")
	public void checkingSlots() {
		try {
			long currentMillis = System.currentTimeMillis();
			// Filter only expired slots (diff >= 90 seconds)
			List<TempBlockingSlot> objectsToRemove = new CopyOnWriteArrayList<>();
			List<TempBlockingSlot> expiredSlots = slots.stream()
					.filter(n -> Math.abs(currentMillis - n.getTimeInMillis()) >= 90000).collect(Collectors.toList());
			expiredSlots.forEach(n -> {
				try {
					BookingResponse bkng = null;
					try {
						bkng = bookingFeign.blockingSlot(keyCloakTokenStore.getAccess_token(),n);
					} catch (Exception e) {
						System.err.println("Feign error: " + e.getMessage());
					}
					if (bkng == null) {
						DoctorSlot doctorSlots = slotRepository.findByDoctorIdAndDateAndBranchId(n.getDoctorId(),
								n.getServiceDate(), n.getBranchId());
						if (doctorSlots != null) {
							doctorSlots.getAvailableSlots().stream()
									.filter(slot -> slot.getSlot().equalsIgnoreCase(n.getServicetime()))
									.forEach(slot -> slot.setSlotbooked(false));

							slotRepository.save(doctorSlots);
							objectsToRemove.add(n);
						}
					} else {
						objectsToRemove.add(n);
					}
				} catch (Exception e) {
					System.err.println("Error processing slot: " + e.getMessage());
				}
			});
			slots.removeAll(objectsToRemove);
		} catch (Exception e) {
			System.err.println("Error in checkingSlots: " + e.getMessage());
		}
	}

// ================= AUTO GENERATED METHOD-SPECIFIC FALLBACKS =================

private Response buildRateLimitResponse(Exception ex){
    log.error("Rate limit exceeded", ex);
    return Response.builder()
            .success(false)
            .status(HttpStatus.TOO_MANY_REQUESTS.value())
            .message("Too many requests. Please try again later.")
            .build();
}

//================= RATE LIMITER FALLBACKS =================

public Response addDoctorFallback(DoctorsDTO dto, Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getAllDoctorsFallback(Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getDoctorsByClinicIdFallback(String hospitalId, Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getDoctorByIdFallback(String id, Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response upDateDoctorByIdFallback(String doctorId, DoctorsDTO dto, Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getDoctorsByClinicIdAndDoctorIdFallback(
        String clinicId,
        String doctorId,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response deleteDoctorByIdFallback(String doctorId, Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response deleteDoctorFromBranchFallback(
        String doctorId,
        String branchId,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response deleteDoctorsByClinicFallback(
        String hospitalId,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response changePasswordFallback(
        ChangeDoctorPasswordDTO updateDTO,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getDoctorsByClinicIdAndBranchIdFallback(
        String hospitalId,
        String branchId,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response saveDoctorSlotFallback(
        String hospitalId,
        String branchId,
        String doctorId,
        DoctorSlotDTO dto,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response saveDoctorSlotFallback(
        String hospitalId,
        String doctorId,
        DoctorSlotDTO dto,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response generateDoctorSlotsFallback(
        String doctorId,
        String branchId,
        String date,
        int intervalMinutes,
        String openingTime,
        String closingTime,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getDoctorSlotsFallback(
        String hospitalId,
        String branchId,
        String doctorId,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getDoctorSlotsFallback(
        String hospitalId,
        String doctorId,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response notificationToClinicFallback(
        String hospitalId,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getRecommendedClinicsAndDoctorsFallback(
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getRecommendedClinicsAndDoctorsFallback(
        List<String> keyPointsFromUser,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getRecommendedClinicsAndDoctorsFallback(
        List<String> keyPointsFromUser,
        int consultationType,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getRecommendedClinicsAndDoctorsFallback(
        String hospitalId,
        List<String> keyPointsFromUser,
        int consultationType,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getDoctorsByHospitalIdAndBranchIdFallback(
        String hospitalId,
        String branchId,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public boolean blockingSlotFallback(
        TempBlockingSlot tempBlockingSlot,
        Exception ex) {
    return false;
}

public Response getRecommendedClinicsAndOneDoctorsFallback(
        List<String> keyPointsFromUser,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getAllDoctorsWithRespectiveClinicFallback(
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getAllDoctorsWithRespectiveClinicFallback(
        int consultationType,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response getAllDoctorsWithRespectiveClinicFallback(
        String hospitalId,
        int consultationType,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response availabilityStatusFallback(
        String doctorId,
        DoctorAvailabilityStatusDTO status,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response deleteDoctorSlotFallback(
        String doctorId,
        String branchId,
        String date,
        String slotToDelete,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response deleteDoctorSlotFallback(
        String doctorId,
        String date,
        String slotToDelete,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response updateDoctorSlotFallback(
        String doctorId,
        String date,
        String oldSlot,
        String newSlot,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response deleteDoctorSlotbyDateFallback(
        String doctorId,
        String date,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public Response deleteDoctorSlotbyDateFallback(
        String doctorId,
        String branchId,
        String date,
        Exception ex) {
    return buildRateLimitResponse(ex);
}

public boolean updateSlotFallback(
        String doctorId,
        String branchId,
        String date,
        String time,
        Exception ex) {
	throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many requests. Please try again after some time.");
}

public boolean makingFalseDoctorSlotFallback(
        String doctorId,
        String branchId,
        String date,
        String time,
        Exception ex) {
	throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many requests. Please try again after some time.");
}


}