package physiotherapydoctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedFlags {

    private Boolean trauma;
    private Boolean weightLoss;
    private Boolean fever;
    private Boolean cancer;
    private Boolean nightPain;
    private Boolean swallowing;
}