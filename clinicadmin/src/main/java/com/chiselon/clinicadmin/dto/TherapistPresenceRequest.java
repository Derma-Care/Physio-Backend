package com.chiselon.clinicadmin.dto;

import lombok.Data;

@Data
public class TherapistPresenceRequest {

    private String clinicId;
    private String branchId;
    private Boolean isPresent;
}