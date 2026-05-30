package com.clinicadmin.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.FeedbackDetailsDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ServiceInfo;
import com.clinicadmin.entity.CustomerOnbording;
import com.clinicadmin.entity.FeedbackDetails;
import com.clinicadmin.feignclient.PhysiotherapyFeignClient;
import com.clinicadmin.repository.CustomerOnboardingRepository;
import com.clinicadmin.repository.FeedbackDetailsRepository;
import com.clinicadmin.service.FeedbackDetailsServcie;

@Service
public class FeedbackDetailsServiceImpl
        implements FeedbackDetailsServcie {

    @Autowired
    private PhysiotherapyFeignClient physiotherapyDoctorFeign;
    
    @Autowired
    private FeedbackDetailsRepository repository;
    
    @Autowired
    private CustomerOnboardingRepository customerOnboardingRepository;
    
    
    @Override
    public Response createFeedback(
            FeedbackDetailsDTO feedbackDetailsDTO) {

        Response response = new Response();

        try {

            // ================= MAP TO ENTITY =================

            FeedbackDetails entity =
                    mapToEntity(
                            feedbackDetailsDTO);

            // ================= ID GENERATION =================

            String feedbackId =
                    "FDBK-" +
                    java.time.LocalDateTime.now()
                            .format(
                                    java.time.format.DateTimeFormatter
                                            .ofPattern(
                                                    "ddMM-HHmmss"));

            entity.setId(feedbackId);
            // ================= DATE & TIME =================

            String currentDateTime =
                    java.time.LocalDateTime.now()
                            .toString();

            entity.setCreatedAt(currentDateTime);
            entity.setUpdatedAt(currentDateTime);
            // ================= SAVE =================

            FeedbackDetails saved =
                    repository.save(entity);

            // ================= RESPONSE =================

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Feedback created successfully");

            response.setData(
                    mapToDTO(saved));

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
            response.setData(null);
        }

        return response;
    }
    
    
    @Override
    public Response getAllFeedbacks() {

        Response response = new Response();

        try {

            List<FeedbackDetailsDTO> result =
                    repository.findAll()
                            .stream()
                            .map(this::mapToDTO)
                            .toList();

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "All feedbacks fetched successfully");

            response.setData(result);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
            response.setData(null);
        }

        return response;
    }
    
    @Override
    public Response getFeedbackById(String id) {

        Response response = new Response();

        try {

            FeedbackDetails feedback =
                    repository.findById(id)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Feedback not found"));

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Feedback fetched successfully");

            response.setData(
                    mapToDTO(feedback));

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(404);
            response.setMessage(e.getMessage());
            response.setData(null);
        }

        return response;
    }
    
    @Override
    public Response updateFeedback(
            String id,
            FeedbackDetailsDTO feedbackDetailsDTO) {

        Response response = new Response();

        try {

            FeedbackDetails existing =
                    repository.findById(id)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Feedback not found"));


            if (feedbackDetailsDTO.getPatientId() != null) {
                existing.setPatientId(
                        feedbackDetailsDTO.getPatientId());
            }

            if (feedbackDetailsDTO.getPatientName() != null) {
                existing.setPatientName(
                        feedbackDetailsDTO.getPatientName());
            }

            if (feedbackDetailsDTO.getMobileNumber() != null) {
                existing.setMobileNumber(
                        feedbackDetailsDTO.getMobileNumber());
            }

            if (feedbackDetailsDTO.getBookingId() != null) {
                existing.setBookingId(
                        feedbackDetailsDTO.getBookingId());
            }

            if (feedbackDetailsDTO.getDoctorId() != null) {
                existing.setDoctorId(
                        feedbackDetailsDTO.getDoctorId());
            }

            if (feedbackDetailsDTO.getDoctorName() != null) {
                existing.setDoctorName(
                        feedbackDetailsDTO.getDoctorName());
            }

            if (feedbackDetailsDTO.getTherapistId() != null) {
                existing.setTherapistId(
                        feedbackDetailsDTO.getTherapistId());
            }

            if (feedbackDetailsDTO.getTherapistName() != null) {
                existing.setTherapistName(
                        feedbackDetailsDTO.getTherapistName());
            }

            if (feedbackDetailsDTO.getTherapistRecordId() != null) {
                existing.setTherapistRecordId(
                        feedbackDetailsDTO.getTherapistRecordId());
            }

            if (feedbackDetailsDTO.getStaffId() != null) {
                existing.setStaffId(
                        feedbackDetailsDTO.getStaffId());
            }

            if (feedbackDetailsDTO.getStaffName() != null) {
                existing.setStaffName(
                        feedbackDetailsDTO.getStaffName());
            }

            if (feedbackDetailsDTO.getServiceType() != null) {
                existing.setServiceType(
                        feedbackDetailsDTO.getServiceType());
            }

            if (feedbackDetailsDTO.getService() != null) {
                existing.setService(
                        feedbackDetailsDTO.getService());
            }

            if (feedbackDetailsDTO.getTotalNoOfSessions() > 0) {
                existing.setTotalNoOfSessions(
                        feedbackDetailsDTO.getTotalNoOfSessions());
            }

            if (feedbackDetailsDTO.getNoOfSessionsCompleted() >= 0) {
                existing.setNoOfSessionsCompleted(
                        feedbackDetailsDTO.getNoOfSessionsCompleted());
            }

            existing.setHalfSessionsCompleted(
                    feedbackDetailsDTO.isHalfSessionsCompleted());

            existing.setFullSessionsCompleted(
                    feedbackDetailsDTO.isFullSessionsCompleted());

            if (feedbackDetailsDTO.getWhatWentWell() != null) {
                existing.setWhatWentWell(
                        feedbackDetailsDTO.getWhatWentWell());
            }

            if (feedbackDetailsDTO.getImprovements() != null) {
                existing.setImprovements(
                        feedbackDetailsDTO.getImprovements());
            }
         // ================= UPDATE DATE & TIME =================

            existing.setUpdatedAt(
                    java.time.LocalDateTime.now()
                            .toString());


            // ================= SAVE =================

            FeedbackDetails updated =
                    repository.save(existing);

            // ================= RESPONSE =================

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Feedback updated successfully");

            response.setData(
                    mapToDTO(updated));

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
            response.setData(null);
        }

        return response;
    }
    
    @Override
    public Response deleteFeedback(String id) {

        Response response = new Response();

        try {

            FeedbackDetails feedback =
                    repository.findById(id)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Feedback not found"));

            repository.delete(feedback);

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Feedback deleted successfully");

            response.setData(null);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
            response.setData(null);
        }

        return response;
    }
    @Override
    public Response getFeedbackDetails(
            String clinicId,
            String branchId) {

        Response response = new Response();

        try {

            Response paymentResponse =
                    physiotherapyDoctorFeign
                            .getPayments(
                                    clinicId,
                                    branchId);

            List<Map<String, Object>> payments =
                    (List<Map<String, Object>>)
                            paymentResponse.getData();

            if (payments == null || payments.isEmpty()) {

                response.setSuccess(false);
                response.setStatus(404);
                response.setMessage(
                        "No payment records found");

                response.setData(null);

                return response;
            }

            List<FeedbackDetailsDTO> result =
                    new ArrayList<>();

            for (Map<String, Object> payment
                    : payments) {

                FeedbackDetailsDTO data =
                        new FeedbackDetailsDTO();

                // ================= BASIC =================
                String patientId =
                        String.valueOf(
                                payment.get("patientId"));

                data.setPatientId(patientId);

                CustomerOnbording customer =
                        customerOnboardingRepository
                                .findByPatientIdAndHospitalIdAndBranchId(
                                        patientId,
                                        clinicId,
                                        branchId)
                                .orElse(null);

                if (customer != null) {

                    data.setPatientName(
                            customer.getFullName());

                    data.setMobileNumber(
                            customer.getMobileNumber());

                } else {

                    data.setPatientName(null);
                    data.setMobileNumber(null);
                }
                data.setBookingId(
                        String.valueOf(
                                payment.get("bookingId")));

                data.setDoctorId(
                        String.valueOf(
                                payment.get("doctorId")));

                data.setDoctorName(
                        String.valueOf(
                                payment.get("doctorName")));

                data.setTherapistId(
                        String.valueOf(
                                payment.get("therapistId")));

                data.setTherapistName(
                        String.valueOf(
                                payment.get("therapistName")));

                data.setTherapistRecordId(
                        String.valueOf(
                                payment.get(
                                        "therapistRecordId")));

                String serviceType =
                        String.valueOf(
                                payment.get("serviceType"));

                data.setServiceType(serviceType);

                // ================= COUNTS =================

                int totalSessions = 0;
                int completedSessions = 0;

                List<ServiceInfo> service =
                        new ArrayList<>();

                List<Map<String, Object>>
                        therapyWithSessions =

                        (List<Map<String, Object>>)
                                payment.get(
                                        "therapyWithSessions");

                if (therapyWithSessions != null) {

                    for (Map<String, Object> pkg
                            : therapyWithSessions) {

                        String packageName =
                                (String) pkg.get(
                                        "packageName");

                        // ================= PACKAGE SERVICE =================

                        if ("package"
                                .equalsIgnoreCase(
                                        serviceType)

                                && packageName != null) {

                            ServiceInfo info =
                                    new ServiceInfo();

                            info.setServiceId(
                                    String.valueOf(
                                            pkg.get(
                                                    "packageId")));

                            info.setServiceName(
                                    packageName);

                            service.add(info);
                        }

                        List<Map<String, Object>>
                        programs =

                        (List<Map<String, Object>>)
                                pkg.get("programs");

                // ================= PROGRAM FALLBACK =================

                if ((programs == null
                        || programs.isEmpty())

                        && "program"
                                .equalsIgnoreCase(
                                        serviceType)) {

                    String programId =
                            String.valueOf(
                                    pkg.get(
                                            "programId"));

                    String programName =
                            String.valueOf(
                                    pkg.get(
                                            "programName"));

                    if (programId != null
                            && !"null"
                                    .equalsIgnoreCase(
                                            programId)

                            && programName != null
                            && !"null"
                                    .equalsIgnoreCase(
                                            programName)) {

                        ServiceInfo info =
                                new ServiceInfo();

                        info.setServiceId(
                                programId);

                        info.setServiceName(
                                programName);

                        service.add(info);
                    }
                }

                // if (programs == null)
//                     continue;

                if (programs == null)
                    continue;

                        for (Map<String, Object> program
                                : programs) {

                            String programName =
                                    (String) program.get(
                                            "programName");

                            // ================= PROGRAM SERVICE =================

                            if ("program"
                                    .equalsIgnoreCase(
                                            serviceType)

                                    && programName != null) {

                                ServiceInfo info =
                                        new ServiceInfo();

                                info.setServiceId(
                                        String.valueOf(
                                                program.get(
                                                        "programId")));

                                info.setServiceName(
                                        programName);

                                service.add(info);
                            }

                            List<Map<String, Object>>
                                    therapyData =

                                    (List<Map<String, Object>>)
                                            program.get(
                                                    "therapyData");

                            if ((therapyData == null
                                    || therapyData.isEmpty())

                                    && "therapy"
                                            .equalsIgnoreCase(
                                                    serviceType)) {

                                String therapyId =
                                        String.valueOf(
                                                program.get(
                                                        "therapyId"));

                                String therapyName =
                                        String.valueOf(
                                                program.get(
                                                        "therapyName"));

                                if (therapyId != null
                                        && !"null"
                                                .equalsIgnoreCase(
                                                        therapyId)

                                        && therapyName != null
                                        && !"null"
                                                .equalsIgnoreCase(
                                                        therapyName)) {

                                    ServiceInfo info =
                                            new ServiceInfo();

                                    info.setServiceId(
                                            therapyId);

                                    info.setServiceName(
                                            therapyName);

                                    service.add(info);
                                }
                            }

                            if (therapyData == null)
                                continue;

                            for (Map<String, Object> therapy
                                    : therapyData) {

                                String therapyName =
                                        (String) therapy.get(
                                                "therapyName");

                                // ================= THERAPY SERVICE =================

                                if ("therapy"
                                        .equalsIgnoreCase(
                                                serviceType)

                                        && therapyName != null) {

                                    ServiceInfo info =
                                            new ServiceInfo();

                                    info.setServiceId(
                                            String.valueOf(
                                                    therapy.get(
                                                            "therapyId")));

                                    info.setServiceName(
                                            therapyName);

                                    service.add(info);
                                }

                                List<Map<String, Object>>
                                        exercises =

                                        (List<Map<String, Object>>)
                                                therapy.get(
                                                        "exercises");

                                if ((exercises == null
                                        || exercises.isEmpty())

                                        && "exercise"
                                                .equalsIgnoreCase(
                                                        serviceType)) {

                                    String exerciseId =
                                            String.valueOf(
                                                    therapy.get(
                                                            "exerciseId"));

                                    String exerciseName =
                                            String.valueOf(
                                                    therapy.get(
                                                            "exerciseName"));

                                    if (exerciseId != null
                                            && !"null"
                                                    .equalsIgnoreCase(
                                                            exerciseId)

                                            && exerciseName != null
                                            && !"null"
                                                    .equalsIgnoreCase(
                                                            exerciseName)) {

                                        ServiceInfo info =
                                                new ServiceInfo();

                                        info.setServiceId(
                                                exerciseId);

                                        info.setServiceName(
                                                exerciseName);

                                        service.add(info);
                                    }
                                }

                                if (exercises == null)
                                    continue;

                                for (Map<String, Object> exercise
                                        : exercises) {

                                    String exerciseName =
                                            (String) exercise.get(
                                                    "exerciseName");

                                    // ================= EXERCISE SERVICE =================

                                    if ("exercise"
                                            .equalsIgnoreCase(
                                                    serviceType)

                                            && exerciseName != null) {

                                        ServiceInfo info =
                                                new ServiceInfo();

                                        info.setServiceId(
                                                String.valueOf(
                                                        exercise.get(
                                                                "exerciseId")));

                                        info.setServiceName(
                                                exerciseName);

                                        service.add(info);
                                    }

                                    // ================= SESSIONS =================

                                    List<Map<String, Object>>
                                            sessions =

                                            (List<Map<String, Object>>)
                                                    exercise.get(
                                                            "sessions");

                                    if (sessions == null)
                                        continue;

                                    totalSessions +=
                                            sessions.size();

                                    for (Map<String, Object> session
                                            : sessions) {

                                        String status =
                                                String.valueOf(
                                                        session.get(
                                                                "status"));

                                        if ("Completed"
                                                .equalsIgnoreCase(
                                                        status)) {

                                            completedSessions++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= REMOVE DUPLICATES =================

                service =
                        service.stream()
                                .collect(
                                        Collectors.collectingAndThen(
                                                Collectors.toMap(
                                                        ServiceInfo::getServiceId,
                                                        s -> s,
                                                        (a, b) -> a),
                                                m -> new ArrayList<>(
                                                        m.values())));

                data.setService(service);

                // ================= FINAL COUNTS =================

                data.setTotalNoOfSessions(
                        totalSessions);

                data.setNoOfSessionsCompleted(
                        completedSessions);

                // ================= HALF COMPLETED =================

                boolean isHalfCompleted = false;

                if (totalSessions > 0) {

                    double percentage =
                            ((double) completedSessions
                                    / totalSessions) * 100;

                    if (percentage >= 50) {

                        isHalfCompleted = true;
                    }
                }

                data.setHalfSessionsCompleted(
                        isHalfCompleted);

                // ================= FULL COMPLETED =================

                boolean isFullCompleted =
                        totalSessions > 0
                        && completedSessions
                        == totalSessions;

                data.setFullSessionsCompleted(
                        isFullCompleted);

                result.add(data);
            }

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Feedback details fetched successfully");

            response.setData(result);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
            response.setData(null);
        }

        return response;
    }
    private FeedbackDetails mapToEntity(
            FeedbackDetailsDTO dto) {

        FeedbackDetails entity =
                new FeedbackDetails();

        entity.setId(dto.getId());
        entity.setClinicId(dto.getClinicId());
        entity.setBranchId(dto.getBranchId());
        
        entity.setPatientId(dto.getPatientId());
        entity.setPatientName(dto.getPatientName());
        entity.setMobileNumber(dto.getMobileNumber());

        entity.setBookingId(dto.getBookingId());

        entity.setDoctorId(dto.getDoctorId());
        entity.setDoctorName(dto.getDoctorName());

        entity.setTherapistId(dto.getTherapistId());
        entity.setTherapistName(dto.getTherapistName());
        entity.setTherapistRecordId(
                dto.getTherapistRecordId());

        entity.setStaffId(dto.getStaffId());
        entity.setStaffName(dto.getStaffName());
        entity.setRating(dto.getRating());
        entity.setServiceType(dto.getServiceType());
        entity.setService(dto.getService());

        entity.setTotalNoOfSessions(
                dto.getTotalNoOfSessions());

        entity.setNoOfSessionsCompleted(
                dto.getNoOfSessionsCompleted());

        entity.setHalfSessionsCompleted(
                dto.isHalfSessionsCompleted());

        entity.setFullSessionsCompleted(
                dto.isFullSessionsCompleted());

        entity.setWhatWentWell(
                dto.getWhatWentWell());

        entity.setImprovements(
                dto.getImprovements());
        
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());

        return entity;
    }
    private FeedbackDetailsDTO mapToDTO(
            FeedbackDetails entity) {

        FeedbackDetailsDTO dto =
                new FeedbackDetailsDTO();

        dto.setId(entity.getId());
        dto.setClinicId(entity.getClinicId());
        dto.setBranchId(entity.getBranchId());
        dto.setPatientId(entity.getPatientId());
        dto.setPatientName(entity.getPatientName());
        dto.setMobileNumber(entity.getMobileNumber());

        dto.setBookingId(entity.getBookingId());
        dto.setRating(entity.getRating());
        dto.setDoctorId(entity.getDoctorId());
        dto.setDoctorName(entity.getDoctorName());

        dto.setTherapistId(entity.getTherapistId());
        dto.setTherapistName(entity.getTherapistName());
        dto.setTherapistRecordId(
                entity.getTherapistRecordId());

        dto.setStaffId(entity.getStaffId());
        dto.setStaffName(entity.getStaffName());

        dto.setServiceType(entity.getServiceType());
        dto.setService(entity.getService());

        dto.setTotalNoOfSessions(
                entity.getTotalNoOfSessions());

        dto.setNoOfSessionsCompleted(
                entity.getNoOfSessionsCompleted());

        dto.setHalfSessionsCompleted(
                entity.isHalfSessionsCompleted());

        dto.setFullSessionsCompleted(
                entity.isFullSessionsCompleted());

        dto.setWhatWentWell(
                entity.getWhatWentWell());

        dto.setImprovements(
                entity.getImprovements());
        // ================= AUDIT =================

        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }


    @Override
    public Response getAllFeedbacksByClinicIdAndBranchId(
            String clinicId,
            String branchId) {

        Response response = new Response();

        try {

            List<FeedbackDetailsDTO> feedbackList =
                    repository.findByClinicIdAndBranchId(
                                    clinicId,
                                    branchId)
                            .stream()
                            .map(this::mapToDTO)
                            .toList();

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Feedbacks fetched successfully");

            response.setData(feedbackList);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(404);
            response.setMessage(e.getMessage());
            response.setData(null);
        }

        return response;
    }
}