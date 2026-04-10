package com.dermacare.category_services.repository;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dermacare.category_services.entity.Services;

@Repository
public interface ServicesRepository extends MongoRepository<Services, ObjectId> {
	
	public List<Services> findByCategoryId(ObjectId categoryId);

	public void deleteAllByCategoryId(ObjectId categoryId);

	public List<Services> findByCategoryName(String categoryName);

   public Services findByCategoryIdAndServiceNameIgnoreCase(ObjectId categoryId, String serviceName);
   public Optional<Services> findByServiceId(String serviceId);
	
	Optional<Services> findByServiceName(String serviceName);

	public List<Services> findByServiceNameIgnoreCase(String serviceName);

}
