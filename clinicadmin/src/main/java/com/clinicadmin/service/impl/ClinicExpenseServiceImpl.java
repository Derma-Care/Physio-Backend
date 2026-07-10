package com.clinicadmin.service.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.Expense;
import com.clinicadmin.repository.ExpenseRepository;
import com.clinicadmin.service.ClinicExpenseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicExpenseServiceImpl implements ClinicExpenseService {

    private final ExpenseRepository expenseRepository;

    @Override
    public ResponseEntity<Response> saveExpense(Expense expense) {

        Response response = new Response();

        try {
            Expense savedExpense = expenseRepository.save(expense);

            response.setSuccess(true);
            response.setData(savedExpense);
            response.setMessage("Expense saved successfully");
            response.setStatus(HttpStatus.CREATED.value());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setMessage("Failed to save expense");
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<Response> getExpenseById(String id) {

        Response response = new Response();

        try {

            Expense expense = expenseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Expense not found"));

            response.setSuccess(true);
            response.setData(expense);
            response.setMessage("Expense retrieved successfully");
            response.setStatus(HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Override
    public ResponseEntity<Response> getExpensesByClinicAndBranch(
            String clinicId, String branchId) {

        Response response = new Response();

        try {

            List<Expense> expenses =
                    expenseRepository.findByClinicIdAndBranchId(clinicId, branchId);

            response.setSuccess(true);
            response.setData(expenses);
            response.setMessage("Expenses retrieved successfully");
            response.setStatus(HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setMessage("Failed to retrieve expenses");
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @Override
    public ResponseEntity<Response> updateExpense(String id, Expense expense) {

        Response response = new Response();

        try {

            Expense existing = expenseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Expense not found"));

            existing.setTitle(expense.getTitle());
            existing.setCategory(expense.getCategory());
            existing.setDate(expense.getDate());
            existing.setAmount(expense.getAmount());
            existing.setMode(expense.getMode());
            existing.setClinicId(expense.getClinicId());
            existing.setBranchId(expense.getBranchId());

            Expense updatedExpense = expenseRepository.save(existing);

            response.setSuccess(true);
            response.setData(updatedExpense);
            response.setMessage("Expense updated successfully");
            response.setStatus(HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Override
    public ResponseEntity<Response> deleteExpense(String id) {

        Response response = new Response();

        try {

            Expense expense = expenseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Expense not found"));

            expenseRepository.delete(expense);

            response.setSuccess(true);
            response.setMessage("Expense deleted successfully");
            response.setStatus(HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}

