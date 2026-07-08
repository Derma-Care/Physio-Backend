package com.clinicadmin.service;

import com.clinicadmin.dto.Response;

public interface AnalyticsService {

    Response getDoctorReferralAnalytics(
            String clinicId,
            String branchId,
            Integer type,
            String startDate,
            String endDate);
}