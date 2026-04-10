package com.pharmacyManagement.service;

import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.dto.ReturnBillDTO;

public interface ReturnBillService {

    Response createReturnBill(ReturnBillDTO dto);

    Response getByreceiptNo(String receiptNo);

    Response getByClinicIdAndBranchId(String clinicId, String branchId);

    Response deleteByreceiptNo(String receiptNo);

	Response getByClinicBranchAndReceiptNo(String clinicId, String branchId, String receiptNo);

	Response updateByReceiptNo(String receiptNo, ReturnBillDTO dto);
}