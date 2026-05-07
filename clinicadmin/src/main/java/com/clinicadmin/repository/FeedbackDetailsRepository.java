package com.clinicadmin.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.clinicadmin.entity.FeedbackDetails;

public interface FeedbackDetailsRepository
        extends MongoRepository<FeedbackDetails, String> {

}
