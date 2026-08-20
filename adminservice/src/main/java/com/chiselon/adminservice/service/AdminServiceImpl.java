 package com.chiselon.adminservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

//import com.AdminService.dto.CategoryDto;
import com.chiselon.adminservice.dto.ClinicCredentialsDTO;
import com.chiselon.adminservice.dto.ClinicDTO;
import com.chiselon.adminservice.dto.DoctorsDTO;
import com.chiselon.adminservice.dto.DoctortInfo;
import com.chiselon.adminservice.dto.LabTestDTO;
import com.chiselon.adminservice.dto.ProbableDiagnosisDTO;
//import com.AdminService.dto.ServicesDto;
//import com.AdminService.dto.SubServicesDto;
//import com.AdminService.dto.SubServicesInfoDto;
import com.chiselon.adminservice.dto.TreatmentDTO;
import com.chiselon.adminservice.dto.UpdateClinicCredentials;
import com.chiselon.adminservice.entity.Branch;
import com.chiselon.adminservice.entity.BranchCounter;
import com.chiselon.adminservice.entity.Clinic;
import com.chiselon.adminservice.entity.ClinicCredentials;
import com.chiselon.adminservice.entity.Counter;
import com.chiselon.adminservice.repository.BranchRepository;
import com.chiselon.adminservice.repository.ClinicCredentialsRepository;
import com.chiselon.adminservice.repository.ClinicRep;
import com.chiselon.adminservice.util.ClinicAdminFeignImpl;
import com.chiselon.adminservice.util.ExtractFeignMessage;
import com.chiselon.adminservice.util.KeyCloakTokenStore;
import com.chiselon.adminservice.util.PermissionsUtil;
import com.chiselon.adminservice.util.Response;
import com.chiselon.adminservice.util.ResponseStructure;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

	@Autowired
	private ClinicRep clinicRep;
	
	@Autowired
	private ClinicCredentialsRepository clinicCredentialsRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private  ClinicAdminFeignImpl clinicAdminFeign;

	@Autowired
	private BranchRepository branchRepository;
	
	@Autowired
	private MongoOperations mongoOperations;
	
   @Autowired
    private  EmailService emailService; // ✅ ADD THIS
   
   @Autowired
   private KeyCloakTokenStore keyCloakTokenStore;
  

   @Override
   @Secured("ROLE_ADMIN")
	@RateLimiter(name = "adminService", fallbackMethod = "createClinicFallback")
   public Response createClinic(ClinicDTO clinic) {

       log.info("Received request to create clinic. Name: {}, Email: {}, Contact: {}",
               clinic.getName(), clinic.getEmailAddress(), clinic.getContactNumber());

       Response response = new Response();

       try {

           log.info("Validating duplicate clinic details.");

           if (clinicRep.findByContactNumber(clinic.getContactNumber()) != null) {
               log.warn("Clinic creation failed. Contact number already exists: {}", clinic.getContactNumber());

               response.setMessage("ContactNumber already exists");
               response.setSuccess(false);
               response.setStatus(409);
               return response;
           }

           if (clinicRep.findByLicenseNumber(clinic.getLicenseNumber()) != null) {
               log.warn("Clinic creation failed. License number already exists: {}", clinic.getLicenseNumber());

               response.setMessage("LicenseNumber already exists");
               response.setSuccess(false);
               response.setStatus(409);
               return response;
           }

           if (clinicRep.findByEmailAddress(clinic.getEmailAddress()) != null) {
               log.warn("Clinic creation failed. Email already exists: {}", clinic.getEmailAddress());

               response.setMessage("EmailAddress already exists");
               response.setSuccess(false);
               response.setStatus(409);
               return response;
           }

           log.info("Duplicate validation completed successfully.");


// ---------------- Save clinic ----------------
           Clinic savedClinic = new Clinic();
           savedClinic.setName(clinic.getName());
           savedClinic.setHospitalId(generateHospitalId());
           savedClinic.setBranch(clinic.getBranch());
           savedClinic.setAddress(clinic.getAddress());
           savedClinic.setCity(clinic.getCity());
           savedClinic.setContactNumber(clinic.getContactNumber());
           savedClinic.setOpeningTime(clinic.getOpeningTime());
           savedClinic.setClosingTime(clinic.getClosingTime());
           savedClinic.setEmailAddress(clinic.getEmailAddress());
           savedClinic.setWebsite(clinic.getWebsite());
           savedClinic.setLicenseNumber(clinic.getLicenseNumber());
           savedClinic.setIssuingAuthority(clinic.getIssuingAuthority());
           savedClinic.setRecommended(clinic.isRecommended());
           savedClinic.setClinicType(clinic.getClinicType());
           savedClinic.setHospitalOverallRating(0.0);
           savedClinic.setSubscription(clinic.getSubscription());
           savedClinic.setFreeFollowUps(clinic.getFreeFollowUps());
           savedClinic.setLatitude(clinic.getLatitude());
           savedClinic.setLongitude(clinic.getLongitude());
           savedClinic.setWalkthrough(clinic.getWalkthrough());
           savedClinic.setNabhScore(clinic.getNabhScore());
//           savedClinic.setLoyaltyPoints(clinic.getLoyaltyPoints());
//           savedClinic.setLocation(clinic.getLocation());
           // ---------------- NGK CORE ----------------
           savedClinic.setStatus("PENDING");
           savedClinic.setRole("ADMIN");
           savedClinic.setPermissions(clinic.getPermissions());
           savedClinic.setCreatedAt(String.valueOf(Instant.now())); // FIXED

           // ❌ Credentials are NOT created here

           decodeBase64Documents(clinic, savedClinic);

           if (clinic.getConsultationExpiration() == null || clinic.getConsultationExpiration().isBlank()) {
               throw new IllegalArgumentException("Consultation expiration is required");
           }
           savedClinic.setConsultationExpiration(clinic.getConsultationExpiration());

           savedClinic.setInstagramHandle(clinic.getInstagramHandle());
           savedClinic.setTwitterHandle(clinic.getTwitterHandle());
           savedClinic.setFacebookHandle(clinic.getFacebookHandle());
           savedClinic.setHospitalId(generateHospitalId());

           log.info("Generated Hospital ID: {}", savedClinic.getHospitalId());

           log.info("Clinic documents decoded successfully.");

           Clinic saved = clinicRep.save(savedClinic);

           log.info("Clinic saved successfully. HospitalId: {}", saved.getHospitalId());

           BranchCounter counter = mongoOperations.findAndModify(
                   Query.query(Criteria.where("_id").is(saved.getHospitalId())),
                   new Update().inc("seq", 1),
                   FindAndModifyOptions.options().returnNew(true).upsert(true),
                   BranchCounter.class
           );

           String branchId = String.format("%04d%02d",
                   Integer.parseInt(saved.getHospitalId()),
                   counter.getSeq());

           log.info("Generated Branch ID: {}", branchId);

           Branch branch = new Branch();

           branch.setClinicId(saved.getHospitalId());
           branch.setHospitalName(saved.getName());
           branch.setBranchId(branchId);
           branch.setBranchName(clinic.getBranch() != null && !clinic.getBranch().isEmpty() ? clinic.getBranch()
                   : saved.getName() + " Main Branch");
           branch.setAddress(saved.getAddress());
           branch.setCity(saved.getCity());
           branch.setContactNumber(saved.getContactNumber());
           branch.setEmail(saved.getEmailAddress());
           branch.setRole("ADMIN");
           branch.setLatitude(String.valueOf(saved.getLatitude()));
           branch.setLongitude(String.valueOf(saved.getLongitude()));
           branch.setPermissions(clinic.getPermissions());
//           branch.setLoyaltyPoints(saved.getLoyaltyPoints());
//           branch.setLocation(saved.getLocation());

           Branch savedBranch = branchRepository.save(branch);

           log.info("Default branch created successfully. BranchId: {}", savedBranch.getBranchId());

           saved.setBranches(List.of(savedBranch));
           clinicRep.save(saved);

           log.info("Branch mapped to clinic successfully.");

           Map<String, String> mailData = new HashMap<>();
           mailData.put("subject", "Clinic Registration Pending");
           mailData.put("message",
                   "Your clinic registration has been received successfully.");

           emailService.sendEmail(saved.getEmailAddress(), mailData);

           log.info("Acknowledgement email sent successfully to {}", saved.getEmailAddress());

           Map<String, Object> data = new HashMap<>();
           data.put("clinicId", saved.getHospitalId());
           data.put("branchId", savedBranch.getBranchId());
           data.put("status", saved.getStatus());

           response.setSuccess(true);
           response.setStatus(200);
           response.setMessage("Clinic registered successfully. Verification pending.");
           response.setData(data);

           log.info("Clinic registration completed successfully. ClinicId: {}, BranchId: {}",
                   saved.getHospitalId(), savedBranch.getBranchId());

           return response;

       } catch (Exception e) {

           log.error("Error while creating clinic. Name: {}, Error: {}",
                   clinic.getName(), e.getMessage(), e);

           Response error = new Response();
           error.setMessage("Error occurred while creating clinic: " + e.getMessage());
           error.setSuccess(false);
           error.setStatus(500);

           return error;
       }
   }
   private void decodeBase64Documents(ClinicDTO clinic, Clinic savedClinic) {

	    log.info("Started decoding clinic documents.");

	    if (clinic.getHospitalLogo() != null && !clinic.getHospitalLogo().isEmpty()) {
	        log.debug("Decoding Hospital Logo.");
	        savedClinic.setHospitalLogo(Base64.getDecoder().decode(clinic.getHospitalLogo()));
	    }

	    if (clinic.getContractorDocuments() != null && !clinic.getContractorDocuments().isEmpty()) {
	        log.debug("Decoding Contractor Documents.");
	        savedClinic.setContractorDocuments(
	                Base64.getDecoder().decode(clinic.getContractorDocuments()));
	    }

	    if (clinic.getHospitalDocuments() != null && !clinic.getHospitalDocuments().isEmpty()) {
	        log.debug("Decoding Hospital Documents.");
	        savedClinic.setHospitalDocuments(
	                Base64.getDecoder().decode(clinic.getHospitalDocuments()));
	    }

	    if (clinic.getClinicalEstablishmentCertificate() != null &&
	            !clinic.getClinicalEstablishmentCertificate().isEmpty()) {

	        log.debug("Decoding Clinical Establishment Certificate.");
	        savedClinic.setClinicalEstablishmentCertificate(
	                Base64.getDecoder().decode(clinic.getClinicalEstablishmentCertificate()));
	    }

	    if (clinic.getBusinessRegistrationCertificate() != null &&
	            !clinic.getBusinessRegistrationCertificate().isEmpty()) {

	        log.debug("Decoding Business Registration Certificate.");
	        savedClinic.setBusinessRegistrationCertificate(
	                Base64.getDecoder().decode(clinic.getBusinessRegistrationCertificate()));
	    }

	    if (clinic.getDrugLicenseCertificate() != null &&
	            !clinic.getDrugLicenseCertificate().isEmpty()) {

	        log.debug("Decoding Drug License Certificate.");
	        savedClinic.setDrugLicenseCertificate(
	                Base64.getDecoder().decode(clinic.getDrugLicenseCertificate()));
	    }

	    if (clinic.getDrugLicenseFormType() != null &&
	            !clinic.getDrugLicenseFormType().isEmpty()) {

	        log.debug("Decoding Drug License Form Type.");
	        savedClinic.setDrugLicenseFormType(
	                Base64.getDecoder().decode(clinic.getDrugLicenseFormType()));
	    }

	    if (clinic.getPharmacistCertificate() != null &&
	            !clinic.getPharmacistCertificate().isEmpty()) {

	        log.debug("Decoding Pharmacist Certificate.");
	        savedClinic.setPharmacistCertificate(
	                Base64.getDecoder().decode(clinic.getPharmacistCertificate()));
	    }

	    if (clinic.getBiomedicalWasteManagementAuth() != null &&
	            !clinic.getBiomedicalWasteManagementAuth().isEmpty()) {

	        log.debug("Decoding Biomedical Waste Management Authorization.");
	        savedClinic.setBiomedicalWasteManagementAuth(
	                Base64.getDecoder().decode(clinic.getBiomedicalWasteManagementAuth()));
	    }

	    if (clinic.getTradeLicense() != null &&
	            !clinic.getTradeLicense().isEmpty()) {

	        log.debug("Decoding Trade License.");
	        savedClinic.setTradeLicense(
	                Base64.getDecoder().decode(clinic.getTradeLicense()));
	    }

	    if (clinic.getFireSafetyCertificate() != null &&
	            !clinic.getFireSafetyCertificate().isEmpty()) {

	        log.debug("Decoding Fire Safety Certificate.");
	        savedClinic.setFireSafetyCertificate(
	                Base64.getDecoder().decode(clinic.getFireSafetyCertificate()));
	    }

	    if (clinic.getProfessionalIndemnityInsurance() != null &&
	            !clinic.getProfessionalIndemnityInsurance().isEmpty()) {

	        log.debug("Decoding Professional Indemnity Insurance.");
	        savedClinic.setProfessionalIndemnityInsurance(
	                Base64.getDecoder().decode(clinic.getProfessionalIndemnityInsurance()));
	    }

	    if (clinic.getGstRegistrationCertificate() != null &&
	            !clinic.getGstRegistrationCertificate().isEmpty()) {

	        log.debug("Decoding GST Registration Certificate.");
	        savedClinic.setGstRegistrationCertificate(
	                Base64.getDecoder().decode(clinic.getGstRegistrationCertificate()));
	    }

	    log.info("Completed decoding clinic documents.");
	}
   @Override
   @Secured("ROLE_ADMIN")
	@RateLimiter(name = "adminService", fallbackMethod = "startVerificationProcessFallback")
   public Response startVerificationProcess(String clinicId) {

       log.info("Received request to start verification process for ClinicId: {}", clinicId);

       Response response = new Response();

       try {

           log.info("Fetching clinic details for ClinicId: {}", clinicId);

           Clinic clinic = findClinic(clinicId);

           log.info("Clinic found. Current Status: {}", clinic.getStatus());

           if (!"PENDING".equals(clinic.getStatus())) {

               log.warn("Verification cannot be started. ClinicId: {}, Current Status: {}",
                       clinicId, clinic.getStatus());

               response.setSuccess(false);
               response.setStatus(400);
               response.setMessage("Clinic is not in PENDING state");
               return response;
           }

           clinic.setStatus("VERIFICATION_IN_PROGRESS");

           clinicRep.save(clinic);

           log.info("Clinic status updated to VERIFICATION_IN_PROGRESS. ClinicId: {}",
                   clinicId);

           // Email notification
           Map<String, String> mailData = new HashMap<>();
           mailData.put("subject", "Clinic Verification Started");
           mailData.put(
                   "message",
                   "Your clinic verification process has started.\n" +
                   "Our team is reviewing your submitted documents."
           );

           emailService.sendEmail(clinic.getEmailAddress(), mailData);

           log.info("Verification notification email sent successfully to {}",
                   clinic.getEmailAddress());

           response.setSuccess(true);
           response.setStatus(200);
           response.setMessage("Verification started successfully");
           response.setHospitalId(clinic.getHospitalId());
           response.setHospitalName(clinic.getName());

           log.info("Verification process started successfully. ClinicId: {}, HospitalName: {}",
                   clinic.getHospitalId(), clinic.getName());

           return response;

       } catch (Exception e) {

           log.error("Failed to start verification process for ClinicId: {}. Error: {}",
                   clinicId, e.getMessage(), e);

           response.setSuccess(false);
           response.setStatus(500);
           response.setMessage("Failed to start verification: " + e.getMessage());

           return response;
       }
   }
   @Override
   @Secured("ROLE_ADMIN")
	@RateLimiter(name = "adminService", fallbackMethod = "verifyClinicFallback")
   public Response verifyClinic(String clinicId) {

       log.info("Received request to verify clinic. ClinicId: {}", clinicId);

       Response response = new Response();

       try {

           log.info("Fetching clinic details for ClinicId: {}", clinicId);

           Clinic clinic = clinicRep.findByHospitalId(clinicId);

           if (clinic == null) {

               log.warn("Clinic not found. ClinicId: {}", clinicId);

               response.setSuccess(false);
               response.setStatus(404);
               response.setMessage("Clinic not found");
               return response;
           }

           log.info("Clinic found. Current Status: {}", clinic.getStatus());

           if (!"VERIFICATION_IN_PROGRESS".equals(clinic.getStatus())) {

               log.warn("Clinic verification failed. ClinicId: {}, Current Status: {}",
                       clinicId, clinic.getStatus());

               response.setSuccess(false);
               response.setStatus(400);
               response.setMessage("Clinic is not under verification");
               return response;
           }

           log.info("Generating temporary credentials for ClinicId: {}", clinicId);

           // Generate password
           String tempPassword = generatePassword(9);

           ClinicCredentials credentials = new ClinicCredentials();
           credentials.setHospitalName(clinic.getName());
           credentials.setUserName(clinic.getHospitalId());
           credentials.setPassword(passwordEncoder.encode(tempPassword));
           credentials.setRoles(Collections.singletonList("ROLE_CLINICADMIN"));

           Map<String, Map<String, List<String>>> permissionWrapper = new HashMap<>();
           permissionWrapper.put("ADMIN", PermissionsUtil.getAdminPermissions());
           credentials.setPermissions(permissionWrapper);

           clinicCredentialsRepository.save(credentials);

           log.info("Clinic credentials saved successfully. Username: {}",
                   credentials.getUserName());

           clinic.setStatus("VERIFIED");
           clinicRep.save(clinic);

           log.info("Clinic status updated to VERIFIED. ClinicId: {}",
                   clinicId);

           // Email
           Map<String, String> mailData = new HashMap<>();
           mailData.put("subject", "Clinic Verified Successfully");
           mailData.put("message",
                   "Congratulations! Your clinic has been verified successfully.");
           mailData.put("username", credentials.getUserName());
           mailData.put("password", tempPassword);

           emailService.sendEmail(clinic.getEmailAddress(), mailData);

           log.info("Verification email sent successfully to {}",
                   clinic.getEmailAddress());

           response.setSuccess(true);
           response.setStatus(200);
           response.setMessage("Clinic verified successfully");
           response.setHospitalId(clinic.getHospitalId());
           response.setPermissions(PermissionsUtil.getAdminPermissions());

           log.info("Clinic verification completed successfully. ClinicId: {}, HospitalName: {}",
                   clinic.getHospitalId(), clinic.getName());

           return response;

       } catch (Exception e) {

           log.error("Failed to verify clinic. ClinicId: {}. Error: {}",
                   clinicId, e.getMessage(), e);

           response.setSuccess(false);
           response.setStatus(500);
           response.setMessage("Failed to verify clinic: " + e.getMessage());

           return response;
       }
   }
   @Override
   @Secured("ROLE_ADMIN")
	@RateLimiter(name = "adminService", fallbackMethod = "rejectClinicFallback")
   public Response rejectClinic(String clinicId, String reason) {

       log.info("Received request to reject clinic. ClinicId: {}", clinicId);

       Response response = new Response();

       try {

           log.info("Fetching clinic details for ClinicId: {}", clinicId);

           Clinic clinic = findClinic(clinicId);

           log.info("Clinic found. Current Status: {}", clinic.getStatus());

           if ("VERIFIED".equals(clinic.getStatus())) {

               log.warn("Reject operation failed. Verified clinic cannot be rejected. ClinicId: {}",
                       clinicId);

               response.setSuccess(false);
               response.setStatus(400);
               response.setMessage("Verified clinic cannot be rejected");
               return response;
           }

           clinic.setStatus("REJECTED");
           clinicRep.save(clinic);

           log.info("Clinic status updated to REJECTED. ClinicId: {}",
                   clinicId);

           // Rejection email
           Map<String, String> mailData = new HashMap<>();
           mailData.put("subject", "Clinic Registration Rejected");
           mailData.put(
                   "message",
                   "Unfortunately, your clinic registration has been rejected."
           );
           mailData.put("reason", reason);

           emailService.sendEmail(clinic.getEmailAddress(), mailData);

           log.info("Rejection email sent successfully to {}",
                   clinic.getEmailAddress());

           response.setSuccess(true);
           response.setStatus(200);
           response.setMessage("Clinic rejected successfully");
           response.setHospitalId(clinic.getHospitalId());

           log.info("Clinic rejected successfully. ClinicId: {}, HospitalName: {}",
                   clinic.getHospitalId(), clinic.getName());

           return response;

       } catch (Exception e) {

           log.error("Failed to reject clinic. ClinicId: {}. Error: {}",
                   clinicId, e.getMessage(), e);

           response.setSuccess(false);
           response.setStatus(500);
           response.setMessage("Failed to reject clinic: " + e.getMessage());

           return response;
       }
   }
   @Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
   private Clinic findClinic(String clinicId) {

       log.info("Searching for clinic. ClinicId: {}", clinicId);

       Clinic clinic = clinicRep.findByHospitalId(clinicId);

       if (clinic == null) {

           log.error("Clinic not found. ClinicId: {}", clinicId);

           throw new RuntimeException("Clinic not found with id: " + clinicId);
       }

       log.info("Clinic found successfully. ClinicId: {}, HospitalName: {}",
               clinic.getHospitalId(), clinic.getName());

       return clinic;
   }

   @Override
   @Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "adminService", fallbackMethod = "getClinicByIdFallback")
   public Response getClinicById(String clinicId) {

       log.info("Received request to fetch clinic details. ClinicId: {}", clinicId);

       Response response = new Response();

       try {

           log.info("Searching clinic with ClinicId: {}", clinicId);

           Clinic clinic = clinicRep.findByHospitalId(clinicId);

           if (clinic != null) {

               log.info("Clinic found successfully. ClinicId: {}, HospitalName: {}",
                       clinic.getHospitalId(), clinic.getName());

               ClinicDTO clnc = new ClinicDTO();

               // Populate DTO...

               log.debug("Mapping Clinic entity to ClinicDTO.");

               // All your existing mapping code goes here
               // (No logging of Base64 encoded documents)

               response.setMessage("Clinic fetched successfully");
               response.setSuccess(true);
               response.setStatus(200);
               response.setData(clnc);

               log.info("Clinic details fetched successfully. ClinicId: {}",
                       clinicId);

               return response;

           } else {

               log.warn("Clinic not found. ClinicId: {}", clinicId);

               response.setMessage("Clinic not found");
               response.setSuccess(false);
               response.setStatus(404);

               return response;
           }

       } catch (Exception e) {

           log.error("Error while fetching clinic details. ClinicId: {}. Error: {}",
                   clinicId, e.getMessage(), e);

           response.setMessage("Error occurred while fetching clinic: " + e.getMessage());
           response.setSuccess(false);
           response.setStatus(500);

           return response;
       }
   }

   @Override
   @Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "adminService", fallbackMethod = "getAllClinicsFallback")
   public Response getAllClinics() {

       log.info("Received request to fetch all clinics.");

       Response response = new Response();

       try {

           log.info("Fetching all clinics from database.");

           List<Clinic> clinics = clinicRep.findAll();

           log.info("Total clinics retrieved from database: {}", clinics.size());

           List<ClinicDTO> list = new ArrayList<>();

           if (!clinics.isEmpty()) {

               for (Clinic clinic : clinics) {

                   log.debug("Mapping Clinic to ClinicDTO. ClinicId: {}, HospitalName: {}",
                           clinic.getHospitalId(), clinic.getName());

                   ClinicDTO clnc = new ClinicDTO();

                   // Your existing mapping code goes here.
                   // Do NOT log Base64 document contents.

                   list.add(clnc);
               }

               response.setData(list);
               response.setMessage("Clinics fetched successfully");
               response.setSuccess(true);
               response.setStatus(200);

               log.info("Successfully fetched {} clinic(s).", list.size());

           } else {

               log.warn("No clinics found in the database.");

               response.setData(null);
               response.setMessage("Clinics Not Found");
               response.setSuccess(true);
               response.setStatus(200);
           }

       } catch (Exception e) {

           log.error("Error occurred while fetching all clinics. Error: {}",
                   e.getMessage(), e);

           response.setData(null);
           response.setMessage("Error: " + e.getMessage());
           response.setSuccess(false);
           response.setStatus(500);
       }

       return response;
   }

   @Override
   @Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "adminService", fallbackMethod = "updateClinicFallback")
   public Response updateClinic(String clinicId, ClinicDTO clinic) {

       log.info("Update clinic request received. ClinicId: {}", clinicId);

       Response response = new Response();

       try {

           Clinic savedClinic = clinicRep.findByHospitalId(clinicId);

           log.debug("Clinic lookup completed. ClinicId: {}, Found: {}",
                   clinicId,
                   savedClinic != null);

           if (savedClinic != null) {

               log.debug("Starting validation for clinic update. ClinicId: {}", clinicId);

               if (!savedClinic.getContactNumber().equalsIgnoreCase(clinic.getContactNumber())) {

                   if (clinicRep.findByContactNumber(clinic.getContactNumber()) != null) {

                       log.warn("Duplicate contact number detected. ClinicId: {}, Contact: {}",
                               clinicId,
                               clinic.getContactNumber());

                       response.setMessage("ContactNumber already exists");
                       response.setSuccess(false);
                       response.setStatus(409);
                       return response;
                   }
               }

               if (!savedClinic.getLicenseNumber().equalsIgnoreCase(clinic.getLicenseNumber())) {

                   if (clinicRep.findByLicenseNumber(clinic.getLicenseNumber()) != null) {

                       log.warn("Duplicate license number detected. ClinicId: {}, License: {}",
                               clinicId,
                               clinic.getLicenseNumber());

                       response.setMessage("LicenseNumber already exists");
                       response.setSuccess(false);
                       response.setStatus(409);
                       return response;
                   }
               }

               if (!savedClinic.getEmailAddress().equalsIgnoreCase(clinic.getEmailAddress())) {

                   if (clinicRep.findByEmailAddress(clinic.getEmailAddress()) != null) {

                       log.warn("Duplicate email detected. ClinicId: {}, Email: {}",
                               clinicId,
                               clinic.getEmailAddress());

                       response.setMessage("EmailAddress already exists");
                       response.setSuccess(false);
                       response.setStatus(409);
                       return response;
                   }
               }

               if (clinic.getAddress() != null)
                   savedClinic.setAddress(clinic.getAddress());

               if (clinic.getCity() != null)
                   savedClinic.setCity(clinic.getCity());

               if (clinic.getName() != null) {

                   log.info("Updating clinic name. ClinicId: {}, New Name: {}",
                           clinicId,
                           clinic.getName());

                   savedClinic.setName(clinic.getName());

                   List<ClinicCredentials> credsList =
                           clinicCredentialsRepository.findAllByUserName(savedClinic.getHospitalId());

                   for (ClinicCredentials creds : credsList) {

                       creds.setHospitalName(clinic.getName());

                       clinicCredentialsRepository.save(creds);
                   }
               }

               // Keep all your existing field update code exactly as it is...

               savedClinic.setRecommended(clinic.isRecommended());

               log.info("Saving updated clinic. ClinicId: {}", clinicId);

               clinicRep.save(savedClinic);

               log.info("Clinic updated successfully. ClinicId: {}", clinicId);

               response.setMessage("Clinic updated successfully");
               response.setSuccess(true);
               response.setStatus(200);

           } else {

               log.warn("Clinic not found for update. ClinicId: {}", clinicId);

               response.setMessage("Clinic not found for update");
               response.setSuccess(false);
               response.setStatus(404);
           }

       } catch (Exception e) {

           log.error("Error occurred while updating clinic. ClinicId: {}, Error: {}",
                   clinicId,
                   e.getMessage(),
                   e);

           response.setMessage("Error occurred while updating the clinic: " + e.getMessage());
           response.setSuccess(false);
           response.setStatus(500);
       }

       return response;
   }
	
	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "adminService", fallbackMethod = "deleteClinicFallback")
	public Response deleteClinic(String clinicId) {

	    log.info("Received request to delete clinic. ClinicId: {}", clinicId);

	    Response response = new Response();

	    try {

	        log.info("Searching clinic with ClinicId: {}", clinicId);

	        Clinic clinic = clinicRep.findByHospitalId(clinicId);

	        if (clinic != null) {

	            log.info("Clinic found. HospitalName: {}, ClinicId: {}",
	                    clinic.getName(), clinicId);

	            // Delete Clinic
	            clinicRep.deleteByHospitalId(clinicId);
	            log.info("Clinic deleted successfully. ClinicId: {}", clinicId);

	            // Delete clinic credentials
	            try {
	                clinicCredentialsRepository.deleteByUserName(clinicId);
	                log.info("Clinic credentials deleted successfully. Username: {}", clinicId);
	            } catch (Exception e) {
	                log.warn("Clinic credentials not found or could not be deleted. Username: {}",
	                        clinicId);
	            }

	            // Delete doctors
	            boolean doctorsDeleted = true;
	            try {

	                log.info("Deleting doctors for ClinicId: {}", clinicId);

	                ResponseEntity<Response> doctorDeleteResponse =
	                        clinicAdminFeign.deleteDoctorsByClinic(
	                                keyCloakTokenStore.getAccess_token(),
	                                clinicId);

	                doctorsDeleted = doctorDeleteResponse.getStatusCode().is2xxSuccessful();

	                log.info("Doctors deletion completed. Status: {}", doctorsDeleted);

	            } catch (Exception e) {

	                log.warn("Doctor deletion failed or no doctors found. ClinicId: {}",
	                        clinicId);

	                doctorsDeleted = e.getMessage().contains("404");
	            }

	            // Delete branches
	            boolean branchesDeleted = true;

	            try {

	                log.info("Deleting branches for ClinicId: {}", clinicId);

	                List<Branch> branches = branchRepository.findByClinicId(clinicId);

	                for (Branch branch : branches) {

	                    branchRepository.deleteByBranchId(branch.getBranchId());
                        clinicCredentialsRepository.deleteByUserName(branch.getBranchId());

	                    log.debug("Deleted Branch: {}", branch.getBranchId());
	                }

	                log.info("All branches deleted successfully.");

	            } catch (Exception e) {

	                branchesDeleted = false;

	                log.error("Failed to delete branches. ClinicId: {}",
	                        clinicId, e);
	            }

	            // Delete Diseases
	            boolean diseasesDeleted = true;

	            try {

	                log.info("Deleting diseases for ClinicId: {}", clinicId);

	                ResponseEntity<ResponseStructure<List<ProbableDiagnosisDTO>>> diseasesResponse =
	                        clinicAdminFeign.getDiseasesByHospitalId(clinicId);

	                if (diseasesResponse.getStatusCode().is2xxSuccessful()) {

	                    List<ProbableDiagnosisDTO> diseases =
	                            diseasesResponse.getBody().getData();

	                    for (ProbableDiagnosisDTO disease : diseases) {
	                        clinicAdminFeign.deleteDiseaseByDiseaseId(
	                                disease.getId(),
	                                clinicId);
	                    }

	                    log.info("Diseases deleted successfully.");
	                }

	            } catch (Exception e) {

	                diseasesDeleted = e.getMessage().contains("404");

	                log.warn("Disease deletion failed or no diseases found. ClinicId: {}",
	                        clinicId);
	            }

	            // Delete Lab Tests
	            boolean labTestsDeleted = true;

	            try {

	                log.info("Deleting lab tests for ClinicId: {}", clinicId);

	                ResponseEntity<ResponseStructure<List<LabTestDTO>>> labTestsResponse =
	                        clinicAdminFeign.getLabTestsByHospitalId(clinicId);

	                if (labTestsResponse.getStatusCode().is2xxSuccessful()) {

	                    List<LabTestDTO> labTests =
	                            labTestsResponse.getBody().getData();

	                    for (LabTestDTO labTest : labTests) {
	                        clinicAdminFeign.deleteLabTest(
	                                labTest.getId(),
	                                clinicId);
	                    }

	                    log.info("Lab tests deleted successfully.");
	                }

	            } catch (Exception e) {

	                labTestsDeleted = e.getMessage().contains("404");

	                log.warn("Lab test deletion failed or no lab tests found. ClinicId: {}",
	                        clinicId);
	            }

	            // Delete Treatments
	            boolean treatmentsDeleted = true;

	            try {

	                log.info("Deleting treatments for ClinicId: {}", clinicId);

	                ResponseEntity<ResponseStructure<List<TreatmentDTO>>> treatmentsResponse =
	                        clinicAdminFeign.getTreatmentsByHospitalId(clinicId);

	                if (treatmentsResponse.getStatusCode().is2xxSuccessful()) {

	                    List<TreatmentDTO> treatments =
	                            treatmentsResponse.getBody().getData();

	                    for (TreatmentDTO treatment : treatments) {
	                        clinicAdminFeign.deleteTreatmentById(
	                                treatment.getId(),
	                                clinicId);
	                    }

	                    log.info("Treatments deleted successfully.");
	                }

	            } catch (Exception e) {

	                treatmentsDeleted = e.getMessage().contains("404");

	                log.warn("Treatment deletion failed or no treatments found. ClinicId: {}",
	                        clinicId);
	            }

	            if (doctorsDeleted && branchesDeleted &&
	                    diseasesDeleted && labTestsDeleted &&
	                    treatmentsDeleted) {

	                log.info("Clinic and all associated entities deleted successfully. ClinicId: {}",
	                        clinicId);

	                response.setMessage("Clinic and all linked entities deleted successfully");
	                response.setSuccess(true);
	                response.setStatus(200);

	            } else {

	                log.warn("Clinic deleted, but some associated entities could not be deleted. ClinicId: {}",
	                        clinicId);

	                response.setMessage("Clinic deleted, but some linked entities failed to delete");
	                response.setSuccess(false);
	                response.setStatus(207);
	            }

	        } else {

	            log.warn("Clinic not found for deletion. ClinicId: {}", clinicId);

	            response.setMessage("Clinic not found for deletion");
	            response.setSuccess(false);
	            response.setStatus(404);
	        }

	    } catch (Exception e) {

	        log.error("Unexpected error while deleting clinic. ClinicId: {}. Error: {}",
	                clinicId, e.getMessage(), e);

	        response.setMessage("Error occurred while deleting the clinic: " + e.getMessage());
	        response.setSuccess(false);
	        response.setStatus(500);
	    }

	    return response;
	}

    //GENERATE RANDOM PASSWORD

    
	private static String generatePassword(int length) {

	    log.info("Generating secure temporary password.");

	    if (length < 4) {

	        log.error("Invalid password length: {}. Minimum length should be 4.", length);

	        throw new IllegalArgumentException("Password length must be at least 4.");
	    }

	    String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	    String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
	    String digits = "0123456789";
	    String specialChars = "!@#$&_";

	    Random random = new Random();

	    char firstChar = upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length()));
	    char specialChar = specialChars.charAt(random.nextInt(specialChars.length()));
	    char digit = digits.charAt(random.nextInt(digits.length()));

	    String allChars = upperCaseLetters + lowerCaseLetters + digits + specialChars;

	    StringBuilder remaining = new StringBuilder();

	    for (int i = 0; i < length - 3; i++) {
	        remaining.append(allChars.charAt(random.nextInt(allChars.length())));
	    }

	    List<Character> passwordChars = new ArrayList<>();

	    for (char c : remaining.toString().toCharArray()) {
	        passwordChars.add(c);
	    }

	    passwordChars.add(specialChar);
	    passwordChars.add(digit);

	    Collections.shuffle(passwordChars);

	    StringBuilder password = new StringBuilder();
	    password.append(firstChar);

	    for (char c : passwordChars) {
	        password.append(c);
	    }

	    log.info("Temporary password generated successfully.");

	    // Never log or print the generated password.

	    return password.toString();
	}

