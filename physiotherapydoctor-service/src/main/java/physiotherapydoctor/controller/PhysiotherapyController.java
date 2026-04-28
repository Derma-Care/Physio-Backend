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
import physiotherapydoctor.dto.PhysiotherapyRecordDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.Session;
import physiotherapydoctor.service.PhysiotherapyService;

@RestController
@RequestMapping("/physiotherapy-doctor")
@RequiredArgsConstructor
public class PhysiotherapyController {

	private final PhysiotherapyService service;

	// ✅ CREATE
	@PostMapping("/physiotherapy-record/create")
	public ResponseEntity<Response> create(@RequestBody PhysiotherapyRecordDTO dto) {

		Response response = service.create(dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ GET BY ID
	@GetMapping("/physiotherapy-recordgetById/{id}")
	public ResponseEntity<Response> getById(@PathVariable String id) {

		Response response = service.getById(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ GET ALL
	@GetMapping("/physiotherapy-record/getAll")
	public ResponseEntity<Response> getAll() {

		Response response = service.getAll();
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/getTherapySessionsByServiceType/{clinicId}/{branchId}/{patientId}/{bookingId}")
	public ResponseEntity<Response> getTherapySessionsByServiceType(@PathVariable String clinicId,
			@PathVariable String branchId, @PathVariable String patientId, @PathVariable String bookingId) {

		return service.getCalculations(clinicId, branchId, patientId, bookingId);
	}

	// ✅ UPDATE
	@PutMapping("/physiotherapy-record/updateById/{id}")
	public ResponseEntity<Response> update(@PathVariable String id, @RequestBody PhysiotherapyRecordDTO dto) {

		Response response = service.update(id, dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ DELETE
	@DeleteMapping("/physiotherapy-record/deleteById/{id}")
	public ResponseEntity<Response> delete(@PathVariable String id) {

		Response response = service.delete(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
	
	@GetMapping("/getPhysioByBookingId/{bookingId}/{date}")
	public ResponseEntity<List<Session>> getPhysioByBookingId(@PathVariable String bookingId,@PathVariable String date) {		
		return service.getSessionsByBookingIdAndDate(bookingId, date);
	}
	
	@GetMapping("/get-record/{clinicId}/{branchId}/{patientId}/{bookingId}/{therapistRecordId}")
	public ResponseEntity<Response> getRecord(@PathVariable String clinicId, @PathVariable String branchId,
			@PathVariable String patientId, @PathVariable String bookingId, @PathVariable String therapistRecordId) {

		Response response = service.getByMultipleFields(clinicId, branchId, patientId, bookingId, therapistRecordId);
		return ResponseEntity.status(response.getStatus()).body(response);

	}

	@GetMapping("/get-record/{clinicId}/{branchId}/{patientId}/{bookingId}")
	public ResponseEntity<Response> getRecordsWithoutTherapistId(@PathVariable String clinicId,
			@PathVariable String branchId, @PathVariable String patientId, @PathVariable String bookingId) {

		Response response = service.getByWithoutTherapistRecordId(clinicId, branchId, patientId, bookingId);
		return ResponseEntity.status(response.getStatus()).body(response);

	}

	// ✅ GET Assigned Patients by clinic + branch + therapist
	@GetMapping("/assigned-patients/{clinicId}/{branchId}/{therapistId}/{overallStatus}")
	public ResponseEntity<Response> getAssignedPatients(@PathVariable String clinicId, @PathVariable String branchId,
			@PathVariable String therapistId, @PathVariable Integer overallStatus) {

		Response response = service.getAssignedPatients(clinicId, branchId, therapistId, overallStatus);
		return ResponseEntity.status(response.getStatus()).body(response);

	}

	@GetMapping("/getProgramAndTherapyInfo/{clinicId}/{branchId}/{patientId}/{bookingId}")
	public ResponseEntity<Response> getProgramAndTherapyInfo(@PathVariable String clinicId,
			@PathVariable String branchId, @PathVariable String patientId, @PathVariable String bookingId) {

		Response response = service.getProgramAndTherapyInfo(clinicId, branchId, patientId, bookingId);
		return ResponseEntity.status(response.getStatus()).body(response);

	}

	@GetMapping("/clinic-branch-booking/{clinicId}/{branchId}/{bookingId}")
	public ResponseEntity<Response> getByClinicBranchAndBooking(@PathVariable String clinicId,
			@PathVariable String branchId, @PathVariable String bookingId) {

		Response response = service.getByClinicBranchAndBooking(clinicId, branchId, bookingId);

		return ResponseEntity.status(response.getStatus()).body(response);
	}
//	@GetMapping("physiotherapy-record/dashboard/{clinicId}/{branchId}/{therapistId}")
//	public ResponseEntity<Response> getDashboard(
//	        @PathVariable String clinicId,
//	        @PathVariable String branchId,
//	        @PathVariable String therapistId) {
//
//	    Response response = service.getTherapistDashboard(clinicId, branchId, therapistId);
//	    return ResponseEntity.status(response.getStatus()).body(response);
//	}
//	
//	@PutMapping("/updateSessionFromTherapist/{therapistRecordId}/{sessionId}")
//	public void updateSessionFromTherapist(
//	        @PathVariable String therapistRecordId,
//	        @PathVariable String sessionId) {
//
//	    service.updateSessionStatusFromTherapist(therapistRecordId, sessionId);
//	}
	
//	-------------------------Booking Api's-----------------------------------------------------
	
	 @GetMapping("/getIn-progressByUsingPatientIdAndBookingId/{patientId}/{bookingId}")
	   public ResponseEntity<?> getInprogressBookingsByPatientId(
				 @PathVariable String patientId, @PathVariable String bookingId){
		   return service.getInProgressBookingsByIds(patientId, bookingId);
		   
	 }
	 @GetMapping("/getTodaysAppointmentsByUsingClinicIdAndDoctorId/{clinicId}/{doctorId}")
	    public ResponseEntity<?> getTodaysAppointments(
	            @PathVariable String clinicId,
	            @PathVariable String doctorId) {
	        return service.getTodaysAppointments(clinicId, doctorId);
	    }
	 @GetMapping("/visitHistoryByUsingPatientIdAndBooking/{patientId}/{bookingId}")
	 public ResponseEntity<Response> getVisitHistory(
	         @PathVariable String patientId,
	         @PathVariable String bookingId) {

	     Response response = service.getVisitHistory(patientId, bookingId);

	     return ResponseEntity
	             .status(response.getStatus())
	             .body(response);
	 }
	 
	 @GetMapping("/patienthistoryByUsingPatientId/{patientId}")
	 public ResponseEntity<Response> getPatientHistory(
	         @PathVariable String patientId) {

	     Response response = service.getPatientHistory(patientId);

	     return ResponseEntity
	             .status(response.getStatus())
	             .body(response);
	 }
}
