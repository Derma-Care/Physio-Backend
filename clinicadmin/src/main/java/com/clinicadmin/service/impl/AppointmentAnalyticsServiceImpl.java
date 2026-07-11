package com.clinicadmin.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.AppointmentSummaryDTO;
import com.clinicadmin.dto.DoctorAnalyticsDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.entity.Doctors;
import com.clinicadmin.feignclient.BookingFeign;
import com.clinicadmin.feignclient.PhysiotherapyFeignClient;
import com.clinicadmin.repository.DoctorsRepository;
import com.clinicadmin.service.AppointmentAnalyticsService;

@Service
public class AppointmentAnalyticsServiceImpl
        implements AppointmentAnalyticsService {

    @Autowired
    private BookingFeign bookingFeign;
    
    @Autowired
    private DoctorsRepository doctorsRepository;

    @Autowired
    private PhysiotherapyFeignClient physiotherapyFeignClient;

    @Override
    public Response getAppointmentAnalytics(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate) {

        Response response = new Response();

        try {

            ResponseEntity<ResponseStructure<List<Map<String, Object>>>> bookingResponse =
                    bookingFeign.getBookedServicesByClinicIdWithBranchId(
                            clinicId,
                            branchId);

            if (bookingResponse == null
                    || bookingResponse.getBody() == null
                    || bookingResponse.getBody().getData() == null) {

                response.setSuccess(false);
                response.setMessage("No booking data found");
                response.setStatus(HttpStatus.NOT_FOUND.value());

                return response;
            }

            List<Map<String, Object>> bookings =
                    bookingResponse.getBody().getData();

            Map<String, DoctorAnalyticsDTO> doctorAnalyticsMap =
                    new HashMap<>();

            LocalDate today = LocalDate.now();

            for (Map<String, Object> booking : bookings) {

                String serviceDateStr =
                        String.valueOf(
                                booking.getOrDefault(
                                        "serviceDate",
                                        ""));

                if (serviceDateStr == null
                        || serviceDateStr.isBlank()) {
                    continue;
                }

                LocalDate serviceDate =
                        LocalDate.parse(
                                serviceDateStr);

                boolean include = false;

                switch (type) {

                    case 1: // Today

                        include =
                                serviceDate.equals(
                                        today);
                        break;

                    case 2: // Weekly

                        include =
                                !serviceDate.isBefore(
                                        today.minusDays(6))
                                && !serviceDate.isAfter(
                                        today);
                        break;

                    case 3: // Monthly

                        include =
                                serviceDate.getMonthValue()
                                        == today.getMonthValue()
                                && serviceDate.getYear()
                                        == today.getYear();
                        break;

                    case 4: // Yearly

                        include =
                                serviceDate.getYear()
                                        == today.getYear();
                        break;

                    case 5: // Custom

                        LocalDate start =
                                LocalDate.parse(
                                        startDate);

                        LocalDate end =
                                LocalDate.parse(
                                        endDate);

                        include =
                                !serviceDate.isBefore(
                                        start)
                                && !serviceDate.isAfter(
                                        end);
                        break;

                    default:

                        include = false;
                }

                if (!include) {
                    continue;
                }

                String doctorId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "doctorId",
                                        ""));

                String doctorName =
                        String.valueOf(
                                booking.getOrDefault(
                                        "doctorName",
                                        ""));

                String speciality =
                        doctorsRepository
                                .findByDoctorId(
                                        doctorId)
                                .map(
                                        Doctors::getSpecialization)
                                .orElse(
                                        "N/A");

                DoctorAnalyticsDTO dto =
                        doctorAnalyticsMap.computeIfAbsent(
                                doctorId,
                                id -> {

                                    DoctorAnalyticsDTO analytics =
                                            new DoctorAnalyticsDTO();

                                    analytics.setDoctorId(
                                            doctorId);

                                    analytics.setDoctorName(
                                            doctorName);

                                    analytics.setSpeciality(
                                            speciality);

                                    return analytics;
                                });

                dto.setTotalScheduled(
                        dto.getTotalScheduled()
                                + 1);

                String bookingId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "bookingId",
                                        ""));

                String status =
                        String.valueOf(
                                booking.getOrDefault(
                                        "status",
                                        ""));

                String followupStatus =
                        String.valueOf(
                                booking.getOrDefault(
                                        "followupStatus",
                                        ""));

                boolean completed =
                        "completed"
                                .equalsIgnoreCase(
                                        status)
                        || "completed"
                                .equalsIgnoreCase(
                                        followupStatus);

                boolean cancelled =
                        "cancelled"
                                .equalsIgnoreCase(
                                        status)
                        || "cancelled"
                                .equalsIgnoreCase(
                                        followupStatus);

                if (completed) {

                    dto.setCompleted(
                            dto.getCompleted()
                                    + 1);
                }

                if (cancelled) {

                    dto.setCancelled(
                            dto.getCancelled()
                                    + 1);
                }

                boolean paymentCompleted =
                        false;

                try {

                    Response paymentResponse =
                            physiotherapyFeignClient
                                    .getPayment(
                                            bookingId);

                    if (paymentResponse != null
                            && paymentResponse
                                    .getData() != null) {

                        Map<String, Object> payment =
                                (Map<String, Object>) paymentResponse
                                        .getData();

                        String overallStatus =
                                String.valueOf(
                                        payment.getOrDefault(
                                                "overallStatus",
                                                ""));

                        paymentCompleted =
                                "completed"
                                        .equalsIgnoreCase(
                                                overallStatus);
                    }

                } catch (Exception e) {

                    System.out.println(
                            "Payment not found for bookingId : "
                                    + bookingId);
                }

                if (!completed
                        && !paymentCompleted
                        && !cancelled) {

                    dto.setMissed(
                            dto.getMissed()
                                    + 1);
                }
            }

            List<DoctorAnalyticsDTO> analyticsList =
                    new ArrayList<>(
                            doctorAnalyticsMap
                                    .values());

            analyticsList.forEach(dto -> {

                if (dto.getTotalScheduled() > 0) {

                    double completionRate =
                            ((double) dto.getCompleted()
                                    / dto.getTotalScheduled())
                                    * 100;

                    dto.setCompletionRate(
                            Math.round(
                                    completionRate
                                            * 100.0)
                                    / 100.0);
                }
            });

            response.setSuccess(true);
            response.setMessage(
                    "Doctor analytics fetched successfully");
            response.setData(
                    analyticsList);
            response.setStatus(
                    HttpStatus.OK.value());

        } catch (Exception e) {

            e.printStackTrace();

            response.setSuccess(false);
            response.setMessage(
                    e.getMessage());
            response.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR
                            .value());
        }

        return response;
    }
    
    @Override
    public Response getAppointmentSummary(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate) {

        Response response = new Response();

        try {

            ResponseEntity<ResponseStructure<List<Map<String, Object>>>> bookingResponse =
                    bookingFeign.getBookedServicesByClinicIdWithBranchId(
                            clinicId,
                            branchId);

            if (bookingResponse == null
                    || bookingResponse.getBody() == null
                    || bookingResponse.getBody().getData() == null) {

                response.setSuccess(false);
                response.setMessage("No booking data found");
                response.setStatus(404);

                return response;
            }

            List<Map<String, Object>> bookings =
                    bookingResponse.getBody().getData();

            LocalDate today = LocalDate.now();

            long totalAppointments = 0;
            long completedCount = 0;
            long cancelledCount = 0;
            long missedCount = 0;

            for (Map<String, Object> booking : bookings) {

                String serviceDateStr =
                        String.valueOf(
                                booking.getOrDefault(
                                        "serviceDate",
                                        ""));

                if (serviceDateStr.isBlank()) {
                    continue;
                }

                LocalDate serviceDate =
                        LocalDate.parse(serviceDateStr);

                boolean include = false;

                switch (type) {

                    case 1:
                        include = serviceDate.equals(today);
                        break;

                    case 2:
                        include =
                                !serviceDate.isBefore(today.minusDays(6))
                                && !serviceDate.isAfter(today);
                        break;

                    case 3:
                        include =
                                serviceDate.getMonthValue() == today.getMonthValue()
                                && serviceDate.getYear() == today.getYear();
                        break;

                    case 4:
                        include =
                                serviceDate.getYear() == today.getYear();
                        break;

                    case 5:

                        LocalDate start =
                                LocalDate.parse(startDate);

                        LocalDate end =
                                LocalDate.parse(endDate);

                        include =
                                !serviceDate.isBefore(start)
                                && !serviceDate.isAfter(end);

                        break;
                }

                if (!include) {
                    continue;
                }

                totalAppointments++;

                String bookingId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "bookingId",
                                        ""));

                String status =
                        String.valueOf(
                                booking.getOrDefault(
                                        "status",
                                        ""));

                String followupStatus =
                        String.valueOf(
                                booking.getOrDefault(
                                        "followupStatus",
                                        ""));

                boolean completed =
                        "completed".equalsIgnoreCase(status)
                        || "completed".equalsIgnoreCase(followupStatus);

                boolean cancelled =
                        "cancelled".equalsIgnoreCase(status)
                        || "cancelled".equalsIgnoreCase(followupStatus);

                if (completed) {
                    completedCount++;
                }

                if (cancelled) {
                    cancelledCount++;
                }

                boolean paymentCompleted = false;

                try {

                    Response paymentResponse =
                            physiotherapyFeignClient
                                    .getPayment(bookingId);

                    if (paymentResponse != null
                            && paymentResponse.getData() != null) {

                        Map<String, Object> payment =
                                (Map<String, Object>) paymentResponse.getData();

                        String overallStatus =
                                String.valueOf(
                                        payment.getOrDefault(
                                                "overallStatus",
                                                ""));

                        paymentCompleted =
                                "completed".equalsIgnoreCase(
                                        overallStatus);
                    }

                } catch (Exception e) {
                }

                if (!completed
                        && !cancelled
                        && !paymentCompleted) {

                    missedCount++;
                }
            }

            AppointmentSummaryDTO dto =
                    new AppointmentSummaryDTO();

            dto.setTotalAppointments(totalAppointments);
            dto.setCompleted(completedCount);
            dto.setCancelled(cancelledCount);
            dto.setMissed(missedCount);

            response.setSuccess(true);
            response.setMessage(
                    "Appointment summary fetched successfully");
            response.setData(dto);
            response.setStatus(200);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }

        return response;
    }
    
}