// CLINIC CREDENTIALS CRUD

    

	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "adminService", fallbackMethod = "getClinicCredentialsFallback")
	public Response getClinicCredentials(String userName) {

	    log.info("Received request to fetch clinic credentials. UserName: {}", userName);

	    Response response = new Response();

	    try {

	        log.info("Searching clinic credentials for UserName: {}", userName);

	        ClinicCredentials clinicCredentials =
	                clinicCredentialsRepository.findByUserName(userName);

	        if (clinicCredentials != null) {

	            log.info("Clinic credentials found for UserName: {}", userName);

	            ClinicCredentialsDTO clinicCredentialsDTO = new ClinicCredentialsDTO();

	            clinicCredentialsDTO.setUserName(clinicCredentials.getUserName());

	            // Never log or expose password in logs
	            clinicCredentialsDTO.setPassword(clinicCredentials.getPassword());

	            clinicCredentialsDTO.setHospitalName(clinicCredentials.getHospitalName());

	            response.setSuccess(true);
	            response.setData(clinicCredentialsDTO);
	            response.setMessage("Clinic Credentials Found.");
	            response.setStatus(200);

	            log.info("Clinic credentials returned successfully for UserName: {}",
	                    userName);

	            return response;

	        } else {

	            log.warn("Clinic credentials not found for UserName: {}", userName);

	            response.setSuccess(true);
	            response.setMessage("Clinic Credentials Are Not Found.");
	            response.setStatus(200);

	            return response;
	        }

	    } catch (Exception e) {

	        log.error("Error while retrieving clinic credentials for UserName: {}. Error: {}",
	                userName, e.getMessage(), e);

	        response.setSuccess(false);
	        response.setMessage("Error Retrieving Clinic Credentials: " + e.getMessage());
	        response.setStatus(500);
	    }

	    return response;
	}
	
	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "adminService", fallbackMethod = "updateClinicCredentialsFallback")
	public Response updateClinicCredentials(UpdateClinicCredentials credentials, String userName) {

	    log.info("Received request to update clinic credentials. UserName: {}", userName);

	    Response response = new Response();

	    try {

	        log.info("Validating clinic credentials for UserName: {}", userName);

	        ClinicCredentials existingCredentials =
	                clinicCredentialsRepository.findByUserNameAndPassword(
	                        userName,
	                        credentials.getPassword());

	        ClinicCredentials existUserName =
	                clinicCredentialsRepository.findByUserName(userName);

	        if (existUserName == null) {

	            log.warn("Invalid username provided: {}", userName);

	            response.setSuccess(false);
	            response.setMessage("Incorrect UserName");
	            response.setStatus(401);

	            return response;
	        }

	        if (existingCredentials != null) {

	            log.info("Username and current password validated successfully for UserName: {}",
	                    userName);

	            if (credentials.getNewPassword().equalsIgnoreCase(credentials.getConfirmPassword())) {

	                log.info("New password and confirm password matched for UserName: {}",
	                        userName);

	                existingCredentials.setPassword(credentials.getNewPassword());

	                ClinicCredentials updatedCredentials =
	                        clinicCredentialsRepository.save(existingCredentials);

	                if (updatedCredentials != null) {

	                    log.info("Clinic credentials updated successfully for UserName: {}",
	                            userName);

	                    response.setSuccess(true);
	                    response.setData(null);
	                    response.setMessage("Clinic Credentials Updated Successfully.");
	                    response.setStatus(200);

	                    return response;

	                } else {

	                    log.error("Failed to update clinic credentials for UserName: {}",
	                            userName);

	                    response.setSuccess(false);
	                    response.setMessage("Failed To Update Clinic Credentials.");
	                    response.setStatus(404);

	                    return response;
	                }

	            } else {

	                log.warn("New password and confirm password do not match for UserName: {}",
	                        userName);

	                response.setSuccess(false);
	                response.setMessage("New password and confirm password do not match.");
	                response.setStatus(401);

	                return response;
	            }

	        } else {

	            log.warn("Incorrect current password provided for UserName: {}",
	                    userName);

	            response.setSuccess(false);
	            response.setMessage("Incorrect Password.");
	            response.setStatus(401);

	            return response;
	        }

	    } catch (Exception e) {

	        log.error("Error while updating clinic credentials for UserName: {}. Error: {}",
	                userName, e.getMessage(), e);

	        response.setSuccess(false);
	        response.setMessage("Error updating clinic credentials: " + e.getMessage());
	        response.setStatus(500);

	        return response;
	    }
	}
	@Override
	@Secured({"ROLE_ADMIN","ROLE_CLINICADMIN"})
	@RateLimiter(name = "adminService", fallbackMethod = "deleteClinicCredentialsFallback")
	public Response deleteClinicCredentials(String userName) {

	    log.info("Received request to delete clinic credentials. UserName: {}", userName);

	    Response response = new Response();

	    try {

	        log.info("Searching clinic credentials for UserName: {}", userName);

	        ClinicCredentials clinicCredentials =
	                clinicCredentialsRepository.findByUserName(userName);

	        if (clinicCredentials != null) {

	            log.info("Clinic credentials found. Deleting credentials for UserName: {}",
	                    userName);

	            clinicCredentialsRepository.delete(clinicCredentials);

	            log.info("Clinic credentials deleted successfully for UserName: {}",
	                    userName);

	            log.info("Deleting clinic details for HospitalId: {}", userName);

	            clinicRep.deleteByHospitalId(userName);

	            log.info("Clinic deleted successfully. HospitalId: {}", userName);

	            response.setSuccess(true);
	            response.setMessage("Clinic Credentials Deleted Successfully.");
	            response.setStatus(200);

	            log.info("Delete clinic credentials process completed successfully. UserName: {}",
	                    userName);

	            return response;

	        } else {

	            log.warn("Clinic credentials not found for UserName: {}", userName);

	            response.setSuccess(false);
	            response.setMessage("Clinic Credentials Are Not Found.");
	            response.setStatus(404);

	            return response;
	        }

	    } catch (Exception e) {

	        log.error("Error while deleting clinic credentials for UserName: {}. Error: {}",
	                userName, e.getMessage(), e);

	        response.setSuccess(false);
	        response.setMessage("Error Deleting Clinic Credentials: " + e.getMessage());
	        response.setStatus(500);
	    }

	    return response;
	}

	// CUSTOMER MANAGEMENT

