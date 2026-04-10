package com.pharmacyManagement.service;

import org.springframework.http.ResponseEntity;
import com.pharmacyManagement.dto.OpSalesRequest;
import com.pharmacyManagement.dto.Response;

public interface Opservice {
	
	// 1. CREATE
	  ResponseEntity<Response> createOpSales(OpSalesRequest request);
		   
    // 2. UPDATE
	 public ResponseEntity<Response> updateOpSales(OpSalesRequest request);
    // 3. GET ALL
    ResponseEntity<Response> getAllOpSales(String clinicId, String branchId);

    // 4a. GET BY BILL NO
    ResponseEntity<Response> getByBillNo(String billNo);

    // 4b. GET BY ID
    ResponseEntity<Response> getById(String id);

    // 5. GET BY OPNO
    public ResponseEntity<Response> getByOpNo(String clinicId, String branchId, String opNo,String mobileNumber);
        
    // 6. DELETE
    ResponseEntity<Response> deleteOpSales(String clinicId, String branchId, String id);

    // 7. FILTER
    ResponseEntity<Response> filterOpSales(
            String clinicId,
            String branchId,
            String billNo,
            String patientName,
            String mobile,
            String consultingDoctor,
            String fromDate,
            String toDate
    );
}
