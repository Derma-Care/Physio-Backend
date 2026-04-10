package com.clinicadmin.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "therapist_records")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TherapistRecord {

    @Id
    private String id;
    private String therapistRecordId;

    private String clinicId;
    private String branchId;
    private String patientId;
    private String bookingId;
    private String therapistId;

    private String patientName;
    private String therapy;

    private String date;
    private String completedDate;
    private String completedTime;

    private String duration;
    private String exercises;

    private String painBefore;
    private String painAfter;

    private String therapistNotes;
    private String patientResponse;

    private String result;
    private String status;
    private String mode;
    private String nextPlan;
    private String sessionId;
    private String beforeImage;
    private String afterImage;
    private String beforeVideo;
    private String afterVideo;

}