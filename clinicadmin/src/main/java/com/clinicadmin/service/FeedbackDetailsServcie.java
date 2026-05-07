package com.clinicadmin.service;

import com.clinicadmin.dto.Response;

public interface FeedbackDetailsServcie {
	
	Response getFeedbackDetails(
	        String clinicId,
	        String branchId);

}
