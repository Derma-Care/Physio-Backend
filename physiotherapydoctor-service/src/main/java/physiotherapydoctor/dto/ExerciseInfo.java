package physiotherapydoctor.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExerciseInfo {

    private String patientId;
    private String exerciseId;
    private String exerciseName;
    private String frequency;

    private LocalDate sessionStartDate;
    private LocalDate sessionEndDate;
}
