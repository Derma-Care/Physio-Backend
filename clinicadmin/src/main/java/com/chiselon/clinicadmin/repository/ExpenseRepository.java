package com.chiselon.clinicadmin.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.chiselon.clinicadmin.entity.Expense;


public interface ExpenseRepository extends MongoRepository<Expense, String> {

    List<Expense> findByClinicIdAndBranchId(String clinicId, String branchId);
}