//    @Override
//	public Response saveCustomerBasicDetails(CustomerDTO customerDTO ) {
//
//		 Response response = new  Response();
//
//	    	try {
//
//	    		ResponseEntity<Response> res = customerFeign.saveCustomerBasicDetails(customerDTO);
//
//	    		  if(res != null) {
//
//	    			  Response rs = res.getBody();
//
//	    			  return rs;
//
//	    		  }}catch(FeignException e) {
//
//	    	            response.setStatus(e.status());
//
//		    			response.setMessage(ExtractFeignMessage.clearMessage(e));
//
//		    			response.setSuccess(false);
//
//	    	        }
//
//	                    return response;}
//
//    
//
//    @Override	
//
//	public ResponseEntity<?> getCustomerByUsernameMobileEmail(String input) {
//
//    	Response response = new Response();
//
//	    	try {
//
//	    		ResponseEntity<?> res = customerFeign.getCustomerByUsernameMobileEmail(input);
//
//	    		  if(res.getBody()!= null) {
//
//	    			  response.setData(res.getBody());
//
//	    			  response.setStatus(res.getStatusCode().value());
//
//	    			  return ResponseEntity.status(res.getStatusCode().value()).body(res.getBody());}
//
//	    		  else {
//
//	    			  response.setMessage("Customer Details Not Found");
//
//	    			  response.setStatus(200);
//
//	    			  response.setSuccess(true);
//
//	    			  return ResponseEntity.status(200).body(response);}
//
//	    		  }catch(FeignException e) {
//
//	    			  response.setMessage(e.getMessage());
//
//	    			  response.setStatus(e.status());
//
//	    			  response.setSuccess(false);
//
//	    			  return ResponseEntity.status(e.status()).body(response);
//
//	    	        }}
//
//   
//
//    
//
//    @Override
//
//	public Response getCustomerBasicDetails(String mobileNumber ) {
//
//		 Response response = new  Response();
//
//	    	try {
//
//	    		ResponseEntity<Response> res = customerFeign.getCustomerBasicDetails(mobileNumber);
//
//	    		  if(res != null) {
//
//	    			  Response rs = res.getBody();
//
//	    			  return rs;
//
//	    		  }}catch(FeignException e) {
//
//	    	            response.setStatus(e.status());
//
//		    			response.setMessage(ExtractFeignMessage.clearMessage(e));
//
//		    			response.setSuccess(false);
//
//	    	        }
//
//	                    return response;	
//
//}
//
//
//
//    @Override
//
//	public Response getAllCustomers(){
//
//		 Response response = new  Response();
//
//	    	try {
//
//	    		ResponseEntity<Response> res = customerFeign.getAllCustomers();
//
//	    		  if(res != null) {
//
//	    			  Response rs = res.getBody();
//
//	    			  return rs;
//
//	    		  }}catch(FeignException e) {
//
//	    	            response.setStatus(e.status());
//
//		    			response.setMessage(ExtractFeignMessage.clearMessage(e));
//
//		    			response.setSuccess(false);
//
//	    	        }
//
//	                    return response;	
//
//}
//
//	
//
//    @Override
//
//	public Response updateCustomerBasicDetails(CustomerDTO customerDTO,String mobileNumber ){
//
//		 Response response = new  Response();
//
//	    	try {
//
//	    		ResponseEntity<Response> res = customerFeign.updateCustomerBasicDetails(customerDTO, mobileNumber);
//
//	    		  if(res != null) {
//
//	    			  Response rs = res.getBody();
//
//	    			  return rs;
//
//	    		  }}catch(FeignException e) {
//
//	    	            response.setStatus(e.status());
//
//		    			response.setMessage(ExtractFeignMessage.clearMessage(e));
//
//		    			response.setSuccess(false);
//
//	    	        }
//
//	                    return response;	
//
//}
//
//	
//
//    @Override
//
//	public Response deleteCustomerBasicDetails(String mobileNumber){
//
//		 Response response = new  Response();
//
//	    	try {
//
//	    		ResponseEntity<Response> res = customerFeign.deleteCustomerBasicDetails(mobileNumber);
//
//	    		  if(res != null) {
//
//	    			  Response rs = res.getBody();
//
//	    			  return rs;
//
//	    		  }}catch(FeignException e) {
//
//	    	            response.setStatus(e.status());
//
//		    			response.setMessage(ExtractFeignMessage.clearMessage(e));
//
//		    			response.setSuccess(false);
//
//	    	        }
//
//	                    return response;	
//
//}

    

    

