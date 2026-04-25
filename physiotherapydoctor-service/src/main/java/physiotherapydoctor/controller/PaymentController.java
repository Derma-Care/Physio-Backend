package physiotherapydoctor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import physiotherapydoctor.dto.PaymentRequest;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.entity.PaymentRecord;
import physiotherapydoctor.service.PaymentService;

@RestController
@RequestMapping("/physiotherapy-doctor")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    // ================= CREATE =================
    @PostMapping("/payment/create")
    public ResponseEntity<Response> create(@RequestBody PaymentRequest req) {

        Response response = new Response();

        try {
            PaymentRecord result = service.createPayment(req);

            response.setSuccess(true);
            response.setData(result);
            response.setMessage("Payment created successfully");
            response.setStatus(200);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(e.getMessage());
            response.setStatus(400);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ================= UPDATE =================
    @PostMapping("/payment/update")
    public ResponseEntity<Response> update(@RequestBody PaymentRequest req) {

        Response response = new Response();

        try {
            PaymentRecord result = service.updatePayment(req);

            response.setSuccess(true);
            response.setData(result);
            response.setMessage("Payment updated successfully");
            response.setStatus(200);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(e.getMessage());
            response.setStatus(400);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ================= GET =================
    @GetMapping("/payment/{bookingId}")
    public ResponseEntity<Response> get(@PathVariable String bookingId) {

        Response response = new Response();

        try {
            PaymentRecord result = service.getByBookingId(bookingId);

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

    // ================= DELETE =================
    @DeleteMapping("/payment/{bookingId}")
    public ResponseEntity<Response> delete(@PathVariable String bookingId) {

        Response response = new Response();

        try {
            service.deleteByBookingId(bookingId);

            response.setSuccess(true);
            response.setData(null);
            response.setMessage("Payment deleted successfully");
            response.setStatus(200);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(e.getMessage());
            response.setStatus(404);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @PutMapping("/updateSessionFromTherapist/{therapistRecordId}/{sessionId}")
    public ResponseEntity<String> updateSessionStatus(
            @PathVariable String therapistRecordId,
            @PathVariable String sessionId) {

        service.updateSessionStatusFromTherapist(therapistRecordId, sessionId);
        return ResponseEntity.ok("Updated Successfully");
    }
}