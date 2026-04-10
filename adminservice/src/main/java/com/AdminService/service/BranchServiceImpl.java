package com.AdminService.service;

import java.util.ArrayList;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.AdminService.dto.BranchDTO;
import com.AdminService.entity.Branch;
import com.AdminService.entity.BranchCounter;
import com.AdminService.entity.BranchCredentials;
import com.AdminService.entity.Clinic;
import com.AdminService.repository.BranchCredentialsRepository;
import com.AdminService.repository.BranchRepository;
import com.AdminService.repository.ClinicRep;
import com.AdminService.util.PermissionsUtil;
import com.AdminService.util.Response;

@Service
public class BranchServiceImpl implements BranchService {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ClinicRep clinicRep;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private BranchCredentialsRepository branchCredentialsRepository;
    
    @Autowired
    private EmailService emailService;

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
    public Response createBranch(BranchDTO dto) {

        Response res = new Response();

        try {
            if (dto.getClinicId() == null || dto.getClinicId().isBlank()) {
                res.setMessage("clinicId is required");
                res.setSuccess(false);
                res.setStatus(400);
                return res;
            }

            String clinicId = dto.getClinicId();
            Clinic clinic = clinicRep.findByHospitalId(clinicId);

            if (clinic == null) {
                res.setMessage("Clinic with ID " + clinicId + " not found");
                res.setSuccess(false);
                res.setStatus(404);
                return res;
            }

            // ---------------- Generate branch ID ----------------
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

            // ---------------- Create Branch ----------------
            Branch branch = convertDtoToEntity(dto, branchId);
            branch.setRole("ADMIN");
            branch.setPermissions(PermissionsUtil.getAdminPermissions());

            // 👉 If you don’t want verification
            branch.setStatus("ACTIVE");
            
            // 🔥 IMPORTANT FIX → ensure email always present
            if (branch.getEmail() == null || branch.getEmail().isBlank()) {
                branch.setEmail(clinic.getEmailAddress());
            }

            Branch savedBranch = branchRepository.save(branch);

            // ---------------- 🔐 Generate Password ----------------
            String tempPassword = PasswordGenerator.generatePassword(10);

            // ---------------- Save Credentials ----------------
            BranchCredentials credentials = new BranchCredentials();
            credentials.setBranchId(branchId);
            credentials.setUserName(branchId);
            credentials.setPassword(tempPassword);
            credentials.setBranchName(savedBranch.getBranchName());
            credentials.setRole(savedBranch.getRole());
            credentials.setPermissions(savedBranch.getPermissions());

            branchCredentialsRepository.save(credentials);

            // ----------------  Send Email ----------------
            if (savedBranch.getEmail() != null && !savedBranch.getEmail().isBlank()) {

             

                //  IMPORTANT FIX → include credentials in message
                Map<String, String> mailData = new HashMap<>();
                mailData.put("subject", "Branch Login Credentials");

                mailData.put("message",
                        "Your branch has been created successfully.\n\n" +
                        "Please use the below credentials to login."
                );

                mailData.put("username", branchId);
                mailData.put("password", tempPassword);

                emailService.sendEmail(savedBranch.getEmail(), mailData);
            }

            // ---------------- Attach to clinic ----------------
            List<Branch> branches = clinic.getBranches();
            if (branches == null) branches = new ArrayList<>();
            branches.add(savedBranch);
            clinic.setBranches(branches);
            clinicRep.save(clinic);

            // ---------------- Response ----------------
            res.setSuccess(true);
            res.setStatus(200);
            res.setMessage("Branch created successfully and credentials sent to email");
            res.setHospitalId(clinicId);
            res.setBranchId(branchId);

            return res;

        } catch (Exception e) {
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
    public ResponseEntity<?> getBranchById(String branchId) {
        Response response = new Response();
        try {
            Optional<Branch> branch = branchRepository.findByBranchId(branchId);
            if (branch.isPresent()) {
                response.setMessage("Branch found");
                response.setSuccess(true);
                response.setStatus(200);
                response.setData(convertEntityToDto(branch.get()));
            } else {
                response.setMessage("Branch not found");
                response.setSuccess(false);
                response.setStatus(404);
            }
        } catch (Exception e) {
            response.setMessage("Error fetching branch: " + e.getMessage());
            response.setSuccess(false);
            response.setStatus(500);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------- UPDATE BRANCH ----------------------
    @Override
    public Response updateBranch(String branchId, BranchDTO branchDto) {
        Response response = new Response();
        try {
            Optional<Branch> existingOpt = branchRepository.findByBranchId(branchId);
            if (existingOpt.isPresent()) {
                Branch branch = existingOpt.get();
                branch.setClinicId(branchDto.getClinicId() != null ? branchDto.getClinicId() : branch.getClinicId());
                branch.setBranchName(branchDto.getBranchName() != null ? branchDto.getBranchName() : branch.getBranchName());
                branch.setAddress(branchDto.getAddress() != null ? branchDto.getAddress() : branch.getAddress());
                branch.setCity(branchDto.getCity() != null ? branchDto.getCity() : branch.getCity());
                branch.setContactNumber(branchDto.getContactNumber() != null ? branchDto.getContactNumber() : branch.getContactNumber());
                branch.setEmail(branchDto.getEmail() != null ? branchDto.getEmail() : branch.getEmail());
                branch.setLatitude(branchDto.getLatitude() != null ? branchDto.getLatitude() : branch.getLatitude());
                branch.setLongitude(branchDto.getLongitude() != null ? branchDto.getLongitude() : branch.getLongitude());
                branch.setVirtualClinicTour(branchDto.getVirtualClinicTour() != null ? branchDto.getVirtualClinicTour() : branch.getVirtualClinicTour());
                branch.setBranchOverallRating(branchDto.getBranchOverallRating());

                Branch updatedBranch = branchRepository.save(branch);

                // ------------------ Update branch in clinic ------------------
                Clinic clinic = clinicRep.findByHospitalId(branch.getClinicId());
                if (clinic != null && clinic.getBranches() != null) {
                    for (Branch b : clinic.getBranches()) {
                        if (b.getBranchId().equals(branchId)) {
                            b.setBranchName(updatedBranch.getBranchName());
                            b.setAddress(updatedBranch.getAddress());
                            b.setCity(updatedBranch.getCity());
                            b.setContactNumber(updatedBranch.getContactNumber());
                            b.setEmail(updatedBranch.getEmail());
                            b.setLatitude(updatedBranch.getLatitude());
                            b.setLongitude(updatedBranch.getLongitude());
                            b.setVirtualClinicTour(updatedBranch.getVirtualClinicTour());
                            b.setBranchOverallRating(updatedBranch.getBranchOverallRating());
                            break;
                        }
                    }
                    clinicRep.save(clinic);
                }

                response.setMessage("Branch updated successfully");
                response.setSuccess(true);
                response.setStatus(200);
                response.setData(convertEntityToDto(updatedBranch));
            } else {
                response.setMessage("Branch not found");
                response.setSuccess(false);
                response.setStatus(404);
            }
        } catch (Exception e) {
            response.setMessage("Error updating branch: " + e.getMessage());
            response.setSuccess(false);
            response.setStatus(500);
        }
        return response;
    }
    // ---------------------- DELETE BRANCH ----------------------
    @Override
    public Response deleteBranch(String branchId) {
        Response response = new Response();
        try {
            Optional<Branch> existingBranch = branchRepository.findByBranchId(branchId);
            if (existingBranch.isPresent()) {
                Branch branch = existingBranch.get();

                // Delete BranchCredentials
                List<BranchCredentials> credentialsList = branchCredentialsRepository.findByBranchId(branchId);
                if (credentialsList != null && !credentialsList.isEmpty()) {
                    branchCredentialsRepository.deleteAll(credentialsList);
                }

                // Delete Branch from Branches collection
                branchRepository.deleteByBranchId(branchId);

                // Remove branch from Clinic's branches array
                String clinicId = branch.getClinicId();
                Clinic clinic = clinicRep.findByHospitalId(clinicId);
                if (clinic != null) {
                    List<Branch> updatedBranches = clinic.getBranches()
                        .stream()
                        .filter(b -> !b.getBranchId().equals(branchId))
                        .collect(Collectors.toList());
                    clinic.setBranches(updatedBranches);
                    clinicRep.save(clinic);
                }

                response.setMessage("Branch and associated credentials deleted successfully");
                response.setSuccess(true);
                response.setStatus(200);
            } else {
                response.setMessage("Branch not found");
                response.setSuccess(false);
                response.setStatus(404);
            }
        } catch (Exception e) {
            response.setMessage("Error deleting branch: " + e.getMessage());
            response.setSuccess(false);
            response.setStatus(500);
        }
        return response;
    }
    // ---------------------- GET BRANCHES BY CLINIC ID ----------------------
    @Override
    public ResponseEntity<?> getBranchByClinicId(String clinicId) {
        Response response = new Response();
        try {
            List<Branch> branches = branchRepository.findByClinicId(clinicId);
            if (branches != null && !branches.isEmpty()) {
                response.setMessage("Branch found");
                response.setSuccess(true);
                response.setStatus(200);
                response.setData(convertEntityListToDtoList(branches));
            } else {
                response.setMessage("Branch not found");
                response.setSuccess(false);
                response.setStatus(404);
            }
        } catch (Exception e) {
            response.setMessage("Error fetching branch: " + e.getMessage());
            response.setSuccess(false);
            response.setStatus(500);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Override
    public Response getBranchesByClinicId(String clinicId) {
        Response response = new Response();
        try {
            List<Branch> branches = branchRepository.findByClinicId(clinicId);
            if (branches == null || branches.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("No branches found for clinicId: " + clinicId);
                response.setStatus(404);
                return response;
            }

            response.setSuccess(true);
            response.setMessage("Branches fetched successfully");
            response.setStatus(200);
            response.setData(convertEntityListToDtoList(branches));
            return response;

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error while fetching branches: " + e.getMessage());
            response.setStatus(500);
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
    public Response getAllBranches() {
        Response response = new Response();
        try {
            List<Branch> branches = branchRepository.findAll();
            List<BranchDTO> branchDtos = convertEntityListToDtoList(branches);
            response.setMessage("Branches fetched successfully");
            response.setSuccess(true);
            response.setStatus(200);
            response.setData(branchDtos);
        } catch (Exception e) {
            response.setMessage("Error fetching branches: " + e.getMessage());
            response.setSuccess(false);
            response.setStatus(500);
        }
        return response;
    }

    @Override
    public ResponseEntity<?> getBranchByClinicAndBranchId(String clinicId, String branchId) {
        Response response = new Response();
        try {
            Optional<Branch> branchOpt = branchRepository.findByClinicIdAndBranchId(clinicId, branchId);

            if (branchOpt.isPresent()) {
                response.setSuccess(true);
                response.setStatus(200);
                response.setMessage("Branch details fetched successfully");
                response.setData(convertEntityToDto(branchOpt.get()));
            } else {
                response.setSuccess(false);
                response.setStatus(404);
                response.setMessage("No branch found for the given clinicId and branchId");
            }

        } catch (Exception e) {
            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage("Something went wrong: " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }
	

}
