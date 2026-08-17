package com.chiselon.clinicadmin.service.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.feignclient.PhysiotherapyFeignClient;
import com.chiselon.clinicadmin.service.RevenueService;
import com.chiselon.clinicadmin.utils.RevenueResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueImpl implements RevenueService {
	
	private final PhysiotherapyFeignClient physiotherapyFeignClient;
	
	 @Override
		public ResponseEntity<RevenueResponse> getRevenueManagement(
				 String clinicId,
				 String branchId,
				 String number){
		 
		 log.info("getRevenueManagement API called clinicId:{},branchId:{},number:{}",
				 clinicId,branchId,number);
					 
			try {
				return physiotherapyFeignClient.getRevenueManagement(clinicId, branchId, number);
			}catch(Exception e) { 
				 log.error("exception:{}",e);
				RevenueResponse res = new RevenueResponse();
				res.setMessage(e.getMessage());
				res.setStatus(500);
				res.setSuccess(false);
				return ResponseEntity.status(500).body(res);
			}}
	    
	 @Override
		public ResponseEntity<RevenueResponse> getRevenueManagementByDateRange(
				 String clinicId,
				 String branchId,
				 String startDate,
				 String endDate){
		 log.info("getRevenueManagementByDateRange API is called with clinicId:{},branchId:{},startDate:{},endDate:{}",
				 clinicId,branchId,startDate,endDate);
			try {
				return physiotherapyFeignClient.getRevenueManagementByDateRange(clinicId, branchId,startDate, endDate);
			}catch(Exception e) { 
				 log.error("exception:{}",e);
				RevenueResponse res = new RevenueResponse();
				res.setMessage(e.getMessage());
				res.setStatus(500);
				res.setSuccess(false);
				return ResponseEntity.status(500).body(res);
			}
		}
	    
	 @Override
		public ResponseEntity<Response> getRevenueSummary(
				 String clinicId,
				 String branchId){
		 log.info("getRevenueSummary API is called with clinicId:{},branchId:{}",
				 clinicId,branchId);
			try {
				return physiotherapyFeignClient.getRevenueSummary(clinicId, branchId);
			}catch(Exception e) { 
				 log.error("exception:{}",e);
				Response res = new Response();
				res.setMessage(e.getMessage());
				res.setStatus(500);
				res.setSuccess(false);
				return ResponseEntity.status(500).body(res);
			}
			
		}
	

}
