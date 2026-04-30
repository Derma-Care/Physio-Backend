package physiotherapydoctor.dto;

import java.util.List;
import lombok.Data;

@Data
public class TherapyWithSessions {

    // ================= PACKAGE LEVEL =================
    private String packageId;
    private String packageName;
    private Double totalPackagePrice;
    private String paymentStatus;
    private List<Program> programs;

    // ================= PROGRAM LEVEL =================
    private String programId;
    private String programName;
    private Double totalProgramPrice;
    private List<TherapyData> therapyData;

    // ================= THERAPY LEVEL =================
    private String therapyId;
    private String therapyName;
    private Double totalTherapyPrice;

    // ================= EXERCISE LEVEL =================
    private List<TherapyExercise> exercises;

    // ================= COMMON =================
    private Double totalPrice;
}