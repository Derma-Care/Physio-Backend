package com.clinicadmin.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.CustomerOnbordingDTO;
import com.clinicadmin.dto.DoctorReferralAnalyticsDTO;
import com.clinicadmin.dto.DoctorReferralPatientDTO;
import com.clinicadmin.dto.ReferralChannelDTO;
import com.clinicadmin.dto.ReferralChannelPatientDTO;
import com.clinicadmin.dto.ReferralSummaryDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.TopReferringDoctorDTO;
import com.clinicadmin.entity.ReferredDoctor;
import com.clinicadmin.feignclient.BookingFeign;
import com.clinicadmin.feignclient.PhysiotherapyFeignClient;
import com.clinicadmin.repository.ReferredDoctorRepository;
import com.clinicadmin.service.AnalyticsService;
import com.clinicadmin.service.CustomerOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private BookingFeign bookingFeign;
    
    @Autowired
    private CustomerOnboardingService customerOnboardingService;
    
    @Autowired
    
    private PhysiotherapyFeignClient PhysiotherapyFeignClient;

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

                String doctorRefCode =
                        String.valueOf(
                                booking.getOrDefault(
                                        "doctorRefCode",
                                        "")).trim();

                String referredByType =
                        String.valueOf(
                                booking.getOrDefault(
                                        "referredByType",
                                        "")).trim();

                String referredByName =
                        String.valueOf(
                                booking.getOrDefault(
                                        "referredByName",
                                        "")).trim();

                String referralKey;

                if (!referralId.isBlank()
                        && !"null".equalsIgnoreCase(referralId)) {

                    referralKey = referralId;

                } else if ("Other".equalsIgnoreCase(doctorRefCode)) {

                    referralKey = "Other Sources";

                } else if (doctorRefCode.isBlank()
                        && referredByType.isBlank()
                        && referredByName.isBlank()) {

                    referralKey = "Others";

                } else {

                    referralKey = "Other Sources";
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
                                referralKey,
                                id -> {

                                    DoctorReferralAnalyticsDTO analytics =
                                            new DoctorReferralAnalyticsDTO();

                                    analytics.setReferralId(
                                            referralKey);

//                                    analytics.setDoctorId(
//                                            doctorId);

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
    
    @Override
    public Response getDoctorReferralPatientDetails(
            String clinicId,
            String branchId,
            String referralId) {

        Response response = new Response();

        try {

            ResponseEntity<ResponseStructure<List<Map<String, Object>>>> bookingResponse =
                    bookingFeign.getBookedServicesByClinicIdWithBranchId(
                            clinicId,
                            branchId);

            List<Map<String, Object>> bookings =
                    bookingResponse.getBody().getData();

            List<DoctorReferralPatientDTO> result =
                    new ArrayList<>();

            for (Map<String, Object> booking : bookings) {

                String referredDoctorId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "referredDoctorId",
                                        ""));

                if (!referralId.equals(referredDoctorId)) {
                    continue;
                }

                String bookingId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "bookingId",
                                        ""));

                String patientId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "patientId",
                                        ""));

                Response paymentResponse =
                        PhysiotherapyFeignClient.getPayment(
                                bookingId);

                if (paymentResponse == null
                        || paymentResponse.getData() == null) {
                    continue;
                }

                Map<String, Object> payment =
                        (Map<String, Object>) paymentResponse.getData();

                String patientName = "";
                String contactNumber = "";

                try {

                    Response customerResponse =
                            customerOnboardingService
                                    .getCustomersByPatientId(
                                            patientId,
                                            clinicId);

                    if (customerResponse != null
                            && customerResponse.getData() != null) {

                        CustomerOnbordingDTO customer =
                                new ObjectMapper()
                                        .convertValue(
                                                customerResponse.getData(),
                                                CustomerOnbordingDTO.class);

                        patientName =
                                customer.getFullName();

                        contactNumber =
                                customer.getMobileNumber();
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                DoctorReferralPatientDTO dto =
                        new DoctorReferralPatientDTO();

                dto.setPatientName(patientName);
                dto.setContactNumber(contactNumber);
                dto.setBookingId(bookingId);
                dto.setServiceDate(
                        String.valueOf(
                                booking.getOrDefault(
                                        "serviceDate",
                                        "")));

                dto.setServiceTime(
                        String.valueOf(
                                booking.getOrDefault(
                                        "servicetime",
                                        "")));
                dto.setDateOfVisit(
                        String.valueOf(
                        		payment.getOrDefault(
                                        "sessionStartDate",
                                        "")));

                dto.setServiceName(
                        String.valueOf(
                                payment.getOrDefault(
                                        "treatmentName",
                                        "")));

                dto.setServiceType(
                        String.valueOf(
                                payment.getOrDefault(
                                        "serviceType",
                                        "")));

                dto.setStatus(
                        String.valueOf(
                                payment.getOrDefault(
                                        "overallStatus",
                                        "")));

                dto.setTotalCost(
                        Double.parseDouble(
                                String.valueOf(
                                        payment.getOrDefault(
                                                "finalAmount",
                                                0))));

                dto.setPaidAmount(
                        Double.parseDouble(
                                String.valueOf(
                                        payment.getOrDefault(
                                                "totalPaid",
                                                0))));

                dto.setPendingAmount(
                        Double.parseDouble(
                                String.valueOf(
                                        payment.getOrDefault(
                                                "balanceAmount",
                                                0))));

                result.add(dto);
            }

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Referral patient details fetched successfully");
            response.setData(result);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
        }

        return response;
    }
    @Override
    public Response getReferralChannels(
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

            List<Map<String, Object>> bookings =
                    bookingResponse.getBody().getData();

            LocalDate today = LocalDate.now();

            Map<String, ReferralChannelDTO> channelMap =
                    new LinkedHashMap<>();

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

                case 2: // Weekly
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

                case 5: // Custom

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
                                        "")).trim();

                String doctorRefCode =
                        String.valueOf(
                                booking.getOrDefault(
                                        "doctorRefCode",
                                        "")).trim();

                String referredByType =
                        String.valueOf(
                                booking.getOrDefault(
                                        "referredByType",
                                        "")).trim();

                String referredByName =
                        String.valueOf(
                                booking.getOrDefault(
                                        "referredByName",
                                        "")).trim();

                String channel;

                if (!referralId.isBlank()
                        && !"null".equalsIgnoreCase(referralId)) {

                    // Referred by registered doctor
                    channel = "Doctor Referral";

                } else if (!referredByType.isBlank()
                        && !"null".equalsIgnoreCase(referredByType)) {

                    // Facebook, Instagram, Google, Website, etc.
                    channel = referredByType.trim();

                } else if (!referredByName.isBlank()
                        && !"null".equalsIgnoreCase(referredByName)) {

                    // Fallback if source is stored in referredByName
                    channel = referredByName.trim();

                } else {

                    channel = "Others";
                }

