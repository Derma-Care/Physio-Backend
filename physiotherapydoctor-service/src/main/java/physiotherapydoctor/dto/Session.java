package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class Session {

    private String date;
    private String status; // Pending / Completed
    private String sessionId;
}
