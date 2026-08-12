package physiotherapydoctor.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;

import physiotherapydoctor.dto.AssignTherapistRequest;
import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import physiotherapydoctor.dto.DoctorLoginDTO;
import physiotherapydoctor.dto.ExercisePlan;
import physiotherapydoctor.dto.PhysiotherapyRecordDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.Session;
import physiotherapydoctor.dto.SessionForBooking;
import physiotherapydoctor.dto.TherapySession;
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

	public ResponseEntity<List<SessionForBooking>> getSessionsByBookingIdAndDate(String bookingId,String startdate,String endDate);

	Response getByClinicBranchAndBooking(String clinicId, String branchId, String bookingId);

	ResponseEntity<?> getInProgressBookingsByIds(String patientId, String bookingId);

	ResponseEntity<?> getTodaysAppointments(String clinicId, String doctorId);



	Response getVisitHistory(String patientId, String bookingId);

	Response getPatientHistory(String patientId);
	public List<String> getTodayFollowUpBookingIds();
	
	 public Response getFirstVisitHistory(String doctorId,
             String patientId,
             String bookingId,
             String clinicId,
             String branchId);
	 
	  public Response getVisitHistoryByDoctor(String doctorId,
              String patientId,
              String bookingId);


	Response updateDoctorAvailability(String doctorId, DoctorAvailabilityStatusDTO availabilityDTO);

	Response changePassword(String username, ChangeDoctorPasswordDTO updateDTO);

	Response login(DoctorLoginDTO loginDTO);
	
	 public  ResponseEntity<?> getDoctorAppointmentsonStatus(String clinicId,String branchId,
		  		String doctorId,String status);
	 
	 public String getByBookingId(String id);

	 public Response getInvestigations(String bookingId, String patientId);


	public Map<String, List<Session>> getPaymentSessionsDetails(
			String clinicId,
			String branchId,
			String initialDay,
			String finalDay,
			List<String> bookingSet);


	Response updateHomeExercisePlanByRecordId(String therapistRecordId, ExercisePlan exercisePlanDto, boolean append);


//	Response getTherapistDashboard(String clinicId, String branchId, String therapistId);

//	void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId);
}