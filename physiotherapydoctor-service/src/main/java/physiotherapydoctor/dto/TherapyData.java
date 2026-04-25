package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class TherapyData {

    private String therapyId;
    private String therapyName;
    private Double totalTherapyPrice;  // ✅ RENAME
    private String paymentStatus;      // ✅ ADD

    private List<TherapyExercise> exercises;
}