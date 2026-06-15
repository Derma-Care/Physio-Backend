package physiotherapydoctor.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.entity.PaymentRecord;
import physiotherapydoctor.feign.BookingFeignClient;

@Service
@Slf4j
public class PaymentWhatsAppService {

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final BookingFeignClient bookingFeignClient;

    @Value("${whatsapp.auth-key}")
    private String authKey;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.payment-confirmation-template-id}")
    private String paymentConfirmationTemplateId;

    @Value("${whatsapp.base-url}")
    private String baseUrl;

    public PaymentWhatsAppService(
            WebClient.Builder webClientBuilder,
            ObjectMapper mapper,
            BookingFeignClient bookingFeignClient,
            @Value("${whatsapp.base-url}") String baseUrl
    ) {
        this.mapper = mapper;
        this.bookingFeignClient = bookingFeignClient;

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    // =====================================================
    // PUBLIC API
    // =====================================================

    public void sendPaymentConfirmation(PaymentRecord record) {

        if (record == null || record.getBookingId() == null) {
            log.warn("WhatsApp skipped — invalid payment record");
            return;
        }

        try {
            BookingResponse booking = fetchBooking(record.getBookingId());

            if (booking == null) {
                log.warn("WhatsApp skipped — booking not found: {}", record.getBookingId());
                return;
            }

            String mobile = resolveMobile(booking);
            String normalizedMobile = normalizeMobile(mobile);

            if (!isValidMobile(normalizedMobile)) {
                log.warn("WhatsApp skipped — invalid mobile bookingId={}", record.getBookingId());
                return;
            }

            String variables = buildVariables(record, booking);

            sendWhatsApp(normalizedMobile, variables, record.getBookingId());

        } catch (Exception ex) {
            log.error("WhatsApp failure bookingId={} error={}",
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
                        .queryParam("message_id", paymentConfirmationTemplateId)
                        .queryParam("phone_number_id", phoneNumberId)
                        .queryParam("numbers", mobile)
                        .queryParam("variables_values", variables)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .retry(1)
                .block();

        log.info("WhatsApp sent bookingId={} response={}", bookingId, response);
    }

    // =====================================================
    // BOOKING FETCH
    // =====================================================

    private BookingResponse fetchBooking(String bookingId) {

        try {
            ResponseEntity<ResponseStructure<BookingResponse>> response =
                    bookingFeignClient.getBookedService(bookingId);

            if (response == null ||
                response.getBody() == null ||
                response.getBody().getData() == null) {
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
    // VARIABLE BUILDER
    // =====================================================

    private String buildVariables(PaymentRecord record, BookingResponse booking) {

        LastPayment last = getLastPayment(record);

        return String.join("|",
                safe(booking.getName(), "Patient"),
                safe(booking.getClinicName(), "Clinic"),
                safe(record.getBookingId(), ""),
                safe(record.getTreatmentName(), "Treatment"),
                fmt(record.getFinalAmount()),
                fmt(record.getTotalPaid()),
                fmt(record.getBalanceAmount()),
                fmt(last != null ? last.amount : 0.0),
                formatDate(last != null ? last.date : null),
                safe(last != null ? last.mode : "NA", "NA"),
                safe(record.getPaymentStatus(), "Pending")
        );
    }

    // =====================================================
    // LAST PAYMENT
    // =====================================================

    private LastPayment getLastPayment(PaymentRecord record) {

        if (record.getPaymentHistory() == null || record.getPaymentHistory().isEmpty()) {
            return null;
        }

        var history = record.getPaymentHistory();
        var last = history.get(history.size() - 1);

        LastPayment lp = new LastPayment();
        lp.amount = last.getAmount();
        lp.date = last.getPaymentDate();
        lp.mode = last.getPaymentMode();

        return lp;
    }

    private static class LastPayment {
        Double amount;
        String date;
        String mode;
    }

    // =====================================================
    // MOBILE HANDLING
    // =====================================================

    private String resolveMobile(BookingResponse booking) {

        if (booking.getPatientMobileNumber() != null &&
            !booking.getPatientMobileNumber().isBlank()) {
            return booking.getPatientMobileNumber();
        }

        return booking.getMobileNumber();
    }

    private String normalizeMobile(String mobile) {
        return mobile == null ? null : mobile.replaceAll("\\D", "");
    }

    private boolean isValidMobile(String mobile) {
        return mobile != null && mobile.matches("^[6-9]\\d{9}$");
    }

    // =====================================================
    // UTIL METHODS
    // =====================================================

    private String fmt(Double amount) {

        double value = (amount == null) ? 0.0 : amount;

        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toString();
    }

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
        return (value == null || value.isBlank())
                ? fallback
                : value.trim();
    }
}