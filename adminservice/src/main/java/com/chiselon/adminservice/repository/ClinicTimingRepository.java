package com.chiselon.adminservice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.adminservice.entity.ClinicTiming;

public interface ClinicTimingRepository
        extends MongoRepository<ClinicTiming, String> {

    List<ClinicTiming> findAllByOrderByStartHourAsc();
}