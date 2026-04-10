package com.clinicadmin.service;

import com.clinicadmin.dto.OpSalesRequest;

public interface OpService {
	
	 public Object createOpSales(
	           OpSalesRequest request);
	 
	 public Object updateOpSales(
             OpSalesRequest request) ;
	 
	 public Object getAllOpSales(
             String clinicId,
             String branchId);
	 
	 public Object getByBillNo(
             String billNo);
	 
	 public Object getById(
             String id) ;
	 
	  public Object getByOpNo(
      		  String clinicId,
      		 String branchId,
      		  String opNo) ;
	  
	   
      public Object deleteOpSales(
             String id,
             String clinicId,
             String branchId) ;
      
      public Object filterOpSales(
              String clinicId,
               String branchId,
               String billNo,
               String patientName,
                String mobile,
               String consultingDoctor,
               String fromDate,
               String toDate) ;
	 

}
