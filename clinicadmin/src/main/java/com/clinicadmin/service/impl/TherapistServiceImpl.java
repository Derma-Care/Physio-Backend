package com.clinicadmin.service.impl;


import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.Branch;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.TherapistDTO;
import com.clinicadmin.entity.DoctorLoginCredentials;
import com.clinicadmin.entity.Documents;
import com.clinicadmin.entity.FeedbackDetails;
import com.clinicadmin.entity.Session;
import com.clinicadmin.entity.Therapist;
import com.clinicadmin.entity.TherapistAttendance;
import com.clinicadmin.feignclient.AdminServiceClient;
import com.clinicadmin.feignclient.PhysiotherapyFeignClient;
import com.clinicadmin.repository.DoctorLoginCredentialsRepository;
import com.clinicadmin.repository.FeedbackDetailsRepository;
import com.clinicadmin.repository.TherapistAttendanceRepository;
import com.clinicadmin.repository.TherapistRepository;
import com.clinicadmin.service.EmailService;
import com.clinicadmin.service.TherapistService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TherapistServiceImpl implements TherapistService {

    @Autowired
    private TherapistRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    DoctorLoginCredentialsRepository credentialsRepository;
    
    @Autowired
    AdminServiceClient adminServiceClient;
    
    @Autowired
    ObjectMapper objectMapper;
    
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private PhysiotherapyFeignClient physiotherapyFeignClient;
	
	@Autowired
	private  FeedbackDetailsRepository feedbackDetailsRepository;
	
	@Autowired
	private TherapistAttendanceRepository therapistAttendanceRepository;
    @Override

    public Response therapistOnboarding(TherapistDTO dto) {

        log.info("Therapist onboarding started for contact number: {}", dto.getContactNumber());

        Response response = new Response();

        try {
//            dto.trimAllFields();

            // -------------------- Validate contact --------------------
            if (dto.getContactNumber() == null || dto.getContactNumber().trim().isEmpty()) {
                response.setSuccess(false);
                response.setMessage("Contact number is required");
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return response;
            }

            String contact = dto.getContactNumber().trim();

            
            // -------------------- Duplicate check --------------------
            if (repository.existsByContactNumber(contact)) {
                response.setSuccess(false);
                response.setMessage("Therapist already exists with this mobile number");
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return response;
            }

            if (credentialsRepository.existsByUsername(contact)) {
                response.setSuccess(false);
                response.setMessage("Login credentials already exist for this mobile number");
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return response;
            }

            // -------------------- Fetch branch --------------------
            ResponseEntity<Response> res = adminServiceClient.getBranchById(dto.getBranchId());
            Branch br = objectMapper.convertValue(res.getBody().getData(), Branch.class);

            // -------------------- Map DTO -> Entity --------------------
            Therapist therapist = mapToEntity(dto);
            therapist.setBranchName(br.getBranchName());

            // -------------------- Generate Therapist ID --------------------
            String therapistId = generateTherapistId();
            therapist.setTherapistId(therapistId);

            // -------------------- Save Therapist --------------------
            Therapist savedTherapist = repository.save(therapist);

            log.info("Therapist saved successfully with therapistId: {}", savedTherapist.getTherapistId());

            // -------------------- Create Credentials --------------------
            String username = savedTherapist.getTherapistId();
            String rawPassword = generatePassword();
            String encodedPassword = passwordEncoder.encode(rawPassword);

            DoctorLoginCredentials credentials = DoctorLoginCredentials.builder()
                    .staffId(savedTherapist.getTherapistId())
                    .staffName(savedTherapist.getFullName())
                    .hospitalId(savedTherapist.getClinicId())
                    .hospitalName(savedTherapist.getClinicName())
                    .branchId(savedTherapist.getBranchId())
                    .branchName(savedTherapist.getBranchName())
                    .emailId(savedTherapist.getEmailId())
                    .username(username)
                    .password(encodedPassword)
                    .role(dto.getRole())
                    .build();

            credentialsRepository.save(credentials);

            log.info("Login credentials created for therapistId: {}", savedTherapist.getTherapistId());

            // -------------------- Send Email (SAME TEMPLATE) --------------------
            try {
                Map<String, String> mailData = new HashMap<>();
                mailData.put("subject", "Therapist Onboarding Successful");
                mailData.put("message",
                        "Welcome to CCMS!\n\n" +
                        "Your account has been created successfully.\n" +
                        "Please use the below credentials to login.\n\n" +
                        "Therapist ID: " + savedTherapist.getTherapistId()
                );

                mailData.put("username", username);
                mailData.put("password", rawPassword);

                emailService.sendEmail(savedTherapist.getEmailId(), mailData);

                log.info("Therapist onboarding email sent to {}", savedTherapist.getEmailId());

            } catch (Exception e) {
                log.error("Failed to send therapist onboarding email: {}", e.getMessage());
            }

            // -------------------- Response --------------------
            TherapistDTO savedDTO = mapToDTO(savedTherapist);
            savedDTO.setUserName(username);
            savedDTO.setPassword(rawPassword);

            Map<String, Object> data = new HashMap<>();
            data.put("therapist", savedDTO);
            data.put("username", username);
            data.put("temporaryPassword", rawPassword);
            data.put("generatedTherapistId", therapistId);

            response.setSuccess(true);
            response.setData(data);
            response.setMessage("Therapist added successfully with login credentials");
            response.setStatus(HttpStatus.CREATED.value());

        } catch (Exception e) {
            log.error("Error occurred while onboarding therapist: {}", e.getMessage());
            response.setSuccess(false);
            response.setMessage("Error occurred while adding therapist: " + e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    
    }    // ================= LOGIN =================
//    @Override
//    public ResponseStructure<TherapistLoginResponseDTO> login(TherapistLoginDTO dto) {
//
//        Therapist user = repository.findByUserName(dto.getUserName())
//                .orElseThrow(() -> new RuntimeException("Invalid username"));
//
//        //  password check
//        if (!dto.getPassword().equals(user.getPassword())) {
//            return ResponseStructure.buildResponse(
//                    null,
//                    "Invalid password",
//                    HttpStatus.UNAUTHORIZED,
//                    401);
//        }
//
//        //  physioType check
//        if (dto.getGetPhysioType() != null && 
//            !dto.getGetPhysioType().equalsIgnoreCase(user.getPhysioType())) {
//
//            return ResponseStructure.buildResponse(
//                    null,
//                    "Invalid physio type",
//                    HttpStatus.UNAUTHORIZED,
//                    401);
//        }
//
//        //  build response DTO
//        TherapistLoginResponseDTO responseDTO = new TherapistLoginResponseDTO();
//        responseDTO.setTherapistId(user.getTherapistId());
//        responseDTO.setClinicId(user.getClinicId());
//        responseDTO.setBranchId(user.getBranchId());
//        responseDTO.setTherapistName(user.getFullName());
//        responseDTO.setPhysioType(user.getPhysioType());
//
//        return ResponseStructure.buildResponse(
//                responseDTO,
//                "Login Success",
//                HttpStatus.OK,
//                200);
//    }
    // ================= GET BY THERAPIST ID =================
    @Override
    public ResponseStructure<TherapistDTO> getBytherapistId(String therapistId) {

        Therapist entity = repository.findByTherapistId(therapistId)
                .orElseThrow(() -> new RuntimeException("Therapist not found"));

        return ResponseStructure.buildResponse(
                mapToDTO(entity),
                "Fetched successfully",
                HttpStatus.OK,
                200);
    }

    // ================= GET BY CLINICID BRANCHID AND THERPISTID =================
    @Override
    public ResponseStructure<List<TherapistDTO>> getByClinicIdBranchIdAndTherapistId(
            String clinicId,
            String branchId,
            String therapistId) {

        List<Therapist> list;

        if (therapistId != null && !therapistId.isBlank()) {
            Therapist t = repository.findByTherapistId(therapistId)
                    .orElseThrow(() -> new RuntimeException("Not found"));
            list = List.of(t);

        } else if (clinicId != null && branchId != null) {
            list = repository.findByClinicIdAndBranchId(clinicId, branchId);

        } else if (clinicId != null) {
            list = repository.findByClinicId(clinicId);

        } else {
            list = repository.findAll();
        }

        List<TherapistDTO> dtos = list.stream()
                .map(this::mapToDTO)
                .toList();

        return ResponseStructure.buildResponse(
                dtos,
                dtos.isEmpty() ? "No data found" : "Fetched successfully",
                HttpStatus.OK,
                200);
    }

    // ================= GET BY CLINICID AND BRANCHID =================
    @Override
    public ResponseStructure<List<TherapistDTO>> getByClinicIdAndBranchId(
            String clinicId,
            String branchId) {

        List<TherapistDTO> list = repository
                .findByClinicIdAndBranchId(clinicId, branchId)
                .stream()
                .map(this::mapToDTO)
                .toList();

        return ResponseStructure.buildResponse(
                list,
                "successfully fetched therapists",
                HttpStatus.OK,
                200);
    }

 // ================= UPDATE BY THERAPISTID=================
    @Override
    public ResponseStructure<TherapistDTO> updateBytherapistId(
            String therapistId,
            TherapistDTO dto) {

        Therapist existing = repository.findByTherapistId(therapistId)
                .orElseThrow(() -> new RuntimeException("Therapist not found"));

        //  Basic Info
        if (dto.getFullName() != null) existing.setFullName(dto.getFullName());
        if (dto.getContactNumber() != null) existing.setContactNumber(dto.getContactNumber());
        if (dto.getGender() != null) existing.setGender(dto.getGender());
        if (dto.getDateOfBirth() != null) existing.setDateOfBirth(dto.getDateOfBirth());

        //  Clinic Info
        if (dto.getClinicId() != null) existing.setClinicId(dto.getClinicId());
        if (dto.getBranchId() != null) existing.setBranchId(dto.getBranchId());

        //  Professional Info
        if (dto.getQualification() != null) existing.setQualification(dto.getQualification());
        if (dto.getYearsOfExperience() != null) existing.setYearsOfExperience(dto.getYearsOfExperience());

        //  Lists
        if (dto.getServices() != null) existing.setServices(dto.getServices());
        if (dto.getSpecializations() != null) existing.setSpecializations(dto.getSpecializations());
        if (dto.getExpertiseAreas() != null) existing.setExpertiseAreas(dto.getExpertiseAreas());
        if (dto.getTreatmentTypes() != null) existing.setTreatmentTypes(dto.getTreatmentTypes());

        if (dto.getAvailability() != null) existing.setAvailability(dto.getAvailability());

        if (dto.getBio() != null) existing.setBio(dto.getBio());

        // ================= BASE64 ENCODE =================
        if (dto.getDocuments() != null) {

            Documents docs = new Documents();

            if (dto.getDocuments().getLicenseCertificate() != null) {
                docs.setLicenseCertificate(
                        java.util.Base64.getEncoder().encodeToString(
                                dto.getDocuments().getLicenseCertificate().getBytes()
                        )
                );
            }

            if (dto.getDocuments().getDegreeCertificate() != null) {
                docs.setDegreeCertificate(
                        java.util.Base64.getEncoder().encodeToString(
                                dto.getDocuments().getDegreeCertificate().getBytes()
                        )
                );
            }

            if (dto.getDocuments().getProfilePhoto() != null) {
                docs.setProfilePhoto(
                        java.util.Base64.getEncoder().encodeToString(
                                dto.getDocuments().getProfilePhoto().getBytes()
                        )
                );
            }

            existing.setDocuments(docs);
        }

        if (dto.getLanguages() != null) existing.setLanguages(dto.getLanguages());

        if (dto.getRole() != null) existing.setRole(dto.getRole());
        if (dto.getPhysioType() != null) existing.setPhysioType(dto.getPhysioType());
        if (dto.getAadharID() != null) existing.setAadharID(dto.getAadharID());
        if (dto.getDateofJoining() != null) existing.setDateofJoining(dto.getDateofJoining());
        if (dto.getEmergencyContact() != null) existing.setEmergencyContact(dto.getEmergencyContact());

        //  Save
        Therapist updated = repository.save(existing);

        // ================= RESPONSE (DECODE BASE64) =================
        TherapistDTO response = new TherapistDTO();

        response.setTherapistId(updated.getTherapistId());
        response.setClinicId(updated.getClinicId());
        response.setBranchId(updated.getBranchId());
        response.setFullName(updated.getFullName());
        response.setContactNumber(updated.getContactNumber());
        response.setGender(updated.getGender());
        response.setDateOfBirth(updated.getDateOfBirth());
        response.setQualification(updated.getQualification());
        response.setYearsOfExperience(updated.getYearsOfExperience());
        response.setServices(updated.getServices());
        response.setSpecializations(updated.getSpecializations());
        response.setExpertiseAreas(updated.getExpertiseAreas());
        response.setTreatmentTypes(updated.getTreatmentTypes());
        response.setAvailability(updated.getAvailability());
        response.setBio(updated.getBio());

        //  Decode documents before sending
        if (updated.getDocuments() != null) {

            Documents docs = new Documents();

            if (updated.getDocuments().getLicenseCertificate() != null) {
                docs.setLicenseCertificate(
                        new String(java.util.Base64.getDecoder().decode(
                                updated.getDocuments().getLicenseCertificate()
                        ))
                );
            }

            if (updated.getDocuments().getDegreeCertificate() != null) {
                docs.setDegreeCertificate(
                        new String(java.util.Base64.getDecoder().decode(
                                updated.getDocuments().getDegreeCertificate()
                        ))
                );
            }

            if (updated.getDocuments().getProfilePhoto() != null) {
                docs.setProfilePhoto(
                        new String(java.util.Base64.getDecoder().decode(
                                updated.getDocuments().getProfilePhoto()
                        ))
                );
            }

            response.setDocuments(docs);
        }

        response.setLanguages(updated.getLanguages());
        response.setRole(updated.getRole());
        response.setPhysioType(updated.getPhysioType());

        return ResponseStructure.buildResponse(
                response,
                "Therapist updated successfully",
                HttpStatus.OK,
                200);
    }

    // ================= DELETEBY THERPIST ID =================
    @Override
    public ResponseStructure<String> deleteBytherapistId(String therapistId) {

        repository.deleteByTherapistId(therapistId);

        return ResponseStructure.buildResponse(
                therapistId,
                "Deleted successfully",
                HttpStatus.OK,
                200);
    }

    private Therapist mapToEntity(TherapistDTO dto) {

        Therapist entity = new Therapist();

        entity.setClinicId(dto.getClinicId());
        entity.setBranchId(dto.getBranchId());
        entity.setFullName(dto.getFullName());
        entity.setContactNumber(dto.getContactNumber());
        entity.setEmailId(dto.getEmailId());
        entity.setGender(dto.getGender());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setQualification(dto.getQualification());
        entity.setYearsOfExperience(dto.getYearsOfExperience());
        entity.setServices(dto.getServices());
        entity.setSpecializations(dto.getSpecializations());
        entity.setExpertiseAreas(dto.getExpertiseAreas());
        entity.setTreatmentTypes(dto.getTreatmentTypes());
        entity.setAvailability(dto.getAvailability());
        entity.setBio(dto.getBio());
        entity.setUserName(dto.getUserName());
        entity.setPassword(dto.getPassword());
        entity.setAadharID(dto.getAadharID());
        entity.setDateofJoining(dto.getDateofJoining());
        entity.setEmergencyContact(dto.getEmergencyContact());

        // ================= BASE64 ENCODE =================
        if (dto.getDocuments() != null) {

            Documents docs = new Documents();

            if (dto.getDocuments().getLicenseCertificate() != null) {
                docs.setLicenseCertificate(
                    java.util.Base64.getEncoder().encodeToString(
                        dto.getDocuments().getLicenseCertificate().getBytes()
                    )
                );
            }

            if (dto.getDocuments().getDegreeCertificate() != null) {
                docs.setDegreeCertificate(
                    java.util.Base64.getEncoder().encodeToString(
                        dto.getDocuments().getDegreeCertificate().getBytes()
                    )
                );
            }

            if (dto.getDocuments().getProfilePhoto() != null) {
                docs.setProfilePhoto(
                    java.util.Base64.getEncoder().encodeToString(
                        dto.getDocuments().getProfilePhoto().getBytes()
                    )
                );
            }

            entity.setDocuments(docs);
        }

        entity.setLanguages(dto.getLanguages());
        entity.setRole(dto.getRole());
        entity.setPhysioType(dto.getPhysioType());

        return entity;
    }
    private TherapistDTO mapToDTO(Therapist entity) {

        TherapistDTO dto = new TherapistDTO();

        dto.setTherapistId(entity.getTherapistId());
        dto.setClinicId(entity.getClinicId());
        dto.setBranchId(entity.getBranchId());
        dto.setFullName(entity.getFullName());
        dto.setContactNumber(entity.getContactNumber());
        dto.setEmailId(entity.getEmailId());
        dto.setGender(entity.getGender());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setQualification(entity.getQualification());
        dto.setYearsOfExperience(entity.getYearsOfExperience());
        dto.setServices(entity.getServices());
        dto.setSpecializations(entity.getSpecializations());
        dto.setExpertiseAreas(entity.getExpertiseAreas());
        dto.setTreatmentTypes(entity.getTreatmentTypes());
        dto.setAvailability(entity.getAvailability());
        dto.setBio(entity.getBio());
        dto.setUserName(entity.getUserName());
        dto.setPassword(entity.getPassword());
        dto.setAadharID(entity.getAadharID());
        dto.setDateofJoining(entity.getDateofJoining());
        dto.setEmergencyContact(entity.getEmergencyContact());

        // ================= BASE64 DECODE =================
        if (entity.getDocuments() != null) {

            Documents docs = new Documents();

            if (entity.getDocuments().getLicenseCertificate() != null) {
                docs.setLicenseCertificate(
                    new String(java.util.Base64.getDecoder().decode(
                        entity.getDocuments().getLicenseCertificate()
                    ))
                );
            }

            if (entity.getDocuments().getDegreeCertificate() != null) {
                docs.setDegreeCertificate(
                    new String(java.util.Base64.getDecoder().decode(
                        entity.getDocuments().getDegreeCertificate()
                    ))
                );
            }

            if (entity.getDocuments().getProfilePhoto() != null) {
                docs.setProfilePhoto(
                    new String(java.util.Base64.getDecoder().decode(
                        entity.getDocuments().getProfilePhoto()
                    ))
                );
            }

            dto.setDocuments(docs);
        }

        dto.setLanguages(entity.getLanguages());
        dto.setRole(entity.getRole());
        dto.setPhysioType(entity.getPhysioType());

        return dto;
    }

    // ================= HELPERS =================
    private String generateTherapistId() {
        return "THER-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generatePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    @Override
    public Response getPaidSessions(String clinicId,
                                    String branchId,
                                    String bookingId,
                                    String therapistRecordId) {

        Response response = new Response();

        try {

            Response paymentResponse =
                    physiotherapyFeignClient.getPayment(bookingId);

            Map<String, Object> data =
                    (Map<String, Object>) paymentResponse.getData();

            if (data == null) {
                throw new RuntimeException("Payment data not found");
            }

            // ✅ Validate fields
            if (!clinicId.equals(String.valueOf(data.get("clinicId")))
                    || !branchId.equals(String.valueOf(data.get("branchId")))
                    || !bookingId.equals(String.valueOf(data.get("bookingId")))
                    || !therapistRecordId.equals(String.valueOf(data.get("therapistRecordId")))) {

                throw new RuntimeException("Record mismatch");
            }

            // 🔥 UPDATED CODE START
            String serviceType =
                    String.valueOf(data.get("serviceType")).trim();

            List<Map<String, Object>> therapyWithSessions = null;

            // For therapy records, use therapyWithSessions
            if ("THERAPY".equalsIgnoreCase(serviceType)) {
                therapyWithSessions =
                        (List<Map<String, Object>>) data.get("therapyWithSessions");
            }

            // For exercise records, use exerciseWithSessions
            else if ("EXERCISE".equalsIgnoreCase(serviceType)) {
                therapyWithSessions =
                        (List<Map<String, Object>>) data.get("exerciseWithSessions");
            }

            // If selected list is null or empty, fallback to therapyWithSessions
            if (therapyWithSessions == null || therapyWithSessions.isEmpty()) {
                therapyWithSessions =
                        (List<Map<String, Object>>) data.get("therapyWithSessions");
            }

            // If still null or empty, fallback to exerciseWithSessions
            if (therapyWithSessions == null || therapyWithSessions.isEmpty()) {
                therapyWithSessions =
                        (List<Map<String, Object>>) data.get("exerciseWithSessions");
            }

            // Final safety check
            if (therapyWithSessions == null) {
                therapyWithSessions = new ArrayList<>();
            }
            // 🔥 UPDATED CODE END

            if (therapyWithSessions != null) {

                List<Map<String, Object>> filteredPackages = new ArrayList<>();

                for (Map<String, Object> pkg : therapyWithSessions) {

                    List<Map<String, Object>> programs;

                    // ✅ PACKAGE TYPE
                    if (pkg.containsKey("programs")) {

                        programs =
                                (List<Map<String, Object>>) pkg.get("programs");

                    }

                    // ✅ PROGRAM TYPE
                    else if (pkg.containsKey("programId")) {

                        programs = new ArrayList<>();
                        programs.add(pkg);

                    }
                 // ✅ THERAPY TYPE
                    else if (pkg.containsKey("therapyId")) {

                        programs = new ArrayList<>();

                        Map<String, Object> therapyWrapper = new HashMap<>();
                        therapyWrapper.put(
                                "therapyData",
                                new ArrayList<>(Arrays.asList(pkg))
                        );

                        programs.add(therapyWrapper);

                    }
                 

                 // ✅ EXERCISE TYPE
                 else if (pkg.containsKey("exerciseId")) {

                     programs = new ArrayList<>();

                     // Create therapy wrapper dynamically from exercise data
                     Map<String, Object> therapyWrapper = new HashMap<>();

                     // Use exerciseId as therapyId
                     therapyWrapper.put("exerciseId", pkg.get("exerciseId"));

                     // Use exerciseName as therapyName
                     therapyWrapper.put("exerciseName", pkg.get("exerciseName"));

                     // Use totalPrice as totalTherapyPrice
                     therapyWrapper.put("totalTherapyPrice", pkg.get("totalPrice"));

                     // Use actual paymentStatus from exercise
                     therapyWrapper.put("paymentStatus", pkg.get("paymentStatus"));

                     // Add the original exercise as-is
                     therapyWrapper.put(
                             "exercises",
                             new ArrayList<>(Arrays.asList(pkg))
                     );

                     // Wrap into program structure expected by existing logic
                     Map<String, Object> programWrapper = new HashMap<>();
                     programWrapper.put(
                             "therapyData",
                             new ArrayList<>(Arrays.asList(therapyWrapper))
                     );

                     programs.add(programWrapper);
                 }

                 // ✅ EXERCISE TYPE
                 else if (pkg.containsKey("exerciseId")) {

                     programs = new ArrayList<>();

                     // Create therapy wrapper dynamically from exercise data
                     Map<String, Object> therapyWrapper = new HashMap<>();

                     // Use exerciseId as therapyId
                     therapyWrapper.put("exerciseId", pkg.get("exerciseId"));

                     // Use exerciseName as therapyName
                     therapyWrapper.put("exerciseName", pkg.get("exerciseName"));

                     // Use totalPrice as totalTherapyPrice
                     therapyWrapper.put("totalTherapyPrice", pkg.get("totalPrice"));

                     // Use actual paymentStatus from exercise
                     therapyWrapper.put("paymentStatus", pkg.get("paymentStatus"));

                     // Add the original exercise as-is
                     therapyWrapper.put(
                             "exercises",
                             new ArrayList<>(Arrays.asList(pkg))
                     );

                     // Wrap into program structure expected by existing logic
                     Map<String, Object> programWrapper = new HashMap<>();
                     programWrapper.put(
                             "therapyData",
                             new ArrayList<>(Arrays.asList(therapyWrapper))
                     );

                     programs.add(programWrapper);
                 }
                    // ✅ INVALID
                    else {
                        continue;
                    }

                    if (programs == null) continue;

                    List<Map<String, Object>> filteredPrograms = new ArrayList<>();

                    for (Map<String, Object> program : programs) {

                        List<Map<String, Object>> therapyData =
                                (List<Map<String, Object>>) program.get("therapyData");

                        if (therapyData == null) continue;

                        List<Map<String, Object>> filteredTherapies = new ArrayList<>();

                        for (Map<String, Object> therapy : therapyData) {

                            List<Map<String, Object>> exercises =
                                    (List<Map<String, Object>>) therapy.get("exercises");

                            if (exercises == null) continue;

                            List<Map<String, Object>> filteredExercises = new ArrayList<>();

                            for (Map<String, Object> exercise : exercises) {

                                List<Map<String, Object>> sessions =
                                        (List<Map<String, Object>>) exercise.get("sessions");

                                if (sessions == null) continue;

                                List<Map<String, Object>> paidSessions =
                                        sessions.stream()
                                                .filter(session -> {

                                                    Object statusObj =
                                                            session.get("paymentStatus");

                                                    if (statusObj == null)
                                                        return false;

                                                    String status =
                                                            statusObj.toString().trim();

                                                    return "Paid"
                                                            .equalsIgnoreCase(status);

                                                })
                                                .collect(Collectors.toList());

                                if (!paidSessions.isEmpty()) {

                                    Map<String, Object> updatedExercise =
                                            new HashMap<>(exercise);

                                    updatedExercise.put(
                                            "sessions",
                                            new ArrayList<>(paidSessions)
                                    );

                                    filteredExercises.add(updatedExercise);
                                }
                            }

                            if (!filteredExercises.isEmpty()) {

                                Map<String, Object> updatedTherapy =
                                        new HashMap<>(therapy);

                                updatedTherapy.put(
                                        "exercises",
                                        new ArrayList<>(filteredExercises)
                                );

                                filteredTherapies.add(updatedTherapy);
                            }
                        }

                        if (!filteredTherapies.isEmpty()) {

                            Map<String, Object> updatedProgram =
                                    new HashMap<>(program);

                            updatedProgram.put(
                                    "therapyData",
                                    new ArrayList<>(filteredTherapies)
                            );

                            filteredPrograms.add(updatedProgram);
                        }
                    }

                    if (!filteredPrograms.isEmpty()) {

                        // ✅ PACKAGE RESPONSE
                        if (pkg.containsKey("programs")) {

                            Map<String, Object> updatedPackage =
                                    new HashMap<>(pkg);

                            updatedPackage.put(
                                    "programs",
                                    new ArrayList<>(filteredPrograms)
                            );

                            filteredPackages.add(updatedPackage);
                        }

                        // ✅ PROGRAM RESPONSE
                        else {

                            filteredPackages.addAll(filteredPrograms);
                        }
                    }
                }

                // 🔥 Replace original data
                data.put("therapyWithSessions", filteredPackages);
            }

            removeNullFields(data);

            response.setSuccess(true);
            response.setData(data);
            response.setMessage("Paid sessions fetched successfully");
            response.setStatus(200);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }

        return response;
    }
    private void removeNullFields(Object obj) {

        if (obj instanceof Map<?, ?> map) {

            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();

                if (entry.getValue() == null) {
                    iterator.remove(); // ❌ remove null field
                } else {
                    removeNullFields(entry.getValue()); // 🔁 recursive
                }
            }

        } else if (obj instanceof List<?> list) {

            for (Object item : list) {
                removeNullFields(item);
            }
        }
        
        
    }
    @Override
    public Response getTherapistPerformanceSummary(String clinicId,String branchId,String therapistId,int year) {

        Response response = new Response();

        try {

            // =========================================================
            // 1. FETCH FEEDBACK RECORDS
            // Used only for calculating average rating.
            // Selected year records only.
            // =========================================================
            List<FeedbackDetails> feedbackList =
                    feedbackDetailsRepository
                            .findByClinicIdAndBranchIdAndTherapistId(
                                    clinicId,
                                    branchId,
                                    therapistId)
                            .stream()
                            .filter(feedback -> {
                                try {
                                    if (feedback.getCreatedAt() == null
                                            || feedback.getCreatedAt()
                                                       .trim()
                                                       .isEmpty()) {
                                        return false;
                                    }

                                    LocalDate feedbackDate =
                                            LocalDate.parse(
                                                    feedback.getCreatedAt()
                                                            .substring(0, 10));

                                    return feedbackDate.getYear() == year;

                                } catch (Exception e) {
                                    return false;
                                }
                            })
                            .collect(Collectors.toList());

            // =========================================================
            // 2. TOTAL NUMBER OF SESSIONS COMPLETED
            // Logic:
            // - Fetch all payments for the clinic and branch.
            // - Filter only the selected therapist.
            // - Consider only valid service types:
            //   PACKAGE / PROGRAM / THERAPY / EXERCISE.
            // - For those records, recursively search for all "sessions"
            //   arrays and count only sessions where status = "Completed".
            // =========================================================
            int totalSessionCompleted = 0;

            Response paymentResponse =
                    physiotherapyFeignClient.getPayments(
                            clinicId,
                            branchId);

            List<Map<String, Object>> payments =
                    (List<Map<String, Object>>) paymentResponse.getData();

            if (payments != null) {

                for (Map<String, Object> payment : payments) {

                    // ---------------------------------------------------------
                    // Filter by selected year
                    // ---------------------------------------------------------
                    Object paymentDateObj = payment.get("sessionStartDate");

                    // If date is missing, skip this payment
                    if (paymentDateObj == null) {
                        continue;
                    }

                    try {
                        LocalDate paymentDate =
                                LocalDate.parse(
                                        paymentDateObj.toString()
                                                .substring(0, 10));

                        // Skip if payment year does not match requested year
                        if (paymentDate.getYear() != year) {
                            continue;
                        }

                    } catch (Exception e) {
                        // Invalid date format, skip this payment
                        continue;
                    }

                    // ---------------------------------------------------------
                    // Filter by therapistId
                    // ---------------------------------------------------------
                    if (!therapistId.equals(
                            String.valueOf(payment.get("therapistId")))) {
                        continue;
                    }

                    // ---------------------------------------------------------
                    // Consider only supported service types
                    // ---------------------------------------------------------
                    String serviceType =
                            String.valueOf(payment.get("serviceType"));

                    if (!"PACKAGE".equalsIgnoreCase(serviceType)
                            && !"PROGRAM".equalsIgnoreCase(serviceType)
                            && !"THERAPY".equalsIgnoreCase(serviceType)
                            && !"EXERCISE".equalsIgnoreCase(serviceType)) {
                        continue;
                    }

                    // ---------------------------------------------------------
                    // Count all sessions with status = "Completed"
                    // ---------------------------------------------------------
                    totalSessionCompleted +=
                            countCompletedSessions(payment);
                }
            }
            // =========================================================
            // 3. TOTAL AVERAGE RATING
            // rating is stored as String in FeedbackDetails
            // =========================================================
            double totalAvgRating = feedbackList.stream()
                    .filter(feedback ->
                            feedback.getRating() != null
                            && !feedback.getRating().trim().isEmpty())
                    .mapToDouble(feedback -> {
                        try {
                            return Double.parseDouble(
                                    feedback.getRating().trim());
                        } catch (NumberFormatException e) {
                            return 0.0;
                        }
                    })
                    .average()
                    .orElse(0.0);

            // Round to 2 decimal places
            totalAvgRating =
                    Math.round(totalAvgRating * 100.0) / 100.0;

            // =========================================================
            // 4. TOTAL IDLE TIME
            // =========================================================
            List<TherapistAttendance> attendanceList =
                    therapistAttendanceRepository
                            .findByTherapistId(therapistId)
                            .stream()
                            .filter(attendance -> {
                                try {
                                    if (attendance.getDate() == null) {
                                        return false;
                                    }

                                    LocalDate attendanceDate =
                                            LocalDate.parse(
                                                    attendance.getDate()
                                                            .toString()
                                                            .substring(0, 10));

                                    return attendanceDate.getYear() == year;
                                } catch (Exception e) {
                                    return false;
                                }
                            })
                            .collect(Collectors.toList());

            long totalIdleMinutes = 0;

            if (attendanceList != null) {

                for (TherapistAttendance attendance : attendanceList) {

                    long logMinutes =
                            convertToMinutes(attendance.getLogTime());

                    long workingMinutes = 0;

                    if (attendance.getSessions() != null) {

                        for (Session session : attendance.getSessions()) {

                            String duration = session.getDuration();

                            if (duration != null
                                    && !duration.trim().isEmpty()) {

                                workingMinutes +=
                                        convertToMinutes(duration);
                            }
                        }
                    }

                    long idleMinutes =
                            logMinutes - workingMinutes;

                    if (idleMinutes < 0) {
                        idleMinutes = 0;
                    }

                    totalIdleMinutes += idleMinutes;
                }
            }

            String formattedIdleTime =
                    formatDuration(totalIdleMinutes);

            // =========================================================
            // 5. TRAINING HOURS DATA
            // =========================================================
            long totalTrainingMinutes = 0;

            if (attendanceList != null) {

                for (TherapistAttendance attendance : attendanceList) {

                    if (attendance.getSessions() != null) {

                        for (Session session : attendance.getSessions()) {

                            String description = session.getDescription();
                            String duration = session.getDuration();

                            if (description != null
                                    && "Training".equalsIgnoreCase(
                                            description.trim())
                                    && duration != null
                                    && !duration.trim().isEmpty()) {

                                totalTrainingMinutes +=
                                        convertToMinutes(duration);
                            }
                        }
                    }
                }
            }

            String formattedTrainingHours =
                    formatDuration(totalTrainingMinutes);

            // =========================================================
            // 6. RESPONSE DATA
            // =========================================================
            Map<String, Object> data = new HashMap<>();
            data.put("clinicId", clinicId);
            data.put("branchId", branchId);
            data.put("therapistId", therapistId);
            data.put("year", year);
            data.put("totalSessionCompleted", totalSessionCompleted);
            data.put("totalIdleTime", formattedIdleTime);
            data.put("totalAvgRating", totalAvgRating);
            data.put("totalTrainingHours", formattedTrainingHours);

            // =========================================================
            // 7. SUCCESS RESPONSE
            // =========================================================
            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Therapist performance summary fetched successfully");
            response.setData(data);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
            response.setData(null);
        }

        return response;
    }
    /**
     * Converts total minutes into hours only.
     *
     * Examples:
     *  - 30 minutes   -> "0.5 hrs"
     *  - 60 minutes   -> "1 hrs"
     *  - 90 minutes   -> "1.5 hrs"
     *  - 120 minutes  -> "2 hrs"
     *  - 1500 minutes -> "25 hrs"
     *
     * This method does NOT return years, months, or days.
     * Everything is converted into total hours only.
     */
    private String formatDuration(long totalMinutes) {

        if (totalMinutes <= 0) {
            return "0 hrs";
        }

        // Convert minutes to hours
        double totalHours = totalMinutes / 60.0;

        // Round to 2 decimal places
        totalHours = Math.round(totalHours * 100.0) / 100.0;

        // Remove trailing .0 for whole numbers
        if (totalHours == (long) totalHours) {
            return ((long) totalHours) + " hrs";
        }

        return totalHours + " hrs";
    }
    /**
     * Converts time strings into total minutes.
     *
     * Supported formats:
     * - "5 min"
     * - "45 mins"
     * - "1 hr"
     * - "2 hrs 30 mins"
     * - "1 day 2 hrs"
     * - "2 months 5 days"
     * - "1 year 3 months"
     * - "7h 0m"
     * - "120"   (treated as minutes)
     *
     * Examples:
     * - "5 min"              -> 5
     * - "1 hr"               -> 60
     * - "1 day"              -> 1440
     * - "2 months"           -> 86400
     * - "1 year"             -> 525600
     * - "1 year 2 months 3 days 4 hrs 5 mins"
     *                        -> total minutes
     */
    private long convertToMinutes(String time) {

        if (time == null || time.trim().isEmpty()) {
            return 0;
        }

        time = time.toLowerCase().trim();

        long totalMinutes = 0;

        // ---------------------------------------------------------
        // Years
        // ---------------------------------------------------------
        java.util.regex.Matcher yearMatcher =
                java.util.regex.Pattern
                        .compile("(\\d+)\\s*(y|yr|yrs|year|years)")
                        .matcher(time);

        while (yearMatcher.find()) {
            long years = Long.parseLong(yearMatcher.group(1));
            totalMinutes += years * 365L * 24 * 60;
        }

        // ---------------------------------------------------------
        // Months
        // ---------------------------------------------------------
        java.util.regex.Matcher monthMatcher =
                java.util.regex.Pattern
                        .compile("(\\d+)\\s*(mo|mon|mons|month|months)")
                        .matcher(time);

        while (monthMatcher.find()) {
            long months = Long.parseLong(monthMatcher.group(1));
            totalMinutes += months * 30L * 24 * 60;
        }

        // ---------------------------------------------------------
        // Days
        // ---------------------------------------------------------
        java.util.regex.Matcher dayMatcher =
                java.util.regex.Pattern
                        .compile("(\\d+)\\s*(d|day|days)")
                        .matcher(time);

        while (dayMatcher.find()) {
            long days = Long.parseLong(dayMatcher.group(1));
            totalMinutes += days * 24L * 60;
        }

        // ---------------------------------------------------------
        // Hours
        // ---------------------------------------------------------
        java.util.regex.Matcher hourMatcher =
                java.util.regex.Pattern
                        .compile("(\\d+)\\s*(h|hr|hrs|hour|hours)")
                        .matcher(time);

        while (hourMatcher.find()) {
            long hours = Long.parseLong(hourMatcher.group(1));
            totalMinutes += hours * 60;
        }

        // ---------------------------------------------------------
        // Minutes
        // ---------------------------------------------------------
        java.util.regex.Matcher minuteMatcher =
                java.util.regex.Pattern
                        .compile("(\\d+)\\s*(m|min|mins|minute|minutes)")
                        .matcher(time);

        while (minuteMatcher.find()) {
            long minutes = Long.parseLong(minuteMatcher.group(1));
            totalMinutes += minutes;
        }

        // ---------------------------------------------------------
        // If input contains only a number, treat it as minutes
        // Example: "120"
        // ---------------------------------------------------------
        if (totalMinutes == 0 && time.matches("\\d+")) {
            totalMinutes = Long.parseLong(time);
        }

        return totalMinutes;
    }
    /**
     * Recursively searches the given object for all keys named "sessions"
     * and counts how many session objects have status = "Completed".
     */
    private int countCompletedSessions(Object obj) {

        int count = 0;

        if (obj instanceof Map<?, ?> map) {

            for (Map.Entry<?, ?> entry : map.entrySet()) {

                // If the current key is "sessions", count completed sessions
                if ("sessions".equals(entry.getKey())
                        && entry.getValue() instanceof List<?> sessions) {

                    for (Object sessionObj : sessions) {

                        if (sessionObj instanceof Map<?, ?> sessionMap) {

                            Object statusObj =
                                    sessionMap.get("status");

                            if (statusObj != null
                                    && "Completed".equalsIgnoreCase(
                                            statusObj.toString())) {
                                count++;
                            }
                        }
                    }
                }

                // Continue searching deeper
                count += countCompletedSessions(entry.getValue());
            }

        } else if (obj instanceof List<?> list) {

            for (Object item : list) {
                count += countCompletedSessions(item);
            }
        }

        return count;
    }
}