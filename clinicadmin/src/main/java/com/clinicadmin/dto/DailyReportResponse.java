package com.clinicadmin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyReportResponse {

    private String date;
    private String loginTime;
    private String logoutTime;
    private String logTime;
    private List<SessionData> sessions;
}
