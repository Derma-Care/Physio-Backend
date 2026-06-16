package physiotherapydoctor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class RecoverySupportDTO {
    private String id;
    private String clinicId;
    private String name;
    private String description;
    private String category;
}
