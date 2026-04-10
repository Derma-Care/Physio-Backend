package com.AdminService.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.AdminService.entity.ServiceMediaCarousel;


public interface ServiceMediaCarousalRepository extends MongoRepository<ServiceMediaCarousel, ObjectId>{

}
