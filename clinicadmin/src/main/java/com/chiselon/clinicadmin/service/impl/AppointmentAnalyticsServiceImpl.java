package com.chiselon.clinicadmin.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.chiselon.clinicadmin.dto.AppointmentSummaryDTO;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.service.AppointmentAnalyticsService;
import com.chiselon.clinicadmin.utils.FeignImpl;
import com.chiselon.clinicadmin.utils.KeyCloakTokenStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AppointmentAnalyticsServiceImpl
        implements AppointmentAnalyticsService {

    @Autowired
    private FeignImpl bookingFeign;
      
    @Autowired
    private FeignImpl adminServiceClient;

    @Autowired
    private FeignImpl physiotherapyFeignClient;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
  
    @Override
    @RateLimiter(
            name = "bookingService",
            fallbackMethod = "getAppointmentAnalyticsFallback")
    public Response getAppointmentAnalytics(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate) {
    	
    	log.info(
    	        "Fetching appointment analytics. clinicId={}, branchId={}, type={}, startDate={}, endDate={}",
    	        clinicId,
    	        branchId,
    	        type,
    	        startDate,
    	        endDate);

        Response response = new Response();

        try {

        	 ResponseEntity<Response> bookingResponse =
                     bookingFeign.getBookedServicesByClinicIdWithBranchId(keyCloakTokenStore.getAccess_token(),
                             clinicId,
                             branchId);
        	 
        	 log.info(
        		        "Calling booking service to fetch bookings. clinicId={}, branchId={}",
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

            ObjectMapper mapper = new ObjectMapper();
   		    mapper.registerModule(new JavaTimeModule());
   		    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
   		  
//            Map<String, DoctorAnalyticsDTO> doctorAnalyticsMap =
//                    new HashMap<>();

            List<Map<String, Object>> bookings = mapper.convertValue( bookingResponse.getBody(),  new TypeReference<List<Map<String, Object>>>() {});
           		
            log.info(
                    "Booking service response received successfully for clinicId={}, branchId={}",
                    clinicId,
                    branchId);
            
            Response clinicResponse =
                    adminServiceClient.getClinicById(keyCloakTokenStore.getAccess_token(),
                            clinicId);
            log.info(
                    "Fetched clinic details for clinicId={}",
                    clinicId);

            Map<String, Object> clinic =
                    (Map<String, Object>) clinicResponse                          
                            .getData();

            LocalTime openingTime =
                    LocalTime.parse(
                            String.valueOf(
                                    clinic.get("openingTime")));

            LocalTime closingTime =
                    LocalTime.parse(
                            String.valueOf(
                                    clinic.get("closingTime")));

            DateTimeFormatter timeFormatter =
                    DateTimeFormatter.ofPattern(
                            "hh:mm a");

            Map<String, Long> chartData =
                    new LinkedHashMap<>();
            log.info(
                    "Preparing chart data for analytics type={}",
                    type);

            switch (type) {

                case 1:

                    LocalTime slot =
                            openingTime;

                    while (slot.isBefore(
                            closingTime)) {

                        chartData.put(
                                slot.format(
                                        timeFormatter),
                                0L);

                        slot =
                                slot.plusHours(2);
                    }

                    break;

                case 2:

                    chartData.put("Monday", 0L);
                    chartData.put("Tuesday", 0L);
                    chartData.put("Wednesday", 0L);
                    chartData.put("Thursday", 0L);
                    chartData.put("Friday", 0L);
                    chartData.put("Saturday", 0L);
                    chartData.put("Sunday", 0L);

                    break;

                case 3:

                    chartData.put("Week 1", 0L);
                    chartData.put("Week 2", 0L);
                    chartData.put("Week 3", 0L);
                    chartData.put("Week 4", 0L);
                    chartData.put("Week 5", 0L);

                    break;

                case 4:

                    chartData.put("January", 0L);
                    chartData.put("February", 0L);
                    chartData.put("March", 0L);
                    chartData.put("April", 0L);
                    chartData.put("May", 0L);
                    chartData.put("June", 0L);
                    chartData.put("July", 0L);
                    chartData.put("August", 0L);
                    chartData.put("September", 0L);
                    chartData.put("October", 0L);
                    chartData.put("November", 0L);
                    chartData.put("December", 0L);

                    break;

                case 5:

                    LocalDate customDate =
                            LocalDate.parse(startDate);

                    LocalDate customEnd =
                            LocalDate.parse(endDate);

                    DateTimeFormatter customFormatter =
                            DateTimeFormatter.ofPattern("MMMM d");

                    while (!customDate.isAfter(customEnd)) {

                        chartData.put(
                                customDate.format(customFormatter),
                                0L);

                        customDate =
                                customDate.plusDays(1);
                    }

                    break;
            }

            long totalAppointments = 0;
            long completedCount = 0;
            long cancelledCount = 0;
            long missedCount = 0;

            LocalDate today =
                    LocalDate.now();

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

                    case 1:

                        include =
                                serviceDate.equals(
                                        today);
                        break;

                    case 2:

                        include =
                                !serviceDate.isBefore(
                                        today.minusDays(6))
                                        && !serviceDate.isAfter(
                                        today);
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

                totalAppointments++;

                String label = "";

                switch (type) {

                    case 1:

                        String serviceTimeStr =
                                String.valueOf(
                                        booking.getOrDefault(
                                                "servicetime",
                                                ""));

                        if (!serviceTimeStr.isBlank()) {

                            LocalTime serviceTime =
                                    LocalTime.parse(
                                            serviceTimeStr,
                                            timeFormatter);

                            LocalTime currentSlot =
                                    openingTime;

                            while (currentSlot.isBefore(
                                    closingTime)) {

                                LocalTime nextSlot =
                                        currentSlot.plusHours(
                                                2);

                                if ((serviceTime.equals(
                                        currentSlot)
                                        || serviceTime.isAfter(
                                        currentSlot))
                                        && serviceTime.isBefore(
                                        nextSlot)) {

                                    label =
                                            currentSlot.format(
                                                    timeFormatter);

                                    break;
                                }

                                currentSlot =
                                        nextSlot;
                            }
                        }

                        break;

                    case 2:

                        label =
                                serviceDate.getDayOfWeek()
                                        .getDisplayName(
                                                TextStyle.FULL,
                                                Locale.ENGLISH);

                        break;

                    case 3:

                        int week =
                                ((serviceDate.getDayOfMonth() - 1) / 7)
                                        + 1;

                        label =
                                "Week " + week;

                        break;

                    case 4:

                        label =
                                serviceDate.getMonth()
                                        .getDisplayName(
                                                TextStyle.FULL,
                                                Locale.ENGLISH);

                        break;

                    case 5:

                        DateTimeFormatter customFormatter =
                                DateTimeFormatter.ofPattern("MMMM d");

                        label =
                                serviceDate.format(customFormatter);

                        break;   
                        
                }

                if (chartData.containsKey(
                        label)) {

                    chartData.put(
                            label,
                            chartData.get(
                                    label) + 1);
                }

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
                        "completed".equalsIgnoreCase(
                                status)
                                || "completed".equalsIgnoreCase(
                                followupStatus);

                boolean cancelled =
                        "cancelled".equalsIgnoreCase(
                                status)
                                || "cancelled".equalsIgnoreCase(
                                followupStatus);

                if (completed) {
                    completedCount++;
                }

                if (cancelled) {
                    cancelledCount++;
                }

                boolean paymentCompleted =
                        false;

                try {

                    Response paymentResponse =
                            physiotherapyFeignClient
                                    .getPayment(keyCloakTokenStore.getAccess_token(),
                                            bookingId);

                    if (paymentResponse != null
                            && paymentResponse.getData() != null) {

                        Map<String, Object> payment =
                                (Map<String, Object>) paymentResponse
                                        .getData();

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

                	log.warn(
                	        "Payment details not found for bookingId={}",
                	        bookingId,
                	        e);
                }

                if (!completed
                        && !paymentCompleted
                        && !cancelled) {

                    missedCount++;
                }
            }

            long bookedCount =
                    totalAppointments
                            - cancelledCount;

            Map<String, Object> summary =
                    new HashMap<>();

            summary.put(
                    "totalAppointments",
                    totalAppointments);

            summary.put(
                    "completed",
                    completedCount);

            summary.put(
                    "cancelled",
                    cancelledCount);

            summary.put(
                    "missed",
                    missedCount);

            summary.put(
                    "booked",
                    bookedCount);

            Map<String, Object> trendData =
                    new HashMap<>();

            trendData.put(
                    "seriesLabels",
                    new ArrayList<>(
                            chartData.keySet()));

            trendData.put(
                    "appointmentVolumes",
                    new ArrayList<>(
                            chartData.values()));

            Map<String, Object> dashboard =
                    new HashMap<>();

            dashboard.put(
                    "summary",
                    summary);

            dashboard.put(
                    "trendData",
                    trendData);

            response.setSuccess(true);
            response.setMessage(
                    "Appointment analytics fetched successfully");
            response.setData(
                    dashboard);
            response.setStatus(
                    HttpStatus.OK.value());
            
            log.info(
                    "Appointment analytics fetched successfully for clinicId={}, branchId={}",
                    clinicId,
                    branchId);
            
            log.info(
                    "Appointment analytics summary generated. totalAppointments={}, completed={}, cancelled={}, missed={}, booked={}",
                    totalAppointments,
                    completedCount,
                    cancelledCount,
                    missedCount,
                    bookedCount);

        } catch (Exception e) {

        	log.error(
        	        "Error while fetching appointment analytics. clinicId={}, branchId={}, error={}",
        	        clinicId,
        	        branchId,
        	        e.getMessage(),
        	        e);

            response.setSuccess(false);
            response.setMessage(
                    e.getMessage());
            response.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }
    
    @Override
    @RateLimiter(
            name = "bookingService",
            fallbackMethod = "getAppointmentSummaryFallback")
    public Response getAppointmentSummary(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate) {

        log.info(
                "Fetching appointment summary. clinicId={}, branchId={}, type={}, startDate={}, endDate={}",
                clinicId,
                branchId,
                type,
                startDate,
                endDate);

        Response response = new Response();

        try {

            log.info(
                    "Calling booking service to fetch bookings. clinicId={}, branchId={}",
                    clinicId,
                    branchId);

            ResponseEntity<Response> bookingResponse =
                    bookingFeign.getBookedServicesByClinicIdWithBranchId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId);

            if (bookingResponse == null
                    || bookingResponse.getBody() == null
                    || bookingResponse.getBody().getData() == null) {

                log.warn(
                        "No booking data found. clinicId={}, branchId={}",
                        clinicId,
                        branchId);

                response.setSuccess(false);
                response.setMessage("No booking data found");
                response.setStatus(HttpStatus.NOT_FOUND.value());

                return response;
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            List<Map<String, Object>> bookings =
                    mapper.convertValue(
                            bookingResponse.getBody().getData(),
                            new TypeReference<List<Map<String, Object>>>() {});

            log.info(
                    "Successfully fetched {} bookings for clinicId={}, branchId={}",
                    bookings.size(),
                    clinicId,
                    branchId);

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
                            physiotherapyFeignClient.getPayment(
                                    keyCloakTokenStore.getAccess_token(),
                                    bookingId);

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

                } catch (Exception ex) {

                    log.warn(
                            "Failed to fetch payment details for bookingId={}",
                            bookingId,
                            ex);
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

            log.info(
                    "Appointment summary generated successfully. totalAppointments={}, completed={}, cancelled={}, missed={}",
                    totalAppointments,
                    completedCount,
                    cancelledCount,
                    missedCount);

            response.setSuccess(true);
            response.setMessage(
                    "Appointment summary fetched successfully");
            response.setData(dto);
            response.setStatus(HttpStatus.OK.value());

            log.info(
                    "Appointment summary response sent successfully. clinicId={}, branchId={}",
                    clinicId,
                    branchId);

        } catch (Exception ex) {

            log.error(
                    "Error while fetching appointment summary. clinicId={}, branchId={}, error={}",
                    clinicId,
                    branchId,
                    ex.getMessage(),
                    ex);

            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }
    
    public Response getAppointmentAnalyticsFallback(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate,
            Exception ex) {

        log.error(
                "Rate limiter triggered while fetching appointment analytics. clinicId={}, branchId={}",
                clinicId,
                branchId,
                ex);


        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again after some time.");
    }
    
    
    
    public Response getAppointmentSummaryFallback(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate,
            Exception ex) {

        log.error(
                "Rate limiter fallback triggered for getAppointmentSummary. clinicId={}, branchId={}",
                clinicId,
                branchId,
                ex);
        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again after some time.");
    }
    
    
    
}