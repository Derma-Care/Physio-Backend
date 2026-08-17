package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.FeedbackDetailsDTO;
import com.chiselon.clinicadmin.dto.Response;

public interface FeedbackDetailsServcie {
	Response createFeedback(FeedbackDetailsDTO feedbackDetailsDTO);

	Response getFeedbackDetails(String clinicId,String branchId);
	
	Response getAllFeedbacks();

	Response getFeedbackById(String id);

	Response updateFeedback(String id,FeedbackDetailsDTO feedbackDetailsDTO);
	
	Response getAllFeedbacksByClinicIdAndBranchId(String clinicId, String branchId);


	Response deleteFeedback(String id);

	void processFeedbackNotification(String clinicId, String branchId);

//	Response getDoctorFeedbackSummary(String clinicId, String doctorId);


}
