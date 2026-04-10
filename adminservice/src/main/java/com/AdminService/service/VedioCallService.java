package com.AdminService.service;

import com.AdminService.dto.VedioCallDTO;
import com.AdminService.util.Response;

public interface VedioCallService {
Response addVedioCallCredential(VedioCallDTO dto);
Response getCredentials();
}