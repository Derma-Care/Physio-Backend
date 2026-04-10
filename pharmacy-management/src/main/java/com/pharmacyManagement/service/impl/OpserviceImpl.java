package com.pharmacyManagement.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacyManagement.dto.CustomerOnbordingDTO;
import com.pharmacyManagement.dto.DoctorSaveDetailsDTO;
import com.pharmacyManagement.dto.OpMedicineDTO;
import com.pharmacyManagement.dto.OpNoResponse;
import com.pharmacyManagement.dto.OpSalesRequest;
import com.pharmacyManagement.dto.OpSalesResponse;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.entity.Medicine;
import com.pharmacyManagement.entity.OpMedicine;
import com.pharmacyManagement.entity.OpSales;
import com.pharmacyManagement.entity.PaymentEntry;
import com.pharmacyManagement.feign.ClinicAdminFeign;
import com.pharmacyManagement.feign.DoctorFeign;
import com.pharmacyManagement.repository.MedicineRepository;
import com.pharmacyManagement.repository.OpSalesRepository;
import com.pharmacyManagement.service.Opservice;
import com.pharmacyManagement.util.ResourceNotFoundException;

@Service
public class OpserviceImpl implements Opservice {

    @Autowired
    private OpSalesRepository opSalesRepository;
    
    @Autowired
    private DoctorFeign doctorFeign;
    
    @Autowired
    private MedicineRepository inventoryRepository;
    
    @Autowired
    private ClinicAdminFeign clinicAdminFeign;

    // ── FIX: inject ObjectMapper as a Spring bean instead of using new ObjectMapper() ──
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private InventoryServiceImpl inventoryServiceImpl;

    private static final Logger log = LoggerFactory.getLogger(OpserviceImpl.class);

    //// GET ALL
    // =========================================================================
    
