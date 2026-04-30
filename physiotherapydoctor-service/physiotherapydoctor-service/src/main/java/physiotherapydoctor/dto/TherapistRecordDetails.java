
package physiotherapydoctor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)

public class TherapistRecordDetails {

    private String therapistRecordId;

    private String serviceType;

    private String packageId;
    private String packageName;

    private String programId;
    private String programName;

    private String therapyId;
    private String therapyName;

    private String exerciseId;
    private String exerciseName;
}