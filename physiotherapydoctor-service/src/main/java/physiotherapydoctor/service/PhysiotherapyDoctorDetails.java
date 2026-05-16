package physiotherapydoctor.service;

import physiotherapydoctor.dto.Response;

public interface PhysiotherapyDoctorDetails {

	 Response getPhysioDoctorDetails(String clinicId,String branchId);
}
