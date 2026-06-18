package physiotherapydoctor.serviceImpl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.BranchDTO;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.entity.PhysiotherapyRecord;
import physiotherapydoctor.feign.AdminFeignClient;
import physiotherapydoctor.feign.BookingFeignClient;

@Service
@Slf4j
public class FollowUpWhatsAppService {

    private final WebClient webClient;
    private final BookingFeignClient bookingFeignClient;
    private final AdminFeignClient adminFeignClient;
    private final ObjectMapper mapper;

    @Value("${whatsapp.auth-key}")
    private String authKey;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.followup-reminder-template-id}")
    private String followUpTemplateId;

    public FollowUpWhatsAppService(
            WebClient.Builder webClientBuilder,
            BookingFeignClient bookingFeignClient,
            AdminFeignClient adminFeignClient,
            ObjectMapper mapper,
            @Value("${whatsapp.base-url}") String baseUrl
    ) {
        this.bookingFeignClient = bookingFeignClient;
        this.adminFeignClient = adminFeignClient;
        this.mapper = mapper;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    // =====================================================
    // PUBLIC API
    // =====================================================

    public void sendFollowUpReminder(PhysiotherapyRecord record, String nextVisitDate) {

        if (record == null || record.getBookingId() == null) {
            log.warn("FollowUp WhatsApp skipped — invalid record");
            return;
        }

        if (nextVisitDate == null || nextVisitDate.isBlank()) {
            log.warn("FollowUp WhatsApp skipped — no nextVisitDate for bookingId={}",
                    record.getBookingId());
            return;
        }

        try {

            // =====================================================
            // FETCH BOOKING
            // =====================================================

            BookingResponse booking = fetchBooking(record.getBookingId());

            if (booking == null) {
                log.warn("FollowUp WhatsApp skipped — booking not found: {}",
                        record.getBookingId());
                return;
            }

            // =====================================================
            // FETCH BRANCH FOR CLINIC CONTACT NUMBER
            // =====================================================

            String clinicContactNumber = "";

            try {
                ResponseEntity<ResponseStructure<BranchDTO>> branchRes =
                		adminFeignClient.getBranchById(booking.getBranchId());

                if (branchRes != null
                        && branchRes.getBody() != null
                        && branchRes.getBody().getData() != null) {

                    clinicContactNumber = safe(
                            branchRes.getBody().getData().getContactNumber(), "");
                }

            } catch (Exception e) {
                log.warn("Branch fetch failed branchId={} : {}",
                        booking.getBranchId(), e.getMessage());
            }

            // =====================================================
            // RESOLVE MOBILE
            // =====================================================

            String mobile = resolveMobile(booking);
            String normalizedMobile = normalizeMobile(mobile);

            if (!isValidMobile(normalizedMobile)) {
                log.warn("FollowUp WhatsApp skipped — invalid mobile bookingId={}",
                        record.getBookingId());
                return;
            }

            // =====================================================
            // BUILD VARIABLES AND SEND
            // =====================================================

            String variables = buildVariables(
                    record, booking, nextVisitDate, clinicContactNumber);

            sendWhatsApp(normalizedMobile, variables, record.getBookingId());

        } catch (Exception ex) {
            log.error("FollowUp WhatsApp failure bookingId={} error={}",
                    record.getBookingId(), ex.getMessage(), ex);
        }
    }

    // =====================================================
    // WHATSAPP CALL
    // =====================================================

    private void sendWhatsApp(String mobile, String variables, String bookingId) {

        String response = webClient.get()
                .uri(uri -> uri
                        .path("/dev/whatsapp")
                        .queryParam("authorization", authKey)
                        .queryParam("message_id", followUpTemplateId)
                        .queryParam("phone_number_id", phoneNumberId)
                        .queryParam("numbers", mobile)
                        .queryParam("variables_values", variables)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .retry(1)
                .block();

        log.info("FollowUp WhatsApp sent bookingId={} response={}", bookingId, response);
    }

    // =====================================================
    // BOOKING FETCH
    // =====================================================

    private BookingResponse fetchBooking(String bookingId) {

        try {
            ResponseEntity<ResponseStructure<BookingResponse>> response =
                    bookingFeignClient.getBookedService(bookingId);

            if (response == null
                    || response.getBody() == null
                    || response.getBody().getData() == null) {
                return null;
            }

            return mapper.convertValue(
                    response.getBody().getData(),
                    BookingResponse.class
            );

        } catch (Exception ex) {
            log.error("Booking fetch failed bookingId={} error={}",
                    bookingId, ex.getMessage(), ex);
            return null;
        }
    }

    // =====================================================
    // VARIABLE BUILDER — matches template exactly
    // =====================================================

    private String buildVariables(PhysiotherapyRecord record,
                                   BookingResponse booking,
                                   String nextVisitDate,
                                   String clinicContactNumber) {
        return String.join("|",
                safe(booking.getName(), "Patient"),        // {{1}} patient name
                safe(booking.getClinicName(), "Clinic"),   // {{2}} clinic name
                formatDate(nextVisitDate),                 // {{3}} date
                safe(record.getBookingId(), ""),           // {{4}} booking id
                safe(booking.getDoctorName(), "Doctor"),   // {{5}} doctor name
                safe(clinicContactNumber, "")              // {{6}} clinic number
        );
    }

    // =====================================================
    // MOBILE HANDLING
    // =====================================================

    private String resolveMobile(BookingResponse booking) {

        if (booking.getPatientMobileNumber() != null
                && !booking.getPatientMobileNumber().isBlank()) {
            return booking.getPatientMobileNumber();
        }
        return booking.getMobileNumber();
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null) return null;
        String digits = mobile.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        }
        return digits;
    }

    private boolean isValidMobile(String mobile) {
        return mobile != null && mobile.matches("^[6-9]\\d{9}$");
    }

    // =====================================================
    // UTILS
    // =====================================================

    private String formatDate(String date) {
        try {
            if (date == null) return "-";
            return LocalDate.parse(date)
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception e) {
            return date;
        }
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}