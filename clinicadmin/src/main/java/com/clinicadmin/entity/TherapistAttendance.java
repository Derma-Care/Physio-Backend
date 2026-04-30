package com.clinicadmin.entity;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

@Document(collection = "therapist_attendance")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TherapistAttendance {

    @Id
    private String id;

    private String therapistId;  
    private String clinicId;
    private String branchId;

    private String date;        

    private String loginTime;   
    private String logoutTime;  

    private String logTime;     
    private String workingHours;
    private String idleTime;

    // ✅ ADD THIS
    private List<Session> sessions;
}