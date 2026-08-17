package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.IncidentDTO;
import com.chiselon.clinicadmin.dto.Response;

public interface IncidentService {
 Response createIncident(IncidentDTO dto);
 Response getAllIncidents();
 Response UpdateIncidentStatu(String id, String status);
 Response deleteIncident(String id);
}
