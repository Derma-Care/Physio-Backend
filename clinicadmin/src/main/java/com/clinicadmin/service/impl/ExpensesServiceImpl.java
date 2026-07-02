package com.clinicadmin.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.ExpensesDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.ExpensesEntity;
import com.clinicadmin.repository.ExpensesRepository;
import com.clinicadmin.service.ExpensesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j

public class ExpensesServiceImpl implements ExpensesService {
	
	@Autowired
	private ExpensesRepository repository;
	
	
	private ExpensesDTO mapToDTO(ExpensesEntity entity) {
	    ExpensesDTO dto = new ExpensesDTO();
	    BeanUtils.copyProperties(entity, dto);
	    return dto;
	}

	private ExpensesEntity mapToEntity(ExpensesDTO dto) {
	    ExpensesEntity entity = new ExpensesEntity();
	    BeanUtils.copyProperties(dto, entity);
	    return entity;
	}
	
	
	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<Response> create(ExpensesDTO dto) {

	    log.info("Received request to create expense. ClinicId: {}, BranchId: {}, ExpenseType: {}",
	            dto.getClinicId(), dto.getBranchId(), dto.getExpense());

	    try {

	        log.debug("Mapping ExpensesDTO to ExpensesEntity.");
	        ExpensesEntity entity = mapToEntity(dto);

	        entity.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
	        log.debug("Timestamp set for expense: {}", entity.getTimestamp());

	        log.info("Saving expense to database.");
	        ExpensesEntity saved = repository.save(entity);

	        log.info("Expense created successfully. ExpenseId: {}", saved.getExpense());

	        log.debug("Mapping saved ExpensesEntity to ExpensesDTO.");
	        ExpensesDTO responseDto = mapToDTO(saved);

	        log.info("Returning success response for ExpenseId: {}", saved.getExpense());

	        return ResponseEntity.ok(
	                Response.builder()
	                        .success(true)
	                        .message("Expense created successfully")
	                        .data(responseDto)
	                        .status(HttpStatus.OK.value())
	                        .build()
	        );

	    } catch (Exception e) {

	        log.error("Exception occurred while creating expense. ClinicId: {}, BranchId: {}, Error: {}",
	                dto.getClinicId(),
	                dto.getBranchId(),
	                e.getMessage(),
	                e);

	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Response.builder()
	                        .success(false)
	                        .message("Error creating expense: " + e.getMessage())
	                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                        .build());
	    }
	}
	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<Response> getAll() {

	    log.info("Received request to fetch all expenses.");

	    try {

	        log.debug("Fetching all expense records from the database.");
	        List<ExpensesDTO> list = repository.findAll()
	                .stream()
	                .map(this::mapToDTO)
	                .collect(Collectors.toList());

	        log.info("Successfully fetched {} expense record(s).", list.size());

	        log.debug("Returning all expense records in response.");

	        return ResponseEntity.ok(
	                Response.builder()
	                        .success(true)
	                        .data(list)
	                        .status(HttpStatus.OK.value())
	                        .build()
	        );

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching all expenses. Error: {}",
	                e.getMessage(), e);

	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Response.builder()
	                        .success(false)
	                        .message("Error fetching expenses: " + e.getMessage())
	                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                        .build());
	    }
	}
	
	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<Response> update(String id, ExpensesDTO dto) {

	    log.info("Received request to update expense. ExpenseId: {}", id);

	    try {

	        log.debug("Checking if expense exists with ExpenseId: {}", id);
	        Optional<ExpensesEntity> optional = repository.findById(id);

	        if (optional.isEmpty()) {
	            log.warn("Expense not found. ExpenseId: {}", id);

	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body(Response.builder()
	                            .success(false)
	                            .message("Expense not found")
	                            .status(HttpStatus.NOT_FOUND.value())
	                            .build());
	        }

	        ExpensesEntity existing = optional.get();
	        log.debug("Expense found. Updating provided fields for ExpenseId: {}", id);

	        if (dto.getClinicId() != null) {
	            existing.setClinicId(dto.getClinicId());
	            log.debug("Updated ClinicId: {}", dto.getClinicId());
	        }

	        if (dto.getBranchId() != null) {
	            existing.setBranchId(dto.getBranchId());
	            log.debug("Updated BranchId: {}", dto.getBranchId());
	        }

	        if (dto.getExpense() != null) {
	            existing.setExpense(dto.getExpense());
	            log.debug("Updated Expense Name: {}", dto.getExpense());
	        }

	        if (dto.getCategory() != null) {
	            existing.setCategory(dto.getCategory());
	            log.debug("Updated Category: {}", dto.getCategory());
	        }

	        if (dto.getAmount() != 0) {
	            existing.setAmount(dto.getAmount());
	            log.debug("Updated Amount: {}", dto.getAmount());
	        }

	        if (dto.getDate() != null) {
	            existing.setDate(dto.getDate());
	            log.debug("Updated Expense Date: {}", dto.getDate());
	        }

	        if (dto.getPaymentMode() != null) {
	            existing.setPaymentMode(dto.getPaymentMode());
	            log.debug("Updated Payment Mode: {}", dto.getPaymentMode());
	        }

	        if (dto.getNotes() != null) {
	            existing.setNotes(dto.getNotes());
	            log.debug("Updated Notes.");
	        }

	        if (dto.getRole() != null) {
	            existing.setRole(dto.getRole());
	            log.debug("Updated Role: {}", dto.getRole());
	        }

	        if (dto.getStaffId() != null) {
	            existing.setStaffId(dto.getStaffId());
	            log.debug("Updated StaffId: {}", dto.getStaffId());
	        }

	        existing.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
	        log.debug("Updated timestamp set: {}", existing.getUpdatedAt());

	        log.info("Saving updated expense. ExpenseId: {}", id);
	        ExpensesEntity updated = repository.save(existing);

	        log.info("Expense updated successfully. ExpenseId: {}", updated.getExpense());

	        ExpensesDTO responseDto = mapToDTO(updated);

	        log.info("Returning success response for updated expense. ExpenseId: {}", updated.getExpense());

	        return ResponseEntity.ok(
	                Response.builder()
	                        .success(true)
	                        .message("Expense updated successfully")
	                        .data(responseDto)
	                        .status(HttpStatus.OK.value())
	                        .build()
	        );

	    } catch (Exception e) {

	        log.error("Exception occurred while updating expense. ExpenseId: {}, Error: {}",
	                id,
	                e.getMessage(),
	                e);

	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Response.builder()
	                        .success(false)
	                        .message("Error updating expense: " + e.getMessage())
	                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                        .build());
	    }
	}
	
	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<Response> delete(String id) {

	    log.info("Received request to delete expense. ExpenseId: {}", id);

	    try {

	        log.debug("Checking whether expense exists. ExpenseId: {}", id);

	        if (!repository.existsById(id)) {

	            log.warn("Expense not found. ExpenseId: {}", id);

	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body(Response.builder()
	                            .success(false)
	                            .message("Expense not found")
	                            .status(HttpStatus.NOT_FOUND.value())
	                            .build());
	        }

	        log.info("Expense found. Deleting expense. ExpenseId: {}", id);

	        repository.deleteById(id);

	        log.info("Expense deleted successfully. ExpenseId: {}", id);

	        log.debug("Preparing success response for deleted expense. ExpenseId: {}", id);

	        return ResponseEntity.ok(
	                Response.builder()
	                        .success(true)
	                        .message("Expense deleted successfully")
	                        .status(HttpStatus.OK.value())
	                        .build()
	        );

	    } catch (Exception e) {

	        log.error("Exception occurred while deleting expense. ExpenseId: {}, Error: {}",
	                id,
	                e.getMessage(),
	                e);

	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Response.builder()
	                        .success(false)
	                        .message("Error deleting expense: " + e.getMessage())
	                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                        .build());
	    }
	}
	@Override
	@Secured("ROLE_CLINICADMIN")
	public ResponseEntity<Response> getByClinicAndBranch(String clinicId, String branchId) {

	    log.info("Received request to fetch expenses. ClinicId: {}, BranchId: {}", clinicId, branchId);

	    try {

	        log.debug("Fetching expenses for ClinicId: {} and BranchId: {}", clinicId, branchId);

	        List<ExpensesDTO> list = repository
	                .findByClinicIdAndBranchId(clinicId, branchId)
	                .stream()
	                .map(this::mapToDTO)
	                .collect(Collectors.toList());

	        if (list.isEmpty()) {

	            log.warn("No expenses found for ClinicId: {} and BranchId: {}", clinicId, branchId);

	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body(Response.builder()
	                            .success(false)
	                            .message("No expenses found")
	                            .status(HttpStatus.NOT_FOUND.value())
	                            .build());
	        }

	        log.info("Successfully fetched {} expense(s) for ClinicId: {} and BranchId: {}",
	                list.size(), clinicId, branchId);

	        log.debug("Returning expense list for ClinicId: {} and BranchId: {}", clinicId, branchId);

	        return ResponseEntity.ok(
	                Response.builder()
	                        .success(true)
	                        .data(list)
	                        .status(HttpStatus.OK.value())
	                        .build()
	        );

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching expenses for ClinicId: {}, BranchId: {}. Error: {}",
	                clinicId,
	                branchId,
	                e.getMessage(),
	                e);

	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Response.builder()
	                        .success(false)
	                        .message("Error fetching expenses: " + e.getMessage())
	                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
	                        .build());
	    }
	}
	@Override
	@Secured({"ROLE_CLINICADMIN","ROLE_BOOKINGSERVICE"})
	public Double getTodayExpenses(String clinicId, String branchId) {

	    LocalDate today = LocalDate.now();

	    log.info("Received request to calculate today's expenses. ClinicId: {}, BranchId: {}, Date: {}",
	            clinicId, branchId, today);

	    try {

	        log.debug("Fetching today's expenses from database. ClinicId: {}, BranchId: {}, Date: {}",
	                clinicId, branchId, today);

	        List<ExpensesEntity> entities = repository
	                .findByClinicIdAndBranchIdAndDate(clinicId, branchId, today);

	        if (entities == null || entities.isEmpty()) {

	            log.warn("No expense records found for ClinicId: {}, BranchId: {}, Date: {}",
	                    clinicId, branchId, today);

	            return 0.0;
	        }

	        log.info("Fetched {} expense record(s) for ClinicId: {}, BranchId: {}",
	                entities.size(), clinicId, branchId);

	        Double totalExpenses = entities.stream()
	                .map(ExpensesEntity::getAmount)
	                .filter(Objects::nonNull)
	                .mapToDouble(Double::doubleValue)
	                .sum();

	        log.info("Today's total expenses calculated successfully. ClinicId: {}, BranchId: {}, TotalAmount: {}",
	                clinicId, branchId, totalExpenses);

	        return totalExpenses;

	    } catch (Exception e) {

	        log.error("Exception occurred while calculating today's expenses. ClinicId: {}, BranchId: {}, Error: {}",
	                clinicId,
	                branchId,
	                e.getMessage(),
	                e);

	        return 0.0;
	    }
	}
	
	
	@Override
	@Secured({"ROLE_CLINICADMIN","ROLE_BOOKINGSERVICE"})
	public Double getWeeklyExpenses(String clinicId, String branchId) {

	    log.info("Received request to calculate weekly expenses. ClinicId: {}, BranchId: {}",
	            clinicId, branchId);

	    try {

	        LocalDate today = LocalDate.now();
	        LocalDate startDate = today.minusDays(6);

	        log.debug("Fetching weekly expenses from database. ClinicId: {}, BranchId: {}, StartDate: {}, EndDate: {}",
	                clinicId, branchId, startDate, today);

	        List<ExpensesEntity> entities = repository
	                .findByClinicIdAndBranchIdAndDateBetween(clinicId, branchId, startDate, today);

	        if (entities == null || entities.isEmpty()) {

	            log.warn("No expense records found for ClinicId: {}, BranchId: {} between {} and {}",
	                    clinicId, branchId, startDate, today);

	            return 0.0;
	        }

	        log.info("Fetched {} expense record(s) for ClinicId: {}, BranchId: {}",
	                entities.size(), clinicId, branchId);

	        Double totalExpenses = entities.stream()
	                .map(ExpensesEntity::getAmount)
	                .filter(Objects::nonNull)
	                .mapToDouble(Double::doubleValue)
	                .sum();

	        log.info("Weekly expenses calculated successfully. ClinicId: {}, BranchId: {}, TotalAmount: {}",
	                clinicId, branchId, totalExpenses);

	        return totalExpenses;

	    } catch (Exception e) {

	        log.error("Exception occurred while calculating weekly expenses. ClinicId: {}, BranchId: {}, Error: {}",
	                clinicId,
	                branchId,
	                e.getMessage(),
	                e);

	        return 0.0;
	    }
	}
	
	
	@Override
	@Secured({"ROLE_CLINICADMIN","ROLE_BOOKINGSERVICE"})
	public Double getMonthlyExpenses(String clinicId, String branchId) {

	    log.info("Received request to calculate monthly expenses. ClinicId: {}, BranchId: {}",
	            clinicId, branchId);

	    try {

	        LocalDate today = LocalDate.now();
	        LocalDate startDate = today.withDayOfMonth(1);

	        log.debug("Fetching monthly expenses from database. ClinicId: {}, BranchId: {}, StartDate: {}, EndDate: {}",
	                clinicId, branchId, startDate, today);

	        List<ExpensesEntity> entities = repository
	                .findByClinicIdAndBranchIdAndDateBetween(clinicId, branchId, startDate, today);

	        if (entities == null || entities.isEmpty()) {

	            log.warn("No expense records found for ClinicId: {}, BranchId: {} between {} and {}",
	                    clinicId, branchId, startDate, today);

	            return 0.0;
	        }

	        log.info("Fetched {} expense record(s) for ClinicId: {}, BranchId: {}",
	                entities.size(), clinicId, branchId);

	        Double totalExpenses = entities.stream()
	                .map(ExpensesEntity::getAmount)
	                .filter(Objects::nonNull)
	                .mapToDouble(Double::doubleValue)
	                .sum();

	        log.info("Monthly expenses calculated successfully. ClinicId: {}, BranchId: {}, TotalAmount: {}",
	                clinicId, branchId, totalExpenses);

	        return totalExpenses;

	    } catch (Exception e) {

	        log.error("Exception occurred while calculating monthly expenses. ClinicId: {}, BranchId: {}, Error: {}",
	                clinicId,
	                branchId,
	                e.getMessage(),
	                e);

	        return 0.0;
	    }
	}
	
	
	@Override
	@Secured({"ROLE_CLINICADMIN","ROLE_BOOKINGSERVICE"})
	public Double customeFilter(String startDate, String endDate) {

	    log.info("Received request to calculate expenses for custom date range. StartDate: {}, EndDate: {}",
	            startDate, endDate);

	    try {

	        LocalDate fromDate = LocalDate.parse(startDate);
	        LocalDate toDate = LocalDate.parse(endDate);

	        log.debug("Fetching expenses between {} and {}", fromDate, toDate);

	        List<ExpensesEntity> entities = repository
	                .findByDateBetween(fromDate, toDate);

	        if (entities == null || entities.isEmpty()) {

	            log.warn("No expense records found between {} and {}",
	                    fromDate, toDate);

	            return 0.0;
	        }

	        log.info("Fetched {} expense record(s) between {} and {}",
	                entities.size(), fromDate, toDate);

	        Double totalExpenses = entities.stream()
	                .map(ExpensesEntity::getAmount)
	                .filter(Objects::nonNull)
	                .mapToDouble(Double::doubleValue)
	                .sum();

	        log.info("Custom date range expenses calculated successfully. StartDate: {}, EndDate: {}, TotalAmount: {}",
	                fromDate, toDate, totalExpenses);

	        return totalExpenses;

	    } catch (Exception e) {

	        log.error("Exception occurred while calculating custom date range expenses. StartDate: {}, EndDate: {}, Error: {}",
	                startDate,
	                endDate,
	                e.getMessage(),
	                e);

	        return 0.0;
	    }
	}
	
}