//GETALLSUBSERVICES

    

//    @Override
//
//   	public Response getAllSubServicesFromClincAdmin(){
//
//   		 Response response = new  Response();
//
//   	    	try {
//
//   	    		ResponseEntity<ResponseStructure<List<SubServicesDto>>> res = clinicAdminFeign.getAllSubServices();
//
//   	    		  if(res.getBody().getData() != null ) {
//
//   	    			 response.setStatus(res.getBody().getHttpStatus().value());
//
//   	    			response.setData(res.getBody());
//
//   	    			  return response;
//
//   	    		  }}catch(FeignException e) {
//
//   	    	            response.setStatus(e.status());
//
//   		    			response.setMessage(ExtractFeignMessage.clearMessage(e));
//
//   		    			response.setSuccess(false);
//
//   	    	        }
//
//   	                    return response;	
//
//   }
//
// 

    ///GETDOCTORINFO  
	@Secured("ROLE_ADMIN")
	@RateLimiter(name = "adminService", fallbackMethod = "getDoctorInfoByDoctorIdFallback")
    public Response getDoctorInfoByDoctorId(String doctorId) {

        Response response = new Response();

        try {

        	ResponseEntity<Response>  res = clinicAdminFeign.getDoctorById(keyCloakTokenStore.getAccess_token(),doctorId);  

                    if (res.getBody() != null ) {

                    if(res.getBody().getData() != null) {

                    DoctorsDTO dto = new ObjectMapper().convertValue(res.getBody().getData(), DoctorsDTO.class);

                    DoctortInfo doctortInfo  = new DoctortInfo();

                    doctortInfo.setDoctorPicture(dto.getDoctorPicture());

                    doctortInfo.setDoctorName(dto.getDoctorName());

                    doctortInfo.setExperience(dto.getExperience());

                    doctortInfo.setProfileDescription(dto.getProfileDescription());

                    doctortInfo.setSpecialization(dto.getSpecialization());

                    response.setData(doctortInfo);

                    response.setStatus(200);

                    response.setMessage("Doctor Details Fetched Successfully");

                    response.setSuccess(true);}

                    }else {   	

                    response.setData(res.getBody());

                    response.setStatus(res.getBody().getStatus());}	                    

            } catch(FeignException e) {

    		response.setStatus(e.status());

    		response.setMessage(ExtractFeignMessage.clearMessage(e));

    		response.setSuccess(false);}

        return response;

    }



    //-----------------------------GET CLINICS BUY RECOMMONDATION == TRUE---------------------------------

  	@Override
	@RateLimiter(name = "adminService", fallbackMethod = "getClinicsByRecommondationFallback")

  	public Response getClinicsByRecommondation() {



  		List<Clinic> clinics = clinicRep.findByRecommendedTrue();

  		List<ClinicDTO> clinicsDTO = new ArrayList<>();

  		for (Clinic clinic : clinics) {

  			ClinicDTO toDto = new ClinicDTO();

  			toDto.setHospitalId(clinic.getHospitalId());

  			toDto.setName(clinic.getName());

  			toDto.setAddress(clinic.getAddress());

  			toDto.setCity(clinic.getCity());

  			toDto.setContactNumber(clinic.getContactNumber());

  			toDto.setHospitalOverallRating(clinic.getHospitalOverallRating());

  			toDto.setOpeningTime(clinic.getOpeningTime());

  			toDto.setClosingTime(clinic.getClosingTime());

  			toDto.setEmailAddress(clinic.getEmailAddress());

  			toDto.setWebsite(clinic.getWebsite());

  			toDto.setLicenseNumber(clinic.getLicenseNumber());

  			toDto.setIssuingAuthority(clinic.getIssuingAuthority());

  			// Hospital Logo

  			toDto.setHospitalLogo(

  					clinic.getHospitalLogo() != null ? Base64.getEncoder().encodeToString(clinic.getHospitalLogo())

  							: "");



  			 // Hospital Documents — single binary

  	        toDto.setHospitalDocuments(

  	            clinic.getHospitalDocuments() != null 

  	                ? Base64.getEncoder().encodeToString(clinic.getHospitalDocuments()) 

  	                : ""

  	        );



  	        toDto.setRecommended(clinic.isRecommended());



  	        clinicsDTO.add(toDto);

  		}

  		Response response = new Response();

  		response.setSuccess(true);

  		response.setData(clinicsDTO);

  		response.setStatus(200);

  		response.setMessage("Clinics Retrive successfully");

  		return response;

  	}

