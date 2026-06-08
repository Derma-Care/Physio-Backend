package physiotherapydoctor.controller;

import java.util.List;

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
import physiotherapydoctor.dto.response.PaymentRecordResponse;
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
			PaymentRecordResponse result = service.createPayment(req);

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
			PaymentRecordResponse result = service.updatePayment(req);

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
			PaymentRecordResponse result = service.getByBookingId(bookingId);

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

	// ================= UPDATE SESSION FROM THERAPIST =================
	@PutMapping("/updateSessionFromTherapist/{therapistRecordId}/{sessionId}")
	public ResponseEntity<Response> updateSessionStatus(@PathVariable String therapistRecordId,
			@PathVariable String sessionId) {

		Response response = new Response();

		try {
			service.updateSessionStatusFromTherapist(therapistRecordId, sessionId);

			response.setSuccess(true);
			response.setData(null);
			response.setMessage("Session updated successfully");
			response.setStatus(200);

		} catch (Exception e) {

			response.setSuccess(false);
			response.setData(null);
			response.setMessage(e.getMessage());
			response.setStatus(400);
		}

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/payment/getExerciseSessionsWithRecords/{clinicId}/{branchId}/{bookingId}/{patientId}/{therapistId}/{therapistRecordId}")
	public ResponseEntity<Response> getExerciseSessionsWithRecords(@PathVariable String clinicId,
			@PathVariable String branchId, @PathVariable String bookingId, @PathVariable String patientId,
			@PathVariable String therapistId,@PathVariable String therapistRecordId) {

		try {
			Response response = service.getExerciseSessionsWithRecords(clinicId, branchId, bookingId, patientId,
					therapistId,therapistRecordId);

			return ResponseEntity.status(response.getStatus()).body(response);

		} catch (Exception e) {

			Response response = new Response();
			response.setSuccess(false);
			response.setData(null);
			response.setMessage(e.getMessage());
			response.setStatus(400);

			return ResponseEntity.status(response.getStatus()).body(response);
		}
	}

	@GetMapping("/getPayments/{clinicId}/{branchId}")
	public ResponseEntity<Response> getPayments(@PathVariable String clinicId, @PathVariable String branchId) {

		Response response = new Response();

		try {

			List<PaymentRecordResponse> records = service.findByClinicIdAndBranchId(clinicId, branchId);

			response.setSuccess(true);
			response.setStatus(200);
			response.setMessage("Payments fetched successfully");
			response.setData(records);

		} catch (Exception e) {

			response.setSuccess(false);
			response.setStatus(400);
			response.setMessage(e.getMessage());
			response.setData(null);
		}

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/getCompletedTherapyRecord/{clinicId}/{branchId}/{therapistRecordId}/{sessionId}")
	public ResponseEntity<Response> getCompletedTherapyRecord(@PathVariable String clinicId,
			@PathVariable String branchId, @PathVariable String therapistRecordId, @PathVariable String sessionId) {

		Response response = service.getCompletedTherapyRecord(clinicId, branchId, therapistRecordId, sessionId);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

}