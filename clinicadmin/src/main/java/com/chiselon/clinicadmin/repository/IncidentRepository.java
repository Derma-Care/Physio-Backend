package com.chiselon.clinicadmin.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.clinicadmin.entity.Incident;
import com.chiselon.clinicadmin.enumclasses.IncidentStatus;

public interface IncidentRepository extends MongoRepository<Incident, String> {
List<Incident>findByStatus(IncidentStatus status);
}
