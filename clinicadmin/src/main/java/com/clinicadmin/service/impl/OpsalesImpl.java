package com.clinicadmin.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.clinicadmin.dto.OpSalesRequest;
import com.clinicadmin.dto.Response;
import com.clinicadmin.feignclient.PharmacyManagementFeignClient;
import com.clinicadmin.service.OpService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OpsalesImpl implements OpService {

  @Autowired
  public PharmacyManagementFeignClient pharmacyManagementFeignClient;
  
  // ─────────────────────────────────────────────────────────────
 
  public ResponseEntity<Response> createOpSales(
           OpSalesRequest request) {
	  ResponseEntity<Response> res = null;
     try {
      log.info("CLINIC-ADMIN:REST request to create OP Sales");
      res = pharmacyManagementFeignClient.createOpSales(request);
     }catch(FeignException e){}
     return res;
  }
      
      public ResponseEntity<Response> updateOpSales(OpSalesRequest request) {
    	  ResponseEntity<Response> res = null;
    	  try {
          log.info("CLINIC-ADMIN:REST request to update OP Sales id: {}", request.getBillNo());
          res = pharmacyManagementFeignClient.updateOpSales(request);}
    	  catch(FeignException e) {}
    	  return res;
      }
      
    
      public ResponseEntity<Response> getAllOpSales(
             String clinicId,
             String branchId) {
    	  ResponseEntity<Response> res = null;
    	  try {
          log.info("CLINIC-ADMIN:REST request to fetch all OP Sales");
          res = pharmacyManagementFeignClient.getAllOpSales(clinicId, branchId);
    	  }catch(FeignException e) {}
    	  return res;
      }
      
     
      public ResponseEntity<Response> getByBillNo(
              String billNo) {
    	  ResponseEntity<Response> res = null;
    	  try {
          log.info("CLINIC-ADMIN: REST request to fetch OP Sales by billNo: {}", billNo);
          res =  pharmacyManagementFeignClient.getByBillNo(billNo);
    	  }catch(FeignException e) {}
    	  return res;
      }
      
      
     
      public ResponseEntity<Response> getById(
              String id) {
    	  ResponseEntity<Response> res = null;
    	  try {
          log.info("CLINIC-ADMIN:REST request to fetch OP Sales by id: {}", id);
          res = pharmacyManagementFeignClient.getById(id);}
    	  catch(FeignException e) {}
    	  return res;
      }
      
    
      public ResponseEntity<Response> getByOpNo(
      		  String clinicId,
      		 String branchId,
      		  String opNo) {
    	  ResponseEntity<Response> res = null;
    	  try {
          log.info("CLINIC-ADMIN: REST request to fetch OP Sales by opNo");
          return pharmacyManagementFeignClient.getByOpNo(clinicId, branchId, opNo);}
    	  catch(FeignException e) {}
    	  return res;
      }
      
      
      
      public ResponseEntity<Response> deleteOpSales(
             String id,
             String clinicId,
             String branchId) {
    	  ResponseEntity<Response> res = null;
    	  try {
          log.info("CLINIC-ADMIN: REST request to delete OP Sales id: {}", id);
          return pharmacyManagementFeignClient.deleteOpSales(clinicId, branchId, id);}
    	  catch(FeignException e) {}
    	  return res;
      }
      
    
      public ResponseEntity<Response> filterOpSales(
             String clinicId,
              String branchId,
              String billNo,
              String patientName,
               String mobile,
              String consultingDoctor,
              String fromDate,
              String toDate) {
    	  ResponseEntity<Response> res = null;
    	  try {
          log.info("CLINIC-ADMIN:REST request to filter OP Sales");

          res = pharmacyManagementFeignClient.filterOpSales(
                  clinicId,
                  branchId,
                  billNo,
                  patientName,
                  mobile,
                  consultingDoctor,
                  fromDate,
                  toDate
          );
    	  }catch(FeignException e) {}
    	  return res;
    	  }

  }

