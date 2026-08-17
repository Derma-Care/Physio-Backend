package com.chiselon.physiotherapydoctor.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.chiselon.physiotherapydoctor.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import com.chiselon.physiotherapydoctor.dto.response.ExerciseResponse;
import com.chiselon.physiotherapydoctor.dto.response.PackageResponse;
import com.chiselon.physiotherapydoctor.dto.response.PaymentRecordResponse;
import com.chiselon.physiotherapydoctor.dto.response.ProgramResponse;
import com.chiselon.physiotherapydoctor.dto.response.TherapyResponse;
import com.chiselon.physiotherapydoctor.entity.PaymentRecord;
import com.chiselon.physiotherapydoctor.entity.PhysiotherapyRecord;
import com.chiselon.physiotherapydoctor.feign.NotificationFeign;
import com.chiselon.physiotherapydoctor.repository.PaymentRepository;
import com.chiselon.physiotherapydoctor.repository.PhysiotherapydoctorRespository;
import com.chiselon.physiotherapydoctor.service.PaymentService;
import com.chiselon.physiotherapydoctor.util.BookingFeignImpl;
import com.chiselon.physiotherapydoctor.util.ClinicAdminFeignImpl;

import com.chiselon.physiotherapydoctor.util.RevenueResponse;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository repo;

	@Autowired
	private ClinicAdminFeignImpl clinicAdminFeign;
	
	@Autowired
	private NotificationFeign notificationFeign;

	@Autowired
	private BookingFeignImpl bookingFeignClient;
		
	@Autowired
	private PhysiotherapydoctorRespository physiotherapydoctorRespository;
		
	@Autowired
	private PaymentWhatsAppService paymentWhatsAppService;

	// ========================================================
	// CREATE
	// ========================================================
	@Override
	@Secured("ROLE_DOCTOR")
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "createPaymentFallback")
	public PaymentRecordResponse createPayment(PaymentRequest req) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered createPayment() with bookingId : {}, patientId : {}, clinicId : {}",
	            req.getBookingId(),
	            req.getPatientId(),
	            req.getClinicId());

	    try {

	        log.debug("Checking existing payment record for bookingId : {}",
	                req.getBookingId());

	        if (repo.findByBookingId(req.getBookingId()).isPresent()) {

	            log.warn("Payment record already exists for bookingId : {}",
	                    req.getBookingId());

	            throw new RuntimeException("Already exists, use update");
	        }

	        if (req.isPayAfterService()) {

	            log.debug("PayAfterService flag is enabled for bookingId : {}",
	                    req.getBookingId());

	            if (req.getAmount() == null || req.getAmount() <= 0) {

	                log.warn("Invalid amount received for PayAfterService. Amount : {}",
	                        req.getAmount());

	                throw new RuntimeException("Amount must be greater than 0");
	            }
	        }

	        if (req.getTherapyWithSessions() == null
	                || req.getTherapyWithSessions().isEmpty()) {

	            log.warn("therapyWithSessions is missing for bookingId : {}",
	                    req.getBookingId());

	            throw new RuntimeException("therapyWithSessions is required");
	        }

	        log.info("Normalizing therapy package payload for bookingId : {}",
	                req.getBookingId());

	        List<TherapyWithSessions> normalized = normalizePayload(req);

	        req.setTherapyWithSessions(normalized);

	        log.info("Normalized {} therapy packages",
	                normalized.size());

	        PaymentRecord record = new PaymentRecord();

	        log.debug("Populating payment record basic information");

	        record.setClinicId(req.getClinicId());
	        record.setBranchId(req.getBranchId());
	        record.setBookingId(req.getBookingId());
	        record.setPatientId(req.getPatientId());
	        record.setOverallStatus("Pending");

	        record.setDoctorId(req.getDoctorId());
	        record.setDoctorName(req.getDoctorName());

	        record.setTherapistId(req.getTherapistId());
	        record.setTherapistName(req.getTherapistName());
	        record.setTherapistRecordId(req.getTherapistRecordId());

	        record.setTreatmentName(req.getTreatmentName());
	        record.setServiceType(req.getServiceType());

	        record.setSessionStartDate(req.getSessionStartDate());

	        log.info("Calculating total payment amount");

	        double total = calculateTotal(req.getTherapyWithSessions());

	        double exerciseDiscount =
	                calculateTotalDiscount(req.getTherapyWithSessions());

	        log.debug("Calculated total amount : {}", total);
	        log.debug("Calculated exercise discount : {}", exerciseDiscount);

	        record.setOverallReceiptNumber(generateOverallReceiptNumber());

	        record.setPaymentHistory(new ArrayList<>());

	        record.getPaymentHistory().add(
	                buildHistory(req, generateReceiptNumber()));

	        double paymentHistoryDiscount =
	                calculatePaymentHistoryDiscount(
	                        record.getPaymentHistory());

	        double finalAmount = total - paymentHistoryDiscount;

	        record.setTotalAmount(total);
	        record.setDiscountAmount(paymentHistoryDiscount);
	        record.setFinalAmount(finalAmount);

	        log.info("Payment calculation completed. Total : {}, Discount : {}, Final : {}",
	                total,
	                paymentHistoryDiscount,
	                finalAmount);

	        double amount =
	                req.isPayAfterService()
	                        ? req.getAmount()
	                        : 0;

	        record.setPayAfterService(req.isPayAfterService());

	        record.setTotalPaid(amount);
	        record.setBalanceAmount(finalAmount - amount);
	        record.setPaymentStatus(getStatus(record));

	        log.info("Payment status calculated : {}",
	                record.getPaymentStatus());

	        log.info("Creating session table records");

	        boolean created =
	                createSessions(
	                        req.getTherapyWithSessions(),
	                        req.getSessionStartDate());

	        record.setSessionTableCreatedStatus(created);

	        log.info("Session table creation status : {}", created);

	        record.setSessionEndDate(
	                getLastSessionDate(req.getTherapyWithSessions()));

	        record.setTherapyWithSessions(
	                req.getTherapyWithSessions());

	        record.setTotalSessionCount(
	                countTotalSessions(record));

	        log.info("Total session count calculated : {}",
	                record.getTotalSessionCount());

	        log.info("Distributing payment across sessions");

	        distributePaymentToSessions(record);

	        record.setPaymentHistory(new ArrayList<>());

	        record.getPaymentHistory().add(
	                buildHistory(req, generateReceiptNumber()));

	        if (req.getPaymentTarget() != null) {

	            log.info("Applying payment level : {}",
	                    req.getPaymentTarget());

	            applyPaymentLevel(record, req);
	        }

	        log.info("Updating payment statuses");

	        updateStatuses(record);

	        log.debug("Saving payment record for bookingId : {}",
	                req.getBookingId());

	        PaymentRecord savedRecord =
	                repo.save(record);

	        log.info("Payment record saved successfully with id : {}",
	                savedRecord.getId());

	        try {	        	
	        	String name = clinicAdminFeign.getCustomername(req.getPatientId());
	            //System.out.println(name);
	  			Map<String,String> map = new LinkedHashMap<>();
	  			map.put("therapistId",req.getTherapistId() );
	  			map.put("therapistName",req.getTherapistName() );
	  			map.put("sessionStartDate", req.getSessionStartDate());
	  			map.put("patientname",name );
	  			//System.out.println(map);
	  			notificationFeign.notificationToTherapist(map);

	            log.info("Triggering WhatsApp payment confirmation for bookingId : {}",
	                    savedRecord.getBookingId());

	            paymentWhatsAppService.sendPaymentConfirmation(
	                    savedRecord);

	            log.info("WhatsApp triggered successfully for bookingId={}",
	                    savedRecord.getBookingId());

	        } catch (Exception e) {

	            log.warn("WhatsApp notification failed for bookingId={} : {}",
	                    savedRecord.getBookingId(),
	                    e.getMessage(),
	                    e);
	        }

	        long executionTime =
	                System.currentTimeMillis() - startTime;

	        log.info("createPayment() completed successfully for bookingId : {} in {} ms",
	                req.getBookingId(),
	                executionTime);

	        return mapToResponse(savedRecord);

	    } catch (Exception e) {

	        log.error("Exception occurred while creating payment for bookingId : {}. Error : {}",
	                req.getBookingId(),
	                e.getMessage(),
	                e);

	        throw e;
	    }
	}
	
	

	// ========================================================
	// UPDATE
	// ========================================================
	@Override
	@Secured("ROLE_DOCTOR")
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updatePaymentFallback")
	public PaymentRecordResponse updatePayment(PaymentRequest req) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered updatePayment() with bookingId : {}",
	            req.getBookingId());

	    try {

	        log.debug("Fetching payment record for bookingId : {}",
	                req.getBookingId());

	        PaymentRecord record = repo.findByBookingId(req.getBookingId())
	                .orElseThrow(() -> {
	                    log.warn("Payment record not found for bookingId : {}",
	                            req.getBookingId());
	                    return new RuntimeException("Payment not found");
	                });

	        log.info("Payment record found for bookingId : {}",
	                req.getBookingId());

	        record.setTreatmentName(req.getTreatmentName());

	        if (req.getTherapyWithSessions() != null) {

	            log.warn("therapyWithSessions received during update for bookingId : {}",
	                    req.getBookingId());

	            throw new RuntimeException("Do not send therapyWithSessions in update");
	        }

	        record.setPayAfterService(req.isPayAfterService());

	        if (req.isPayAfterService()) {

	            if (req.getAmount() == null || req.getAmount() <= 0) {

	                log.warn("Invalid payment amount received : {}",
	                        req.getAmount());

	                throw new RuntimeException("Amount must be greater than 0");
	            }
	        }

	        double amount = req.isPayAfterService() ? req.getAmount() : 0;

	        double additionalDiscount =
	                req.getDiscountAmount() != null
	                        ? req.getDiscountAmount()
	                        : 0;

	        if (additionalDiscount > 0) {

	            log.info("Applying additional discount : {}",
	                    additionalDiscount);

	            double newFinalAmount =
	                    record.getFinalAmount() - additionalDiscount;

	            record.setDiscountAmount(
	                    record.getDiscountAmount() + additionalDiscount);

	            record.setFinalAmount(newFinalAmount);
	        }

	        double currentPaid = record.getTotalPaid();
	        double finalAmount = record.getFinalAmount();
	        double remaining = finalAmount - currentPaid;

	        double newPaid = currentPaid + amount;

	        log.debug("Current Paid : {}, New Payment : {}, Final Amount : {}",
	                currentPaid,
	                amount,
	                finalAmount);

	        if (newPaid > finalAmount) {

	            log.warn("Overpayment detected. Remaining payable amount : {}",
	                    remaining);

	            throw new RuntimeException(
	                    "Payment exceeds final amount. Remaining payable: "
	                            + remaining);
	        }

	        record.setTotalPaid(newPaid);
	        record.setBalanceAmount(finalAmount - newPaid);
	        record.setPaymentStatus(getStatus(record));

	        log.info("Updated payment status : {}",
	                record.getPaymentStatus());

	        log.debug("Distributing payment across sessions");

	        distributePaymentToSessions(record);

	        log.debug("Applying payment level");

	        applyPaymentLevel(record, req);

	        int completed = countCompleted(record);

	        record.setNoOfSessionCompletedCount(completed);
	        record.setNoOfSessionCompletedStatus(
	                completed >= record.getTotalSessionCount());

	        log.info("Completed sessions : {} out of {}",
	                completed,
	                record.getTotalSessionCount());

	        log.debug("Adding payment history entry");

	        record.getPaymentHistory()
	                .add(buildHistory(req, generateReceiptNumber()));

	        updateStatuses(record);

	        log.debug("Saving updated payment record");

	        PaymentRecord savedRecord = repo.save(record);

	        log.info("Payment record updated successfully. Id : {}",
	                savedRecord.getId());

	        try {

	            log.info("Triggering WhatsApp notification for bookingId : {}",
	                    savedRecord.getBookingId());

	            paymentWhatsAppService.sendPaymentConfirmation(savedRecord);

	            log.info("WhatsApp update triggered successfully for bookingId={}",
	                    savedRecord.getBookingId());

	        } catch (Exception e) {

	            log.warn("WhatsApp update notification failed for bookingId={} : {}",
	                    savedRecord.getBookingId(),
	                    e.getMessage(),
	                    e);
	        }

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("updatePayment() completed successfully in {} ms",
	                executionTime);

	        return mapToResponse(savedRecord);

	    } catch (Exception e) {

	        log.error("Exception occurred while updating payment for bookingId : {}. Error : {}",
	                req.getBookingId(),
	                e.getMessage(),
	                e);

	        throw e;
	    }
	}

	private double calculatePaymentHistoryDiscount(List<PaymentHistory> paymentHistory) {

		if (paymentHistory == null || paymentHistory.isEmpty()) {
			return 0;
		}

		return paymentHistory.stream().mapToDouble(ph -> ph.getDiscountAmount() != null ? ph.getDiscountAmount() : 0)
				.sum();
	}

	private double calculateTotalDiscount(List<TherapyWithSessions> data) {

		double totalDiscount = 0;

		for (var pkg : data) {
			for (var prog : pkg.getPrograms()) {
				for (var therapy : prog.getTherapyData()) {
					for (var ex : therapy.getExercises()) {

						if (ex.getDiscountAmount() != 0) {
							totalDiscount += ex.getDiscountAmount();
						} else if (ex.getDiscountPercentage() != 0 && ex.getDiscountPercentage() > 0) {
							double exPrice = ex.getPricePerSession() * ex.getNoOfSessions();
							totalDiscount += (exPrice * ex.getDiscountPercentage() / 100);
						}
					}
				}
			}
		}

		return totalDiscount;
	}

	// ========================================================
	// GET BY BOOKING ID
	// ========================================================
	@Override
	@Secured({"ROLE_DOCTOR","ROLE_CLINICADMIN"})
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getByBookingIdFallback")
	public PaymentRecordResponse getByBookingId(String bookingId) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered getByBookingId() with bookingId : {}",
	            bookingId);

	    try {

	        log.debug("Fetching payment record for bookingId : {}",
	                bookingId);

	        PaymentRecord record = repo.findByBookingId(bookingId)
	                .orElseThrow(() -> {
	                    log.warn("Payment record not found for bookingId : {}",
	                            bookingId);
	                    return new RuntimeException(
	                            "Payment not found for bookingId: " + bookingId);
	                });

	        int completed = countCompleted(record);

	        record.setNoOfSessionCompletedCount(completed);
	        record.setNoOfSessionCompletedStatus(
	                completed >= record.getTotalSessionCount());

	        log.info("Completed sessions : {} out of {}",
	                completed,
	                record.getTotalSessionCount());

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("getByBookingId() completed successfully in {} ms",
	                executionTime);

	        return mapToResponse(record);

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching payment for bookingId : {}. Error : {}",
	                bookingId,
	                e.getMessage(),
	                e);

	        throw e;
	    }
	}
	
	@Override
	@Secured("ROLE_DOCTOR")
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "deleteByBookingIdFallback")
	public void deleteByBookingId(String bookingId) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered deleteByBookingId() with bookingId : {}",
	            bookingId);

	    try {

	        PaymentRecord record = repo.findByBookingId(bookingId)
	                .orElseThrow(() -> {
	                    log.warn("Payment record not found for bookingId : {}",
	                            bookingId);
	                    return new RuntimeException(
	                            "Payment not found for bookingId: " + bookingId);
	                });

	        log.info("Deleting payment record with id : {}",
	                record.getId());

	        repo.delete(record);

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("deleteByBookingId() completed successfully in {} ms",
	                executionTime);

	    } catch (Exception e) {

	        log.error("Exception occurred while deleting payment for bookingId : {}. Error : {}",
	                bookingId,
	                e.getMessage(),
	                e);

	        throw e;
	    }
	}
	
	@Override
	@Secured({"ROLE_DOCTOR","ROLE_CLINICADMIN"})
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateSessionStatusFromTherapistFallback")
	public void updateSessionStatusFromTherapist(String therapistRecordId,
	                                             String sessionId) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered updateSessionStatusFromTherapist() with therapistRecordId : {}, sessionId : {}",
	            therapistRecordId,
	            sessionId);

	    try {

	        log.debug("Fetching payment records for therapistRecordId : {}",
	                therapistRecordId);

	        List<PaymentRecord> records =
	                repo.findByTherapistRecordId(therapistRecordId);

	        if (records == null || records.isEmpty()) {

	            log.warn("No payment records found for therapistRecordId : {}",
	                    therapistRecordId);

	            throw new RuntimeException(
	                    "No payment records found for therapistRecordId: "
	                            + therapistRecordId);
	        }

	        log.info("Fetched {} payment records",
	                records.size());

	        PaymentRecord targetRecord = null;
	        boolean sessionFound = false;

	        outer:
	        for (PaymentRecord record : records) {

	            List<TherapyWithSessions> packageList =
	                    record.getTherapyWithSessions();

	            if (packageList == null || packageList.isEmpty()) {
	                continue;
	            }

	            for (TherapyWithSessions pkg : packageList) {

	                if (pkg.getPrograms() == null) continue;

	                for (Program program : pkg.getPrograms()) {

	                    if (program.getTherapyData() == null) continue;

	                    for (TherapyData therapy : program.getTherapyData()) {

	                        if (therapy.getExercises() == null) continue;

	                        for (TherapyExercise exercise : therapy.getExercises()) {

	                            if (exercise.getSessions() == null) continue;

	                            for (Session session : exercise.getSessions()) {

	                                if (sessionId.equals(session.getSessionId())) {

	                                    session.setStatus("Completed");

	                                    targetRecord = record;
	                                    sessionFound = true;

	                                    log.info("Session marked as completed. SessionId : {}",
	                                            sessionId);

	                                    break outer;
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        }

	        if (!sessionFound || targetRecord == null) {

	            log.warn("Session not found with sessionId : {}",
	                    sessionId);

	            throw new RuntimeException(
	                    "Session not found with ID: " + sessionId);
	        }

	        targetRecord.setOverallStatus(
	                calculateOverallStatus(targetRecord));

	        log.info("Updated overall status : {}",
	                targetRecord.getOverallStatus());

	        repo.save(targetRecord);

	        log.info("Payment record updated successfully after session completion");

	        updateBookingStatus(targetRecord);

	        log.info("Booking status updated successfully");

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("updateSessionStatusFromTherapist() completed successfully in {} ms",
	                executionTime);

	    } catch (Exception e) {

	        log.error("Exception occurred while updating session status. therapistRecordId : {}, sessionId : {}, Error : {}",
	                therapistRecordId,
	                sessionId,
	                e.getMessage(),
	                e);

	        throw e;
	    }
	}
	// ========================================================
	// NORMALIZE PAYLOAD
	// ========================================================
	private List<TherapyWithSessions> normalizePayload(PaymentRequest req) {

	    String serviceType = req.getServiceType() != null
	            ? req.getServiceType().toLowerCase()
	            : "package";

	    log.info("Normalizing payload. ServiceType={}", serviceType);

	    List<TherapyWithSessions> incoming = req.getTherapyWithSessions();

	    if (incoming == null || incoming.isEmpty()) {
	        log.error("therapyWithSessions is null or empty");
	        throw new RuntimeException("therapyWithSessions is required");
	    }

	    log.info("Received {} therapyWithSessions records", incoming.size());

	    switch (serviceType) {

	    case "package":
	        log.info("Package level payload detected. Returning incoming payload directly");
	        return incoming;

	    case "program": {

	        log.info("Program level payload detected");

	        TherapyWithSessions dummyPackage = new TherapyWithSessions();
	        dummyPackage.setPackageId("PKG_AUTO");
	        dummyPackage.setPackageName("Auto Package");

	        List<Program> programs = new ArrayList<>();

	        for (TherapyWithSessions item : incoming) {

	            log.debug("Processing ProgramId={}, ProgramName={}",
	                    item.getProgramId(),
	                    item.getProgramName());

	            if (item.getProgramId() == null) {
	                log.warn("Skipping record because ProgramId is null");
	                continue;
	            }

	            Program prog = new Program();
	            prog.setProgramId(item.getProgramId());
	            prog.setProgramName(item.getProgramName());
	            prog.setTherapyData(item.getTherapyData());

	            programs.add(prog);

	            log.debug("Added ProgramId={} to normalized payload",
	                    item.getProgramId());
	        }

	        if (programs.isEmpty()) {
	            log.error("No valid program data found");
	            throw new RuntimeException("No valid program data found");
	        }

	        log.info("Program normalization completed. ProgramCount={}",
	                programs.size());

	        dummyPackage.setPrograms(programs);
	        return List.of(dummyPackage);
	    }

	    case "therapy": {

	        log.info("Therapy level payload detected");

	        TherapyWithSessions dummyPackage = new TherapyWithSessions();
	        dummyPackage.setPackageId("PKG_AUTO");
	        dummyPackage.setPackageName("Auto Package");

	        Program dummyProgram = new Program();
	        dummyProgram.setProgramId("PROG_AUTO");
	        dummyProgram.setProgramName("Auto Program");

	        List<TherapyData> therapyList = new ArrayList<>();

	        for (TherapyWithSessions item : incoming) {

	            log.debug("Processing TherapyId={}, TherapyName={}",
	                    item.getTherapyId(),
	                    item.getTherapyName());

	            if (item.getTherapyId() == null) {
	                log.warn("Skipping record because TherapyId is null");
	                continue;
	            }

	            TherapyData therapy = new TherapyData();
	            therapy.setTherapyId(item.getTherapyId());
	            therapy.setTherapyName(item.getTherapyName());
	            therapy.setExercises(item.getExercises());

	            therapyList.add(therapy);

	            log.debug("Added TherapyId={} to normalized payload",
	                    item.getTherapyId());
	        }

	        if (therapyList.isEmpty()) {
	            log.error("No valid therapy data found");
	            throw new RuntimeException("No valid therapy data found");
	        }

	        log.info("Therapy normalization completed. TherapyCount={}",
	                therapyList.size());

	        dummyProgram.setTherapyData(therapyList);
	        dummyPackage.setPrograms(List.of(dummyProgram));

	        return List.of(dummyPackage);
	    }

	    case "exercise": {

	        log.info("Exercise level payload detected");

	        TherapyWithSessions dummyPackage = new TherapyWithSessions();
	        dummyPackage.setPackageId("PKG_AUTO");
	        dummyPackage.setPackageName("Auto Package");

	        Program dummyProgram = new Program();
	        dummyProgram.setProgramId("PROG_AUTO");
	        dummyProgram.setProgramName("Auto Program");

	        TherapyData dummyTherapy = new TherapyData();
	        dummyTherapy.setTherapyId("THER_AUTO");
	        dummyTherapy.setTherapyName("Auto Therapy");

	        List<TherapyExercise> allExercises = new ArrayList<>();

	        for (TherapyWithSessions item : incoming) {

	            if (item.getExercises() != null) {

	                log.debug(
	                        "Adding {} exercises from TherapyId={}",
	                        item.getExercises().size(),
	                        item.getTherapyId());

	                allExercises.addAll(item.getExercises());
	            }
	        }

	        if (allExercises.isEmpty()) {
	            log.error("No valid exercise data found");
	            throw new RuntimeException("No valid exercise data found");
	        }

	        log.info("Exercise normalization completed. ExerciseCount={}",
	                allExercises.size());

	        dummyTherapy.setExercises(allExercises);
	        dummyProgram.setTherapyData(List.of(dummyTherapy));
	        dummyPackage.setPrograms(List.of(dummyProgram));

	        return List.of(dummyPackage);
	    }

	    default:
	        log.error("Invalid serviceType received={}", serviceType);
	        throw new RuntimeException("Invalid serviceType: " + serviceType);
	    }
	}
	
	private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {

	    log.info("Applying payment level");

	    if (req.getPaymentLevel() == null || req.getPaymentTarget() == null) {
	        log.warn("PaymentLevel or PaymentTarget is null. Skipping payment level processing");
	        return;
	    }

	    String level = req.getPaymentLevel().toUpperCase();
	    String status = getStatus(record);

	    log.info("PaymentLevel={}, Status={}", level, status);

	    switch (level) {

	    case "PACKAGE":
	        log.info("Updating package payment status");
	        updatePackageStatus(record,
	                req.getPaymentTarget().getPackageIds(),
	                status);
	        break;

	    case "PROGRAM":
	        log.info("Updating program payment status");
	        updateProgramStatus(record,
	                req.getPaymentTarget().getProgramIds(),
	                status);
	        break;

	    case "THERAPY":
	        log.info("Updating therapy payment status");
	        updateTherapyStatus(record,
	                req.getPaymentTarget().getTherapyIds(),
	                status);
	        break;

	    case "EXERCISE":
	        log.info("Updating exercise payment status");
	        updateExerciseStatus(record,
	                req.getPaymentTarget().getExerciseIds(),
	                status);
	        break;

	    case "SESSION":
	        log.info("Updating session payment status");
	        paySessions(record,
	                req.getPaymentTarget().getSessionIds());
	        break;

	    default:
	        log.warn("Unsupported payment level received={}", level);
	    }

	    log.info("Payment level processing completed");
	}
	
	private void distributePaymentToSessions(PaymentRecord record) {

	    log.info("Starting payment distribution to sessions");

	    if (record.getTherapyWithSessions() == null) {
	        log.warn("TherapyWithSessions is null. Skipping payment distribution");
	        return;
	    }

	    double remaining = record.getTotalPaid();
	    double totalAmount = record.getTotalAmount();
	    double finalAmount = record.getFinalAmount();

	    double discountRatio =
	            (totalAmount > 0)
	                    ? (finalAmount / totalAmount)
	                    : 1.0;

	    log.info(
	            "Distribution started. TotalPaid={}, TotalAmount={}, FinalAmount={}, DiscountRatio={}",
	            remaining,
	            totalAmount,
	            finalAmount,
	            discountRatio);

	    for (var pkg : record.getTherapyWithSessions()) {

	        log.debug("Processing PackageId={}", pkg.getPackageId());

	        if (pkg.getPrograms() == null)
	            continue;

	        for (var prog : pkg.getPrograms()) {

	            log.debug("Processing ProgramId={}",
	                    prog.getProgramId());

	            if (prog.getTherapyData() == null)
	                continue;

	            for (var therapy : prog.getTherapyData()) {

	                log.debug("Processing TherapyId={}",
	                        therapy.getTherapyId());

	                if (therapy.getExercises() == null)
	                    continue;

	                for (var ex : therapy.getExercises()) {

	                    if (ex.getSessions() == null)
	                        continue;

	                    double rawPrice =
	                            ex.getPricePerSession() != null
	                                    ? ex.getPricePerSession()
	                                    : 0;

	                    double effectivePrice =
	                            Math.round(rawPrice * discountRatio * 100.0) / 100.0;

	                    log.debug(
	                            "ExerciseId={}, RawPrice={}, EffectivePrice={}, SessionCount={}",
	                            ex.getExerciseId(),
	                            rawPrice,
	                            effectivePrice,
	                            ex.getSessions().size());

	                    for (var s : ex.getSessions()) {

	                        if (remaining + 0.01 >= effectivePrice) {

	                            s.setPaymentStatus("Paid");

	                            remaining -= effectivePrice;
	                            remaining =
	                                    Math.round(remaining * 100.0) / 100.0;

	                            log.debug(
	                                    "SessionId={} marked PAID. RemainingAmount={}",
	                                    s.getSessionId(),
	                                    remaining);

	                        } else {

	                            s.setPaymentStatus("Unpaid");

	                            log.debug(
	                                    "SessionId={} marked UNPAID. RemainingAmount={}",
	                                    s.getSessionId(),
	                                    remaining);
	                        }
	                    }
	                }
	            }
	        }
	    }

	    log.info(
	            "Payment distribution completed. RemainingAmount={}",
	            remaining);
	}
	
	// ========================================================
	// PACKAGE STATUS UPDATE
	// ========================================================
	// ========================================================
	// PACKAGE STATUS UPDATE
	// ========================================================
	private void updatePackageStatus(PaymentRecord record, List<String> ids, String status) {

	    log.info("Updating package status. Status={}, PackageIds={}", status, ids);

	    if (ids == null) {
	        log.warn("Package ids are null. Skipping package status update");
	        return;
	    }

	    for (var pkg : record.getTherapyWithSessions()) {

	        log.debug("Checking PackageId={}", pkg.getPackageId());

	        if (ids.contains(pkg.getPackageId())) {

	            log.info("Updating PackageId={} with status={}",
	                    pkg.getPackageId(),
	                    status);

	            pkg.setPaymentStatus(status);

	            for (var prog : pkg.getPrograms()) {

	                log.debug("Updating ProgramId={} under PackageId={}",
	                        prog.getProgramId(),
	                        pkg.getPackageId());

	                prog.setPaymentStatus(status);

	                for (var therapy : prog.getTherapyData()) {

	                    log.debug("Updating TherapyId={} under ProgramId={}",
	                            therapy.getTherapyId(),
	                            prog.getProgramId());

	                    therapy.setPaymentStatus(status);

	                    for (var ex : therapy.getExercises()) {

	                        log.debug("Updating ExerciseId={} under TherapyId={}",
	                                ex.getExerciseId(),
	                                therapy.getTherapyId());

	                        ex.setPaymentStatus(status);
	                    }
	                }
	            }
	        }
	    }

	    log.info("Package status update completed");
	}

	// ========================================================
	// PROGRAM STATUS UPDATE
	// ========================================================
	private void updateProgramStatus(PaymentRecord record, List<String> ids, String status) {

	    log.info("Updating program status. Status={}, ProgramIds={}", status, ids);

	    if (ids == null) {
	        log.warn("Program ids are null. Skipping program status update");
	        return;
	    }

	    for (var pkg : record.getTherapyWithSessions()) {

	        if (pkg.getPrograms() == null) {
	            log.debug("No programs found for PackageId={}", pkg.getPackageId());
	            continue;
	        }

	        for (var prog : pkg.getPrograms()) {

	            log.debug("Checking ProgramId={}", prog.getProgramId());

	            if (ids.contains(prog.getProgramId())) {

	                log.info("Updating ProgramId={} with status={}",
	                        prog.getProgramId(),
	                        status);

	                prog.setPaymentStatus(status);

	                for (var therapy : prog.getTherapyData()) {

	                    log.debug("Updating TherapyId={} under ProgramId={}",
	                            therapy.getTherapyId(),
	                            prog.getProgramId());

	                    therapy.setPaymentStatus(status);

	                    for (var ex : therapy.getExercises()) {

	                        log.debug("Updating ExerciseId={} under TherapyId={}",
	                                ex.getExerciseId(),
	                                therapy.getTherapyId());

	                        ex.setPaymentStatus(status);
	                    }
	                }
	            }
	        }
	    }

	    log.info("Program status update completed");
	}

	// ========================================================
	// THERAPY STATUS UPDATE
	// ========================================================
	private void updateTherapyStatus(PaymentRecord record, List<String> ids, String status) {

	    log.info("Updating therapy status. Status={}, TherapyIds={}", status, ids);

	    if (ids == null) {
	        log.warn("Therapy ids are null. Skipping therapy status update");
	        return;
	    }

	    for (var pkg : record.getTherapyWithSessions()) {

	        if (pkg.getPrograms() == null) {
	            continue;
	        }

	        for (var prog : pkg.getPrograms()) {

	            if (prog.getTherapyData() == null) {
	                continue;
	            }

	            for (var therapy : prog.getTherapyData()) {

	                log.debug("Checking TherapyId={}", therapy.getTherapyId());

	                if (ids.contains(therapy.getTherapyId())) {

	                    log.info("Updating TherapyId={} with status={}",
	                            therapy.getTherapyId(),
	                            status);

	                    therapy.setPaymentStatus(status);

	                    for (var ex : therapy.getExercises()) {

	                        log.debug("Updating ExerciseId={} under TherapyId={}",
	                                ex.getExerciseId(),
	                                therapy.getTherapyId());

	                        ex.setPaymentStatus(status);
	                    }
	                }
	            }
	        }
	    }

	    log.info("Therapy status update completed");
	}

	// ========================================================
	// EXERCISE STATUS UPDATE
	// ========================================================
	private void updateExerciseStatus(PaymentRecord record, List<String> ids, String status) {

	    log.info("Updating exercise status. Status={}, ExerciseIds={}", status, ids);

	    if (ids == null) {
	        log.warn("Exercise ids are null. Skipping exercise status update");
	        return;
	    }

	    for (var pkg : record.getTherapyWithSessions()) {

	        if (pkg.getPrograms() == null) {
	            continue;
	        }

	        for (var prog : pkg.getPrograms()) {

	            if (prog.getTherapyData() == null) {
	                continue;
	            }

	            for (var therapy : prog.getTherapyData()) {

	                if (therapy.getExercises() == null) {
	                    continue;
	                }

	                for (var ex : therapy.getExercises()) {

	                    log.debug("Checking ExerciseId={}", ex.getExerciseId());

	                    if (ids.contains(ex.getExerciseId())) {

	                        log.info("Updating ExerciseId={} with status={}",
	                                ex.getExerciseId(),
	                                status);

	                        ex.setPaymentStatus(status);
	                    }
	                }
	            }
	        }
	    }

	    log.info("Exercise status update completed");
	}

	// ========================================================
	// SESSION PAY
	// ========================================================
	private void paySessions(PaymentRecord record, List<String> ids) {

	    log.info("Processing session payment. SessionIds={}", ids);

	    if (ids == null) {
	        log.warn("Session ids are null. Skipping session payment");
	        return;
	    }

	    for (var pkg : record.getTherapyWithSessions()) {

	        if (pkg.getPrograms() == null) {
	            continue;
	        }

	        for (var prog : pkg.getPrograms()) {

	            if (prog.getTherapyData() == null) {
	                continue;
	            }

	            for (var therapy : prog.getTherapyData()) {

	                if (therapy.getExercises() == null) {
	                    continue;
	                }

	                for (var ex : therapy.getExercises()) {

	                    if (ex.getSessions() == null) {
	                        continue;
	                    }

	                    for (var s : ex.getSessions()) {

	                        log.debug("Checking SessionId={}", s.getSessionId());

	                        if (ids.contains(s.getSessionId())) {

	                            log.info("Marking SessionId={} as Paid",
	                                    s.getSessionId());

	                            s.setPaymentStatus("Paid");
	                        }
	                    }

	                    boolean allPaid = ex.getSessions()
	                            .stream()
	                            .allMatch(x -> "Paid".equalsIgnoreCase(x.getPaymentStatus()));

	                    ex.setPaymentStatus(allPaid ? "Paid" : "Unpaid");

	                    log.debug(
	                            "ExerciseId={} payment status updated to {}",
	                            ex.getExerciseId(),
	                            ex.getPaymentStatus());
	                }
	            }
	        }
	    }

	    log.info("Session payment processing completed");
	}
	// ========================================================
	// STATUS PROPAGATION
	// ========================================================
	// ========================================================
	// UPDATE HIERARCHY PAYMENT STATUSES
	// ========================================================
	private void updateStatuses(PaymentRecord record) {

	    log.info("Starting hierarchy payment status update");

	    if (record.getTherapyWithSessions() == null) {
	        log.warn("TherapyWithSessions is null. Skipping status update");
	        return;
	    }

	    for (var pkg : record.getTherapyWithSessions()) {

	        log.debug("Processing PackageId={}", pkg.getPackageId());

	        if (pkg.getPrograms() == null) {
	            log.debug("No programs found for PackageId={}", pkg.getPackageId());
	            continue;
	        }

	        for (var prog : pkg.getPrograms()) {

	            log.debug("Processing ProgramId={}", prog.getProgramId());

	            if (prog.getTherapyData() == null) {
	                log.debug("No therapies found for ProgramId={}", prog.getProgramId());
	                continue;
	            }

	            for (var therapy : prog.getTherapyData()) {

	                log.debug("Processing TherapyId={}", therapy.getTherapyId());

	                if (therapy.getExercises() == null) {
	                    log.debug("No exercises found for TherapyId={}", therapy.getTherapyId());
	                    continue;
	                }

	                for (var ex : therapy.getExercises()) {

	                    log.debug("Processing ExerciseId={}", ex.getExerciseId());

	                    if (ex.getSessions() == null || ex.getSessions().isEmpty()) {

	                        log.debug(
	                                "No sessions found for ExerciseId={}. Marking as Unpaid",
	                                ex.getExerciseId());

	                        ex.setPaymentStatus("Unpaid");
	                        continue;
	                    }

	                    boolean allPaid = ex.getSessions()
	                            .stream()
	                            .allMatch(s -> "Paid".equalsIgnoreCase(s.getPaymentStatus()));

	                    ex.setPaymentStatus(allPaid ? "Paid" : "Unpaid");

	                    log.debug(
	                            "ExerciseId={} PaymentStatus={}",
	                            ex.getExerciseId(),
	                            ex.getPaymentStatus());
	                }

	                boolean allTherapyPaid = therapy.getExercises()
	                        .stream()
	                        .allMatch(e -> "Paid".equalsIgnoreCase(e.getPaymentStatus()));

	                therapy.setPaymentStatus(allTherapyPaid ? "Paid" : "Unpaid");

	                log.debug(
	                        "TherapyId={} PaymentStatus={}",
	                        therapy.getTherapyId(),
	                        therapy.getPaymentStatus());
	            }

	            boolean allProgPaid = prog.getTherapyData()
	                    .stream()
	                    .allMatch(t -> "Paid".equalsIgnoreCase(t.getPaymentStatus()));

	            prog.setPaymentStatus(allProgPaid ? "Paid" : "Unpaid");

	            log.debug(
	                    "ProgramId={} PaymentStatus={}",
	                    prog.getProgramId(),
	                    prog.getPaymentStatus());
	        }

	        boolean allPkgPaid = pkg.getPrograms()
	                .stream()
	                .allMatch(p -> "Paid".equalsIgnoreCase(p.getPaymentStatus()));

	        pkg.setPaymentStatus(allPkgPaid ? "Paid" : "Unpaid");

	        log.debug(
	                "PackageId={} PaymentStatus={}",
	                pkg.getPackageId(),
	                pkg.getPaymentStatus());
	    }

	    log.info("Hierarchy payment status update completed");
	}

	// ========================================================
	// CALCULATE OVERALL STATUS
	// ========================================================
	private String calculateOverallStatus(PaymentRecord record) {

	    log.info("Calculating overall status for PaymentRecord");

	    if (record.getTherapyWithSessions() == null) {
	        log.warn("TherapyWithSessions is null. Returning Pending");
	        return "Pending";
	    }

	    boolean allCompleted = true;
	    boolean anyCompleted = false;

	    for (var pkg : record.getTherapyWithSessions()) {

	        if (pkg.getPrograms() == null)
	            continue;

	        for (var prog : pkg.getPrograms()) {

	            if (prog.getTherapyData() == null)
	                continue;

	            for (var therapy : prog.getTherapyData()) {

	                if (therapy.getExercises() == null)
	                    continue;

	                for (var ex : therapy.getExercises()) {

	                    if (ex.getSessions() == null)
	                        continue;

	                    for (var s : ex.getSessions()) {

	                        log.debug(
	                                "SessionId={}, SessionStatus={}",
	                                s.getSessionId(),
	                                s.getStatus());

	                        if ("Completed".equalsIgnoreCase(s.getStatus())) {

	                            anyCompleted = true;

	                        } else {

	                            allCompleted = false;
	                        }
	                    }
	                }
	            }
	        }
	    }

	    String overallStatus;

	    if (allCompleted && anyCompleted) {
	        overallStatus = "Completed";
	    } else if (anyCompleted) {
	        overallStatus = "Active";
	    } else {
	        overallStatus = "Pending";
	    }

	    log.info(
	            "Overall status calculated. anyCompleted={}, allCompleted={}, overallStatus={}",
	            anyCompleted,
	            allCompleted,
	            overallStatus);

	    return overallStatus;
	}

	// ========================================================
	// UPDATE BOOKING STATUS
	// ========================================================
	private void updateBookingStatus(PaymentRecord record) {

	    log.info(
	            "Updating booking status. BookingId={}, OverallStatus={}",
	            record.getBookingId(),
	            record.getOverallStatus());

	    if (record.getBookingId() == null
	            || record.getBookingId().trim().isEmpty()) {

	        log.warn("BookingId is null or empty. Skipping booking status update");
	        return;
	    }

	    try {

	        BookingResponse request = new BookingResponse();
	        request.setBookingId(record.getBookingId().trim());

	        if ("Completed".equalsIgnoreCase(record.getOverallStatus())) {

	            request.setStatus("completed");

	        } else if ("Active".equalsIgnoreCase(record.getOverallStatus())) {

	            request.setStatus("in-progress");

	        } else {

	            request.setStatus("pending");
	        }

	        log.info(
	                "Calling clinic-admin updateAppointment. BookingId={}, Status={}",
	                request.getBookingId(),
	                request.getStatus());

	        clinicAdminFeign.updateAppointment(request);

	        log.info(
	                "Booking status updated successfully. BookingId={}, Status={}",
	                request.getBookingId(),
	                request.getStatus());

	    } catch (Exception e) {

	        log.error(
	                "Booking status update failed. BookingId={}, Error={}",
	                record.getBookingId(),
	                e.getMessage(),
	                e);
	    }
	}
	// ========================================================
	// MAP TO RESPONSE
	// ========================================================
	private PaymentRecordResponse mapToResponse(PaymentRecord record) {

		PaymentRecordResponse res = new PaymentRecordResponse();

		res.setId(record.getId());
		res.setPayAfterService(record.isPayAfterService());
		res.setClinicId(record.getClinicId());
		res.setBranchId(record.getBranchId());
		res.setBookingId(record.getBookingId());
		res.setPatientId(record.getPatientId());
		res.setDoctorId(record.getDoctorId());
		res.setDoctorName(record.getDoctorName());
		res.setTherapistId(record.getTherapistId());
		res.setTherapistName(record.getTherapistName());
		res.setTherapistRecordId(record.getTherapistRecordId());
		res.setTreatmentName(record.getTreatmentName());
		res.setServiceType(record.getServiceType());
		res.setOverallStatus(record.getOverallStatus());
		res.setTotalAmount(record.getTotalAmount());
		res.setDiscountAmount(record.getDiscountAmount());
		res.setFinalAmount(record.getFinalAmount());
		res.setTotalPaid(record.getTotalPaid());
		res.setBalanceAmount(record.getBalanceAmount());
		res.setPaymentStatus(record.getPaymentStatus());
		res.setSessionStartDate(record.getSessionStartDate());
		res.setSessionEndDate(record.getSessionEndDate());
		res.setTotalSessionCount(record.getTotalSessionCount());
		res.setNoOfSessionCompletedCount(record.getNoOfSessionCompletedCount());
		res.setNoOfSessionCompletedStatus(record.isNoOfSessionCompletedStatus());
		res.setSessionTableCreatedStatus(record.isSessionTableCreatedStatus());
		res.setOverallReceiptNumber(record.getOverallReceiptNumber());
		res.setPaymentHistory(record.getPaymentHistory());

		String serviceType = record.getServiceType() != null ? record.getServiceType().toLowerCase() : "package";

		switch (serviceType) {
		case "package":
			res.setTherapyWithSessions(mapPackages(record));
			break;
		case "program":
			res.setTherapyWithSessions(mapPrograms(record));
			break;
		case "therapy":
			res.setTherapyWithSessions(mapTherapies(record));
			break;
		case "exercise":
			res.setTherapyWithSessions(mapExercises(record));
			break;
		default:
			res.setTherapyWithSessions(record.getTherapyWithSessions());
		}

		return res;
	}

	// ========================================================
	// PACKAGE MAPPER
	// ========================================================
	private List<PackageResponse> mapPackages(PaymentRecord record) {
		List<PackageResponse> result = new ArrayList<>();
		if (record.getTherapyWithSessions() == null)
			return result;
		for (var pkg : record.getTherapyWithSessions()) {
			PackageResponse p = new PackageResponse();
			p.setPackageId(pkg.getPackageId());
			p.setPackageName(pkg.getPackageName());
			p.setTotalPackagePrice(pkg.getTotalPackagePrice());
			p.setPaymentStatus(pkg.getPaymentStatus());
			p.setPrograms(mapProgramList(pkg.getPrograms()));
			result.add(p);
		}
		return result;
	}

	// ========================================================
	// PROGRAM MAPPER
	// ========================================================
	private List<ProgramResponse> mapPrograms(PaymentRecord record) {
		List<ProgramResponse> result = new ArrayList<>();
		if (record.getTherapyWithSessions() == null)
			return result;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() != null) {
				result.addAll(mapProgramList(pkg.getPrograms()));
			}
		}
		return result;
	}

	// ========================================================
	// THERAPY MAPPER
	// ========================================================
	private List<TherapyResponse> mapTherapies(PaymentRecord record) {
		List<TherapyResponse> result = new ArrayList<>();
		if (record.getTherapyWithSessions() == null)
			return result;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() != null) {
					result.addAll(mapTherapyList(prog.getTherapyData()));
				}
			}
		}
		return result;
	}

	// ========================================================
	// EXERCISE MAPPER
	// ========================================================
	private List<ExerciseResponse> mapExercises(PaymentRecord record) {
		List<ExerciseResponse> result = new ArrayList<>();
		if (record.getTherapyWithSessions() == null)
			return result;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() != null) {
						result.addAll(mapExerciseList(therapy.getExercises()));
					}
				}
			}
		}
		return result;
	}

	// ========================================================
	// SHARED LIST MAPPERS
	// ========================================================
	private List<ProgramResponse> mapProgramList(List<Program> programs) {
		List<ProgramResponse> result = new ArrayList<>();
		if (programs == null)
			return result;
		for (var prog : programs) {
			ProgramResponse p = new ProgramResponse();
			p.setProgramId(prog.getProgramId());
			p.setProgramName(prog.getProgramName());
			p.setTotalProgramPrice(prog.getTotalProgramPrice());
			p.setPaymentStatus(prog.getPaymentStatus());
			p.setTherapyData(mapTherapyList(prog.getTherapyData()));
			result.add(p);
		}
		return result;
	}

	private List<TherapyResponse> mapTherapyList(List<TherapyData> therapies) {
		List<TherapyResponse> result = new ArrayList<>();
		if (therapies == null)
			return result;
		for (var t : therapies) {
			TherapyResponse tr = new TherapyResponse();
			tr.setTherapyId(t.getTherapyId());
			tr.setTherapyName(t.getTherapyName());
			tr.setTotalTherapyPrice(t.getTotalTherapyPrice());
			tr.setPaymentStatus(t.getPaymentStatus());
			tr.setExercises(mapExerciseList(t.getExercises()));
			result.add(tr);
		}
		return result;
	}

	private List<ExerciseResponse> mapExerciseList(List<TherapyExercise> exercises) {
		List<ExerciseResponse> result = new ArrayList<>();
		if (exercises == null)
			return result;
		for (TherapyExercise ex : exercises) {
			ExerciseResponse er = new ExerciseResponse();
			er.setExerciseId(ex.getExerciseId());
			er.setExerciseName(ex.getExerciseName());
			er.setPricePerSession(ex.getPricePerSession());
			er.setNoOfSessions(ex.getNoOfSessions());
			er.setDiscountPercentage(ex.getDiscountPercentage());
			er.setDiscountAmount(ex.getDiscountAmount());
			er.setGst(ex.getGst());
			er.setOtherTax(ex.getOtherTax());
			er.setTotalExercisePrice(ex.getTotalExercisePrice());
			er.setTotalPrice(ex.getTotalPrice());
			er.setPaymentStatus(ex.getPaymentStatus());
			er.setRepetitions(ex.getRepetitions());
			er.setFrequency(ex.getFrequency());
			er.setSets(ex.getSets());
			er.setYoutubeUrl(ex.getYoutubeUrl());
			er.setNotes(ex.getNotes());
			er.setTechnique(ex.getTechnique());
			er.setMachine(ex.getMachine());
			er.setIntensity(ex.getIntensity());
			er.setAssistanceLevel(ex.getAssistanceLevel());
			er.setType(ex.getType());
			er.setArea(ex.getArea());
			er.setMetric(ex.getMetric());
			er.setValue(ex.getValue());
			er.setUnit(ex.getUnit());
			er.setBodyPart(ex.getBodyPart());
			er.setActivityType(ex.getActivityType());
			er.setActivityDuration(ex.getActivityDuration());
			er.setSessions(ex.getSessions());
			result.add(er);
		}
		return result;
	}

	// ========================================================
	// UTILS
	// ========================================================
	private String getStatus(PaymentRecord r) {
		if (r.getTotalPaid() <= 0)
			return "Unpaid";
		if (r.getTotalPaid() < r.getFinalAmount())
			return "Partial";
		if (Double.compare(r.getTotalPaid(), r.getFinalAmount()) == 0)
			return "Paid";
		return "Overpaid";
	}

	private PaymentHistory buildHistory(PaymentRequest req, String receiptNumber) {

		PaymentHistory history = new PaymentHistory();

		history.setAmount(req.getAmount());
		history.setPaymentMode(req.getPaymentMode());
		history.setPaymentType(req.getPaymentType());
		history.setPaymentDate(req.getPaymentDate());
		history.setPaymentLevel(req.getPaymentLevel());
		history.setDiscountAmount(req.getDiscountAmount());
		history.setDiscountIssuedBy(req.getDiscountIssuedBy());
		history.setReceiptNumber(receiptNumber);

		return history;
	}

	private String generateOverallReceiptNumber() {

		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

		return "OVR-" + date + "-" + random;
	}

	private String generateReceiptNumber() {

		String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

		String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

		return "PAY-" + dateTime + "-" + random;
	}

	// ========================================================
	// CALCULATE TOTAL
	// ========================================================
	private double calculateTotal(List<TherapyWithSessions> data) {

		double total = 0;

		for (var pkg : data) {
			double pkgTotal = 0;
			for (var prog : pkg.getPrograms()) {
				double progTotal = 0;
				for (var therapy : prog.getTherapyData()) {
					double therapyTotal = 0;
					for (var ex : therapy.getExercises()) {
						double exTotal;
						if (ex.getTotalExercisePrice() != null && ex.getTotalExercisePrice() > 0) {
							exTotal = ex.getTotalExercisePrice();
						} else {
							exTotal = ex.getPricePerSession() * ex.getNoOfSessions();
							ex.setTotalExercisePrice(Double.valueOf(exTotal));
						}
						therapyTotal += exTotal;
					}
					therapy.setTotalTherapyPrice(Double.valueOf(therapyTotal));
					progTotal += therapyTotal;
				}
				prog.setTotalProgramPrice(Double.valueOf(progTotal));
				pkgTotal += progTotal;
			}
			pkg.setTotalPackagePrice(Double.valueOf(pkgTotal));
			total += pkgTotal;
		}

		return total;
	}

	// ========================================================
	// COUNT TOTAL SESSIONS
	// ========================================================
	private int countTotalSessions(PaymentRecord record) {
		int count = 0;
		if (record.getTherapyWithSessions() == null)
			return count;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() == null)
						continue;
					for (var ex : therapy.getExercises()) {
						if (ex.getSessions() != null) {
							count += ex.getSessions().size();
						}
					}
				}
			}
		}
		return count;
	}

	// ========================================================
	// CREATE SESSIONS
	// ========================================================
	private boolean createSessions(List<TherapyWithSessions> data, String startDate) {

		boolean created = false;

		for (var pkg : data) {
			for (var prog : pkg.getPrograms()) {
				for (var therapy : prog.getTherapyData()) {
					for (var ex : therapy.getExercises()) {

						List<Session> sessions = new ArrayList<>();
						LocalDate currentDate = LocalDate.parse(startDate);
						int noOfSessions = ex.getNoOfSessions();
						String freqType = parseFrequencyType(ex.getFrequency());
						int sessionNo = 1;

						if (freqType.equals("day")) {
							for (int i = 1; i <= noOfSessions; i++) {
								sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
								sessionNo++;
								currentDate = currentDate.plusDays(1);
							}
						} else if (freqType.equals("week")) {
							int gapDays = noOfSessions > 0 ? 7 / noOfSessions : 1;
							for (int i = 1; i <= noOfSessions; i++) {
								sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
								sessionNo++;
								currentDate = currentDate.plusDays(gapDays);
							}
						} else if (freqType.equals("month")) {
							int gapDays = noOfSessions > 0 ? 30 / noOfSessions : 1;
							for (int i = 1; i <= noOfSessions; i++) {
								sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
								sessionNo++;
								currentDate = currentDate.plusDays(gapDays);
							}
						}

						if (!sessions.isEmpty())
							created = true;
						ex.setSessions(sessions);
					}
				}
			}
		}

		return created;
	}

	// ========================================================
	// BUILD SESSION HELPER
	// ========================================================
	private Session buildSession(String exerciseId, int sessionNo, LocalDate date) {
		String uniqueSessionId = exerciseId + "_" + sessionNo + "_"
				+ UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		return new Session(uniqueSessionId, Integer.valueOf(sessionNo), date.toString(), "Pending", "Unpaid");
	}

	// ========================================================
	// PARSE FREQUENCY TYPE
	// ========================================================
	private String parseFrequencyType(String frequency) {
		if (frequency == null || frequency.trim().isEmpty()) {
			return "day";
		}
		String lower = frequency.toLowerCase().trim();
		if (lower.contains("month"))
			return "month";
		if (lower.contains("week"))
			return "week";
		return "day";
	}

	// ========================================================
	// COUNT COMPLETED
	// ========================================================
	private int countCompleted(PaymentRecord record) {
		int count = 0;
		if (record.getTherapyWithSessions() == null)
			return count;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() == null)
						continue;
					for (var ex : therapy.getExercises()) {
						if (ex.getSessions() == null)
							continue;
						for (var s : ex.getSessions()) {
							if ("Completed".equalsIgnoreCase(s.getStatus())) {
								count++;
							}
						}
					}
				}
			}
		}
		return count;
	}

	// ========================================================
	// GET EXERCISE SESSIONS WITH RECORDS
	// ========================================================
	@Override
	@Secured({"ROLE_DOCTOR","ROLE_CUSTOMER"})
	public Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId,
	        String patientId, String therapistId, String therapistRecordId) {

	    log.info(
	            "Fetching exercise sessions. clinicId={}, branchId={}, bookingId={}, patientId={}, therapistId={}, therapistRecordId={}",
	            clinicId, branchId, bookingId, patientId, therapistId, therapistRecordId);

	    Response response = new Response();

	    try {

	        PaymentRecord record = repo
	                .findByClinicIdAndBranchIdAndBookingIdAndPatientIdAndTherapistIdAndTherapistRecordId(
	                        clinicId, branchId, bookingId, patientId, therapistId, therapistRecordId)
	                .get();

	        log.info("Payment record fetched successfully for bookingId={}", bookingId);

	        List<Object> exerciseList = new ArrayList<>();

	        if (record != null) {

	            for (TherapyWithSessions pkg : record.getTherapyWithSessions()) {

	                log.debug("Processing packageId={}", pkg.getPackageId());

	                if (pkg.getPrograms() == null)
	                    continue;

	                for (Program program : pkg.getPrograms()) {

	                    log.debug("Processing programId={}", program.getProgramId());

	                    if (program.getTherapyData() == null)
	                        continue;

	                    for (TherapyData therapy : program.getTherapyData()) {

	                        log.debug("Processing therapyId={}", therapy.getTherapyId());

	                        if (therapy.getExercises() == null)
	                            continue;

	                        for (TherapyExercise exercise : therapy.getExercises()) {

	                            log.debug(
	                                    "Processing exerciseId={}, exerciseName={}",
	                                    exercise.getExerciseId(),
	                                    exercise.getExerciseName());

	                            List<Object> sessionList = new ArrayList<>();

	                            if (exercise.getSessions() == null)
	                                continue;

	                            for (Session session : exercise.getSessions()) {

	                                log.debug(
	                                        "Processing sessionId={}, sessionNo={}",
	                                        session.getSessionId(),
	                                        session.getSessionNo());

	                                Map<String, Object> map = new LinkedHashMap<>();
	                                map.put("sessionId", session.getSessionId());
	                                map.put("sessionNo", session.getSessionNo());
	                                map.put("date", session.getDate());
	                                map.put("paymentStatus", session.getPaymentStatus());

	                                try {

	                                    ResponseEntity<ResponseStructure<TherapistRecordDTO>> tr =
	                                            clinicAdminFeign.getRecordBySession(
	                                                    clinicId,
	                                                    branchId,
	                                                    bookingId,
	                                                    patientId,
	                                                    session.getSessionId());

	                                    if (tr != null
	                                            && tr.getBody() != null
	                                            && tr.getBody().getData() != null) {

	                                        log.debug(
	                                                "Therapist record found for sessionId={}",
	                                                session.getSessionId());

	                                        map.put("status", "Completed");

	                                    } else {

	                                        log.debug(
	                                                "Therapist record not found for sessionId={}",
	                                                session.getSessionId());

	                                        map.put("status", session.getStatus());
	                                    }

	                                } catch (Exception e) {

	                                    log.error(
	                                            "Error fetching therapist record for sessionId={}. Error={}",
	                                            session.getSessionId(),
	                                            e.getMessage());

	                                    map.put("status", session.getStatus());
	                                }

	                                sessionList.add(map);
	                            }

	                            Map<String, Object> exerciseData = new LinkedHashMap<>();
	                            exerciseData.put("exerciseId", exercise.getExerciseId());
	                            exerciseData.put("exerciseName", exercise.getExerciseName());
	                            exerciseData.put("sessions", sessionList);

	                            exerciseList.add(exerciseData);
	                        }
	                    }
	                }
	            }

	            response.setSuccess(true);
	            response.setStatus(200);
	            response.setMessage("All exercises fetched successfully");
	            response.setData(exerciseList);

	            log.info("Successfully fetched {} exercises", exerciseList.size());

	        } else {

	            log.warn(
	                    "Payment record not found. bookingId={}, patientId={}",
	                    bookingId,
	                    patientId);

	            response.setSuccess(false);
	            response.setStatus(200);
	            response.setMessage("Record not found");
	        }

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching exercise sessions. bookingId={}, error={}",
	                bookingId,
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setStatus(500);
	        response.setMessage(e.getMessage());
	    }

	    log.info("Completed getExerciseSessionsWithRecords for bookingId={}", bookingId);

	    return response;
	}


	@Override
	@Secured({"ROLE_DOCTOR","ROLE_CLINICADMIN"})
	public int getTodaySessionCount(String clinicId,
	                                String branchId,
	                                String therapistId) {

	    log.info(
	            "Calculating today's session count. clinicId={}, branchId={}, therapistId={}",
	            clinicId,
	            branchId,
	            therapistId);

		try {

			List<PaymentRecord> records = repo.findByClinicIdAndBranchIdAndTherapistId(clinicId, branchId, therapistId);

			if (records == null || records.isEmpty()) {

	            log.warn(
	                    "No payment records found for therapistId={}",
	                    therapistId);

	            return 0;
	        }

	        log.info("Found {} payment records", records.size());

	        String today = LocalDate.now()
	                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

			int todaySessionCount = 0;

			for (PaymentRecord record : records) {

	            log.debug("Processing paymentRecordId={}",
	                    record.getId());

	            if (record.getTherapyWithSessions() == null)
	                continue;

				for (TherapyWithSessions pkg : record.getTherapyWithSessions()) {

					if (pkg.getPrograms() == null)
						continue;

					for (Program program : pkg.getPrograms()) {

						if (program.getTherapyData() == null)
							continue;

						for (TherapyData therapy : program.getTherapyData()) {

							if (therapy.getExercises() == null)
								continue;

							for (TherapyExercise exercise : therapy.getExercises()) {

								if (exercise.getSessions() == null)
									continue;

								for (Session session : exercise.getSessions()) {

	                                if (today.equals(session.getDate())) {

	                                    todaySessionCount++;

	                                    log.debug(
	                                            "Today's session found. sessionId={}, date={}",
	                                            session.getSessionId(),
	                                            session.getDate());
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        }

	        log.info(
	                "Today's session count for therapistId={} is {}",
	                therapistId,
	                todaySessionCount);

	        return todaySessionCount;


		} catch (Exception e) {

	        log.error(
	                "Error while calculating today's session count. therapistId={}, error={}",
	                therapistId,
	                e.getMessage(),
	                e);

	        return 0;
	    }
	}
	// ========================================================
	// GET COMPLETED THERAPY RECORD
	// ========================================================
	@Override
	@Secured("ROLE_DOCTOR")
	public Response getCompletedTherapyRecord(String clinicId,
	                                          String branchId,
	                                          String therapistRecordId,
	                                          String sessionId) {


	    log.info(
	            "Fetching completed therapy record. clinicId={}, branchId={}, therapistRecordId={}, sessionId={}",
	            clinicId,
	            branchId,
	            therapistRecordId,
	            sessionId);

	    Response response = new Response();

	    try {

	        ResponseEntity<ResponseStructure<TherapistRecordDTO>> tr =
	                clinicAdminFeign.getCompletedTherapyRecord(
	                        clinicId,
	                        branchId,
	                        therapistRecordId,
	                        sessionId);

	        log.debug(
	                "Received response from clinic-admin for therapistRecordId={}",
	                therapistRecordId);

	        if (tr != null
	                && tr.getBody() != null
	                && tr.getBody().getData() != null) {

	            log.info(
	                    "Completed therapy record found for therapistRecordId={}",
	                    therapistRecordId);

	            TherapistRecordDTO dto = tr.getBody().getData();

	            log.debug("Refreshing signed URLs for therapy record attachments");

	            dto.setConsentPdfUrl(refreshSignedUrl(dto.getConsentPdfUrl()));
	            dto.setBeforeImage(refreshSignedUrl(dto.getBeforeImage()));
	            dto.setAfterImage(refreshSignedUrl(dto.getAfterImage()));
	            dto.setBeforeVideo(refreshSignedUrl(dto.getBeforeVideo()));
	            dto.setAfterVideo(refreshSignedUrl(dto.getAfterVideo()));
	            dto.setVoiceRecord(refreshSignedUrl(dto.getVoiceRecord()));

	            response.setSuccess(true);
	            response.setStatus(200);
	            response.setMessage("Therapy record fetched successfully");
	            response.setData(dto);

	            log.info(
	                    "Therapy record fetched successfully. therapistRecordId={}",
	                    therapistRecordId);

	        } else {

	            log.warn(
	                    "Therapy record not found. therapistRecordId={}, sessionId={}",
	                    therapistRecordId,
	                    sessionId);

	            response.setSuccess(false);
	            response.setStatus(404);
	            response.setMessage("Therapy record not found");
	        }

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching completed therapy record. therapistRecordId={}, error={}",
	                therapistRecordId,
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setStatus(500);
	        response.setMessage(e.getMessage());
	    }

	    log.info(
	            "Completed getCompletedTherapyRecord. therapistRecordId={}",
	            therapistRecordId);

	    return response;
	}

	private String refreshSignedUrl(String signedUrl) {

	    log.debug("Refreshing signed URL");

	    if (signedUrl == null || signedUrl.isBlank()) {

	        log.debug("Signed URL is null or blank");
	        return signedUrl;
	    }

	    try {

	        String fileKey = extractKey(signedUrl);

	        log.debug("Extracted fileKey={}", fileKey);

	        ResponseEntity<String> result =
	                clinicAdminFeign.getSignedUrl(fileKey);

	        if (result != null && result.getBody() != null) {

	            log.debug("Successfully refreshed signed URL for fileKey={}", fileKey);

	            return result.getBody();
	        }

	        log.warn(
	                "Signed URL refresh response is empty for fileKey={}",
	                fileKey);

	    } catch (Exception e) {

	        log.error(
	                "Failed to refresh signed URL. error={}",
	                e.getMessage(),
	                e);
	    }

	    return signedUrl;

	}

	private String extractKey(String signedUrl) {

	    try {

	        String withoutQuery = signedUrl.split("\\?")[0];

	        java.net.URI uri = new java.net.URI(withoutQuery);

	        String path = uri.getPath();

	        String key =
	                path.startsWith("/")
	                        ? path.substring(1)
	                        : path;

	        log.debug("Extracted file key={}", key);

	        return key;

	    } catch (Exception e) {

	        log.error(
	                "Failed to extract key from signed URL. error={}",
	                e.getMessage());

	        return signedUrl;
	    }
	}

	// ========================================================
	// FIND BY CLINIC AND BRANCH
	// ========================================================
	@Override
	@Secured({"ROLE_DOCTOR","ROLE_CLINICADMIN"})
	@RateLimiter(
	        name = "physiotherapydoctorService",
	        fallbackMethod = "getExerciseSessionsWithRecordsFallback")
	public List<PaymentRecordResponse> findByClinicIdAndBranchId(
	        String clinicId,
	        String branchId) {
	    log.info(
	            "Fetching payment records. clinicId={}, branchId={}",
	            clinicId,
	            branchId);

	    try {

	        List<PaymentRecord> records =
	                repo.findByClinicIdAndBranchId(clinicId, branchId);

	        log.info(
	                "Total payment records found={}",
	                records.size());

	        List<PaymentRecordResponse> response =
	                records.stream()
	                        .map(this::mapToResponse)
	                        .toList();

	        log.info(
	                "Successfully mapped {} payment records",
	                response.size());

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching payment records. clinicId={}, branchId={}, error={}",
	                clinicId,
	                branchId,
	                e.getMessage(),
	                e);

	        throw e;
	    }
	}

	private String getLastSessionDate(List<TherapyWithSessions> data) {

	    log.debug("Calculating last session date");

	    if (data == null || data.isEmpty()) {

	        log.debug("TherapyWithSessions data is null or empty");

	        return null;
	    }

	    LocalDate lastDate = null;

	    for (var pkg : data) {

	        log.debug("Processing packageId={}", pkg.getPackageId());

	        if (pkg.getPrograms() == null)
	            continue;

	        for (var prog : pkg.getPrograms()) {

	            log.debug("Processing programId={}", prog.getProgramId());

	            if (prog.getTherapyData() == null)
	                continue;

	            for (var therapy : prog.getTherapyData()) {

	                log.debug("Processing therapyId={}", therapy.getTherapyId());

	                if (therapy.getExercises() == null)
	                    continue;

	                for (var ex : therapy.getExercises()) {

	                    log.debug("Processing exerciseId={}", ex.getExerciseId());

	                    if (ex.getSessions() == null || ex.getSessions().isEmpty())
	                        continue;

	                    Session lastSession =
	                            ex.getSessions().get(ex.getSessions().size() - 1);

	                    LocalDate sessionDate =
	                            LocalDate.parse(lastSession.getDate());

	                    log.debug(
	                            "Last session found. sessionId={}, sessionDate={}",
	                            lastSession.getSessionId(),
	                            sessionDate);

	                    if (lastDate == null || sessionDate.isAfter(lastDate)) {

	                        lastDate = sessionDate;

	                        log.debug(
	                                "Updated latest session date={}",
	                                lastDate);
	                    }
	                }
	            }
	        }
	    }

	    String result =
	            lastDate != null
	                    ? lastDate.toString()
	                    : null;

	    log.debug(
	            "Last session date calculation completed. result={}",
	            result);

	    return result;
	}

	public PaymentRecordResponse createPaymentFallback(PaymentRequest req, Exception ex) {

		throw new RuntimeException("physiotherapydoctorService is temporarily unavailable", ex);
	}

	public PaymentRecordResponse updatePaymentFallback(PaymentRequest req, Exception ex) {

		throw new RuntimeException("physiotherapydoctorService is temporarily unavailable", ex);
	}

	public PaymentRecordResponse getByBookingIdFallback(String bookingId, Exception ex) {

		throw new RuntimeException("physiotherapydoctorService is temporarily unavailable", ex);
	}

	public void deleteByBookingIdFallback(String bookingId, Exception ex) {

		throw new RuntimeException("physiotherapydoctorService is temporarily unavailable", ex);
	}

	public void updateSessionStatusFromTherapistFallback(String therapistRecordId, String sessionId, Exception ex) {

		throw new RuntimeException("physiotherapydoctorService is temporarily unavailable", ex);
	}

	public Response getExerciseSessionsWithRecordsFallback(String clinicId, String branchId, String bookingId,
			String patientId, String therapistId, String therapistRecordId, Exception ex) {

		return buildRateLimitResponse(ex);
	}

	public int getTodaySessionCountFallback(String clinicId, String branchId, String therapistId, Exception ex) {

		throw new RuntimeException("physiotherapydoctorService is temporarily unavailable", ex);
	}

	public Response getCompletedTherapyRecordFallback(String clinicId, String branchId, String therapistRecordId,
			String sessionId, Exception ex) {

		return buildRateLimitResponse(ex);
	}

	public List<PaymentRecordResponse> findByClinicIdAndBranchIdFallback(String clinicId, String branchId,
			Exception ex) {

		throw new RuntimeException("physiotherapydoctorService is temporarily unavailable", ex);
	}

	private Response buildRateLimitResponse(Exception ex) {
		Response response = new Response();
		response.setSuccess(false);
		response.setStatus(429);
		response.setMessage("Rate limit exceeded. Please try again later.");
		response.setData(null);
		return response;
	}


	@Override
	public ResponseEntity<RevenueResponse> getRevenueManagement(
			String clinicId,
			String branchId,
			String number) {

		try {

			List<PaymentRecord> payments =
					repo.findByClinicIdAndBranchId(
							clinicId,
							branchId);
			
			List<PhysiotherapyRecord> records = physiotherapydoctorRespository.
					findByClinicIdAndBranchId(clinicId, branchId);

			LocalDate today = LocalDate.now();

			if (StringUtils.hasText(number)) {

				switch (number) {

					case "1":

						payments = payments.stream()
								.filter(p -> LocalDate.parse(
												p.getSessionStartDate())
										.isEqual(today))
								.toList();
						
						 records = records.stream().filter(n->LocalDate.parse(n.getCreatedAt()).isEqual(today)).toList();

						break;

					case "2":

						LocalDate weekStart =
								today.minusDays(7);

						payments = payments.stream()
								.filter(p -> {
									LocalDate date =
											LocalDate.parse(
													p.getSessionStartDate());

									return !date.isBefore(weekStart)
											&& !date.isAfter(today);
								})
								.toList();
						
						 records = records.stream().filter(n->{LocalDate date = LocalDate.parse(n.getCreatedAt());
								return !date.isBefore(weekStart)&& !date.isAfter(today); }).toList();					

						break;

					case "3":

						LocalDate monthStart =
								today.minusMonths(1);

						payments = payments.stream()
								.filter(p -> {
									LocalDate date =
											LocalDate.parse(
													p.getSessionStartDate());

									return !date.isBefore(monthStart)
											&& !date.isAfter(today);
								})
								.toList();
						 records = records.stream().filter(n->{LocalDate date = LocalDate.parse(n.getCreatedAt());
							return !date.isBefore(monthStart)&& !date.isAfter(today); }).toList();					

						break;

					case "4":

						LocalDate yearStart =
								today.minusYears(1);

						payments = payments.stream()
								.filter(p -> {
									LocalDate date =
											LocalDate.parse(
													p.getSessionStartDate());

									return !date.isBefore(yearStart)
											&& !date.isAfter(today);
									
								})
								.toList();
						records = records.stream().filter(n->{LocalDate date = LocalDate.parse(n.getCreatedAt());
						return !date.isBefore(yearStart)&& !date.isAfter(today); }).toList();					

						break;

					default:
						break;
				}
			}

			List<RevenueManagementDTO> responseData =
					prepareRevenueResponse(payments,records);
			Double totalFinalAmount = responseData.stream()
			        .map(RevenueManagementDTO::getFinalAmount)
			        .filter(Objects::nonNull)
			        .mapToDouble(Double::doubleValue)
			        .sum();
			Double totalConsultationFee = responseData.stream()
			        .map(RevenueManagementDTO::getConsultationFee)
			        .filter(Objects::nonNull)
			        .mapToDouble(Double::doubleValue)
			        .sum();
			Double totalTherapyFee = responseData.stream()
			        .map(RevenueManagementDTO::getTherapyFee)
			        .filter(Objects::nonNull)
			        .mapToDouble(Double::doubleValue)
			        .sum();
			Double totalDueAmount = responseData.stream()
			        .map(RevenueManagementDTO::getDueAmount)
			        .filter(Objects::nonNull)
			        .mapToDouble(Double::doubleValue)
			        .sum();
			
			Double total = totalFinalAmount + totalConsultationFee + totalTherapyFee + totalDueAmount;
			
			RevenueResponse response =
					RevenueResponse.builder()
					.grandTotal(total).consultationTotal(totalConsultationFee)
					.totalFinalAmount(totalFinalAmount).therapyFeeTotal(totalTherapyFee).dueAmountTotal(totalDueAmount)
							.success(true)
							.data(responseData)
							.message("Revenue records fetched successfully")
							.status(HttpStatus.OK.value())
							.build();

			return ResponseEntity.ok(response);

		} catch (Exception e) {

			RevenueResponse response =
					RevenueResponse.builder()
							.success(false)
							.message("Failed to fetch revenue records")
							.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
							.build();

			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(response);
		}
	}

	@Override
	public ResponseEntity<RevenueResponse> getRevenueManagementByDateRange(
			String clinicId,
			String branchId,
			String startDate,
			String endDate) {

		try {

			List<PaymentRecord> payments =
					repo.findByClinicIdAndBranchId(
							clinicId,
							branchId);
			
			List<PhysiotherapyRecord> records = physiotherapydoctorRespository.
					findByClinicIdAndBranchId(clinicId, branchId);

			LocalDate start =
					LocalDate.parse(startDate);

			LocalDate end =
					LocalDate.parse(endDate);

			payments = payments.stream()
					.filter(p -> {
						LocalDate serviceDate =
								LocalDate.parse(
										p.getSessionStartDate());

						return !serviceDate.isBefore(start)
								&& !serviceDate.isAfter(end);
					})
					.toList();
			
			 records = records.stream()
						.filter(p -> {
							LocalDate serviceDate =
									LocalDate.parse(
											p.getCreatedAt());

							return !serviceDate.isBefore(start)
									&& !serviceDate.isAfter(end);
						})
						.toList();

			List<RevenueManagementDTO> responseData =
					prepareRevenueResponse(payments,records);
			Double totalFinalAmount = responseData.stream()
			        .map(RevenueManagementDTO::getFinalAmount)
			        .filter(Objects::nonNull)
			        .mapToDouble(Double::doubleValue)
			        .sum();
			Double totalConsultationFee = responseData.stream()
			        .map(RevenueManagementDTO::getConsultationFee)
			        .filter(Objects::nonNull)
			        .mapToDouble(Double::doubleValue)
			        .sum();
			Double totalTherapyFee = responseData.stream()
			        .map(RevenueManagementDTO::getTherapyFee)
			        .filter(Objects::nonNull)
			        .mapToDouble(Double::doubleValue)
			        .sum();
			Double totalDueAmount = responseData.stream()
			        .map(RevenueManagementDTO::getDueAmount)
			        .filter(Objects::nonNull)
			        .mapToDouble(Double::doubleValue)
			        .sum();
			
			Double total = totalFinalAmount + totalConsultationFee + totalTherapyFee + totalDueAmount;
			
			RevenueResponse response =
					RevenueResponse.builder()
					.grandTotal(total).consultationTotal(totalConsultationFee)
					.totalFinalAmount(totalFinalAmount).therapyFeeTotal(totalTherapyFee).dueAmountTotal(totalDueAmount)
							.success(true)
							.data(responseData)
							.message("Revenue records fetched successfully")
							.status(HttpStatus.OK.value())
							.build();


			return ResponseEntity.ok(response);

		} catch (Exception e) {

			RevenueResponse response =
					RevenueResponse.builder()
							.success(false)
							.message("Failed to fetch revenue records")
							.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
							.build();

			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(response);
		}
	}

	private List<RevenueManagementDTO> prepareRevenueResponse(
			List<PaymentRecord> payments,List<PhysiotherapyRecord> records) {

		try {
		List<RevenueManagementDTO> response =
				new ArrayList<>();

		for (PaymentRecord payment : payments) {

			RevenueManagementDTO dto =
					new RevenueManagementDTO();

			dto.setBookingId(payment.getBookingId());
			dto.setDoctorName(payment.getDoctorName());
			dto.setTherapistId(payment.getTherapistId());
			dto.setTherapistName(payment.getTherapistName());
			dto.setTherapistRecordId(
					payment.getTherapistRecordId());

			String customer =
					clinicAdminFeign.getPatientname(
							payment.getPatientId());

			if (customer != null) {
				dto.setPatientName(customer);
			}

			BookingResponse booking =
					bookingFeignClient
							.getBookedService(
									payment.getBookingId())
							.getBody()
							.getData();

			if (booking != null) {

				dto.setServiceDate(
						booking.getServiceDate());

				dto.setServiceTime(
						booking.getServicetime());

				dto.setConsultationFee(
						booking.getConsultationFee());
			}

			dto.setTherapyFee(payment.getTotalAmount());

			dto.setFinalAmount(
                    Double.valueOf(payment.getFinalAmount()));

			dto.setDueAmount(
                    Double.valueOf(payment.getBalanceAmount()));

			response.add(dto);
		}
		
		for (PhysiotherapyRecord payment : records) {

			RevenueManagementDTO dto =
					new RevenueManagementDTO();

			dto.setBookingId(payment.getBookingId());
			dto.setDoctorName(payment.getTreatmentPlan().getDoctorName());
			dto.setTherapistId(	payment.getTreatmentPlan().getTherapistId());
			dto.setTherapistName(payment.getTreatmentPlan().getTherapistName());
			dto.setTherapistRecordId(
					payment.getTherapistRecordId());

			String customer =
					clinicAdminFeign.getPatientname(
							payment.getPatientInfo().getPatientId());

			if (customer != null) {
				dto.setPatientName(payment.getPatientInfo().getPatientName());
			}

			BookingResponse booking =
					bookingFeignClient
							.getBookedService(
									payment.getBookingId())
							.getBody()
							.getData();

			if (booking != null) {

				dto.setServiceDate(
						booking.getServiceDate());

				dto.setServiceTime(
						booking.getServicetime());

				dto.setConsultationFee(
						booking.getConsultationFee());
			}			
			dto.setTherapyFee(null);

			dto.setFinalAmount(
                   null);

			dto.setDueAmount(
                   null);

			response.add(dto);
		}			
		return response;
	}catch(Exception e) {
		return Collections.emptyList();
	}}


	@Override
	public ResponseEntity<Response> getRevenueSummary(
			String clinicId,
			String branchId) {

		List<PaymentRecord> payments =
				repo.findByClinicIdAndBranchId(
						clinicId,
						branchId);

		LocalDate today = LocalDate.now();

		LocalDate weekStart = today.minusDays(7);
		LocalDate monthStart = today.minusMonths(1);
		LocalDate yearStart = today.minusYears(1);

		double todayRevenue = payments.stream()
				.filter(p -> LocalDate.parse(
								p.getSessionStartDate())
						.isEqual(today))
				.mapToDouble(PaymentRecord::getFinalAmount)
				.sum();

		double lastWeekRevenue = payments.stream()
				.filter(p -> {
					LocalDate date =
							LocalDate.parse(
									p.getSessionStartDate());

					return !date.isBefore(weekStart)
							&& !date.isAfter(today);
				})
				.mapToDouble(PaymentRecord::getFinalAmount)
				.sum();

		double lastMonthRevenue = payments.stream()
				.filter(p -> {
					LocalDate date =
							LocalDate.parse(
									p.getSessionStartDate());

					return !date.isBefore(monthStart)
							&& !date.isAfter(today);
				})
				.mapToDouble(PaymentRecord::getFinalAmount)
				.sum();

		double lastYearRevenue = payments.stream()
				.filter(p -> {
					LocalDate date =
							LocalDate.parse(
									p.getSessionStartDate());

					return !date.isBefore(yearStart)
							&& !date.isAfter(today);
				})
				.mapToDouble(PaymentRecord::getFinalAmount)
				.sum();

		RevenueSummaryDTO revenueSummary =
				RevenueSummaryDTO.builder()
						.todayRevenue(todayRevenue)
						.lastWeekRevenue(lastWeekRevenue)
						.lastMonthRevenue(lastMonthRevenue)
						.lastYearRevenue(lastYearRevenue)
						.build();

		Response response =
				Response.builder()
						.success(true)
						.data(revenueSummary)
						.message("Revenue summary fetched successfully")
						.status(HttpStatus.OK.value())
						.build();

		return ResponseEntity.ok(response);
	}

}