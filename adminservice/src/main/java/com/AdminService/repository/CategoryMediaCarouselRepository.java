package com.AdminService.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.AdminService.entity.CategoryMediaCarousel;


public interface CategoryMediaCarouselRepository extends MongoRepository<CategoryMediaCarousel, ObjectId> {
}
