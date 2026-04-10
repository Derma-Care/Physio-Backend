package com.dermacare.category_services.repository;

import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.dermacare.category_services.entity.Services;
import com.dermacare.category_services.entity.SubServices;

@Repository
public interface SubServiceRepository extends MongoRepository<SubServices,ObjectId> {
	
	public List<SubServices> findByServiceId(ObjectId serviceId);
	public List<SubServices> findByCategoryId(ObjectId categoryId);
	public void deleteByCategoryId(ObjectId categoryId);
	public Optional<Services> findByServiceIdAndSubServiceNameIgnoreCase(ObjectId objectId, String subServiceName);
	List<SubServices> findByCategoryName(String categoryName);
	List<SubServices> findByServiceName(String serviceName);
	List<SubServices> findAll();
	public SubServices findByHospitalIdAndSubServiceId(String hospitalId, ObjectId subServiceId);
	public Optional<SubServices> deleteByHospitalIdAndSubServiceId(String hospitalId, ObjectId subServiceId);
	public List<SubServices> findBySubServiceId(ObjectId subServiceId);
	public List<SubServices> findByHospitalId(String hospitalId);
	public SubServices findByHospitalIdAndSubServiceIdAndSubServiceNameIgnoreCase(String hospitalId, ObjectId objectId,
			String subServiceName);
}
