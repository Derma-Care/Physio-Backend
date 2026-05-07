package com.clinicadmin.dto;



import java.util.List;

import lombok.Data;

@Data
public class FeedbackDetailsDTO {

    // ================= PATIENT =================
    private String patientId;
    private String patientName;
    private String mobileNumber;

    // ================= BOOKING =================
    private String bookingId;

    // ================= DOCTOR =================
    private String doctorId;
    private String doctorName;

    // ================= THERAPIST =================
    private String therapistId;
    private String therapistName;
    private String therapistRecordId;

    // ================= SERVICE =================
    private String serviceType;
    private List<ServiceInfo> service;

    // ================= SESSION =================
    private int totalNoOfSessions;
    private int noOfSessionsCompleted;

    // ================= STATUS =================
    private boolean isHalfSessionsCompleted;
    private boolean isFullSessionsCompleted;
}
