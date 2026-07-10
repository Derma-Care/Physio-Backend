package com.clinicadmin.service;

import org.springframework.http.ResponseEntity;

import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.Expense;

public interface ExpenseService {

    ResponseEntity<Response> saveExpense(Expense expense);

    ResponseEntity<Response> updateExpense(String id, Expense expense);

    ResponseEntity<Response> getExpenseById(String id);

    ResponseEntity<Response> getExpensesByClinicAndBranch(
            String clinicId,
            String branchId);

    ResponseEntity<Response> deleteExpense(String id);
}
