package com.clinicadmin.service;

import com.clinicadmin.dto.FeedbackDetailsDTO;
import com.clinicadmin.dto.Response;

public interface FeedbackDetailsServcie {
	Response createFeedback(FeedbackDetailsDTO feedbackDetailsDTO);

	Response getFeedbackDetails(String clinicId,String branchId);
	
	Response getAllFeedbacks();

	Response getFeedbackById(String id);

	Response updateFeedback(String id,FeedbackDetailsDTO feedbackDetailsDTO);

	Response deleteFeedback(String id);


}
