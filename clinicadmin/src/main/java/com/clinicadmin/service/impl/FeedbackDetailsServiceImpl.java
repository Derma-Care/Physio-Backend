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
import com.clinicadmin.entity.FeedbackDetails;
import com.clinicadmin.feignclient.PhysiotherapyFeignClient;
import com.clinicadmin.repository.FeedbackDetailsRepository;
import com.clinicadmin.service.FeedbackDetailsServcie;

@Service
public class FeedbackDetailsServiceImpl
        implements FeedbackDetailsServcie {

    @Autowired
    private PhysiotherapyFeignClient physiotherapyDoctorFeign;
    
    @Autowired
    private FeedbackDetailsRepository repository;
    
    
    @Override
    public Response createFeedback(FeedbackDetails feedbackDetails) {

        Response response = new Response();

        try {

            // ================= ID GENERATION =================

            String feedbackId =
                    "FDBK-" +
                    java.time.LocalDateTime.now()
                            .format(
                                    java.time.format.DateTimeFormatter
                                            .ofPattern(
                                                    "ddMM-HHmmss"));

            feedbackDetails.setId(feedbackId);

            // ================= SAVE =================

            FeedbackDetails saved =
                    repository.save(feedbackDetails);

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Feedback created successfully");

            response.setData(saved);

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

                data.setPatientId(
                        String.valueOf(
                                payment.get("patientId")));

                data.setPatientName(
                        String.valueOf(
                                payment.get("patientName")));

                data.setMobileNumber(
                        String.valueOf(
                                payment.get("mobileNumber")));

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

                        List<Map<String, Object>>
                                programs =

                                (List<Map<String, Object>>)
                                        pkg.get("programs");

                        if (programs == null)
                            continue;

                        for (Map<String, Object> program
                                : programs) {

                            String programName =
                                    (String) program.get(
                                            "programName");

                            List<Map<String, Object>>
                                    therapyData =

                                    (List<Map<String, Object>>)
                                            program.get(
                                                    "therapyData");

                            if (therapyData == null)
                                continue;

                            for (Map<String, Object> therapy
                                    : therapyData) {

                                String therapyName =
                                        (String) therapy.get(
                                                "therapyName");

                                List<Map<String, Object>>
                                        exercises =

                                        (List<Map<String, Object>>)
                                                therapy.get(
                                                        "exercises");

                                if (exercises == null)
                                    continue;

                                for (Map<String, Object> exercise
                                        : exercises) {

                                    String exerciseName =
                                            (String) exercise.get(
                                                    "exerciseName");

                                    // ================= SERVICE =================

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
}