    @Override
    public ResponseEntity<Response> createOpSales(OpSalesRequest request){
    	Response res = new Response();
    	OpSales opsale = null;
    	//System.out.println(request.getMedicines());
    	try {
    		Optional<OpSales> opsales = opSalesRepository.findByBillNo(request.getBillNo());
    		if(opsales.isPresent()) {
    			res.setMessage("Bill Number already exist");
    			res.setStatus(409);
    			res.setSuccess(false);
    		}else {
    		List<OpMedicine> lst = validateMedicinesForPostReq(request.getMedicines());  
    		opsale = calculateValues(lst,request);
    		opsale.setMedicines(lst);
    		//System.out.println(opsale.getMedicines());
    		opSalesRepository.save(opsale);
    		updateInventory(opsale);
    		res.setMessage("Opsales saved successfully");
			res.setStatus(200);
			res.setSuccess(true);
			res.setData(objectMapper.convertValue(opsale, OpSalesResponse.class));
    		}}catch(Exception e) {}
    	return ResponseEntity.status(res.getStatus()).body(res);
    }
     
    
    public void updateInventory(OpSales opsale) {
    	try {
    		for(OpMedicine m : opsale.getMedicines() ) {
    	inventoryServiceImpl.updateInventoryByMedicineId(m.getMedicineId(), m.getBatchNo(), "-", m.getQty());
      }}catch(Exception e) {}}
    
   
    public ResponseEntity<Response> updateOpSales(OpSalesRequest request){
    	Response res = new Response();
    	try {
    		Optional<OpSales> opsales = opSalesRepository.findByBillNo(request.getBillNo());
    		if(opsales.isPresent()) {
    			if (request.getBillNo() != null || !request.getBillNo().isBlank()) {
    				opsales.get().setBillNo(request.getBillNo());
    				//System.out.println(request.getBillNo());
    		    }
    		    // ── billDate ─────────────────────────────────────────────────────────────
    		    if (request.getBillDate() != null || !request.getBillDate().isBlank()) {
    		    	opsales.get().setBillDate(request.getBillDate());
    		    	//System.out.println(request.getBillDate());
    		    }

    		    // ── billTime ─────────────────────────────────────────────────────────────
    		    if (request.getBillTime() != null || !request.getBillTime().isBlank()) {
    		    	opsales.get().setBillTime(request.getBillTime());
    		    	//System.out.println(request.getBillTime() );
    		    }

    		    // ── patientName ──────────────────────────────────────────────────────────
    		    if (request.getPatientName() != null || !request.getPatientName().isBlank()) {
    		    	opsales.get().setPatientName(request.getPatientName());
    		    	///System.out.println(request.getPatientName());
    		    }

    		    // ── includeReturns ───────────────────────────────────────────────────────
    		    if (request.getIncludeReturns() != null) {
    		    	opsales.get().setIncludeReturns(request.getIncludeReturns());
    		    	//System.out.println(request.getIncludeReturns());
    		    }
    		    //System.out.println(request.getMedicines());
    		  // ── medicines ────────────────────────────────────────────────────────────
    		    if (request.getMedicines() != null || !request.getMedicines().isEmpty()) {
    		    	//System.out.println("yyy");
    		    	List<OpMedicine> lst = validateMedicines(request.getMedicines(),opsales.get().getBillNo());
    		    	opsales.get().setMedicines(lst);
    		    	//System.out.println(lst);
    		    }   		    
    		    // ── clinicId ─────────────────────────────────────────────────────────────
    		    if (request.getClinicId() != null || !request.getClinicId().isBlank()) {
    		    	opsales.get().setClinicId(request.getClinicId() );
    		    	//System.out.println(request.getClinicId());
    		    }

    		    // ── branchId ─────────────────────────────────────────────────────────────
    		    if (request.getBranchId() != null || !request.getBranchId().isBlank()) {
    		    	opsales.get().setBranchId(request.getBranchId());}
    		    
    		    	OpSales opsle = calculateAndUpdateValues(request.getAmountPaid(),opsales.get());
    		    	//System.out.println(opsle);
    		    	if(opsle != null) {
    		    		OpSales op = opSalesRepository.save(opsle);  
    		    	 // System.out.println(op);
    		    		res.setMessage("Opsales updated successfully");
    					res.setStatus(200);
    					res.setSuccess(true);
    					res.setData(objectMapper.convertValue(op, OpSalesResponse.class));  		    		
    		        }}else {  		 
    		res.setMessage("Opsale Not Found to Update");
			res.setStatus(404);
			res.setSuccess(false);
    		}}catch(Exception e) {}
    	return ResponseEntity.status(res.getStatus()).body(res);}
    
        
    @Override
    public ResponseEntity<Response> getAllOpSales(String clinicId, String branchId) {
        Response res = new Response();
        log.info("Fetching all OP Sales for clinicId: {}, branchId: {}", clinicId, branchId);

        List<OpSales> lst = opSalesRepository
                .findByClinicIdAndBranchId(clinicId, branchId);
        List<OpSalesResponse> response = objectMapper.convertValue(lst, new TypeReference<List<OpSalesResponse>>() {
		});
                
        if (!response.isEmpty()) {
            res.setMessage("OP Sales retrieved successfully");
            res.setStatus(200);
            res.setData(response);
            res.setSuccess(true);
        } else {
            res.setMessage("Unable to retrieve OP Sales");
            res.setStatus(404);
            res.setSuccess(false);
        }
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================================================================
    //  4a. GET BY BILL NO
    // =========================================================================
    @Override
    public ResponseEntity<Response> getByBillNo(String billNo) {
        Response res = new Response();
        log.info("Fetching OP Sales by billNo: {}", billNo);

        OpSales ops = opSalesRepository.findByBillNo(billNo)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "OP Sales not found with billNo: " + billNo));
        OpSalesResponse response = objectMapper.convertValue(ops, OpSalesResponse.class);
        if (response != null) {
            res.setMessage("OP Sales retrieved successfully");
            res.setStatus(200);
            res.setData(ops);
            res.setSuccess(true);
        } else {
            res.setMessage("Unable to retrieve OP Sales");
            res.setStatus(404);
            res.setSuccess(false);
        }
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================================================================
    //  4b. GET BY ID
    // =========================================================================
    @Override
    public ResponseEntity<Response> getById(String id) {
        Response res = new Response();
        log.info("Fetching OP Sales by id: {}", id);

        OpSales ops = opSalesRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "OP Sales not found with id: " + id));
        OpSalesResponse response = objectMapper.convertValue(ops, OpSalesResponse.class);
        
        if (response != null) {
            res.setMessage("OP Sales retrieved successfully");
            res.setStatus(200);
            res.setData(ops);
            res.setSuccess(true);
        } else {
            res.setMessage("Unable to retrieve OP Sales");
            res.setStatus(404);
            res.setSuccess(false);
        }
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================================================================
    //  5. GET BY OPNO
    // =========================================================================
    @Override
    public ResponseEntity<Response> getByOpNo(String clinicId, String branchId, String opNo,String mobileNumber) {
        Response res = new Response();
        CustomerOnbordingDTO customerOnbordingDTO = null;
        log.info("Fetching by opNo: {}, clinicId: {}, branchId: {}", opNo, clinicId, branchId);
        try {
        if(opNo != null) {
        System.out.println(opNo);
        System.out.println(clinicAdminFeign.getCustomerById(opNo));
        ResponseEntity<Response> respnse = clinicAdminFeign.getCustomerById(opNo);
        System.out.println(respnse);
        if(respnse.hasBody()){ 	
        customerOnbordingDTO = objectMapper.convertValue(clinicAdminFeign.getCustomerById(opNo).getBody().getData(),CustomerOnbordingDTO.class);}
        System.out.println(customerOnbordingDTO);
        }else{
        ResponseEntity<Response> respnse = clinicAdminFeign.getCustomerById(opNo);
        if(respnse.hasBody()){ 
        customerOnbordingDTO = objectMapper.convertValue(clinicAdminFeign.getCustomerByMobileNumber(mobileNumber).getBody().getData(),CustomerOnbordingDTO.class);}}
        if(customerOnbordingDTO.getCustomerId() != null) {
        ResponseEntity<DoctorSaveDetailsDTO>  doctorSaveDetailsDTO = doctorFeign.getDoctorSaveDetails(opNo);
        System.out.println(doctorSaveDetailsDTO);
        if(doctorSaveDetailsDTO.hasBody() && !doctorSaveDetailsDTO.getBody().getPrescription().getMedicines().isEmpty()) {
        List<String> lst = doctorSaveDetailsDTO.getBody().getPrescription().getMedicines().stream().map(n->n.getId()).toList();
        System.out.println(lst);
        List<Medicine> lt = new ArrayList<>();
        for(String s : lst) {
         Medicine invent = inventoryRepository.findById(s).get();
        lt.add(invent);}
        List<OpSales> lsts = opSalesRepository.findByClinicIdAndBranchIdAndOpNo(clinicId, branchId, opNo);
        if(!lsts.isEmpty()) {
        //OpSales opSales = lsts.get(lsts.size()-1);
        OpNoResponse opNoResponse = new OpNoResponse();
        opNoResponse.setAge(Integer.valueOf(customerOnbordingDTO.getAge()));
        opNoResponse.setBranchId(customerOnbordingDTO.getBranchId());
        opNoResponse.setClinicId(customerOnbordingDTO.getHospitalId());
        //opNoResponse.setMedicines(lt);
        opNoResponse.setMobile(customerOnbordingDTO.getMobileNumber());
        opNoResponse.setOpNo(customerOnbordingDTO.getCustomerId());
        opNoResponse.setPatientName(customerOnbordingDTO.getFullName());
        opNoResponse.setSex(customerOnbordingDTO.getGender());
       // opNoResponse.setVisitType();
        }else{
            res.setMessage("OP Sales Found with clincId,BranchId,OpNo");
            res.setStatus(404);
            res.setSuccess(false);
        }}else{
        	 res.setMessage("medicines Not found with OpNo in Doctorsavedetails");
             res.setStatus(404);
             res.setSuccess(false);
      
        }}else{
        	 res.setMessage("Customer Not Found");
             res.setStatus(404);
             res.setSuccess(false);	
        }}catch(Exception e) {}
        return ResponseEntity.status(res.getStatus()).body(res);}
  

    // =========================================================================
    //  6. DELETE
    // =========================================================================
    @Override
    public ResponseEntity<Response> deleteOpSales(String clinicId, String branchId, String id) {
        Response res = new Response();
        log.info("Deleting OP Sales id: {}, clinicId: {}, branchId: {}", id, clinicId, branchId);

        OpSales opSales = opSalesRepository
                .findByBillNoIgnoreCaseAndClinicIdAndBranchId(id, clinicId, branchId)            
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OP Sales not found with id: " + id + " for clinic/branch"));
        if(opSales != null) {
        opSalesRepository.delete(opSales);
        res.setMessage("OP Sales Deleted Successfully");
        res.setStatus(200);
        res.setSuccess(true);}
        else {
        	 res.setMessage("OP Sales Not Found");
             res.setStatus(404);
             res.setSuccess(false);	
        }
        log.info("OP Sales deleted with id: {}", id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    
    @Override
    public ResponseEntity<Response> filterOpSales(String clinicId, String branchId,
            String billNo, String patientName,
            String mobile, String consultingDoctor,
            String fromDate, String toDate) {

        Response res = new Response();
        log.info("Filtering OP Sales for clinicId: {}, branchId: {}", clinicId, branchId);

        List<OpSales> rawList = opSalesRepository
                .findByClinicIdAndBranchIdAndBillNoContainingIgnoreCaseAndPatientNameContainingIgnoreCaseAndMobileAndConsultingDoctorContainingIgnoreCase(
                        clinicId,
                        branchId,
                        billNo,
                        patientName,
                        mobile,
                        consultingDoctor);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate from = isPresent(fromDate) ? LocalDate.parse(fromDate, formatter) : null;
        LocalDate to   = isPresent(toDate)   ? LocalDate.parse(toDate,   formatter) : null;

        List<OpSales> list = rawList.stream()
                .filter(sale -> {
                    if (sale.getBillDate() == null) return false;
                    LocalDate billDate = LocalDate.parse(sale.getBillDate(), formatter);
                    if (from != null && billDate.isBefore(from)) return false;
                    if (to   != null && billDate.isAfter(to))    return false;
                    return true;
                }).collect(Collectors.toList());
        
        List<OpSalesResponse> lst = objectMapper.convertValue(list,new TypeReference< List<OpSalesResponse>>() {
		});
        if (!lst.isEmpty()) {
            res.setMessage("OP Sales retrieved successfully");
            res.setStatus(200);
            res.setData(lst);
            res.setSuccess(true);
        } else {
            res.setMessage("OP Sales Not Found");
            res.setStatus(404);
            res.setSuccess(false);
        }
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    private boolean isPresent(String val) {
        return val != null && !val.isBlank();
    }
    
    
    
    public List<OpMedicine> validateMedicinesForPostReq(List<OpMedicineDTO> dto){
    	List<OpMedicineDTO> lst = dto.stream().map(d -> {
    	    // Calculations on DTO
    	    Double totalA = d.getRate() * d.getQty();
    	    d.setTotalA(totalA);

    	    Double netAmtAB = d.getTotalA() - d.getDiscAmtB();
    	    d.setNetAmtAB(netAmtAB);

    	    d.setFinalAmountABC(netAmtAB);

    	    return d;

    	}).toList();

    	List<OpMedicine> opMedicineList = objectMapper.convertValue(
    	        lst,
    	        new TypeReference<List<OpMedicine>>() {}
    	);

    	return opMedicineList;}
    
    
    public List<OpMedicine> validateMedicines(List<OpMedicineDTO> dto,String billNo){   	
    	       OpSales opSales = null;
    	try{
    		  opSales = opSalesRepository
  		            .findByBillNo(billNo).get();
    		  boolean isexist = false;
  		    if (opSales != null) {
    		for (OpMedicineDTO d : dto) {
    		        for (OpMedicine n : opSales.getMedicines()) {

    		            if (n.getMedicineId().equals(d.getMedicineId())) {    		            
    		                isexist = true;
    		                if (n.getQty() != null && d.getQty() != null) {
    		                    n.setQty(n.getQty() + d.getQty());
    		                }
    		                // rate
    		                if (n.getRate() != null && d.getRate() != null) {
    		                    n.setRate(n.getRate() + d.getRate());
    		                }

    		                // totalA
    		                if (n.getTotalA() != null && d.getTotalA() != null) {
    		                    n.setTotalA(n.getTotalA() + d.getTotalA());
    		                }

    		                // discPercent
    		                if (n.getDiscPercent() != null && d.getDiscPercent() != null) {
    		                    n.setDiscPercent(n.getDiscPercent() + d.getDiscPercent());
    		                }

    		                // discAmtB
    		                if (n.getDiscAmtB() != null && d.getDiscAmtB() != null) {
    		                    n.setDiscAmtB(n.getDiscAmtB() + d.getDiscAmtB());
    		                }

    		                // netAmtAB
    		                if (n.getNetAmtAB() != null && d.getNetAmtAB() != null) {
    		                    n.setNetAmtAB(n.getNetAmtAB() + d.getNetAmtAB());
    		                }

    		                // gstPercent
    		                if (n.getGstPercent() != null && d.getGstPercent() != null) {
    		                    n.setGstPercent(n.getGstPercent() + d.getGstPercent());
    		                }

    		                // gstAmtC
    		                if (n.getGstAmtC() != null && d.getGstAmtC() != null) {
    		                    n.setGstAmtC(n.getGstAmtC() + d.getGstAmtC());
    		                }

    		                // finalAmountABC
    		                if (n.getFinalAmountABC() != null && d.getFinalAmountABC() != null) {
    		                    n.setFinalAmountABC(n.getFinalAmountABC() + d.getFinalAmountABC());
    		                }}break;} if(!isexist){ opSales.getMedicines().add(
	    		                    objectMapper.convertValue(d, OpMedicine.class)
	    		            );}}}
    		    if(opSales != null) {
    		    for (OpMedicine d : opSales.getMedicines()) {

    		        Double rate = d.getRate() != null ? d.getRate() : 0.0;
    		        Double qty = d.getQty() != null ? d.getQty() : 0.0;

    		        Double totalA = rate * qty;
    		        d.setTotalA(totalA);

    		        Double disc = d.getDiscAmtB() != null ? d.getDiscAmtB() : 0.0;
    		        Double netAmtAB = totalA - disc;
    		        d.setNetAmtAB(netAmtAB);

    		        d.setFinalAmountABC(netAmtAB);
    		    }}}catch(Exception e) {System.out.println(e.getMessage());
    		    	}return opSales.getMedicines();}
    		    
    
    public OpSales calculateValues(List<OpMedicine> dto,OpSalesRequest request) {
    OpSales opsales = new ObjectMapper().convertValue(request, OpSales.class);
    	Double totalAmt = 0.0;
    	double totalDiscPct = 0.0;
    	double totalDiscAmt = 0.0;
    	for(OpMedicine o : dto) {
    		totalAmt += o.getTotalA();
    		totalDiscPct += o.getDiscPercent();
    		totalDiscAmt += Math.abs(o.getDiscAmtB());
    	}
    	double avgDiscPercent = Math.round(totalDiscPct / dto.size()*100.0/100.0); // average disc %
    	Double netAmount = totalAmt - totalDiscAmt;
    	opsales.setTotalAmt(totalAmt);
    	opsales.setTotalDiscAmt(totalDiscAmt);
        opsales.setAvgDiscPercent(avgDiscPercent);
        opsales.setNetAmount(netAmount);
        opsales.setFinalTotal(netAmount);
        opsales.setMedicines(dto);
        opsales.setCurrentPaymentAmount(request.getAmountPaid());
        opsales.setAlreadyPaidAmount(0.0);
        opsales.setTotalPaidAmount(opsales.getAlreadyPaidAmount()+ opsales.getCurrentPaymentAmount());
        opsales.setDueAmount(netAmount - opsales.getTotalPaidAmount());
        PaymentEntry paymentEntry = new PaymentEntry();
        paymentEntry.setAmountPaid(request.getAmountPaid());
        paymentEntry.setAlreadyPaid(0.0);
        paymentEntry.setTotalPaidSoFar(paymentEntry.getAmountPaid() + paymentEntry.getAlreadyPaid());
        paymentEntry.setDueAmount(netAmount - paymentEntry.getTotalPaidSoFar());
        paymentEntry.setPaidAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        List<PaymentEntry> pEntry = new ArrayList<>();
        pEntry.add(paymentEntry);
        opsales.setPaymentHistory(pEntry);
    	return opsales;
    }
    
    public OpSales calculateAndUpdateValues(double amountPaid, OpSales opsales ) {
         	try {
    	    Double totalAmt = 0.0;
        	double totalDiscPct = 0.0;
        	double totalDiscAmt = 0.0;
        	for(OpMedicine o : opsales.getMedicines()) {
        		totalAmt += o.getTotalA();
        		totalDiscPct += o.getDiscPercent();
        		totalDiscAmt += Math.abs(o.getDiscAmtB());
        	}
        	double avgDiscPercent = Math.round(totalDiscPct/opsales.getMedicines().size()*100.0/100.0); // average disc %
        	Double netAmount = totalAmt - totalDiscAmt;
        	opsales.setTotalAmt(totalAmt);
        	opsales.setTotalDiscAmt(totalDiscAmt);
            opsales.setAvgDiscPercent(avgDiscPercent);
            opsales.setNetAmount(netAmount);
            opsales.setFinalTotal(netAmount);
            opsales.setCurrentPaymentAmount(amountPaid);
            opsales.setAlreadyPaidAmount(opsales.getTotalPaidAmount());
            opsales.setTotalPaidAmount(amountPaid + opsales.getAlreadyPaidAmount());
            opsales.setDueAmount(netAmount - opsales.getTotalPaidAmount());
            PaymentEntry paymentEntry = new PaymentEntry();
            paymentEntry.setAmountPaid(amountPaid);
            paymentEntry.setAlreadyPaid(opsales.getAlreadyPaidAmount());
            paymentEntry.setTotalPaidSoFar(amountPaid + paymentEntry.getAlreadyPaid());
            paymentEntry.setDueAmount(netAmount - opsales.getTotalPaidAmount());
            paymentEntry.setPaidAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            List<PaymentEntry> pEntry = opsales.getPaymentHistory();
            pEntry.add(paymentEntry);
            opsales.setPaymentHistory(pEntry);
         	}catch(Exception e) {}
         	return opsales;
        }
        
}