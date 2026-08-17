package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.PrivacyPolicyDTO;
import com.chiselon.clinicadmin.dto.Response;

public interface PrivacyPolicyService {

    Response createPolicy(PrivacyPolicyDTO dto);

  
    Response getAllPolicies();

   
    Response getPolicyById(String id);

    Response updatePolicy(PrivacyPolicyDTO dto);

    Response deletePolicy(String id);


	Response getPoliciesByClinicId(String clinicId);
}