package com.pharmacyManagement.service;

import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.dto.SupplierDTO;
import com.pharmacyManagement.dto.SupplierLoginRequest;

public interface SupplierService {

	Response addSupplier(SupplierDTO dto);

	Response updateSupplier(String supplierId, SupplierDTO dto);

	Response getSupplierById(String supplierId);

	Response getAllSuppliers();

	Response deleteSupplier(String supplierId);

	Response supplierLogin(SupplierLoginRequest request);

	Response getSuppliersByClinicAndBranch(String clinicId, String branchId);
}
