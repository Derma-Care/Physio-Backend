package com.AdminService.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.transaction.annotation.Transactional;

import com.AdminService.dto.BranchDTO;
import com.AdminService.entity.Branch;
import com.AdminService.entity.BranchCounter;
import com.AdminService.entity.BranchCredentials;
import com.AdminService.entity.Clinic;
import com.AdminService.entity.ClinicCredentials;
import com.AdminService.repository.BranchCredentialsRepository;
import com.AdminService.repository.BranchRepository;
import com.AdminService.repository.ClinicCredentialsRepository;
import com.AdminService.repository.ClinicRep;
import com.AdminService.util.PermissionsUtil;
import com.AdminService.util.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j

public class BranchServiceImpl implements BranchService {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ClinicRep clinicRep;

    @Autowired
    private MongoOperations mongoOperations;

	
	@Autowired
	private ClinicCredentialsRepository clinicCredentialsRepository;
	
//	@Autowired
//	private BranchCredentialsRepository branchCredentialsRepository;
//	    

    @Autowired
    private EmailService emailService;
    
    @Autowired
	private PasswordEncoder passwordEncoder;


    private static class PasswordGenerator {
        private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
        private static final String DIGITS = "0123456789";
        private static final String SYMBOLS = "!@#$%^&*()_-+=<>?";

        private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;
        private static final Random random = new Random();

        public static String generatePassword(int length) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append(ALL.charAt(random.nextInt(ALL.length())));
            }
            return sb.toString();
        }
    }

 // ---------------------- CREATE BRANCH  ----------------------
    @Override
    @Transactional
    @Secured("ROLE_ADMIN")
    public Response createBranch(BranchDTO dto) {

        Response res = new Response();

        try {
            log.info("===== Create Branch API Started =====");

            if (dto.getClinicId() == null || dto.getClinicId().isBlank()) {
                log.warn("Clinic ID is missing.");
                res.setMessage("clinicId is required");
                res.setSuccess(false);
                res.setStatus(400);
                return res;
            }

            String clinicId = dto.getClinicId();
            log.info("Received Clinic ID: {}", clinicId);

            Clinic clinic = clinicRep.findByHospitalId(clinicId);

            if (clinic == null) {
                log.error("Clinic not found for Clinic ID: {}", clinicId);
                res.setMessage("Clinic with ID " + clinicId + " not found");
                res.setSuccess(false);
                res.setStatus(404);
                return res;
            }

            log.info("Clinic found: {}", clinic.getHospitalId());

            // Generate Branch Counter
            log.info("Generating Branch ID...");

            BranchCounter counter = mongoOperations.findAndModify(
                    Query.query(Criteria.where("_id").is(clinicId)),
                    new Update().inc("seq", 1),
                    FindAndModifyOptions.options().returnNew(true).upsert(true),
                    BranchCounter.class
            );

            String branchId = String.format(
                    "%04d%02d",
                    Integer.parseInt(clinicId),
                    counter.getSeq()
            );

            log.info("Generated Branch ID: {}", branchId);

            // Create Branch
            Branch branch = convertDtoToEntity(dto, branchId);
            branch.setRole("ROLE_ADMIN");
            branch.setPermissions(PermissionsUtil.getAdminPermissions());
            branch.setStatus("ACTIVE");

            log.info("Branch entity created.");

            // Email Validation
            String emailToUse = branch.getEmail();

            if (emailToUse == null || emailToUse.isBlank()) {
                log.info("Branch email not provided. Using clinic email.");
                emailToUse = clinic.getEmailAddress();
            }

            if (emailToUse == null || emailToUse.isBlank()) {
                log.error("No email found for Branch ID: {}", branchId);
                throw new RuntimeException("No email found for branch or clinic");
            }

            branch.setEmail(emailToUse);
            log.info("Email assigned: {}", emailToUse);

            Branch savedBranch = branchRepository.save(branch);
            log.info("Branch saved successfully with Branch ID: {}", savedBranch.getBranchId());

            // Generate Password
            String tempPassword = PasswordGenerator.generatePassword(10);
            log.info("Temporary password generated.");

            // Save Credentials
            ClinicCredentials credentials = new ClinicCredentials();
            credentials.setUserName(branchId);
            credentials.setPassword(passwordEncoder.encode(tempPassword));
            credentials.setHospitalName(savedBranch.getHospitalName());
            credentials.setRoles(Collections.singletonList("ROLE_CLINICADMIN"));

            Map<String, Map<String, List<String>>> permissionWrapper = new HashMap<>();
            permissionWrapper.put("ADMIN", PermissionsUtil.getAdminPermissions());
            credentials.setPermissions(permissionWrapper);

            clinicCredentialsRepository.save(credentials);

            log.info("Login credentials saved for Branch ID: {}", branchId);

            // Send Email
            if (savedBranch.getEmail() != null && !savedBranch.getEmail().isBlank()) {

                log.info("Sending login credentials email to {}", savedBranch.getEmail());

                Map<String, String> mailData = new HashMap<>();
                mailData.put("subject", "Branch Login Credentials");

                mailData.put("message",
                        "Welcome to CCMS KINETIX!\n\n" +
                        "Your account has been created successfully.\n" +
                        "Please use the below credentials to login.\n\n" +
                        "Branch ID: " + branchId);

                mailData.put("username", branchId);
                mailData.put("password", tempPassword);

                emailService.sendEmail(savedBranch.getEmail(), mailData);

                log.info("Email sent successfully.");
            } else {
                log.warn("Email not sent because email address is empty.");
            }

            // Attach Branch to Clinic
            List<Branch> branches = clinic.getBranches();

            if (branches == null) {
                branches = new ArrayList<>();
            }

            branches.add(savedBranch);
            clinic.setBranches(branches);

            clinicRep.save(clinic);

            log.info("Branch attached to Clinic successfully.");

            res.setSuccess(true);
            res.setStatus(200);
            res.setMessage("Branch created successfully and credentials sent to email");
            res.setHospitalId(clinicId);
            res.setBranchId(branchId);

            log.info("===== Create Branch API Completed Successfully =====");

            return res;

        } catch (Exception e) {

            log.error("Exception occurred while creating branch.", e);

            res.setSuccess(false);
            res.setStatus(500);
            res.setMessage("Error while creating branch: " + e.getMessage());

            return res;
        }
    }
