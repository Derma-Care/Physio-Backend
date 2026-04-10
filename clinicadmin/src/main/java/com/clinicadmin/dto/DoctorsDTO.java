package com.clinicadmin.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.clinicadmin.validations.FormatChecks;
import com.clinicadmin.validations.RequiredChecks;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorsDTO {

	private String id;

	private String doctorId;

	private String role;

	private String deviceId;

	@NotBlank(message = "DoctorEmail is required", groups = RequiredChecks.class)
	private String doctorEmail;

	@NotBlank(message = "Clinic id is required", groups = RequiredChecks.class)
	private String hospitalId;
	private String branchId;
	private String hospitalName;

	private String doctorPicture;

	@NotBlank(message = "Doctor licence is required", groups = RequiredChecks.class)
	@Size(max = 100, message = "Doctor licence number should not exceed 100 characters", groups = FormatChecks.class)
	private String doctorLicence;

	@NotBlank(message = "Mobile number is required", groups = RequiredChecks.class)
	@Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number", groups = FormatChecks.class)
	private String doctorMobileNumber;

	@NotBlank(message = "Doctor name is required", groups = RequiredChecks.class)
	@Size(min = 3, max = 50, message = "Doctor name must be between 3 and 50 characters", groups = FormatChecks.class)
	private String doctorName;

	@Valid
	@NotNull(message = "Category list cannot be null", groups = RequiredChecks.class)
	@Size(min = 1, message = "At least one Category is required", groups = FormatChecks.class)
	private List<@Valid DoctorCategoryDTO> category;
	@Valid
	@NotNull(message = "Services list cannot be null", groups = RequiredChecks.class)
	@Size(min = 1, message = "At least one service is required", groups = FormatChecks.class)
	private List<@Valid DoctorServicesDTO> service;

	@Valid
	@NotNull(message = "Sub-services list cannot be null", groups = RequiredChecks.class)
	@Size(min = 1, message = "At least one sub-service is required", groups = FormatChecks.class)
	private List<@Valid DoctorSubServiceDTO> subServices;

	@NotBlank(message = "Specialization is required", groups = RequiredChecks.class)
	private String specialization;

	@NotBlank(message = "Gender is required", groups = RequiredChecks.class)
	@Pattern(regexp = "Male|Female|Other", message = "Gender must be Male, Female, or Other", groups = FormatChecks.class)
	private String gender;

	@NotBlank(message = "Experience is required", groups = RequiredChecks.class)
	@Pattern(regexp = "^\\d{1,2}(\\+)?$", message = "Experience should be a number like '5' or '5+'", groups = FormatChecks.class)
	private String experience;

	@NotBlank(message = "Qualification is required", groups = RequiredChecks.class)
	private String qualification;

	@NotBlank(message = "Available days are required", groups = RequiredChecks.class)
	private String availableDays;

	@NotBlank(message = "Available times are required", groups = RequiredChecks.class)
	private String availableTimes;

	@Size(max = 1000, message = "Profile description should not exceed 1000 characters", groups = FormatChecks.class)
	private String profileDescription;

	@Valid
	@NotNull(message = "Doctor fees must not be null", groups = RequiredChecks.class)
	private DoctorFeeDTO doctorFees;

	@Size(max = 10, message = "Maximum 10 focus areas allowed", groups = FormatChecks.class)
	private List<@NotBlank(message = "Focus area cannot be blank", groups = RequiredChecks.class) String> focusAreas;

	@Size(max = 5, message = "Maximum 5 languages allowed", groups = FormatChecks.class)
	private List<@NotBlank(message = "Language cannot be blank", groups = RequiredChecks.class) String> languages;

	@Size(max = 10, message = "Maximum 10 career path items allowed", groups = FormatChecks.class)
	private List<@NotBlank(message = "Career path item cannot be blank", groups = RequiredChecks.class) String> careerPath;

	@Size(max = 10, message = "Maximum 10 highlights allowed", groups = FormatChecks.class)
	private List<@NotBlank(message = "Highlight cannot be blank", groups = RequiredChecks.class) String> highlights;

	private boolean doctorAvailabilityStatus = true;

	private boolean recommendation;

	private double doctorAverageRating;

	private String doctorSignature;

	private boolean associatedWithIADVC;

	private String associationsOrMemberships;

	private List<DoctorBranches> branches;

	private ConsultationTypeDTO Consultation;

	private Map<String, List<String>> permissions;

	private String createdBy;

	private String createdAt;

	private String updatedDate;

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
