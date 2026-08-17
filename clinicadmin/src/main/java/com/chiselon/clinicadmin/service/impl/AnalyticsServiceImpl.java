package com.chiselon.clinicadmin.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.chiselon.clinicadmin.dto.CustomerOnbordingDTO;
import com.chiselon.clinicadmin.dto.DoctorReferralAnalyticsDTO;
import com.chiselon.clinicadmin.dto.DoctorReferralPatientDTO;
import com.chiselon.clinicadmin.dto.ReferralChannelDTO;
import com.chiselon.clinicadmin.dto.ReferralChannelPatientDTO;
import com.chiselon.clinicadmin.dto.ReferralSummaryDTO;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.dto.TopReferringDoctorDTO;
import com.chiselon.clinicadmin.entity.ReferredDoctor;
import com.chiselon.clinicadmin.feignclient.BookingFeign;
import com.chiselon.clinicadmin.feignclient.PhysiotherapyFeignClient;
import com.chiselon.clinicadmin.repository.ReferredDoctorRepository;
import com.chiselon.clinicadmin.service.AnalyticsService;
import com.chiselon.clinicadmin.service.CustomerOnboardingService;
import com.chiselon.clinicadmin.utils.KeyCloakTokenStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private BookingFeign bookingFeign;
    
    @Autowired
    private CustomerOnboardingService customerOnboardingService;
    
    @Autowired
    
    private PhysiotherapyFeignClient PhysiotherapyFeignClient;

    @Autowired
    private ReferredDoctorRepository referredDoctorRepository;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
   

    @Override
    @RateLimiter(
            name = "bookingService",
            fallbackMethod = "getDoctorReferralAnalyticsFallback")
    public Response getDoctorReferralAnalytics(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate) {

        log.info(
                "Fetching doctor referral analytics. ClinicId: {}, BranchId: {}, Type: {}, StartDate: {}, EndDate: {}",
                clinicId,
                branchId,
                type,
                startDate,
                endDate);

        Response response = new Response();

        try {

            ResponseEntity<Response> bookingResponse =
                    bookingFeign.getBookedServicesByClinicIdWithBranchId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId);

            if (bookingResponse == null
                    || bookingResponse.getBody() == null) {

                log.warn(
                        "No booking data found. ClinicId: {}, BranchId: {}",
                        clinicId,
                        branchId);

                response.setSuccess(false);
                response.setMessage("No booking data found");

                return response;
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            List<Map<String, Object>> bookings =
                    mapper.convertValue(
                            bookingResponse.getBody(),
                            new TypeReference<List<Map<String, Object>>>() {
                            });

            log.info(
                    "Total bookings received: {}. ClinicId: {}, BranchId: {}",
                    bookings.size(),
                    clinicId,
                    branchId);

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

                if (referralId.isBlank()
                        || referralId.equals("null")) {
                    continue;
                }

                Double revenue = 0.0;

                Object totalFeeObj =
                        booking.get("totalFee");

                if (totalFeeObj != null) {

                    try {

                        revenue =
                                Double.parseDouble(
                                        totalFeeObj.toString());

                    } catch (Exception ex) {

                        log.warn(
                                "Invalid totalFee value for referralId: {}",
                                referralId);

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
                                    }

                                    return analytics;
                                });

                dto.setPatientsReferred(
                        dto.getPatientsReferred() + 1);

                dto.setRevenueGenerated(
                        dto.getRevenueGenerated() + revenue);
            }

            log.info(
                    "Doctor referral analytics generated successfully. Total Referrals: {}",
                    doctorAnalytics.size());

            response.setSuccess(true);
            response.setData(
                    new ArrayList<>(
                            doctorAnalytics.values()));
            response.setMessage(
                    "Doctor referral analytics fetched successfully");
            response.setStatus(200);

        } catch (Exception e) {

            log.error(
                    "Error while fetching doctor referral analytics. ClinicId: {}, BranchId: {}",
                    clinicId,
                    branchId,
                    e);

            response.setSuccess(false);
            response.setMessage(
                    e.getMessage());
            response.setStatus(500);
        }

        return response;
    }
    
    
   
    @Override
    @RateLimiter(
            name = "bookingService",
            fallbackMethod = "getDoctorReferralPatientDetailsFallback")
    public Response getDoctorReferralPatientDetails(
            String clinicId,
            String branchId,
            String referralId) {

        log.info(
                "Fetching referral patient details. clinicId={}, branchId={}, referralId={}",
                clinicId,
                branchId,
                referralId);

        Response response = new Response();

        try {

            ResponseEntity<Response> bookingResponse =
                    bookingFeign.getBookedServicesByClinicIdWithBranchId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId);

            log.info(
                    "Received booking response for clinicId={}, branchId={}",
                    clinicId,
                    branchId);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            List<Map<String, Object>> bookings =
                    mapper.convertValue(
                            bookingResponse.getBody().getData(),
                            new TypeReference<List<Map<String, Object>>>() {});

            log.info("Total bookings fetched: {}", bookings.size());

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

                log.debug(
                        "Processing bookingId={}, patientId={}",
                        bookingId,
                        patientId);

                Response paymentResponse =
                        PhysiotherapyFeignClient.getPayment(
                                keyCloakTokenStore.getAccess_token(),
                                bookingId);

                if (paymentResponse == null
                        || paymentResponse.getData() == null) {

                    log.warn(
                            "Payment details not found for bookingId={}",
                            bookingId);
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
                                new ObjectMapper().convertValue(
                                        customerResponse.getData(),
                                        CustomerOnbordingDTO.class);

                        patientName =
                                customer.getFullName();

                        contactNumber =
                                customer.getMobileNumber();
                    }

                } catch (Exception ex) {

                    log.error(
                            "Error fetching customer details for patientId={}",
                            patientId,
                            ex);
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

            log.info(
                    "Referral patient details fetched successfully. Total records={}",
                    result.size());

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Referral patient details fetched successfully");
            response.setData(result);

        } catch (Exception e) {

            log.error(
                    "Error while fetching referral patient details for referralId={}",
                    referralId,
                    e);

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
        }

        return response;
    }
    
    
    
   
    @Override
    @RateLimiter(
            name = "bookingService",
            fallbackMethod = "getReferralChannelsFallback")
    public Response getReferralChannels(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate) {

        log.info(
                "Fetching referral channel analytics. clinicId={}, branchId={}, type={}, startDate={}, endDate={}",
                clinicId,
                branchId,
                type,
                startDate,
                endDate);

        Response response = new Response();

        try {

            ResponseEntity<Response> bookingResponse =
                    bookingFeign.getBookedServicesByClinicIdWithBranchId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId);

            log.info(
                    "Successfully fetched bookings for clinicId={}, branchId={}",
                    clinicId,
                    branchId);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            List<Map<String, Object>> bookings =
                    mapper.convertValue(
                            bookingResponse.getBody().getData(),
                            new TypeReference<List<Map<String, Object>>>() {});

            log.info("Total bookings fetched: {}", bookings.size());

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

                case 1:
                    include = serviceDate.equals(today);
                    break;

                case 2:
                    include = !serviceDate.isBefore(today.minusDays(6))
                            && !serviceDate.isAfter(today);
                    break;

                case 3:
                    include = serviceDate.getMonthValue()
                            == today.getMonthValue()
                            && serviceDate.getYear()
                            == today.getYear();
                    break;

                case 4:
                    include = serviceDate.getYear()
                            == today.getYear();
                    break;

                case 5:

                    LocalDate start =
                            LocalDate.parse(startDate);

                    LocalDate end =
                            LocalDate.parse(endDate);

                    include = !serviceDate.isBefore(start)
                            && !serviceDate.isAfter(end);

                    break;

                default:
                    include = false;
                }

                if (!include) {
                    continue;
                }

                String channel =
                        String.valueOf(
                                booking.getOrDefault(
                                        "referredByType",
                                        "Other"));

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
                                    r.setPatientsReferred(0L);
                                    r.setRevenueGenerated(0.0);

                                    return r;
                                });

                dto.setPatientsReferred(
                        dto.getPatientsReferred() + 1);

                dto.setRevenueGenerated(
                        dto.getRevenueGenerated() + revenue);
            }

            log.info(
                    "Referral analytics generated successfully. Total channels={}",
                    channelMap.size());

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Referral channel analytics fetched successfully");
            response.setData(
                    new ArrayList<>(channelMap.values()));

        } catch (Exception e) {

            log.error(
                    "Error while fetching referral channel analytics. clinicId={}, branchId={}",
                    clinicId,
                    branchId,
                    e);

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
        }

        return response;
    }
    
    
    @Override
    @RateLimiter(
            name = "bookingService",
            fallbackMethod = "getReferralChannelPatientDetailsFallback")
    public Response getReferralChannelPatientDetails(
            String clinicId,
            String branchId,
            String channel) {

        log.info(
                "Fetching referral channel patient details. clinicId={}, branchId={}, channel={}",
                clinicId,
                branchId,
                channel);

        Response response = new Response();

        try {

            ResponseEntity<Response> bookingResponse =
                    bookingFeign.getBookedServicesByClinicIdWithBranchId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            List<Map<String, Object>> bookings =
                    mapper.convertValue(
                            bookingResponse.getBody().getData(),
                            new TypeReference<List<Map<String, Object>>>() {});

            log.info(
                    "Total bookings fetched for channel analysis: {}",
                    bookings.size());

            List<ReferralChannelPatientDTO> result =
                    new ArrayList<>();

            for (Map<String, Object> booking : bookings) {

                String referredByType =
                        String.valueOf(
                                booking.getOrDefault(
                                        "referredByType",
                                        ""));

                if (!channel.equalsIgnoreCase(referredByType)) {
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

                log.debug(
                        "Processing bookingId={}, patientId={}",
                        bookingId,
                        patientId);

                Response paymentResponse =
                        PhysiotherapyFeignClient.getPayment(
                                keyCloakTokenStore.getAccess_token(),
                                bookingId);

                if (paymentResponse == null
                        || paymentResponse.getData() == null) {

                    log.warn(
                            "Payment details not found for bookingId={}",
                            bookingId);
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
                                new ObjectMapper().convertValue(
                                        customerResponse.getData(),
                                        CustomerOnbordingDTO.class);

                        patientName = customer.getFullName();
                        contactNumber = customer.getMobileNumber();
                    }

                } catch (Exception ex) {

                    log.error(
                            "Error fetching customer details for patientId={}",
                            patientId,
                            ex);
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

            log.info(
                    "Referral channel patient details fetched successfully. channel={}, totalPatients={}",
                    channel,
                    result.size());

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Referral channel patient details fetched successfully");
            response.setData(result);

        } catch (Exception e) {

            log.error(
                    "Error while fetching referral channel patient details. clinicId={}, branchId={}, channel={}",
                    clinicId,
                    branchId,
                    channel,
                    e);

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
        }

        return response;
    }
    
    @Override
    @RateLimiter(
            name = "bookingService",
            fallbackMethod = "getReferralSummaryFallback")
    public Response getReferralSummary(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate) {

        log.info(
                "Fetching referral summary. clinicId={}, branchId={}, type={}, startDate={}, endDate={}",
                clinicId,
                branchId,
                type,
                startDate,
                endDate);

        Response response = new Response();

        try {

            ResponseEntity<Response> bookingResponse =
                    bookingFeign.getBookedServicesByClinicIdWithBranchId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            List<Map<String, Object>> bookings =
                    mapper.convertValue(
                            bookingResponse.getBody().getData(),
                            new TypeReference<List<Map<String, Object>>>() {});

            log.info(
                    "Total bookings fetched for referral summary: {}",
                    bookings.size());

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
                    include = !serviceDate.isBefore(today.minusDays(6))
                            && !serviceDate.isAfter(today);
                    break;

                case 3:
                    include = serviceDate.getMonthValue()
                            == today.getMonthValue()
                            && serviceDate.getYear()
                            == today.getYear();
                    break;

                case 4:
                    include = serviceDate.getYear()
                            == today.getYear();
                    break;

                case 5:

                    LocalDate start =
                            LocalDate.parse(startDate);

                    LocalDate end =
                            LocalDate.parse(endDate);

                    include = !serviceDate.isBefore(start)
                            && !serviceDate.isAfter(end);

                    break;

                default:
                    include = false;
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
                        && !"null".equalsIgnoreCase(referralId)) {

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
                            : (otherChannelsReferrals * 100.0)
                            / totalReferrals;

            String topDoctorName = "N/A";
            long topDoctorPatients = 0;

            for (Map.Entry<String, Long> entry :
                    doctorCountMap.entrySet()) {

                if (entry.getValue() > topDoctorPatients) {

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

            log.info(
                    "Referral summary generated successfully. totalReferrals={}, doctorReferrals={}, otherChannelsReferrals={}",
                    totalReferrals,
                    doctorReferrals,
                    otherChannelsReferrals);

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage(
                    "Referral summary fetched successfully");
            response.setData(dto);

        } catch (Exception e) {

            log.error(
                    "Error while fetching referral summary. clinicId={}, branchId={}",
                    clinicId,
                    branchId,
                    e);

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(
                    e.getMessage());
        }

        return response;
    }
    
    
    public Response getDoctorReferralAnalyticsFallback(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate,
            Exception ex) {

        log.error(
                "Rate limiter fallback triggered for getDoctorReferralAnalytics. ClinicId: {}, BranchId: {}",
                clinicId,
                branchId,
                ex);

        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again after some time.");
    }
    
    public Response getDoctorReferralPatientDetailsFallback(
            String clinicId,
            String branchId,
            String referralId,
            Exception ex) {

        log.error(
                "Rate limiter fallback triggered for referral patient details. clinicId={}, branchId={}, referralId={}",
                clinicId,
                branchId,
                referralId,
                ex);


        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again after some time.");
    }
    
    public Response getReferralChannelsFallback(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate,
            Exception ex) {

        log.error(
                "Rate limiter fallback triggered for getReferralChannels. clinicId={}, branchId={}, type={}",
                clinicId,
                branchId,
                type,
                ex);

        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again after some time.");
    }
    
    public Response getReferralChannelPatientDetailsFallback(
            String clinicId,
            String branchId,
            String channel,
            Exception ex) {

        log.error(
                "Fallback triggered for getReferralChannelPatientDetails. clinicId={}, branchId={}, channel={}",
                clinicId,
                branchId,
                channel,
                ex);
        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again after some time.");
    }
    
    
    public Response getReferralSummaryFallback(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate,
            Exception ex) {

        log.error(
                "Fallback triggered for getReferralSummary. clinicId={}, branchId={}, type={}",
                clinicId,
                branchId,
                type,
                ex);

        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again after some time.");
    }
    
    
}