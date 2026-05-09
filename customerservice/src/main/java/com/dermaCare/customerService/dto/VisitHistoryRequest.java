package com.dermaCare.customerService.dto;

import lombok.Data;

@Data
public class VisitHistoryRequest {

    private String doctorId;
    private String patientId;
    private String bookingId;

}