//                String referredByName =
//                        String.valueOf(
//                                booking.getOrDefault(
//                                        "referredByName",
//                                        ""));

                Double revenue =
                        Double.parseDouble(
                                String.valueOf(
                                        booking.getOrDefault(
                                                "totalFee",
                                                0)));

                ReferralChannelDTO dto =
                        channelMap.computeIfAbsent(
                                channel,
                                k -> {
                                    ReferralChannelDTO r =
                                            new ReferralChannelDTO();

                                    r.setChannel(k);
//                                    r.setReferredByName(
//                                            referredByName);

                                    r.setPatientsReferred(0L);
                                    r.setRevenueGenerated(0.0);

                                    return r;
                                });

                dto.setPatientsReferred(
                        dto.getPatientsReferred() + 1);

                dto.setRevenueGenerated(
                        dto.getRevenueGenerated() + revenue);
            }

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Referral channel analytics fetched successfully");
            response.setData(
                    new ArrayList<>(channelMap.values()));

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
        }

        return response;
    }
    @Override
    public Response getReferralChannelPatientDetails(
            String clinicId,
            String branchId,
            String channel) {

        Response response = new Response();

        try {

            ResponseEntity<ResponseStructure<List<Map<String, Object>>>> bookingResponse =
                    bookingFeign.getBookedServicesByClinicIdWithBranchId(
                            clinicId,
                            branchId);

            List<Map<String, Object>> bookings =
                    bookingResponse.getBody().getData();

            List<ReferralChannelPatientDTO> result =
                    new ArrayList<>();

            for (Map<String, Object> booking : bookings) {

            	String referralId =
            	        String.valueOf(
            	                booking.getOrDefault(
            	                        "referredDoctorId",
            	                        "")).trim();

            	String doctorRefCode =
            	        String.valueOf(
            	                booking.getOrDefault(
            	                        "doctorRefCode",
            	                        "")).trim();

            	String referredByType =
            	        String.valueOf(
            	                booking.getOrDefault(
            	                        "referredByType",
            	                        "")).trim();

            	String referredByName =
            	        String.valueOf(
            	                booking.getOrDefault(
            	                        "referredByName",
            	                        "")).trim();

            	String derivedChannel;

            	if (!referralId.isBlank()
            	        && !"null".equalsIgnoreCase(referralId)) {

            	    derivedChannel = "Doctor Referral";

            	} else if ("Other".equalsIgnoreCase(doctorRefCode)) {

            	    derivedChannel = "Other Sources";

            	} else if (doctorRefCode.isBlank()
            	        && referredByType.isBlank()
            	        && referredByName.isBlank()) {

            	    derivedChannel = "Others";

            	} else {

            	    derivedChannel = "Other Sources";
            	}
                

            	if (!channel.equalsIgnoreCase(
            	        derivedChannel)) {
            	    continue;
            	}

                String bookingId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "bookingId",
                                        ""));

                String patientId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "patientId",
                                        ""));

                Response paymentResponse =
                        PhysiotherapyFeignClient
                                .getPayment(
                                        bookingId);

                if (paymentResponse == null
                        || paymentResponse.getData() == null) {
                    continue;
                }

                Map<String, Object> payment =
                        (Map<String, Object>)
                                paymentResponse.getData();

                String patientName = "";
                String contactNumber = "";

                try {

                    Response customerResponse =
                            customerOnboardingService
                                    .getCustomersByPatientId(
                                            patientId,
                                            clinicId);

                    if (customerResponse != null
                            && customerResponse.getData() != null) {

                        CustomerOnbordingDTO customer =
                                new ObjectMapper()
                                        .convertValue(
                                                customerResponse.getData(),
                                                CustomerOnbordingDTO.class);

                        patientName =
                                customer.getFullName();

                        contactNumber =
                                customer.getMobileNumber();
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                ReferralChannelPatientDTO dto =
                        new ReferralChannelPatientDTO();

                dto.setPatientName(patientName);
                dto.setContactNumber(contactNumber);
                dto.setBookingId(bookingId);

                dto.setServiceDate(
                        String.valueOf(
                                booking.getOrDefault(
                                        "serviceDate",
                                        "")));

                dto.setServiceTime(
                        String.valueOf(
                                booking.getOrDefault(
                                        "servicetime",
                                        "")));

                dto.setServiceName(
                        String.valueOf(
                                payment.getOrDefault(
                                        "treatmentName",
                                        "")));

                dto.setServiceType(
                        String.valueOf(
                                payment.getOrDefault(
                                        "serviceType",
                                        "")));

                dto.setStatus(
                        String.valueOf(
                                payment.getOrDefault(
                                        "overallStatus",
                                        "")));

                dto.setReferredByPerson(
                        String.valueOf(
                                booking.getOrDefault(
                                        "referredByName",
                                        "")));

                dto.setTotalCost(
                        Double.parseDouble(
                                String.valueOf(
                                        payment.getOrDefault(
                                                "finalAmount",
                                                0))));

                dto.setPaidAmount(
                        Double.parseDouble(
                                String.valueOf(
                                        payment.getOrDefault(
                                                "totalPaid",
                                                0))));

                dto.setPendingAmount(
                        Double.parseDouble(
                                String.valueOf(
                                        payment.getOrDefault(
                                                "balanceAmount",
                                                0))));
                dto.setDateOfVisit(
                        String.valueOf(
                                payment.getOrDefault(
                                        "sessionStartDate",
                                        "")));

                result.add(dto);
            }

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Referral channel patient details fetched successfully");
            response.setData(result);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
        }

        return response;
    }
    
    @Override
    public Response getReferralSummary(
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

            List<Map<String, Object>> bookings =
                    bookingResponse.getBody().getData();

            LocalDate today = LocalDate.now();

            long totalReferrals = 0;
            long doctorReferrals = 0;
            long otherChannelsReferrals = 0;

            Map<String, Long> doctorCountMap =
                    new HashMap<>();

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
                            !serviceDate.isBefore(
                                    today.minusDays(6))
                            && !serviceDate.isAfter(today);
                    break;

                case 3:
                    include =
                            serviceDate.getMonthValue()
                                    == today.getMonthValue()
                            && serviceDate.getYear()
                                    == today.getYear();
                    break;

                case 4:
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
                }

                if (!include) {
                    continue;
                }

                totalReferrals++;

                String referralId =
                        String.valueOf(
                                booking.getOrDefault(
                                        "referredDoctorId",
                                        "")).trim();

                if (!referralId.isBlank()
                        && !"null".equalsIgnoreCase(
                                referralId)) {

                    doctorReferrals++;

                    doctorCountMap.merge(
                            referralId,
                            1L,
                            Long::sum);

                } else {

                    otherChannelsReferrals++;
                }
            }

            double doctorPercentage =
                    totalReferrals == 0
                            ? 0
                            : (doctorReferrals * 100.0)
                                    / totalReferrals;

            double otherPercentage =
                    totalReferrals == 0
                            ? 0
                            : (otherChannelsReferrals
                                    * 100.0)
                                    / totalReferrals;

            String topDoctorName = "N/A";
            long topDoctorPatients = 0;

            for (Map.Entry<String, Long> entry :
                    doctorCountMap.entrySet()) {

                if (entry.getValue()
                        > topDoctorPatients) {

                    topDoctorPatients =
                            entry.getValue();

                    ReferredDoctor doctor =
                            referredDoctorRepository
                                    .findByReferralId(
                                            entry.getKey())
                                    .orElse(null);

                    if (doctor != null) {
                        topDoctorName =
                                doctor.getFullName();
                    }
                }
            }

            TopReferringDoctorDTO topDoctor =
                    new TopReferringDoctorDTO();

            topDoctor.setFullName(
                    topDoctorName);

            topDoctor.setPatientsReferred(
                    topDoctorPatients);

            ReferralSummaryDTO dto =
                    new ReferralSummaryDTO();

            dto.setTotalReferrals(
                    totalReferrals);

            dto.setDoctorReferrals(
                    doctorReferrals);

            dto.setDoctorReferralsPercentage(
                    Math.round(
                            doctorPercentage * 100.0)
                            / 100.0);

            dto.setOtherChannelsReferrals(
                    otherChannelsReferrals);

            dto.setOtherChannelsReferralsPercentage(
                    Math.round(
                            otherPercentage * 100.0)
                            / 100.0);

            dto.setTopReferringDoctor(
                    topDoctor);

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Referral summary fetched successfully");
            response.setData(dto);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(
                    e.getMessage());
        }

        return response;
    }
}