package com.clinicadmin.entity;

import java.util.List;

import org.springframework.data.annotation.Id;

import com.clinicadmin.dto.ServiceInfo;

import lombok.Data;

@Data
public class FeedbackDetails {

    @Id
    private String id;

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

    // ================= STAFF =================
    private String staffId;
    private String staffName;

    // ================= SERVICE =================
    private String serviceType;
    private List<ServiceInfo> service;

    // ================= SESSION =================
    private int totalNoOfSessions;
    private int noOfSessionsCompleted;

    // ================= STATUS =================
    private boolean isHalfSessionsCompleted;
    private boolean isFullSessionsCompleted;

    // ================= FEEDBACK =================
    private String whatWentWell;
    private String improvements;
}