package physiotherapydoctor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import physiotherapydoctor.dto.PaymentRequest;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.service.PaymentService;

@RestController
@RequestMapping("/physiotherapy-doctor")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    // ✅ CREATE / UPDATE PAYMENT
    @PostMapping("/payment/create")
    public ResponseEntity<Response> create(@RequestBody PaymentRequest request) {

        Response response = new Response();

        try {
            var result = service.createOrUpdatePayment(request);

            response.setSuccess(true);
            response.setData(result);
            response.setMessage("Payment processed successfully");
            response.setStatus(200);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setData(null);
            response.setMessage(e.getMessage());
            response.setStatus(400);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ✅ GET PAYMENT BY BOOKING ID
    @GetMapping("/payment/getByBookingId/{bookingId}")
    public ResponseEntity<Response> getByBookingId(@PathVariable String bookingId) {

        Response response = new Response();

        try {
            var result = service.getByBookingId(bookingId);

            response.setSuccess(true);
            response.setData(result);
            response.setMessage("Payment fetched successfully");
            response.setStatus(200);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setData(null);
            response.setMessage(e.getMessage());
            response.setStatus(404);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }
}