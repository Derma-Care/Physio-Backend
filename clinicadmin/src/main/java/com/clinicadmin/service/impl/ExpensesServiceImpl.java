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
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import com.clinicadmin.dto.ExpensesDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.ExpensesEntity;
import com.clinicadmin.repository.ExpensesRepository;
import com.clinicadmin.service.ExpensesService;

@Service
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
	@RateLimiter(name = "expensesApi", fallbackMethod = "createFallback")
	public ResponseEntity<Response> create(ExpensesDTO dto) {

	    try {
	        ExpensesEntity entity = mapToEntity(dto);
	        //entity.setDate(LocalDate.now());	        
	        entity.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

	        ExpensesEntity saved = repository.save(entity);
	        ExpensesDTO responseDto = mapToDTO(saved);

	        return ResponseEntity.ok(
	                Response.builder()
	                        .success(true)
	                        .message("Expense created successfully")
	                        .data(responseDto)
	                        .status(HttpStatus.OK.value())
	                        .build()
	        );

	    } catch (Exception e) {
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
	@RateLimiter(name = "expensesApi", fallbackMethod = "getAllFallback")
	public ResponseEntity<Response> getAll() {

	    try {
	        List<ExpensesDTO> list = repository.findAll()
	                .stream()
	                .map(this::mapToDTO)
	                .collect(Collectors.toList());

	        return ResponseEntity.ok(
	                Response.builder()
	                        .success(true)
	                        .data(list)
	                        .status(HttpStatus.OK.value())
	                        .build()
	        );

	    } catch (Exception e) {
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
	@RateLimiter(name = "expensesApi", fallbackMethod = "updateFallback")
	public ResponseEntity<Response> update(String id, ExpensesDTO dto) {

	    try {
	        Optional<ExpensesEntity> optional = repository.findById(id);

	        if (optional.isEmpty()) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body(Response.builder()
	                            .success(false)
	                            .message("Expense not found")
	                            .status(HttpStatus.NOT_FOUND.value())
	                            .build());
	        }

	        ExpensesEntity existing = optional.get();

	        if (dto.getClinicId() != null) {
	            existing.setClinicId(dto.getClinicId());
	        }

	        if (dto.getBranchId() != null) {
	            existing.setBranchId(dto.getBranchId());
	        }

	        if (dto.getExpense() != null) {
	            existing.setExpense(dto.getExpense());
	        }

	        if (dto.getCategory() != null) {
	            existing.setCategory(dto.getCategory());
	        }

	        if (dto.getAmount() != 0) {
	            existing.setAmount(dto.getAmount());
	        }

	        if (dto.getDate() != null) {
	            existing.setDate(dto.getDate());
	        }

	        if (dto.getPaymentMode() != null) {
	            existing.setPaymentMode(dto.getPaymentMode());
	        }

	        if (dto.getNotes() != null) {
	            existing.setNotes(dto.getNotes());
	        }

	        if (dto.getRole() != null) {
	            existing.setRole(dto.getRole());
	        }

	        if (dto.getStaffId() != null) {
	            existing.setStaffId(dto.getStaffId());
	        }

	        existing.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

	        ExpensesEntity updated = repository.save(existing);
	        ExpensesDTO responseDto = mapToDTO(updated);

	        return ResponseEntity.ok(
	                Response.builder()
	                        .success(true)
	                        .message("Expense updated successfully")
	                        .data(responseDto)
	                        .status(HttpStatus.OK.value())
	                        .build()
	        );

	    } catch (Exception e) {
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
	@RateLimiter(name = "expensesApi", fallbackMethod = "deleteFallback")
	public ResponseEntity<Response> delete(String id) {

	    try {
	        if (!repository.existsById(id)) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body(Response.builder()
	                            .success(false)
	                            .message("Expense not found")
	                            .status(HttpStatus.NOT_FOUND.value())
	                            .build());
	        }

	        repository.deleteById(id);

	        return ResponseEntity.ok(
	                Response.builder()
	                        .success(true)
	                        .message("Expense deleted successfully")
	                        .status(HttpStatus.OK.value())
	                        .build()
	        );

	    } catch (Exception e) {
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
	@RateLimiter(name = "expensesApi", fallbackMethod = "getByClinicAndBranchFallback")
	public ResponseEntity<Response> getByClinicAndBranch(String clinicId, String branchId) {

	    try {
	        List<ExpensesDTO> list = repository
	                .findByClinicIdAndBranchId(clinicId, branchId)
	                .stream()
	                .map(this::mapToDTO)
	                .collect(Collectors.toList());

	        if (list.isEmpty()) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body(Response.builder()
	                            .success(false)
	                            .message("No expenses found")
	                            .status(HttpStatus.NOT_FOUND.value())
	                            .build());
	        }

	        return ResponseEntity.ok(
	                Response.builder()
	                        .success(true)
	                        .data(list)
	                        .status(HttpStatus.OK.value())
	                        .build()
	        );

	    } catch (Exception e) {
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
	@RateLimiter(name = "expensesApi", fallbackMethod = "getTodayExpensesFallback")
	public Double getTodayExpenses(String clinicId, String branchId) {

	    LocalDate today = LocalDate.now();
        try{
	    List<ExpensesEntity> entities = repository
	            .findByClinicIdAndBranchIdAndDate(clinicId, branchId, today);

	    if (entities == null || entities.isEmpty()) {
	        return 0.0;
	    }

	    return entities.stream()
	            .map(ExpensesEntity::getAmount)
	            .filter(Objects::nonNull)
	            .mapToDouble(Double::doubleValue)
	            .sum();
	}catch(Exception e) {
		return 0.0;
	}}
	
	
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_BOOKINGSERVICE"})
	@RateLimiter(name = "expensesApi", fallbackMethod = "getWeeklyExpensesFallback")
	public Double getWeeklyExpenses(String clinicId, String branchId) {

		try {
	    LocalDate today = LocalDate.now();
	    LocalDate startDate = today.minusDays(6);

	    List<ExpensesEntity> entities = repository
	            .findByClinicIdAndBranchIdAndDateBetween(clinicId, branchId, startDate, today);

	    if (entities == null || entities.isEmpty()) {
	        return 0.0;
	    }

	    return entities.stream()
	            .map(ExpensesEntity::getAmount)
	            .filter(Objects::nonNull)
	            .mapToDouble(Double::doubleValue)
	            .sum();
		}catch(Exception e) {
			return 0.0;
		}}
	
	
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_BOOKINGSERVICE"})
	@RateLimiter(name = "expensesApi", fallbackMethod = "getMonthlyExpensesFallback")
	public Double getMonthlyExpenses(String clinicId, String branchId) {

		try {
	    LocalDate today = LocalDate.now();
	    LocalDate startDate = today.withDayOfMonth(1);

	    List<ExpensesEntity> entities = repository
	            .findByClinicIdAndBranchIdAndDateBetween(clinicId, branchId, startDate, today);

	    if (entities == null || entities.isEmpty()) {
	        return 0.0;
	    }

	    return entities.stream()
	            .map(ExpensesEntity::getAmount)
	            .filter(Objects::nonNull)
	            .mapToDouble(Double::doubleValue)
	            .sum();
		}catch(Exception e) {
			return 0.0;
		}}
	
	
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_BOOKINGSERVICE"})
	@RateLimiter(name = "expensesApi", fallbackMethod = "customeFilterFallback")
	public Double customeFilter(String startDate, String endDate) {
		try {
	    List<ExpensesEntity> entities = repository
	            .findByDateBetween(LocalDate.parse(startDate), LocalDate.parse(endDate));

	    if (entities == null || entities.isEmpty()) {
	        return 0.0;
	    }

	    return entities.stream()
	            .map(ExpensesEntity::getAmount)
	            .filter(Objects::nonNull)
	            .mapToDouble(Double::doubleValue)
	            .sum();
		}catch(Exception e) {
			return 0.0;
		}}
	
	
	////// FALLBACK METHODS //////
	
public ResponseEntity<Response> createFallback(ExpensesDTO dto, Exception ex){ return buildRateLimitResponse(); }
public ResponseEntity<Response> getAllFallback(Exception ex){ return buildRateLimitResponse(); }
public ResponseEntity<Response> updateFallback(String id, ExpensesDTO dto, Exception ex){ return buildRateLimitResponse(); }
public ResponseEntity<Response> deleteFallback(String id, Exception ex){ return buildRateLimitResponse(); }
public ResponseEntity<Response> getByClinicAndBranchFallback(String clinicId, String branchId, Exception ex){ return buildRateLimitResponse(); }
public Double getTodayExpensesFallback(String clinicId, String branchId, Exception ex){ return 0.0; }
public Double getWeeklyExpensesFallback(String clinicId, String branchId, Exception ex){ return 0.0; }
public Double getMonthlyExpensesFallback(String clinicId, String branchId, Exception ex){ return 0.0; }
public Double customeFilterFallback(String startDate, String endDate, Exception ex){ return 0.0; }

public ResponseEntity<Response> buildRateLimitResponse() {
    Response response = Response.builder()
            .success(false)
            .status(HttpStatus.TOO_MANY_REQUESTS.value())
            .message("Too many requests. Please try again later.")
            .build();
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
}

}