package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class HomeExercise {
    private String id;
    private String name;
    private String sets;
    private String reps;
    private String duration;
    private String frequency;
    private String instructions;
    private String videoUrl;
    private String thumbnail;
}
