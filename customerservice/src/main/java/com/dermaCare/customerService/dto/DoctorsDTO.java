package com.dermaCare.customerService.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoctorsDTO {

	private String id;

	private String doctorId;

	private String role;

	private String deviceId;
	
	private String doctorEmail;

	private String hospitalId;
	private String branchId;
	private String hospitalName;
	private String doctorPicture;

	private String doctorLicence;

	private String doctorMobileNumber;

	private String doctorName;


	private List< DoctorCategoryDTO> category;

	private List< DoctorServicesDTO> service;

	private List< DoctorSubServiceDTO> subServices;

	private String specialization;

	private String gender;

	private String experience;

	private String qualification;

	private String availableDays;

	private String availableTimes;

	private String profileDescription;


	private DoctorFeeDTO doctorFees;

	private List< String> focusAreas;

	private List<String> languages;

	private List< String> careerPath;

	private List<String> highlights;

	private boolean doctorAvailabilityStatus = true;

	private boolean recommendation;

	private double doctorAverageRating;

	private String doctorSignature;

	private boolean associatedWithIADVC;

	private String associationsOrMemberships;

	private List<DoctorBranches> branches;

	private ConsultationTypeDTO Consultation;
	  private Map<String, List<String>> permissions;

	public void trimAllDoctorFields() {
		id = trim(id);
		doctorId = trim(doctorId);
		hospitalId = trim(hospitalId);
		doctorPicture = trim(doctorPicture);
		doctorLicence = trim(doctorLicence);
		doctorMobileNumber = trim(doctorMobileNumber);
		doctorName = trim(doctorName);
		specialization = trim(specialization);
		gender = trim(gender);
		experience = trim(experience);
		qualification = trim(qualification);
		profileDescription = trim(profileDescription);
		availableDays = trim(availableDays);
		availableTimes = trim(availableTimes);

		focusAreas = trimList(focusAreas);
		languages = trimList(languages);
		careerPath = trimList(careerPath);
		highlights = trimList(highlights);
	}

	private String trim(String value) {
		return (value != null) ? value.trim() : null;
	}

	private List<String> trimList(List<String> list) {
		return (list != null) ? list.stream().map(this::trim).collect(Collectors.toList()) : null;
	}



}