// // ---------------------- START BRANCH VERIFICATION ----------------------
//    @Override
//    public Response startBranchVerification(String branchId) {
//
//        Response res = new Response();
//
//        Optional<Branch> optionalBranch = branchRepository.findByBranchId(branchId);
//
//        if (optionalBranch.isEmpty()) {
//            res.setSuccess(false);
//            res.setStatus(404);
//            res.setMessage("Branch not found");
//            return res;
//        }
//
//        Branch branch = optionalBranch.get();
//
//        // ✅ Null-safe + strict state check
//        if (branch.getStatus() == null || !"PENDING".equals(branch.getStatus())) {
//            res.setSuccess(false);
//            res.setStatus(400);
//            res.setMessage("Branch is not in pending state");
//            return res;
//        }
//
//        // ✅ Update status
//        branch.setStatus("VERIFICATION_IN_PROGRESS");
//        branchRepository.save(branch);
//
//        // 📧 Optional: email notification
//        Map<String, String> mailData = new HashMap<>();
//        mailData.put("subject", "Branch Verification Started");
//        mailData.put(
//                "message",
//                "Your branch verification has started. Our team is reviewing your details."
//        );
//
//        if (branch.getEmail() != null) {
//            emailService.sendEmail(branch.getEmail(), mailData);
//        }
//
//        // ✅ Response
//        res.setSuccess(true);
//        res.setStatus(200);
//        res.setMessage("Branch verification started");
//        res.setBranchId(branchId);
//
//        return res;
//    }

