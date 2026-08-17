package com.chiselon.clinicadmin.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.chiselon.clinicadmin.entity.FollowOption;

public interface FollowOptionRepository extends MongoRepository<FollowOption, String> {
}
