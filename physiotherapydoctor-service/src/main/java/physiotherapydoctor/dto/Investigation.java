package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class Investigation {

	private List<String> tests;
	private String reason;

}
