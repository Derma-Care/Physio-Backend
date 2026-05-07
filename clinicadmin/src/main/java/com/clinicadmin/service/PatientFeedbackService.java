package com.clinicadmin.service;


import com.clinicadmin.dto.PatientFeedbackDTO;
import com.clinicadmin.dto.Response;

public interface PatientFeedbackService {

    Response createFeedback(PatientFeedbackDTO dto);

    Response getAllFeedbacks();

    Response getFeedbackById(String id);

    Response updateFeedback(String id,
                            PatientFeedbackDTO dto);

    Response deleteFeedback(String id);
}