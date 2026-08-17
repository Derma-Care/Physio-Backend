package com.chiselon.clinicadmin.service;

import org.springframework.http.ResponseEntity;

import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.entity.Expense;

public interface ClinicExpenseService {

    ResponseEntity<Response> saveExpense(Expense expense);

    ResponseEntity<Response> updateExpense(String id, Expense expense);

    ResponseEntity<Response> getExpenseById(String id);

    ResponseEntity<?> getExpensesByClinicAndBranch(
            String clinicId,
            String branchId);

    ResponseEntity<Response> deleteExpense(String id);
}
