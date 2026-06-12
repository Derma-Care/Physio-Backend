package physiotherapydoctor.service;

import org.springframework.http.ResponseEntity;

import physiotherapydoctor.dto.PhysiotherapyRecordTemplateDTO;
import physiotherapydoctor.dto.Response;

public interface PhysiotherapyRecordTemplateService {

	Response create(PhysiotherapyRecordTemplateDTO dto);

	Response getById(String id);

	Response getAll();

	Response update(String id, PhysiotherapyRecordTemplateDTO dto);

	Response delete(String id);

	Response getByMultipleFields(String clinicId, String branchId, String bookingId, String templateRecordId);

	Response getByWithoutTherapistRecordId(String clinicId, String branchId, String bookingId);

	ResponseEntity<Response> getCalculations(String clinicId, String branchId, String bookingId);

	Response getByClinicBranchAndBooking(String clinicId, String branchId, String bookingId);

	Response getTemplatesByClinicId(String clinicId);

	Response getTemplateByClinicIdAndTemplateId(String clinicId, String templateRecordId);
	
}
