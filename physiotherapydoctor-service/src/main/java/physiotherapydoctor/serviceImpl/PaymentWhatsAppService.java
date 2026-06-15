package physiotherapydoctor.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.entity.PaymentRecord;
import physiotherapydoctor.feign.BookingFeignClient;

@Service
@Slf4j
public class PaymentWhatsAppService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://www.fast2sms.com")
            .build();

    // =====================================================
    // ✅ FIX: ObjectMapper with JavaTimeModule registered
    // so LocalDate/LocalDateTime fields (e.g. date_TIME in
    // ConsultationFeesDTO) don't throw InvalidDefinitionException
    // =====================================================
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private BookingFeignClient bookingFeignClient;

    @Value("${whatsapp.auth-key}")
    private String authKey;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.message-id}")
    private String messageId;

    // =====================================================
    // TEMPLATE : payment_confirmation
    //
    // Hi {{1}} 👋, your payment at *{{2}}* has been
    // recorded successfully. ✅
    //
    // 🗒️ Booking {{3}} · {{4}}
    //
    // 💰 Final amount    Rs. {{5}}
    // ✅ Amount paid     Rs. {{6}}
    // ⏳ Balance due     Rs. {{7}}
    //
    // 📋 Payment status: *{{8}}*
    //
    // For any queries please contact your clinic directly.
    // 🙏 Wishing you a speedy recovery!
    // =====================================================

    public void sendPaymentConfirmation(PaymentRecord record) {

        try {

            // =====================================================
            // STEP 1 — FETCH BOOKING DATA VIA FEIGN
            // =====================================================

            BookingResponse booking = fetchBookingData(record.getBookingId());

            if (booking == null) {
                log.warn("WhatsApp skipped — booking data not found for bookingId={}",
                        record.getBookingId());
                return;
            }

            // =====================================================
            // STEP 2 — EXTRACT FIELDS FROM BOOKING RESPONSE
            // name           → patientName   → {{1}}
            // clinicName     → clinicName    → {{2}}
            // patientMobileNumber → mobile (fallback to mobileNumber)
            // =====================================================

            String patientName = safe(booking.getName(), "Patient");

            String clinicName  = safe(booking.getClinicName(), "Clinic");

            // patientMobileNumber first, fallback to mobileNumber
            String rawMobile = (booking.getPatientMobileNumber() != null
                    && !booking.getPatientMobileNumber().isBlank())
                    ? booking.getPatientMobileNumber()
                    : booking.getMobileNumber();

            // =====================================================
            // STEP 3 — VALIDATE & NORMALIZE MOBILE
            // =====================================================

            if (rawMobile == null || rawMobile.isBlank()) {
                log.warn("WhatsApp skipped — no mobile number for bookingId={}",
                        record.getBookingId());
                return;
            }

            String mobile = rawMobile.replaceAll("[^0-9]", "");

            if (mobile.startsWith("91") && mobile.length() == 12) {
                mobile = mobile.substring(2);
            }

            if (mobile.length() != 10) {
                log.warn("WhatsApp skipped — invalid mobile length={} bookingId={}",
                        mobile.length(), record.getBookingId());
                return;
            }

            final String cleanMobile = mobile;

            // =====================================================
            // STEP 4 — BUILD VARIABLES STRING
            // =====================================================

            String variables = String.join("|",
                    patientName,                                    // {{1}}
                    clinicName,                                     // {{2}}
                    safe(record.getBookingId(),     ""),            // {{3}}
                    safe(record.getTreatmentName(), "Treatment"),   // {{4}}
                    fmt(record.getFinalAmount()),                   // {{5}}
                    fmt(record.getTotalPaid()),                     // {{6}}
                    fmt(record.getBalanceAmount()),                 // {{7}}
                    safe(record.getPaymentStatus(), "Pending")      // {{8}}
            );

            log.info("Sending payment WhatsApp | bookingId={} | mobile={} | patient={} | clinic={}",
                    record.getBookingId(), cleanMobile, patientName, clinicName);

            log.debug("WhatsApp variables={}", variables);

            // =====================================================
            // STEP 5 — CALL FAST2SMS API
            // =====================================================

            String response = webClient.get()
                    .uri(ub -> ub
                            .path("/dev/whatsapp")
                            .queryParam("authorization",    authKey)
                            .queryParam("message_id",       messageId)
                            .queryParam("phone_number_id",  phoneNumberId)
                            .queryParam("numbers",          cleanMobile)
                            .queryParam("variables_values", variables)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("WhatsApp sent successfully | bookingId={} | response={}",
                    record.getBookingId(), response);

        } catch (Exception e) {

            // ✅ NEVER fail payment due to WhatsApp error
            log.error("WhatsApp failed | bookingId={} | error={}",
                    record.getBookingId(), e.getMessage(), e);
        }
    }

    // =====================================================
    // FETCH BOOKING DATA FROM BOOKING SERVICE
    // =====================================================

    private BookingResponse fetchBookingData(String bookingId) {

        try {

            ResponseEntity<ResponseStructure<BookingResponse>> responseEntity =
                    bookingFeignClient.getBookedService(bookingId);

            if (responseEntity == null
                    || responseEntity.getBody() == null
                    || responseEntity.getBody().getData() == null) {

                log.warn("Booking data empty for bookingId={}", bookingId);
                return null;
            }

            Object data = responseEntity.getBody().getData();

            // Response.data is Object — convert using ObjectMapper
            // (mapper now has JavaTimeModule registered — fixes
            // LocalDateTime serialization failure on date_TIME field)
            BookingResponse booking = mapper.convertValue(data, BookingResponse.class);

            log.info("Booking fetched | bookingId={} | patient={} | mobile={} | clinic={}",
                    bookingId,
                    booking.getName(),
                    booking.getPatientMobileNumber(),
                    booking.getClinicName());

            return booking;

        } catch (Exception e) {
            log.error("Failed to fetch booking | bookingId={} | error={}",
                    bookingId, e.getMessage(), e);
            return null;
        }
    }

    // =====================================================
    // UTILS
    // =====================================================

    private String fmt(double amount) {
        return String.format("%.2f", amount);
    }

    private String safe(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value.trim() : fallback;
    }
}