package com.pharmacyManagement.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import com.pharmacyManagement.dto.SalesReturnFilterRequest;
import com.pharmacyManagement.entity.SalesReturn;
import com.pharmacyManagement.enums.ReturnStatus;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SalesReturnCustomRepository {

    private final MongoTemplate mongoTemplate;

    public List<SalesReturn> filterReturns(SalesReturnFilterRequest filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Always filter ACTIVE only
        criteriaList.add(Criteria.where("status").is(ReturnStatus.ACTIVE));

        if (StringUtils.hasText(filter.getPatientName())) {
            criteriaList.add(Criteria.where("patient_name")
                    .regex(filter.getPatientName(), "i")); // case-insensitive regex
        }

        if (StringUtils.hasText(filter.getMobileNo())) {
            criteriaList.add(Criteria.where("mobile_no").is(filter.getMobileNo()));
        }

        if (StringUtils.hasText(filter.getBillNo())) {
            criteriaList.add(Criteria.where("original_bill_no").is(filter.getBillNo()));
        }

        if (filter.getReturnType() != null) {
            criteriaList.add(Criteria.where("return_type").is(filter.getReturnType().name()));
        }

        if (filter.getClinicId() != null) {
            criteriaList.add(Criteria.where("clinic_id").is(filter.getClinicId()));
        }

        if (filter.getBranchId() != null) {
            criteriaList.add(Criteria.where("branch_id").is(filter.getBranchId()));
        }

        // Date range on return_date
        if (filter.getFromDate() != null || filter.getToDate() != null) {
            Criteria dateCriteria = Criteria.where("return_date");
            if (filter.getFromDate() != null) {
                dateCriteria = dateCriteria.gte(filter.getFromDate().atStartOfDay());
            }
            if (filter.getToDate() != null) {
                dateCriteria = dateCriteria.lte(filter.getToDate().atTime(23, 59, 59));
            }
            criteriaList.add(dateCriteria);
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, SalesReturn.class);
    }
}

