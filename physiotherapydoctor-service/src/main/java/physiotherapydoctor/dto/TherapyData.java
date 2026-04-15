package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class TherapyData {

    private String therapyId;
    private String therapyName;
    private Double totalPrice;

    private List<TherapyExercise> exercises;
}