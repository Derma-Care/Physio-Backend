package com.clinicadmin.service;

import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.FeedbackDetails;

public interface FeedbackDetailsServcie {
	  Response createFeedback(FeedbackDetails feedbackDetails);
	
	Response getFeedbackDetails(String clinicId,String branchId);

}