//	---------------------------get All Clincs first recommonded then another clincs----------------------------------
  	@Override
	@RateLimiter(name = "adminService", fallbackMethod = "getAllRecommendClinicThenAnotherClincsFallback")
  	public Response getAllRecommendClinicThenAnotherClincs() {
  	    Response response = new Response();
  	    try {
  	        List<Clinic> clinics = clinicRep.findAllByOrderByRecommendedDescNameAsc();

  	        List<ClinicDTO> dtoList = clinics.stream().map(clinic -> {
  	            ClinicDTO dto = new ClinicDTO();

  	            dto.setHospitalId(clinic.getHospitalId());
  	            dto.setName(clinic.getName());
  	            dto.setAddress(clinic.getAddress());
  	            dto.setCity(clinic.getCity());
  	            dto.setHospitalOverallRating(clinic.getHospitalOverallRating());
  	            dto.setContactNumber(clinic.getContactNumber());
  	            dto.setOpeningTime(clinic.getOpeningTime());
  	            dto.setClosingTime(clinic.getClosingTime());

  	            // Convert byte[] → Base64
  	            dto.setHospitalLogo(clinic.getHospitalLogo() != null ? Base64.getEncoder().encodeToString(clinic.getHospitalLogo()) : null);
  	            dto.setEmailAddress(clinic.getEmailAddress());
  	            dto.setWebsite(clinic.getWebsite());
  	            dto.setLicenseNumber(clinic.getLicenseNumber());
  	            dto.setIssuingAuthority(clinic.getIssuingAuthority());

  	            dto.setContractorDocuments(clinic.getContractorDocuments() != null ? Base64.getEncoder().encodeToString(clinic.getContractorDocuments()) : null);
  	            dto.setHospitalDocuments(clinic.getHospitalDocuments() != null ? Base64.getEncoder().encodeToString(clinic.getHospitalDocuments()) : null);

  	            dto.setRecommended(clinic.isRecommended());
  	            dto.setClinicalEstablishmentCertificate(clinic.getClinicalEstablishmentCertificate() != null ? Base64.getEncoder().encodeToString(clinic.getClinicalEstablishmentCertificate()) : null);
  	            dto.setBusinessRegistrationCertificate(clinic.getBusinessRegistrationCertificate() != null ? Base64.getEncoder().encodeToString(clinic.getBusinessRegistrationCertificate()) : null);

  	            dto.setClinicType(clinic.getClinicType());
  	            dto.setMedicinesSoldOnSite(clinic.getMedicinesSoldOnSite());
  	            dto.setDrugLicenseCertificate(clinic.getDrugLicenseCertificate() != null ? Base64.getEncoder().encodeToString(clinic.getDrugLicenseCertificate()) : null);
  	            dto.setDrugLicenseFormType(clinic.getDrugLicenseFormType() != null ? Base64.getEncoder().encodeToString(clinic.getDrugLicenseFormType()) : null);

  	            dto.setHasPharmacist(clinic.getHasPharmacist());
  	            dto.setPharmacistCertificate(clinic.getPharmacistCertificate() != null ? Base64.getEncoder().encodeToString(clinic.getPharmacistCertificate()) : null);

  	            dto.setBiomedicalWasteManagementAuth(clinic.getBiomedicalWasteManagementAuth() != null ? Base64.getEncoder().encodeToString(clinic.getBiomedicalWasteManagementAuth()) : null);
  	            dto.setTradeLicense(clinic.getTradeLicense() != null ? Base64.getEncoder().encodeToString(clinic.getTradeLicense()) : null);
  	            dto.setFireSafetyCertificate(clinic.getFireSafetyCertificate() != null ? Base64.getEncoder().encodeToString(clinic.getFireSafetyCertificate()) : null);
  	            dto.setProfessionalIndemnityInsurance(clinic.getProfessionalIndemnityInsurance() != null ? Base64.getEncoder().encodeToString(clinic.getProfessionalIndemnityInsurance()) : null);
  	            dto.setGstRegistrationCertificate(clinic.getGstRegistrationCertificate() != null ? Base64.getEncoder().encodeToString(clinic.getGstRegistrationCertificate()) : null);

  	            dto.setConsultationExpiration(clinic.getConsultationExpiration());
  	            dto.setSubscription(clinic.getSubscription());

  	            // Convert List<byte[]> → List<String>
  	            dto.setOthers(clinic.getOthers() != null ?
  	                    clinic.getOthers().stream()
  	                            .map(b -> Base64.getEncoder().encodeToString(b))
  	                            .collect(Collectors.toList())
  	                    : null);

  	            dto.setFreeFollowUps(clinic.getFreeFollowUps());
  	            dto.setLatitude(clinic.getLatitude());
  	            dto.setLongitude(clinic.getLongitude());
  	            dto.setNabhScore(clinic.getNabhScore());
  	         
  	            dto.setWalkthrough(clinic.getWalkthrough());

  	            dto.setInstagramHandle(clinic.getInstagramHandle());
  	            dto.setTwitterHandle(clinic.getTwitterHandle());
  	            dto.setFacebookHandle(clinic.getFacebookHandle());

  	            return dto;
  	        }).collect(Collectors.toList());

  	        response.setSuccess(true);
  	        response.setData(dtoList);
  	        response.setMessage("Clinics fetched successfully (Recommended first).");
  	        response.setStatus(200);

  	    } catch (Exception e) {
  	        response.setSuccess(false);
  	        response.setMessage("Error occurred while fetching clinics: " + e.getMessage());
  	        response.setStatus(500);
  	    }
  	    return response;
  	}
  	
  	


	// === Helper methods ===

