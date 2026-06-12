package physiotherapydoctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemplateSummaryDTO {
	private String templateRecordId;
	private String physioDiagnosis;
}
