package physiotherapydoctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RadiationNeuro {

    private Boolean radiating;
    private Boolean numbness;
    private Boolean weakness;
    private Boolean gripDifficulty;
}