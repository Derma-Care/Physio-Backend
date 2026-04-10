package com.dermacare.category_services.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.dermacare.category_services.entity.Category;

public interface CategoryRepository extends MongoRepository<Category,ObjectId> {

	public Optional<Category> findByCategoryName(String categoryName);

	public Optional<Category> findById(ObjectId categoryId);
	
	public Optional<Category> findByCategoryId(String categoryId);

	public boolean existsByCategoryNameIgnoreCase(String categoryName);

	public Optional<Category> findByCategoryNameIgnoreCase(String newCategoryName);

}