//	private ResponseEntity<ResponseStructure<SubServicesDto>> buildErrorResponse(String message, int statusCode) {
//		ResponseStructure<SubServicesDto> errorResponse = ResponseStructure.<SubServicesDto>builder().data(null)
//				.message(extractCleanMessage(message)).httpStatus(HttpStatus.valueOf(statusCode)).statusCode(statusCode)
//				.build();
//		return ResponseEntity.status(statusCode).body(errorResponse);
//	}

//	private ResponseEntity<ResponseStructure<List<SubServicesDto>>> buildErrorResponseList(String message,
//			int statusCode) {
//		ResponseStructure<List<SubServicesDto>> errorResponse = ResponseStructure.<List<SubServicesDto>>builder()
//				.data(null) // <-- changed from null to empty list
//				.message(extractCleanMessage(message)).httpStatus(HttpStatus.valueOf(statusCode)).statusCode(statusCode)
//				.build();
//		return ResponseEntity.status(statusCode).body(errorResponse);
//	}

	private String extractCleanMessage(String rawMessage) {
		// Try to extract the "message" value from JSON string if included
		try {
			int msgStart = rawMessage.indexOf("\"message\":\"");
			if (msgStart != -1) {
				int start = msgStart + 10;
				int end = rawMessage.indexOf("\"", start);
				return rawMessage.substring(start, end);
			}
		} catch (Exception ignored) {
		}
		return rawMessage;}
	
	@RateLimiter(name = "adminService", fallbackMethod = "generateHospitalIdFallback")
	public String generateHospitalId() {

	    log.info("Generating new Hospital ID.");

	    try {

	        // Create a query for the counter document
	        Query query = new Query();
	        query.addCriteria(Criteria.where("_id").is("clinicId"));

	        // Increment the sequence by 1
	        Update update = new Update().inc("seq", 1);

	        // Atomically find & increment, return the updated document
	        FindAndModifyOptions options = FindAndModifyOptions.options()
	                .upsert(true)
	                .returnNew(true);

	        Counter counter = mongoOperations.findAndModify(
	                query,
	                update,
	                options,
	                Counter.class);

	        String hospitalId = String.format("%04d", counter.getSeq());

	        log.info("Hospital ID generated successfully. HospitalId: {}", hospitalId);

	        return hospitalId;

	    } catch (Exception e) {

	        log.error("Error while generating Hospital ID. Error: {}",
	                e.getMessage(), e);

	        throw e;
	    }
	}
	

	// Rate limiter fallback methods

	public Response buildRateLimitResponse(Exception ex) {
    Response response = new Response();
    response.setStatus(429);
    response.setMessage("Rate limit exceeded. Please try again after some time.");
    response.setData(null);
    return response;
}

	public Response createClinicFallback(ClinicDTO clinic, Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response startVerificationProcessFallback(String clinicId, Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response verifyClinicFallback(String clinicId, Exception ex) {
    return buildRateLimitResponse(ex);
}
	public Response rejectClinicFallback(String clinicId, String reason, Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response getClinicByIdFallback(String clinicId, Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response getAllClinicsFallback(Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response updateClinicFallback(String clinicId, ClinicDTO clinic, Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response deleteClinicFallback(String clinicId, Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response getClinicCredentialsFallback(String userName, Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response updateClinicCredentialsFallback(UpdateClinicCredentials credentials, String userName, Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response deleteClinicCredentialsFallback(String userName, Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response getDoctorInfoByDoctorIdFallback(String doctorId, Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response getClinicsByRecommondationFallback(Exception ex) {
    return buildRateLimitResponse(ex);
}

	public Response getAllRecommendClinicThenAnotherClincsFallback(Exception ex) {
    return buildRateLimitResponse(ex);
}

	}



