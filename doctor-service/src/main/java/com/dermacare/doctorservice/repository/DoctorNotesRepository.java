package com.dermacare.doctorservice.repository;

import com.dermacare.doctorservice.model.DoctorNotes;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DoctorNotesRepository extends MongoRepository<DoctorNotes, String> {

	DoctorNotes findByDoctorId(String doctorId);
}
