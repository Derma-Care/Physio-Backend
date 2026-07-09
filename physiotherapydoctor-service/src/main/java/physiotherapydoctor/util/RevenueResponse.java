package physiotherapydoctor.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevenueResponse {

	private Double grandTotal;
	private boolean success;
	private Object data;
	private String message;
	private int status;
}
