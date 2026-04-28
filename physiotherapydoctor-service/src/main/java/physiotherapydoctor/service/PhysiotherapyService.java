package physiotherapydoctor.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import physiotherapydoctor.dto.PhysiotherapyRecordDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.Session;
import physiotherapydoctor.entity.PhysiotherapyRecord;

public interface PhysiotherapyService {

    // CREATE
    Response create(PhysiotherapyRecordDTO dto);

    // GET BY ID
    Response getById(String id);

    // GET ALL
    Response getAll();

    // UPDATE
    Response update(String id, PhysiotherapyRecordDTO dto);

    // DELETE
    Response delete(String id);

	Response getByMultipleFields(String clinicId, String branchId, String patientId, String bookingId,
			String therapistRecordId);

	Response getByWithoutTherapistRecordId(String clinicId, String branchId, String patientId, String bookingId);


	public Response getProgramAndTherapyInfo(String clinicId, String branchId,
            String patientId, String bookingId);
//	Response getAssignedPatients(String clinicId, String branchId, String therapistId);

	Response getAssignedPatients(String clinicId, String branchId, String therapistId, Integer overallStatus);
	public ResponseEntity<Response> getCalculations(String clinicId, String branchId, String patientId, String bookingId);

	public ResponseEntity< List<Session>> getSessionsByBookingIdAndDate(String bookingId, String date);

	Response getByClinicBranchAndBooking(String clinicId, String branchId, String bookingId);

	ResponseEntity<?> getInProgressBookingsByIds(String patientId, String bookingId);

	ResponseEntity<?> getTodaysAppointments(String clinicId, String doctorId);



	Response getVisitHistory(String patientId, String bookingId);

	Response getPatientHistory(String patientId);



//	Response getTherapistDashboard(String clinicId, String branchId, String therapistId);

//	void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId);
}