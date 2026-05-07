package physiotherapydoctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Psychosocial {

    private String stressLevel;
    private Boolean workSatisfaction;
    private Boolean fearOfMovement;
}