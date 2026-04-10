package com.dermacare.bookingService.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentDetailsDTO {
	 private List<DatesDTO> dates;
	    private String reason;
	    private String frequency;
	    private Integer sittings;
	    private String startDate;
	    private Integer totalSittings;
	    private Integer takenSittings;
	    private Integer pendingSittings;
	    private Integer currentSitting;
	    private String status; // e.g., "In-Progress", "Completed"
}
