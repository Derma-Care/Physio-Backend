package com.pharmacyManagement.service;

import com.pharmacyManagement.dto.Response;

public interface ReportService {

    Response getExpiredMedicineReport(String clinicId, String branchId);

}