// // ---------------------- VERIFY BRANCH (GENERATE CREDENTIALS) ----------------------
//    @Override
//    public Response verifyBranch(String branchId) {
//
//        Response res = new Response();
//
//        try {
//            Optional<Branch> optionalBranch = branchRepository.findByBranchId(branchId);
//
//            if (optionalBranch.isEmpty()) {
//                res.setSuccess(false);
//                res.setStatus(404);
//                res.setMessage("Branch not found");
//                return res;
//            }
//
//            Branch branch = optionalBranch.get();
//
//            if (!"VERIFICATION_IN_PROGRESS".equals(branch.getStatus())) {
//                res.setSuccess(false);
//                res.setStatus(400);
//                res.setMessage("Branch is not under verification");
//                return res;
//            }
//
//            // 🔐 Generate branch credentials
//            String tempPassword = PasswordGenerator.generatePassword(10);
//
//            BranchCredentials credentials = new BranchCredentials();
//            credentials.setBranchId(branchId);
//            credentials.setUserName(branchId);
//            credentials.setPassword(tempPassword);
//            credentials.setBranchName(branch.getBranchName());
//            credentials.setRole(branch.getRole());
//            credentials.setPermissions(branch.getPermissions());
//
//            branchCredentialsRepository.save(credentials);
//
//            // ✅ Update branch status
//            branch.setStatus("VERIFIED");
//            branchRepository.save(branch);
//
//            // 📧 SEND EMAIL WITH CREDENTIALS
//            Map<String, String> mailData = new HashMap<>();
//            mailData.put("subject", "Branch Verified Successfully");
//            mailData.put(
//                    "message",
//                    "Your branch has been verified successfully.\n" +
//                    "Please use the credentials below to log in."
//            );
//            mailData.put("username", credentials.getUserName());
//            mailData.put("password", tempPassword);
//
//            if (branch.getEmail() != null && !branch.getEmail().isBlank()) {
//                emailService.sendEmail(branch.getEmail(), mailData);
//            }
//
//            // ✅ Response
//            res.setSuccess(true);
//            res.setStatus(200);
//            res.setMessage("Branch verified successfully");
//            res.setBranchId(branchId);
//
//            return res;
//
//        } catch (Exception e) {
//            res.setSuccess(false);
//            res.setStatus(500);
//            res.setMessage("Failed to verify branch: " + e.getMessage());
//            return res;
//        }
//    }
//
//    @Override
//    public Response rejectBranch(String branchId, String reason) {
//
//        Response res = new Response();
//
//        try {
//            Optional<Branch> optionalBranch = branchRepository.findByBranchId(branchId);
//
//            if (optionalBranch.isEmpty()) {
//                res.setSuccess(false);
//                res.setStatus(404);
//                res.setMessage("Branch not found");
//                return res;
//            }
//
//            Branch branch = optionalBranch.get();
//
//            if ("VERIFIED".equals(branch.getStatus())) {
//                res.setSuccess(false);
//                res.setStatus(400);
//                res.setMessage("Verified branch cannot be rejected");
//                return res;
//            }
//
//            // ❌ Reject branch
//            branch.setStatus("REJECTED");
//            branchRepository.save(branch);
//
//            // 📧 (Optional) Email notification
//            Map<String, String> mailData = new HashMap<>();
//            mailData.put("subject", "Branch Registration Rejected");
//            mailData.put(
//                    "message",
//                    "Your branch registration has been rejected.\nReason: " + reason
//            );
//
//            emailService.sendEmail(branch.getEmail(), mailData);
//
//            res.setSuccess(true);
//            res.setStatus(200);
//            res.setMessage("Branch rejected successfully");
//            res.setBranchId(branchId);
//
//            return res;
//
//        } catch (Exception e) {
//            res.setSuccess(false);
//            res.setStatus(500);
//            res.setMessage("Failed to reject branch: " + e.getMessage());
//            return res;
//        }
//    }



    // ---------------------- GET BRANCH BY ID ----------------------
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<?> getBranchById(String branchId) {

        log.info("===== Get Branch By ID API Started =====");
        log.info("Fetching branch with Branch ID: {}", branchId);

        Response response = new Response();

        try {

            Optional<Branch> branch = branchRepository.findByBranchId(branchId);

            if (branch.isPresent()) {

                log.info("Branch found for Branch ID: {}", branchId);

                response.setMessage("Branch found");
                response.setSuccess(true);
                response.setStatus(200);
                response.setData(convertEntityToDto(branch.get()));

                log.info("Branch details returned successfully.");

            } else {

                log.warn("Branch not found for Branch ID: {}", branchId);

                response.setMessage("Branch not found");
                response.setSuccess(false);
                response.setStatus(404);
            }

        } catch (Exception e) {

            log.error("Error occurred while fetching branch with Branch ID: {}", branchId, e);

            response.setMessage("Error fetching branch: " + e.getMessage());
            response.setSuccess(false);
            response.setStatus(500);
        }

        log.info("===== Get Branch By ID API Completed =====");

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------- UPDATE BRANCH ----------------------
    @Override
    @Secured("ROLE_ADMIN")
    public Response updateBranch(String branchId, BranchDTO branchDto) {

        log.info("===== Update Branch API Started =====");
        log.info("Updating branch with Branch ID: {}", branchId);

        Response response = new Response();

        try {

            Optional<Branch> existingOpt = branchRepository.findByBranchId(branchId);

            if (existingOpt.isPresent()) {

                log.info("Branch found. Updating details for Branch ID: {}", branchId);

                Branch branch = existingOpt.get();

                if (branchDto.getClinicId() != null && !branchDto.getClinicId().isBlank()) {
                    branch.setClinicId(branchDto.getClinicId());
                    log.info("Clinic ID updated.");
                }

                if (branchDto.getBranchName() != null && !branchDto.getBranchName().isBlank()) {
                    branch.setBranchName(branchDto.getBranchName());
                    log.info("Branch Name updated.");
                }

                if (branchDto.getAddress() != null && !branchDto.getAddress().isBlank()) {
                    branch.setAddress(branchDto.getAddress());
                    log.info("Address updated.");
                }

                if (branchDto.getCity() != null && !branchDto.getCity().isBlank()) {
                    branch.setCity(branchDto.getCity());
                    log.info("City updated.");
                }

                if (branchDto.getContactNumber() != null && !branchDto.getContactNumber().isBlank()) {
                    branch.setContactNumber(branchDto.getContactNumber());
                    log.info("Contact Number updated.");
                }

                if (branchDto.getEmail() != null && !branchDto.getEmail().isBlank()) {
                    branch.setEmail(branchDto.getEmail());
                    log.info("Email updated.");
                }

                if (branchDto.getLatitude() != null && !branchDto.getLatitude().isBlank()) {
                    branch.setLatitude(branchDto.getLatitude());
                    log.info("Latitude updated.");
                }

                if (branchDto.getLongitude() != null && !branchDto.getLongitude().isBlank()) {
                    branch.setLongitude(branchDto.getLongitude());
                    log.info("Longitude updated.");
                }

                if (branchDto.getVirtualClinicTour() != null
                        && !branchDto.getVirtualClinicTour().isBlank()) {
                    branch.setVirtualClinicTour(branchDto.getVirtualClinicTour());
                    log.info("Virtual Clinic Tour updated.");
                }

                if (branchDto.getBranchOverallRating() != 0.0) {
                    branch.setBranchOverallRating(branchDto.getBranchOverallRating());
                    log.info("Branch Overall Rating updated.");
                }

                // Save Branch
                Branch updatedBranch = branchRepository.save(branch);
                log.info("Branch details saved successfully for Branch ID: {}", branchId);

                // Update embedded branch in Clinic document
                Clinic clinic = clinicRep.findByHospitalId(updatedBranch.getClinicId());

                if (clinic != null && clinic.getBranches() != null) {

                    log.info("Updating embedded branch inside Clinic document.");

                    List<Branch> clinicBranches = clinic.getBranches();

                    for (Branch b : clinicBranches) {

                        if (branchId.equals(b.getBranchId())) {

                            b.setClinicId(updatedBranch.getClinicId());
                            b.setBranchName(updatedBranch.getBranchName());
                            b.setAddress(updatedBranch.getAddress());
                            b.setCity(updatedBranch.getCity());
                            b.setContactNumber(updatedBranch.getContactNumber());
                            b.setEmail(updatedBranch.getEmail());
                            b.setLatitude(updatedBranch.getLatitude());
                            b.setLongitude(updatedBranch.getLongitude());
                            b.setVirtualClinicTour(updatedBranch.getVirtualClinicTour());
                            b.setBranchOverallRating(updatedBranch.getBranchOverallRating());

                            log.info("Embedded branch updated successfully.");

                            break;
                        }
                    }

                    clinic.setBranches(clinicBranches);
                    clinicRep.save(clinic);

                    log.info("Clinic document saved successfully.");
                } else {
                    log.warn("Clinic document or embedded branches not found for Clinic ID: {}",
                            updatedBranch.getClinicId());
                }

                response.setSuccess(true);
                response.setStatus(200);
                response.setMessage("Branch updated successfully");
                response.setData(convertEntityToDto(updatedBranch));

                log.info("Branch updated successfully for Branch ID: {}", branchId);

            } else {

                log.warn("Branch not found for Branch ID: {}", branchId);

                response.setSuccess(false);
                response.setStatus(404);
                response.setMessage("Branch not found");
            }

        } catch (Exception e) {

            log.error("Error occurred while updating Branch ID: {}", branchId, e);

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage("Error updating branch: " + e.getMessage());
        }

        log.info("===== Update Branch API Completed =====");

        return response;
    }
    // ---------------------- DELETE BRANCH ----------------------
    @Override
    @Secured("ROLE_ADMIN")
    public Response deleteBranch(String branchId) {

        log.info("===== Delete Branch API Started =====");
        log.info("Deleting Branch with Branch ID: {}", branchId);

        Response response = new Response();

        try {

            Optional<Branch> existingBranch = branchRepository.findByBranchId(branchId);

            if (existingBranch.isPresent()) {

                log.info("Branch found for Branch ID: {}", branchId);

                Branch branch = existingBranch.get();

                // Delete Branch Credentials
                ClinicCredentials credentials = clinicCredentialsRepository.findByUserName(branchId);

                if (credentials != null) {
                    clinicCredentialsRepository.delete(credentials);
                    log.info("Branch credentials deleted successfully for Branch ID: {}", branchId);
                } else {
                    log.warn("No credentials found for Branch ID: {}", branchId);
                }

                // Delete Branch
                branchRepository.deleteByBranchId(branchId);
                log.info("Branch deleted successfully from Branch collection.");

                // Remove Branch from Clinic document
                String clinicId = branch.getClinicId();
                Clinic clinic = clinicRep.findByHospitalId(clinicId);

                if (clinic != null) {

                    log.info("Removing Branch from Clinic document. Clinic ID: {}", clinicId);

                    List<Branch> updatedBranches = clinic.getBranches()
                            .stream()
                            .filter(b -> !b.getBranchId().equals(branchId))
                            .collect(Collectors.toList());

                    clinic.setBranches(updatedBranches);
                    clinicRep.save(clinic);

                    log.info("Branch removed successfully from Clinic document.");
                } else {
                    log.warn("Clinic not found for Clinic ID: {}", clinicId);
                }

                response.setMessage("Branch and associated credentials deleted successfully");
                response.setSuccess(true);
                response.setStatus(200);

                log.info("Branch deletion completed successfully for Branch ID: {}", branchId);

            } else {

                log.warn("Branch not found for Branch ID: {}", branchId);

                response.setMessage("Branch not found");
                response.setSuccess(false);
                response.setStatus(404);
            }

        } catch (Exception e) {

            log.error("Error occurred while deleting Branch ID: {}", branchId, e);

            response.setMessage("Error deleting branch: " + e.getMessage());
            response.setSuccess(false);
            response.setStatus(500);
        }

        log.info("===== Delete Branch API Completed =====");

        return response;
    }
    // ---------------------- GET BRANCHES BY CLINIC ID ----------------------
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<?> getBranchByClinicId(String clinicId) {

        log.info("===== Get Branches By Clinic ID API Started =====");
        log.info("Fetching branches for Clinic ID: {}", clinicId);

        Response response = new Response();

        try {

            List<Branch> branches = branchRepository.findByClinicId(clinicId);

            if (branches != null && !branches.isEmpty()) {

                log.info("Found {} branch(es) for Clinic ID: {}", branches.size(), clinicId);

                response.setMessage("Branch found");
                response.setSuccess(true);
                response.setStatus(200);
                response.setData(convertEntityListToDtoList(branches));

                log.info("Branch details returned successfully.");

            } else {

                log.warn("No branches found for Clinic ID: {}", clinicId);

                response.setMessage("Branch not found");
                response.setSuccess(false);
                response.setStatus(404);
            }

        } catch (Exception e) {

            log.error("Error occurred while fetching branches for Clinic ID: {}", clinicId, e);

            response.setMessage("Error fetching branch: " + e.getMessage());
            response.setSuccess(false);
            response.setStatus(500);
        }

        log.info("===== Get Branches By Clinic ID API Completed =====");

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response getBranchesByClinicId(String clinicId) {

        log.info("===== Get Branches By Clinic ID Service Started =====");
        log.info("Fetching branches for Clinic ID: {}", clinicId);

        Response response = new Response();

        try {

            List<Branch> branches = branchRepository.findByClinicId(clinicId);

            if (branches == null || branches.isEmpty()) {

                log.warn("No branches found for Clinic ID: {}", clinicId);

                response.setSuccess(false);
                response.setMessage("No branches found for clinicId: " + clinicId);
                response.setStatus(404);

                log.info("===== Get Branches By Clinic ID Service Completed =====");

                return response;
            }

            log.info("Found {} branch(es) for Clinic ID: {}", branches.size(), clinicId);

            response.setSuccess(true);
            response.setMessage("Branches fetched successfully");
            response.setStatus(200);
            response.setData(convertEntityListToDtoList(branches));

            log.info("Branches fetched successfully for Clinic ID: {}", clinicId);
            log.info("===== Get Branches By Clinic ID Service Completed =====");

            return response;

        } catch (Exception e) {

            log.error("Error occurred while fetching branches for Clinic ID: {}", clinicId, e);

            response.setSuccess(false);
            response.setMessage("Error while fetching branches: " + e.getMessage());
            response.setStatus(500);

            log.info("===== Get Branches By Clinic ID Service Completed =====");

            return response;
        }
    }

    // ---------------------- MAPPERS ----------------------
    private Branch convertDtoToEntity(BranchDTO dto, String generatedBranchId) {
        if (dto == null) return null;
        Branch branch = new Branch();
        branch.setClinicId(dto.getClinicId());
        branch.setHospitalName(dto.getHospitalName());
        branch.setBranchId(generatedBranchId); // Always numeric branch ID
        branch.setBranchName(dto.getBranchName());
        branch.setAddress(dto.getAddress());
        branch.setCity(dto.getCity());
        branch.setContactNumber(dto.getContactNumber());
        branch.setEmail(dto.getEmail());
        branch.setLatitude(dto.getLatitude());
        branch.setLongitude(dto.getLongitude());
        branch.setVirtualClinicTour(dto.getVirtualClinicTour());
        branch.setRole(dto.getRole());
        branch.setPermissions(dto.getPermissions());
        return branch;
    }

    private BranchDTO convertEntityToDto(Branch branch) {
        if (branch == null) return null;
        BranchDTO dto = new BranchDTO();
        dto.setClinicId(branch.getClinicId());
        dto.setHospitalName(branch.getHospitalName());
        dto.setBranchId(branch.getBranchId());
        dto.setBranchName(branch.getBranchName());
        dto.setAddress(branch.getAddress());
        dto.setCity(branch.getCity());
        dto.setContactNumber(branch.getContactNumber());
        dto.setEmail(branch.getEmail());
        dto.setLatitude(branch.getLatitude());
        dto.setLongitude(branch.getLongitude());
        dto.setVirtualClinicTour(branch.getVirtualClinicTour());
        dto.setRole(branch.getRole());
        dto.setPermissions(branch.getPermissions());
        dto.setBranchOverallRating(branch.getBranchOverallRating());
        return dto;
    }

    private List<BranchDTO> convertEntityListToDtoList(List<Branch> branches) {
        List<BranchDTO> dtoList = new ArrayList<>();
        if (branches != null) {
            for (Branch b : branches) {
                dtoList.add(convertEntityToDto(b));
            }
        }
        return dtoList;
    }

   
    public int getNextBranchSequence(String clinicId) {
        BranchCounter counter = mongoOperations.findAndModify(
            Query.query(Criteria.where("_id").is(clinicId)),
            new Update().inc("seq", 1),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            BranchCounter.class
        );
        return counter.getSeq();
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response getAllBranches() {

        log.info("===== Get All Branches API Started =====");

        Response response = new Response();

        try {

            log.info("Fetching all branches from the database.");

            List<Branch> branches = branchRepository.findAll();

            log.info("Retrieved {} branch(es) from the database.", branches.size());

            List<BranchDTO> branchDtos = convertEntityListToDtoList(branches);

            log.info("Successfully converted {} branch entity(ies) to DTO(s).", branchDtos.size());

            response.setMessage("Branches fetched successfully");
            response.setSuccess(true);
            response.setStatus(200);
            response.setData(branchDtos);

            log.info("All branches fetched successfully.");

        } catch (Exception e) {

            log.error("Exception occurred while fetching all branches.", e);

            response.setMessage("Error fetching branches: " + e.getMessage());
            response.setSuccess(false);
            response.setStatus(500);
        }

        log.info("===== Get All Branches API Completed =====");

        return response;
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<?> getBranchByClinicAndBranchId(String clinicId, String branchId) {

        log.info("===== Get Branch By Clinic ID And Branch ID API Started =====");
        log.info("Fetching branch details for Clinic ID: {} and Branch ID: {}", clinicId, branchId);

        Response response = new Response();

        try {

            Optional<Branch> branchOpt = branchRepository.findByClinicIdAndBranchId(clinicId, branchId);

            if (branchOpt.isPresent()) {

                log.info("Branch found for Clinic ID: {} and Branch ID: {}", clinicId, branchId);

                response.setSuccess(true);
                response.setStatus(200);
                response.setMessage("Branch details fetched successfully");
                response.setData(convertEntityToDto(branchOpt.get()));

                log.info("Branch details converted to DTO and returned successfully.");

            } else {

                log.warn("No branch found for Clinic ID: {} and Branch ID: {}", clinicId, branchId);

                response.setSuccess(false);
                response.setStatus(404);
                response.setMessage("No branch found for the given clinicId and branchId");
            }

        } catch (Exception e) {

            log.error("Exception occurred while fetching branch for Clinic ID: {} and Branch ID: {}",
                    clinicId, branchId, e);

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage("Something went wrong: " + e.getMessage());
        }

        log.info("===== Get Branch By Clinic ID And Branch ID API Completed =====");

        return ResponseEntity.status(response.getStatus()).body(response);
    }

}
