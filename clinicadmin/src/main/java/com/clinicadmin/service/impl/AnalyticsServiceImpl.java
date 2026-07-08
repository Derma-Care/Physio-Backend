package com.clinicadmin.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.DoctorReferralAnalyticsDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.entity.ReferredDoctor;
import com.clinicadmin.feignclient.BookingFeign;
import com.clinicadmin.repository.ReferredDoctorRepository;
import com.clinicadmin.service.AnalyticsService;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private BookingFeign bookingFeign;

    @Autowired
    private ReferredDoctorRepository referredDoctorRepository;

    @Override
    public Response getDoctorReferralAnalytics(
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
                return response;
            }

            List<Map<String, Object>> bookings =
                    bookingResponse.getBody().getData();

            LocalDate today = LocalDate.now();

            Map<String, DoctorReferralAnalyticsDTO> doctorAnalytics =
                    new HashMap<>();

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
                        LocalDate.parse(serviceDateStr);

                boolean include = false;

                switch (type) {

                case 1: // Today
                    include = serviceDate.equals(today);
                    break;

                case 2: // Weekly (Last 7 Days including today)
                    include =
                            !serviceDate.isBefore(
                                    today.minusDays(6))
                            && !serviceDate.isAfter(today);
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

                case 5:

                    LocalDate start =
                            LocalDate.parse(startDate);

                    LocalDate end =
                            LocalDate.parse(endDate);

                    include =
                            !serviceDate.isBefore(start)
                                    && !serviceDate.isAfter(end);

                    break;

                default:
                    include = false;
            }

                if (!include) {
                    continue;
                }

                String referralId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "referredDoctorId",
                                        ""))
                                .trim();

                if (referralId == null
                        || referralId.isBlank()
                        || referralId.equals("null")) {
                    continue;
                }

                String doctorId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "doctorId",
                                        ""));

                Double revenue = 0.0;

                Object totalFeeObj =
                        booking.get("totalFee");

                if (totalFeeObj != null) {

                    try {

                        revenue =
                                Double.parseDouble(
                                        totalFeeObj.toString());

                    } catch (Exception e) {

                        revenue = 0.0;
                    }
                }

                DoctorReferralAnalyticsDTO dto =
                        doctorAnalytics.computeIfAbsent(
                                referralId,
                                id -> {

                                    DoctorReferralAnalyticsDTO analytics =
                                            new DoctorReferralAnalyticsDTO();

                                    analytics.setReferralId(
                                            referralId);

                                    analytics.setDoctorId(
                                            doctorId);

                                    ReferredDoctor referredDoctor =
                                            referredDoctorRepository
                                                    .findByReferralId(
                                                            referralId)
                                                    .orElse(null);

                                    if (referredDoctor != null) {

                                        analytics.setDoctorName(
                                                referredDoctor.getFullName());

                                        analytics.setClinicHospitalName(
                                                referredDoctor.getCurrentHospitalName());

                                        analytics.setSpecialization(
                                                referredDoctor.getSpecialization());

                                        analytics.setContactInfo(
                                                referredDoctor.getMobileNumber());

                                    } else {

                                        analytics.setDoctorName("N/A");

                                        analytics.setClinicHospitalName("N/A");

                                        analytics.setSpecialization("N/A");

                                        analytics.setContactInfo("N/A");
                                    }

                                    analytics.setPatientsReferred(0);

                                    analytics.setRevenueGenerated(0.0);

                                    return analytics;
                                });

                dto.setPatientsReferred(
                        dto.getPatientsReferred() + 1);

                dto.setRevenueGenerated(
                        dto.getRevenueGenerated() + revenue);
            }

            response.setSuccess(true);
            response.setData(
                    new ArrayList<>(
                            doctorAnalytics.values()));
            response.setMessage(
                    "Doctor referral analytics fetched successfully");
            response.setStatus(200);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setMessage(
                    e.getMessage());
            response.setStatus(500);
        }

        return response;
    }
}