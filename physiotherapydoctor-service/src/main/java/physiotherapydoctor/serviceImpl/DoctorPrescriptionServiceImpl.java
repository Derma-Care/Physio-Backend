package physiotherapydoctor.serviceImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.DoctorPrescriptionDTO;
import physiotherapydoctor.dto.MedicineDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.entity.DoctorPrescription;
import physiotherapydoctor.entity.Medicine;
import physiotherapydoctor.repository.DoctorPrescriptionRepository;
import physiotherapydoctor.service.DoctorPrescriptionService;

@Service
@Slf4j
public class DoctorPrescriptionServiceImpl implements DoctorPrescriptionService {

	@Autowired
	private DoctorPrescriptionRepository repository;

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "createPrescriptionFallback")
	@Secured("ROLE_DOCTOR")
	public Response createPrescription(DoctorPrescriptionDTO dto) {

	    long startTime = System.currentTimeMillis();

	    log.info("Create prescription request received. ClinicId={}, MedicineCount={}",
	            dto != null ? dto.getClinicId() : null,
	            (dto != null && dto.getMedicines() != null)
	                    ? dto.getMedicines().size()
	                    : 0);

	    try {

	        log.debug("Validating prescription request");

	        if (dto == null || dto.getMedicines() == null || dto.getMedicines().isEmpty()) {

	            log.warn("Prescription validation failed. No medicines provided.");

	            return new Response(
	                    false,
	                    null,
	                    "Prescription must have at least one medicine",
	                    HttpStatus.BAD_REQUEST.value());
	        }

	        if (dto.getClinicId() == null || dto.getClinicId().isBlank()) {

	            log.warn("Prescription validation failed. ClinicId is missing.");

	            return new Response(
	                    false,
	                    null,
	                    "Clinic ID is required",
	                    HttpStatus.BAD_REQUEST.value());
	        }

	        log.info("Fetching existing prescription. ClinicId={}",
	                dto.getClinicId());

	        List<DoctorPrescription> prescriptions =
	                repository.findByClinicId(dto.getClinicId());

	        log.info("Existing prescription records found={}",
	                prescriptions.size());

	        DoctorPrescription entity =
	                prescriptions.isEmpty()
	                        ? new DoctorPrescription()
	                        : prescriptions.get(0);

	        entity.setClinicId(dto.getClinicId());

	        List<Medicine> existingMedicines =
	                Optional.ofNullable(entity.getMedicines())
	                        .orElse(new ArrayList<>());

	        log.debug("Existing medicines count={}",
	                existingMedicines.size());

	        boolean updatedExistingMedicine = false;
	        boolean addedNewMedicine = false;

	        for (MedicineDTO incomingMed : dto.getMedicines()) {

	            if (incomingMed == null
	                    || incomingMed.getName() == null
	                    || incomingMed.getName().isBlank()) {

	                log.warn("Skipping invalid medicine entry");
	                continue;
	            }

	            String normalizedName =
	                    incomingMed.getName()
	                            .trim()
	                            .replaceAll("\\s+", " ")
	                            .toLowerCase();

	            log.debug("Processing medicine={}", normalizedName);

	            Optional<Medicine> existingMedOpt =
	                    existingMedicines.stream()
	                            .filter(m -> m.getName() != null
	                                    && m.getName()
	                                            .trim()
	                                            .replaceAll("\\s+", " ")
	                                            .toLowerCase()
	                                            .equals(normalizedName))
	                            .findFirst();

	            if (existingMedOpt.isPresent()) {

	                log.info("Existing medicine found. Updating medicine={}",
	                        normalizedName);

	                Medicine existingMed = existingMedOpt.get();

	                existingMed.setDose(incomingMed.getDose());
	                existingMed.setDuration(incomingMed.getDuration());
	                existingMed.setDurationUnit(incomingMed.getDurationUnit());
	                existingMed.setNote(incomingMed.getNote());
	                existingMed.setFood(incomingMed.getFood());
	                existingMed.setMedicineType(incomingMed.getMedicineType());
	                existingMed.setRemindWhen(incomingMed.getRemindWhen());
	                existingMed.setTimes(incomingMed.getTimes());
	                existingMed.setOthers(incomingMed.getOthers());
	                existingMed.setSerialNumber(incomingMed.getSerialNumber());
	                existingMed.setGenericName(incomingMed.getGenericName());
	                existingMed.setBrandName(incomingMed.getBrandName());
	                existingMed.setNameAndAddressOfTheManufacturer(
	                        incomingMed.getNameAndAddressOfTheManufacturer());
	                existingMed.setBatchNumber(incomingMed.getBatchNumber());
	                existingMed.setDateOfManufacturing(
	                        incomingMed.getDateOfManufacturing());
	                existingMed.setDateOfExpriy(
	                        incomingMed.getDateOfExpriy());
	                existingMed.setManufacturingLicenseNumber(
	                        incomingMed.getManufacturingLicenseNumber());
	                existingMed.setStock(incomingMed.getStock());

	                updatedExistingMedicine = true;

	            } else {

	                log.info("Adding new medicine={}", normalizedName);

	                existingMedicines.add(
	                        new Medicine(
	                                UUID.randomUUID().toString(),
	                                incomingMed.getName().trim(),
	                                incomingMed.getDose(),
	                                incomingMed.getDuration(),
	                                incomingMed.getDurationUnit(),
	                                incomingMed.getNote(),
	                                incomingMed.getFood(),
	                                incomingMed.getMedicineType(),
	                                incomingMed.getRemindWhen(),
	                                incomingMed.getOthers(),
	                                incomingMed.getTimes(),
	                                incomingMed.getSerialNumber(),
	                                incomingMed.getGenericName(),
	                                incomingMed.getBrandName(),
	                                incomingMed.getNameAndAddressOfTheManufacturer(),
	                                incomingMed.getBatchNumber(),
	                                incomingMed.getDateOfManufacturing(),
	                                incomingMed.getDateOfExpriy(),
	                                incomingMed.getManufacturingLicenseNumber(),
	                                incomingMed.getStock()));

	                addedNewMedicine = true;
	            }
	        }

	        log.info(
	                "Medicine processing completed. UpdatedExistingMedicine={}, AddedNewMedicine={}, FinalMedicineCount={}",
	                updatedExistingMedicine,
	                addedNewMedicine,
	                existingMedicines.size());

	        entity.setMedicines(existingMedicines);

	        log.info("Saving prescription. ClinicId={}, MedicineCount={}",
	                dto.getClinicId(),
	                existingMedicines.size());

	        DoctorPrescription saved =
	                repository.save(entity);

	        log.info("Prescription saved successfully. PrescriptionId={}, ClinicId={}",
	                saved.getId(),
	                saved.getClinicId());

	        DoctorPrescriptionDTO responseDTO =
	                new DoctorPrescriptionDTO();

	        responseDTO.setId(saved.getId());
	        responseDTO.setClinicId(saved.getClinicId());

	        responseDTO.setMedicines(
	                saved.getMedicines()
	                        .stream()
	                        .map(m -> new MedicineDTO(
	                                m.getId(),
	                                m.getName(),
	                                m.getDose(),
	                                m.getDuration(),
	                                m.getDurationUnit(),
	                                m.getNote(),
	                                m.getFood(),
	                                m.getMedicineType(),
	                                m.getRemindWhen(),
	                                m.getTimes(),
	                                m.getOthers(),
	                                m.getSerialNumber(),
	                                m.getGenericName(),
	                                m.getBrandName(),
	                                m.getNameAndAddressOfTheManufacturer(),
	                                m.getBatchNumber(),
	                                m.getDateOfManufacturing(),
	                                m.getDateOfExpriy(),
	                                m.getManufacturingLicenseNumber(),
	                                m.getStock()))
	                        .collect(Collectors.toList()));

	        String finalMessage;

	        if (updatedExistingMedicine && addedNewMedicine) {
	            finalMessage =
	                    "Prescription updated with new and existing medicines";
	        } else if (updatedExistingMedicine) {
	            finalMessage =
	                    "Existing medicines updated successfully";
	        } else if (addedNewMedicine) {
	            finalMessage =
	                    "Prescription created successfully";
	        } else {
	            finalMessage =
	                    "No changes were made to the prescription";
	        }

	        log.info("Prescription operation completed successfully. Message={}",
	                finalMessage);

	        return new Response(
	                true,
	                responseDTO,
	                finalMessage,
	                HttpStatus.CREATED.value());

	    } catch (Exception e) {

	        log.error(
	                "Failed to create/update prescription. ClinicId={}, Error={}",
	                dto != null ? dto.getClinicId() : null,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to create/update prescription: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());

	    } finally {

	        log.info(
	                "createPrescription completed. ClinicId={}, ExecutionTime={} ms",
	                dto != null ? dto.getClinicId() : null,
	                (System.currentTimeMillis() - startTime));
	    }
	}

	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getPrescriptionsByClinicIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response getPrescriptionsByClinicId(String clinicId) {

	    long startTime = System.currentTimeMillis();
	    log.info("Entered getPrescriptionsByClinicId() with clinicId : {}", clinicId);

	    try {

	    	log.debug("Fetching prescriptions from repository for clinicId : {}", clinicId);

	        List<DoctorPrescription> prescriptions = repository.findByClinicId(clinicId);

	        log.info("Retrieved {} prescriptions for clinicId : {}", prescriptions.size(), clinicId);

	        List<DoctorPrescriptionDTO> dtos = prescriptions.stream().map(p -> {

	            DoctorPrescriptionDTO dto = new DoctorPrescriptionDTO();
	            dto.setId(p.getId());
	            dto.setClinicId(p.getClinicId());

	            List<MedicineDTO> meds = Optional.ofNullable(p.getMedicines())
	                    .orElse(List.of())
	                    .stream()
	                    .map(m -> new MedicineDTO(
	                            m.getId(),
	                            m.getName(),
	                            m.getDose(),
	                            m.getDuration(),
	                            m.getDurationUnit(),
	                            m.getNote(),
	                            m.getFood(),
	                            m.getMedicineType(),
	                            m.getRemindWhen(),
	                            m.getTimes(),
	                            m.getOthers(),
	                            m.getSerialNumber(),
	                            m.getGenericName(),
	                            m.getBrandName(),
	                            m.getNameAndAddressOfTheManufacturer(),
	                            m.getBatchNumber(),
	                            m.getDateOfManufacturing(),
	                            m.getDateOfExpriy(),
	                            m.getManufacturingLicenseNumber(),
	                            m.getStock()))
	                    .collect(Collectors.toList());

	            dto.setMedicines(meds);
	            return dto;

	        }).collect(Collectors.toList());

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("Successfully fetched prescriptions for clinicId : {}. Total records : {}. Execution Time : {} ms",
	                clinicId, dtos.size(), executionTime);

	        return new Response(
	                true,
	                dtos,
	                "Prescriptions fetched successfully for clinicId: " + clinicId,
	                HttpStatus.OK.value());

	    } catch (Exception e) {

	    	log.error("Exception occurred while fetching prescriptions for clinicId : {}. Error : {}",
	                clinicId,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Error fetching prescriptions by clinicId: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updatePrescriptionFallback")
	@Secured("ROLE_DOCTOR")
	public Response updatePrescription(String id, DoctorPrescriptionDTO dto) {

	    long startTime = System.currentTimeMillis();
	    log.info("Entered updatePrescription() with prescriptionId : {}", id);

	    try {

	        log.debug("Fetching prescription from repository with id : {}", id);

	        Optional<DoctorPrescription> optional = repository.findById(id);

	        if (optional.isEmpty()) {

	            log.warn("Prescription not found with id : {}", id);

	            return new Response(
	                    false,
	                    null,
	                    "Prescription not found with id: " + id,
	                    HttpStatus.NOT_FOUND.value());
	        }

	        DoctorPrescription existingPrescription = optional.get();

	        log.info("Prescription found. Updating details for id : {}", id);

	        if (dto.getClinicId() != null && !dto.getClinicId().isBlank()) {

	            log.debug("Updating clinicId from {} to {}",
	                    existingPrescription.getClinicId(),
	                    dto.getClinicId());

	            existingPrescription.setClinicId(dto.getClinicId());
	        }

	        List<Medicine> updatedMedicines = new ArrayList<>();

	        if (dto.getMedicines() != null && !dto.getMedicines().isEmpty()) {

	            log.info("Processing {} medicines for prescription id : {}",
	                    dto.getMedicines().size(),
	                    id);

	            for (MedicineDTO medDto : dto.getMedicines()) {

	                if (medDto.getId() != null) {

	                    Optional<Medicine> existingMedOpt =
	                            existingPrescription.getMedicines()
	                                    .stream()
	                                    .filter(m -> m.getId().equals(medDto.getId()))
	                                    .findFirst();

	                    if (existingMedOpt.isPresent()) {

	                        log.debug("Updating existing medicine with id : {}",
	                                medDto.getId());

	                        Medicine existingMed = existingMedOpt.get();

	                        existingMed.setName(medDto.getName());
	                        existingMed.setDose(medDto.getDose());
	                        existingMed.setDuration(medDto.getDuration());
	                        existingMed.setDurationUnit(medDto.getDurationUnit());
	                        existingMed.setNote(medDto.getNote());
	                        existingMed.setFood(medDto.getFood());
	                        existingMed.setMedicineType(medDto.getMedicineType());
	                        existingMed.setRemindWhen(medDto.getRemindWhen());
	                        existingMed.setTimes(medDto.getTimes());
	                        existingMed.setOthers(medDto.getOthers());
	                        existingMed.setSerialNumber(medDto.getSerialNumber());
	                        existingMed.setGenericName(medDto.getGenericName());
	                        existingMed.setBrandName(medDto.getBrandName());
	                        existingMed.setNameAndAddressOfTheManufacturer(
	                                medDto.getNameAndAddressOfTheManufacturer());
	                        existingMed.setBatchNumber(medDto.getBatchNumber());
	                        existingMed.setDateOfManufacturing(medDto.getDateOfManufacturing());
	                        existingMed.setDateOfExpriy(medDto.getDateOfExpriy());
	                        existingMed.setManufacturingLicenseNumber(
	                                medDto.getManufacturingLicenseNumber());
	                        existingMed.setStock(medDto.getStock());

	                        updatedMedicines.add(existingMed);

	                    } else {

	                        log.info("Medicine id : {} not found. Creating new medicine entry.",
	                                medDto.getId());

	                        updatedMedicines.add(new Medicine(
	                                medDto.getId(),
	                                medDto.getName(),
	                                medDto.getDose(),
	                                medDto.getDuration(),
	                                medDto.getDurationUnit(),
	                                medDto.getNote(),
	                                medDto.getFood(),
	                                medDto.getMedicineType(),
	                                medDto.getRemindWhen(),
	                                medDto.getOthers(),
	                                medDto.getTimes(),
	                                medDto.getSerialNumber(),
	                                medDto.getGenericName(),
	                                medDto.getBrandName(),
	                                medDto.getNameAndAddressOfTheManufacturer(),
	                                medDto.getBatchNumber(),
	                                medDto.getDateOfManufacturing(),
	                                medDto.getDateOfExpriy(),
	                                medDto.getManufacturingLicenseNumber(),
	                                medDto.getStock()));
	                    }

	                } else {

	                    String generatedMedicineId = UUID.randomUUID().toString();

	                    log.info("Creating new medicine with generated id : {}",
	                            generatedMedicineId);

	                    updatedMedicines.add(new Medicine(
	                            generatedMedicineId,
	                            medDto.getName(),
	                            medDto.getDose(),
	                            medDto.getDuration(),
	                            medDto.getDurationUnit(),
	                            medDto.getNote(),
	                            medDto.getFood(),
	                            medDto.getMedicineType(),
	                            medDto.getRemindWhen(),
	                            medDto.getOthers(),
	                            medDto.getTimes(),
	                            medDto.getSerialNumber(),
	                            medDto.getGenericName(),
	                            medDto.getBrandName(),
	                            medDto.getNameAndAddressOfTheManufacturer(),
	                            medDto.getBatchNumber(),
	                            medDto.getDateOfManufacturing(),
	                            medDto.getDateOfExpriy(),
	                            medDto.getManufacturingLicenseNumber(),
	                            medDto.getStock()));
	                }
	            }
	        }

	        existingPrescription.setMedicines(updatedMedicines);

	        log.debug("Saving updated prescription with id : {}", id);

	        DoctorPrescription saved = repository.save(existingPrescription);

	        log.info("Prescription saved successfully with id : {}", saved.getId());

	        DoctorPrescriptionDTO responseDTO = new DoctorPrescriptionDTO();
	        responseDTO.setId(saved.getId());
	        responseDTO.setClinicId(saved.getClinicId());

	        responseDTO.setMedicines(
	                saved.getMedicines()
	                        .stream()
	                        .map(m -> new MedicineDTO(
	                                m.getId(),
	                                m.getName(),
	                                m.getDose(),
	                                m.getDuration(),
	                                m.getDurationUnit(),
	                                m.getNote(),
	                                m.getFood(),
	                                m.getMedicineType(),
	                                m.getRemindWhen(),
	                                m.getTimes(),
	                                m.getOthers(),
	                                m.getSerialNumber(),
	                                m.getGenericName(),
	                                m.getBrandName(),
	                                m.getNameAndAddressOfTheManufacturer(),
	                                m.getBatchNumber(),
	                                m.getDateOfManufacturing(),
	                                m.getDateOfExpriy(),
	                                m.getManufacturingLicenseNumber(),
	                                m.getStock()))
	                        .collect(Collectors.toList()));

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("updatePrescription() completed successfully for id : {} in {} ms",
	                id,
	                executionTime);

	        return new Response(
	                true,
	                responseDTO,
	                "Prescription updated successfully",
	                HttpStatus.OK.value());

	    } catch (Exception e) {

	        log.error("Exception occurred while updating prescription id : {}. Error : {}",
	                id,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to update prescription: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateMedicineByIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response updateMedicineById(String medicineId, MedicineDTO dto) {

	    long startTime = System.currentTimeMillis();
	    log.info("Entered updateMedicineById() with medicineId : {}", medicineId);

	    try {

	        log.debug("Searching medicine in prescriptions. medicineId : {}", medicineId);

	        Optional<DoctorPrescription> prescriptionOpt = repository.findAll()
	                .stream()
	                .filter(p -> p.getMedicines() != null
	                        && p.getMedicines()
	                                .stream()
	                                .anyMatch(m -> m.getId().equals(medicineId)))
	                .findFirst();

	        if (prescriptionOpt.isEmpty()) {

	            log.warn("Medicine not found with id : {}", medicineId);

	            return new Response(
	                    false,
	                    null,
	                    "Medicine not found with id: " + medicineId,
	                    HttpStatus.NOT_FOUND.value());
	        }

	        DoctorPrescription prescription = prescriptionOpt.get();

	        log.info("Medicine found in prescription id : {}", prescription.getId());

	        Medicine medicine = prescription.getMedicines()
	                .stream()
	                .filter(m -> m.getId().equals(medicineId))
	                .findFirst()
	                .get();

	        log.debug("Updating medicine details for medicineId : {}", medicineId);

	        if (dto.getName() != null)
	            medicine.setName(dto.getName());

	        if (dto.getDose() != null)
	            medicine.setDose(dto.getDose());

	        if (dto.getDuration() != null)
	            medicine.setDuration(dto.getDuration());

	        if (dto.getDurationUnit() != null)
	            medicine.setDurationUnit(dto.getDurationUnit());

	        if (dto.getNote() != null)
	            medicine.setNote(dto.getNote());

	        if (dto.getFood() != null)
	            medicine.setFood(dto.getFood());

	        if (dto.getMedicineType() != null)
	            medicine.setMedicineType(dto.getMedicineType());

	        if (dto.getRemindWhen() != null)
	            medicine.setRemindWhen(dto.getRemindWhen());

	        if (dto.getTimes() != null)
	            medicine.setTimes(dto.getTimes());

	        if (dto.getOthers() != null)
	            medicine.setOthers(dto.getOthers());

	        if (dto.getSerialNumber() != null)
	            medicine.setSerialNumber(dto.getSerialNumber());

	        if (dto.getGenericName() != null)
	            medicine.setGenericName(dto.getGenericName());

	        if (dto.getBrandName() != null)
	            medicine.setBrandName(dto.getBrandName());

	        if (dto.getNameAndAddressOfTheManufacturer() != null)
	            medicine.setNameAndAddressOfTheManufacturer(
	                    dto.getNameAndAddressOfTheManufacturer());

	        if (dto.getBatchNumber() != null)
	            medicine.setBatchNumber(dto.getBatchNumber());

	        if (dto.getDateOfManufacturing() != null)
	            medicine.setDateOfManufacturing(dto.getDateOfManufacturing());

	        if (dto.getDateOfExpriy() != null)
	            medicine.setDateOfExpriy(dto.getDateOfExpriy());

	        if (dto.getManufacturingLicenseNumber() != null)
	            medicine.setManufacturingLicenseNumber(
	                    dto.getManufacturingLicenseNumber());

	        if (dto.getStock() != null)
	            medicine.setStock(dto.getStock());

	        log.debug("Saving prescription after medicine update. prescriptionId : {}",
	                prescription.getId());

	        repository.save(prescription);

	        log.info("Medicine updated successfully. medicineId : {}", medicineId);

	        MedicineDTO updatedDto = new MedicineDTO(
	                medicine.getId(),
	                medicine.getName(),
	                medicine.getDose(),
	                medicine.getDuration(),
	                medicine.getDurationUnit(),
	                medicine.getNote(),
	                medicine.getFood(),
	                medicine.getMedicineType(),
	                medicine.getRemindWhen(),
	                medicine.getTimes(),
	                medicine.getOthers(),
	                medicine.getSerialNumber(),
	                medicine.getGenericName(),
	                medicine.getBrandName(),
	                medicine.getNameAndAddressOfTheManufacturer(),
	                medicine.getBatchNumber(),
	                medicine.getDateOfManufacturing(),
	                medicine.getDateOfExpriy(),
	                medicine.getManufacturingLicenseNumber(),
	                medicine.getStock());

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info(
	                "updateMedicineById() completed successfully for medicineId : {} in {} ms",
	                medicineId,
	                executionTime);

	        return new Response(
	                true,
	                updatedDto,
	                "Medicine updated successfully",
	                HttpStatus.OK.value());

	    } catch (Exception e) {

	        log.error(
	                "Exception occurred while updating medicineId : {}. Error : {}",
	                medicineId,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to update medicine: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAllPrescriptionsFallback")
	@Secured("ROLE_DOCTOR")
	public Response getAllPrescriptions() {

	    long startTime = System.currentTimeMillis();

	    log.info("Get all prescriptions request received");

	    try {

	        log.debug("Fetching all prescriptions from repository");

	        List<DoctorPrescription> prescriptions = repository.findAll();

	        log.info("Total prescriptions fetched from database={}",
	                prescriptions.size());

	        List<DoctorPrescriptionDTO> dtos = prescriptions.stream()
	                .map(p -> {

	                    log.debug(
	                            "Processing prescription. PrescriptionId={}, ClinicId={}, MedicineCount={}",
	                            p.getId(),
	                            p.getClinicId(),
	                            p.getMedicines() != null
	                                    ? p.getMedicines().size()
	                                    : 0);

	                    DoctorPrescriptionDTO dto =
	                            new DoctorPrescriptionDTO();

	                    dto.setId(p.getId());
	                    dto.setClinicId(p.getClinicId());

	                    List<MedicineDTO> meds =
	                            Optional.ofNullable(p.getMedicines())
	                                    .orElse(List.of())
	                                    .stream()
	                                    .map(m -> new MedicineDTO(
	                                            m.getId(),
	                                            m.getName(),
	                                            m.getDose(),
	                                            m.getDuration(),
	                                            m.getDurationUnit(),
	                                            m.getNote(),
	                                            m.getFood(),
	                                            m.getMedicineType(),
	                                            m.getRemindWhen(),
	                                            m.getTimes(),
	                                            m.getOthers(),
	                                            m.getSerialNumber(),
	                                            m.getGenericName(),
	                                            m.getBrandName(),
	                                            m.getNameAndAddressOfTheManufacturer(),
	                                            m.getBatchNumber(),
	                                            m.getDateOfManufacturing(),
	                                            m.getDateOfExpriy(),
	                                            m.getManufacturingLicenseNumber(),
	                                            m.getStock()))
	                                    .collect(Collectors.toList());

	                    dto.setMedicines(meds);

	                    return dto;

	                })
	                .collect(Collectors.toList());

	        log.info(
	                "Successfully fetched all prescriptions. ResponseCount={}",
	                dtos.size());

	        return new Response(
	                true,
	                dtos,
	                "Fetched all prescriptions successfully",
	                HttpStatus.OK.value());

	    } catch (Exception e) {

	        log.error(
	                "Exception occurred while fetching prescriptions. Error={}",
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Error fetching prescriptions: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());

	    } finally {

	        log.info(
	                "getAllPrescriptions completed. ExecutionTime={} ms",
	                (System.currentTimeMillis() - startTime));
	    }
	}
	
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getPrescriptionByIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response getPrescriptionById(String id) {

	    long startTime = System.currentTimeMillis();

	    log.info(
	            "Get prescription by id request received. PrescriptionId={}",
	            id);

	    try {

	        log.debug(
	                "Fetching prescription from repository. PrescriptionId={}",
	                id);

	        Optional<DoctorPrescription> optional =
	                repository.findById(id);

	        if (optional.isPresent()) {

	            DoctorPrescription p = optional.get();

	            log.info(
	                    "Prescription found. PrescriptionId={}, ClinicId={}",
	                    p.getId(),
	                    p.getClinicId());

	            DoctorPrescriptionDTO dto =
	                    new DoctorPrescriptionDTO();

	            dto.setId(p.getId());
	            dto.setClinicId(p.getClinicId());

	            List<MedicineDTO> meds =
	                    Optional.ofNullable(p.getMedicines())
	                            .orElse(List.of())
	                            .stream()
	                            .map(m -> new MedicineDTO(
	                                    m.getId(),
	                                    m.getName(),
	                                    m.getDose(),
	                                    m.getDuration(),
	                                    m.getDurationUnit(),
	                                    m.getNote(),
	                                    m.getFood(),
	                                    m.getMedicineType(),
	                                    m.getRemindWhen(),
	                                    m.getTimes(),
	                                    m.getOthers(),
	                                    m.getSerialNumber(),
	                                    m.getGenericName(),
	                                    m.getBrandName(),
	                                    m.getNameAndAddressOfTheManufacturer(),
	                                    m.getBatchNumber(),
	                                    m.getDateOfManufacturing(),
	                                    m.getDateOfExpriy(),
	                                    m.getManufacturingLicenseNumber(),
	                                    m.getStock()))
	                            .collect(Collectors.toList());

	            dto.setMedicines(meds);

	            log.info(
	                    "Returning prescription successfully. PrescriptionId={}",
	                    id);

	            return new Response(
	                    true,
	                    dto,
	                    "Prescription found",
	                    HttpStatus.OK.value());

	        } else {

	            log.warn(
	                    "Prescription not found. PrescriptionId={}",
	                    id);

	            return new Response(
	                    false,
	                    null,
	                    "Prescription not found",
	                    HttpStatus.NOT_FOUND.value());
	        }

	    } catch (Exception e) {

	        log.error(
	                "Exception occurred while fetching prescription. PrescriptionId={}, Error={}",
	                id,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Error retrieving prescription: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());

	    } finally {

	        log.info(
	                "getPrescriptionById completed. PrescriptionId={}, ExecutionTime={} ms",
	                id,
	                (System.currentTimeMillis() - startTime));
	    }
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getMedicineByIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response getMedicineById(String medicineId) {

	    long startTime = System.currentTimeMillis();

	    log.info(
	            "Get medicine request received. MedicineId={}",
	            medicineId);

	    try {

	        log.debug(
	                "Searching medicine in repository. MedicineId={}",
	                medicineId);

	        List<Medicine> matches =
	                repository.findAll()
	                        .stream()
	                        .flatMap(p ->
	                                Optional.ofNullable(p.getMedicines())
	                                        .orElse(List.of())
	                                        .stream())
	                        .filter(m ->
	                                m.getId() != null
	                                        && m.getId().equals(medicineId))
	                        .distinct()
	                        .collect(Collectors.toList());

	        log.info(
	                "Medicine search completed. MedicineId={}, MatchCount={}",
	                medicineId,
	                matches.size());

	        if (!matches.isEmpty()) {

	            List<MedicineDTO> dtos =
	                    matches.stream()
	                            .map(m -> new MedicineDTO(
	                                    m.getId(),
	                                    m.getName(),
	                                    m.getDose(),
	                                    m.getDuration(),
	                                    m.getDurationUnit(),
	                                    m.getNote(),
	                                    m.getFood(),
	                                    m.getMedicineType(),
	                                    m.getRemindWhen(),
	                                    m.getTimes(),
	                                    m.getOthers(),
	                                    m.getSerialNumber(),
	                                    m.getGenericName(),
	                                    m.getBrandName(),
	                                    m.getNameAndAddressOfTheManufacturer(),
	                                    m.getBatchNumber(),
	                                    m.getDateOfManufacturing(),
	                                    m.getDateOfExpriy(),
	                                    m.getManufacturingLicenseNumber(),
	                                    m.getStock()))
	                            .collect(Collectors.toList());

	            log.info(
	                    "Medicine found successfully. MedicineId={}",
	                    medicineId);

	            return new Response(
	                    true,
	                    dtos,
	                    "Medicine found",
	                    HttpStatus.OK.value());

	        } else {

	            log.warn(
	                    "Medicine not found. MedicineId={}",
	                    medicineId);

	            return new Response(
	                    false,
	                    null,
	                    "No medicine found with given ID",
	                    HttpStatus.NOT_FOUND.value());
	        }

	    } catch (Exception e) {

	        log.error(
	                "Exception occurred while fetching medicine. MedicineId={}, Error={}",
	                medicineId,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Error while fetching medicine: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());

	    } finally {

	        log.info(
	                "getMedicineById completed. MedicineId={}, ExecutionTime={} ms",
	                medicineId,
	                (System.currentTimeMillis() - startTime));
	    }
	}
	
	
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "deletePrescriptionFallback")
	@Secured("ROLE_DOCTOR")
	public Response deletePrescription(String id) {

	    long startTime = System.currentTimeMillis();

	    log.info("Delete prescription request received. PrescriptionId={}", id);

	    try {

	        log.debug("Checking prescription existence. PrescriptionId={}", id);

	        boolean exists = repository.existsById(id);

	        log.debug("Prescription existence check result. PrescriptionId={}, Exists={}",
	                id,
	                exists);

	        if (exists) {

	            log.info("Deleting prescription. PrescriptionId={}", id);

	            repository.deleteById(id);

	            log.info("Prescription deleted successfully. PrescriptionId={}", id);

	            return new Response(
	                    true,
	                    null,
	                    "Prescription deleted successfully",
	                    HttpStatus.OK.value());

	        } else {

	            log.warn("Prescription not found. PrescriptionId={}", id);

	            return new Response(
	                    false,
	                    null,
	                    "Prescription not found",
	                    HttpStatus.NOT_FOUND.value());
	        }

	    } catch (Exception e) {

	        log.error(
	                "Failed to delete prescription. PrescriptionId={}, Error={}",
	                id,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to delete: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());

	    } finally {

	        log.info(
	                "deletePrescription completed. PrescriptionId={}, ExecutionTime={} ms",
	                id,
	                (System.currentTimeMillis() - startTime));
	    }
	}
	
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "deleteMedicineByIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response deleteMedicineById(String medicineId) {

	    long startTime = System.currentTimeMillis();

	    log.info(
	            "Delete medicine request received. MedicineId={}",
	            medicineId);

	    try {

	        log.debug("Fetching all prescriptions from repository");

	        List<DoctorPrescription> allPrescriptions =
	                repository.findAll();

	        log.info(
	                "Total prescriptions fetched={}",
	                allPrescriptions.size());

	        boolean medicineDeleted = false;

	        for (DoctorPrescription prescription : allPrescriptions) {

	            log.debug(
	                    "Checking prescription. PrescriptionId={}",
	                    prescription.getId());

	            List<Medicine> medicines =
	                    prescription.getMedicines() != null
	                            ? new ArrayList<>(prescription.getMedicines())
	                            : new ArrayList<>();

	            boolean removed =
	                    medicines.removeIf(
	                            med -> med.getId() != null
	                                    && med.getId().equals(medicineId));

	            if (removed) {

	                log.info(
	                        "Medicine found and removed. MedicineId={}, PrescriptionId={}",
	                        medicineId,
	                        prescription.getId());

	                prescription.setMedicines(medicines);

	                log.debug(
	                        "Saving updated prescription. PrescriptionId={}",
	                        prescription.getId());

	                repository.save(prescription);

	                medicineDeleted = true;
	            }
	        }

	        if (medicineDeleted) {

	            log.info(
	                    "Medicine deleted successfully. MedicineId={}",
	                    medicineId);

	            return new Response(
	                    true,
	                    null,
	                    "Medicine deleted successfully",
	                    HttpStatus.OK.value());

	        } else {

	            log.warn(
	                    "Medicine not found. MedicineId={}",
	                    medicineId);

	            return new Response(
	                    false,
	                    null,
	                    "Medicine not found",
	                    HttpStatus.NOT_FOUND.value());
	        }

	    } catch (Exception e) {

	        log.error(
	                "Error while deleting medicine. MedicineId={}, Error={}",
	                medicineId,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Error deleting medicine: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());

	    } finally {

	        log.info(
	                "deleteMedicineById completed. MedicineId={}, ExecutionTime={} ms",
	                medicineId,
	                (System.currentTimeMillis() - startTime));
	    }
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "searchMedicinesByNameFallback")
	@Secured("ROLE_DOCTOR")
	public Response searchMedicinesByName(String keyword) {

	    long startTime = System.currentTimeMillis();

	    log.info(
	            "Search medicine request received. Keyword={}",
	            keyword);

	    try {

	        log.debug("Validating search keyword");

	        if (keyword == null || keyword.trim().isEmpty()) {

	            log.warn("Invalid keyword received for medicine search");

	            return new Response(
	                    false,
	                    null,
	                    "Keyword must not be empty",
	                    HttpStatus.BAD_REQUEST.value());
	        }

	        String normalizedKeyword =
	                keyword.trim()
	                        .replaceAll("\\s+", " ")
	                        .toLowerCase();

	        log.debug(
	                "Normalized keyword={}",
	                normalizedKeyword);

	        log.debug("Searching medicines in repository");

	        Optional<Medicine> latestMedicine =
	                repository.findAll()
	                        .stream()
	                        .flatMap(p ->
	                                Optional.ofNullable(p.getMedicines())
	                                        .orElse(List.of())
	                                        .stream())
	                        .filter(m ->
	                                m.getName() != null
	                                        && m.getName()
	                                                .trim()
	                                                .replaceAll("\\s+", " ")
	                                                .toLowerCase()
	                                                .equals(normalizedKeyword))
	                        .max(Comparator.comparing(Medicine::getId));

	        if (latestMedicine.isEmpty()) {

	            log.warn(
	                    "No medicine found with keyword={}",
	                    keyword);

	            return new Response(
	                    false,
	                    null,
	                    "No medicine found with exact name: " + keyword,
	                    HttpStatus.NOT_FOUND.value());
	        }

	        Medicine m = latestMedicine.get();

	        log.info(
	                "Medicine found. MedicineId={}, MedicineName={}",
	                m.getId(),
	                m.getName());

	        MedicineDTO dto =
	                new MedicineDTO(
	                        m.getId(),
	                        m.getName(),
	                        m.getDose(),
	                        m.getDuration(),
	                        m.getDurationUnit(),
	                        m.getNote(),
	                        m.getFood(),
	                        m.getMedicineType(),
	                        m.getRemindWhen(),
	                        m.getTimes(),
	                        m.getOthers(),
	                        m.getSerialNumber(),
	                        m.getGenericName(),
	                        m.getBrandName(),
	                        m.getNameAndAddressOfTheManufacturer(),
	                        m.getBatchNumber(),
	                        m.getDateOfManufacturing(),
	                        m.getDateOfExpriy(),
	                        m.getManufacturingLicenseNumber(),
	                        m.getStock());

	        log.info(
	                "Returning medicine search response successfully. MedicineId={}",
	                m.getId());

	        return new Response(
	                true,
	                List.of(dto),
	                "Medicine found",
	                HttpStatus.OK.value());

	    } catch (Exception e) {

	        log.error(
	                "Error while searching medicine. Keyword={}, Error={}",
	                keyword,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Error searching medicine: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());

	    } finally {

	        log.info(
	                "searchMedicinesByName completed. Keyword={}, ExecutionTime={} ms",
	                keyword,
	                (System.currentTimeMillis() - startTime));
	    }
	}
	
	
    private Response buildRateLimitResponse(Exception ex) {
        return new Response(false, null,
                "Rate limit exceeded. Please try again later.",
                429);
    }

    public Response createPrescriptionFallback(DoctorPrescriptionDTO dto, Exception ex){ return buildRateLimitResponse(ex); }
    public Response getAllPrescriptionsFallback(Exception ex){ return buildRateLimitResponse(ex); }
    public Response getPrescriptionByIdFallback(String id, Exception ex){ return buildRateLimitResponse(ex); }
    public Response getMedicineByIdFallback(String medicineId, Exception ex){ return buildRateLimitResponse(ex); }
    public Response deletePrescriptionFallback(String id, Exception ex){ return buildRateLimitResponse(ex); }
    public Response deleteMedicineByIdFallback(String medicineId, Exception ex){ return buildRateLimitResponse(ex); }
    public Response searchMedicinesByNameFallback(String keyword, Exception ex){ return buildRateLimitResponse(ex); }
    public Response getPrescriptionsByClinicIdFallback(String clinicId, Exception ex){ return buildRateLimitResponse(ex); }
    public Response updatePrescriptionFallback(String id, DoctorPrescriptionDTO dto, Exception ex){ return buildRateLimitResponse(ex); }
    public Response updateMedicineByIdFallback(String medicineId, MedicineDTO dto, Exception ex){ return buildRateLimitResponse(ex); }

}