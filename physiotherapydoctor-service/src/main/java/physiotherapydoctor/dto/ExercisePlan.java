package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class ExercisePlan {

    private String homeAdvice;
    private List<HomeExercise> homeExercises;
}
