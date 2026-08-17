package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.dto.TherapistCertificateDTO;


public interface TherapistCertificateService {

    Response createCertificate(
            TherapistCertificateDTO dto);

    Response getAllCertificates();

    Response getCertificateById(String id);

    Response getCertificatesByClinicAndBranch(
            String clinicId,
            String branchId);

    Response updateCertificate(
            String id,
            TherapistCertificateDTO dto);

    Response deleteCertificate(String id);

	Response getCertificatesByClinicBranchAndTherapist(String clinicId, String branchId, String therapistId);

}