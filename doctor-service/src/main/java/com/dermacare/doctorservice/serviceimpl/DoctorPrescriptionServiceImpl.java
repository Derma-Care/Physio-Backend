package com.dermacare.doctorservice.serviceimpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.dermacare.doctorservice.dto.DoctorPrescriptionDTO;
import com.dermacare.doctorservice.dto.MedicineDTO;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.model.DoctorPrescription;
import com.dermacare.doctorservice.model.Medicine;
import com.dermacare.doctorservice.repository.DoctorPrescriptionRepository;
import com.dermacare.doctorservice.service.DoctorPrescriptionService;

@Service
public class DoctorPrescriptionServiceImpl implements DoctorPrescriptionService {

    @Autowired
    private DoctorPrescriptionRepository repository;


    @Override
    public Response createPrescription(DoctorPrescriptionDTO dto) {
        try {
            // 1. Validate input
            if (dto == null || dto.getMedicines() == null || dto.getMedicines().isEmpty()) {
                return new Response(false, null,
                        "Prescription must have at least one medicine",
                        HttpStatus.BAD_REQUEST.value());
            }

            if (dto.getClinicId() == null || dto.getClinicId().isBlank()) {
                return new Response(false, null,
                        "Clinic ID is required",
                        HttpStatus.BAD_REQUEST.value());
            }

            // 2. Fetch existing prescription for this clinic
            List<DoctorPrescription> prescriptions = repository.findByClinicId(dto.getClinicId());
            DoctorPrescription entity = prescriptions.isEmpty()
                    ? new DoctorPrescription()
                    : prescriptions.get(0); // only one prescription per clinic

            entity.setClinicId(dto.getClinicId());

            List<Medicine> existingMedicines = Optional.ofNullable(entity.getMedicines())
                    .orElse(new ArrayList<>());

            boolean updatedExistingMedicine = false;
            boolean addedNewMedicine = false;

            // 3. Process incoming medicines
            for (MedicineDTO incomingMed : dto.getMedicines()) {
                if (incomingMed == null || incomingMed.getName() == null || incomingMed.getName().isBlank()) {
                    continue;
                }

                String normalizedName = incomingMed.getName().trim().replaceAll("\\s+", " ").toLowerCase();

                Optional<Medicine> existingMedOpt = existingMedicines.stream()
                        .filter(m -> m.getName() != null &&
                                m.getName().trim().replaceAll("\\s+", " ").toLowerCase().equals(normalizedName))
                        .findFirst();

                if (existingMedOpt.isPresent()) {
                    // Update existing medicine
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
                    existingMed.setNameAndAddressOfTheManufacturer(incomingMed.getNameAndAddressOfTheManufacturer());
                    existingMed.setBatchNumber(incomingMed.getBatchNumber());               
                    existingMed.setDateOfManufacturing(incomingMed.getDateOfManufacturing());               
                    existingMed.setDateOfExpriy(incomingMed.getDateOfExpriy());               
                    existingMed.setManufacturingLicenseNumber(incomingMed.getManufacturingLicenseNumber());               
                    existingMed.setStock(incomingMed.getStock());               

                    updatedExistingMedicine = true;
                } else {
                    // Add new medicine
                    existingMedicines.add(new Medicine(
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
                            incomingMed.getStock()
                    ));
                    addedNewMedicine = true;
                }
            }

            // 4. Save prescription
            entity.setMedicines(existingMedicines);
            DoctorPrescription saved = repository.save(entity);

            // 5. Build response DTO
            DoctorPrescriptionDTO responseDTO = new DoctorPrescriptionDTO();
            responseDTO.setId(saved.getId());
            responseDTO.setClinicId(saved.getClinicId());
            responseDTO.setMedicines(saved.getMedicines().stream()
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
                            m.getStock()
                    ))
                    .collect(Collectors.toList())
            );

            String finalMessage;
            if (updatedExistingMedicine && addedNewMedicine) {
                finalMessage = "Prescription updated with new and existing medicines";
            } else if (updatedExistingMedicine) {
                finalMessage = "Existing medicines updated successfully";
            } else if (addedNewMedicine) {
                finalMessage = "Prescription created successfully";
            } else {
                finalMessage = "No changes were made to the prescription";
            }

            return new Response(true, responseDTO, finalMessage, HttpStatus.CREATED.value());

        } catch (Exception e) {
            return new Response(false, null,
                    "Failed to create/update prescription: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public Response getAllPrescriptions() {
        try {
            List<DoctorPrescriptionDTO> dtos = repository.findAll().stream().map(p -> {
                DoctorPrescriptionDTO dto = new DoctorPrescriptionDTO();
                dto.setId(p.getId());
                dto.setClinicId(p.getClinicId());

                List<MedicineDTO> meds = Optional.ofNullable(p.getMedicines()).orElse(List.of()).stream()
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
                                m.getStock()
                        ))
                        .collect(Collectors.toList());

                dto.setMedicines(meds);
                return dto;
            }).collect(Collectors.toList());

            return new Response(true, dtos, "Fetched all prescriptions successfully", HttpStatus.OK.value());

        } catch (Exception e) {
            return new Response(false, null, "Error fetching prescriptions: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public Response getPrescriptionById(String id) {
        try {
            Optional<DoctorPrescription> optional = repository.findById(id);
            if (optional.isPresent()) {
                DoctorPrescription p = optional.get();
                DoctorPrescriptionDTO dto = new DoctorPrescriptionDTO();
                dto.setId(p.getId());
                dto.setClinicId(p.getClinicId());

                List<MedicineDTO> meds = Optional.ofNullable(p.getMedicines()).orElse(List.of()).stream()
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
                                m.getStock()
                        ))
                        .collect(Collectors.toList());

                dto.setMedicines(meds);
                return new Response(true, dto, "Prescription found", HttpStatus.OK.value());
            } else {
                return new Response(false, null, "Prescription not found", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            return new Response(false, null, "Error retrieving prescription: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public Response getMedicineById(String medicineId) {
        try {
            List<Medicine> matches = repository.findAll().stream()
                    .flatMap(p -> Optional.ofNullable(p.getMedicines()).orElse(List.of()).stream())
                    .filter(m -> m.getId() != null && m.getId().equals(medicineId))
                    .distinct()
                    .collect(Collectors.toList());

            if (!matches.isEmpty()) {
                List<MedicineDTO> dtos = matches.stream().map(m -> new MedicineDTO(
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
                        m.getStock()
                )).collect(Collectors.toList());

                return new Response(true, dtos, "Medicine found", HttpStatus.OK.value());
            } else {
                return new Response(false, null, "No medicine found with given ID", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            return new Response(false, null, "Error while fetching medicine: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public Response deletePrescription(String id) {
        try {
            if (repository.existsById(id)) {
                repository.deleteById(id);
                return new Response(true, null, "Prescription deleted successfully", HttpStatus.OK.value());
            } else {
                return new Response(false, null, "Prescription not found", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            return new Response(false, null, "Failed to delete: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public Response deleteMedicineById(String medicineId) {
        try {
            List<DoctorPrescription> allPrescriptions = repository.findAll();
            boolean medicineDeleted = false;

            for (DoctorPrescription prescription : allPrescriptions) {
                List<Medicine> medicines = prescription.getMedicines() != null
                        ? new ArrayList<>(prescription.getMedicines())
                        : new ArrayList<>();

                boolean removed = medicines.removeIf(med -> med.getId() != null && med.getId().equals(medicineId));

                if (removed) {
                    prescription.setMedicines(medicines);
                    repository.save(prescription);
                    medicineDeleted = true;
                }
            }

            if (medicineDeleted) {
                return new Response(true, null, "Medicine deleted successfully", HttpStatus.OK.value());
            } else {
                return new Response(false, null, "Medicine not found", HttpStatus.NOT_FOUND.value());
            }

        } catch (Exception e) {
            return new Response(false, null, "Error deleting medicine: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public Response searchMedicinesByName(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return new Response(false, null,
                        "Keyword must not be empty",
                        HttpStatus.BAD_REQUEST.value());
            }

            String normalizedKeyword = keyword.trim().replaceAll("\\s+", " ").toLowerCase();

            Optional<Medicine> latestMedicine = repository.findAll().stream()
                    .flatMap(p -> Optional.ofNullable(p.getMedicines()).orElse(List.of()).stream())
                    .filter(m -> m.getName() != null &&
                            m.getName().trim().replaceAll("\\s+", " ").toLowerCase().equals(normalizedKeyword))
                    .max(Comparator.comparing(Medicine::getId));

            if (latestMedicine.isEmpty()) {
                return new Response(false, null,
                        "No medicine found with exact name: " + keyword,
                        HttpStatus.NOT_FOUND.value());
            }

            Medicine m = latestMedicine.get();
            MedicineDTO dto = new MedicineDTO(
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
                    m.getStock()
            );

            return new Response(true, List.of(dto), "Medicine found", HttpStatus.OK.value());

        } catch (Exception e) {
            return new Response(false, null,
                    "Error searching medicine: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public Response getPrescriptionsByClinicId(String clinicId) {
        try {
            List<DoctorPrescription> prescriptions = repository.findByClinicId(clinicId);
            List<DoctorPrescriptionDTO> dtos = prescriptions.stream().map(p -> {
                DoctorPrescriptionDTO dto = new DoctorPrescriptionDTO();
                dto.setId(p.getId());
                dto.setClinicId(p.getClinicId());

                List<MedicineDTO> meds = Optional.ofNullable(p.getMedicines()).orElse(List.of()).stream()
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
                                m.getStock()
                        ))
                        .collect(Collectors.toList());

                dto.setMedicines(meds);
                return dto;
            }).collect(Collectors.toList());

            return new Response(true, dtos, "Prescriptions fetched successfully for clinicId: " + clinicId,
                    HttpStatus.OK.value());

        } catch (Exception e) {
            return new Response(false, null, "Error fetching prescriptions by clinicId: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    @Override
    public Response updatePrescription(String id, DoctorPrescriptionDTO dto) {
        try {
            Optional<DoctorPrescription> optional = repository.findById(id);
            if (optional.isEmpty()) {
                return new Response(false, null,
                        "Prescription not found with id: " + id,
                        HttpStatus.NOT_FOUND.value());
            }

            DoctorPrescription existingPrescription = optional.get();

            // Update clinicId if provided
            if (dto.getClinicId() != null && !dto.getClinicId().isBlank()) {
                existingPrescription.setClinicId(dto.getClinicId());
            }

            List<Medicine> updatedMedicines = new ArrayList<>();

            if (dto.getMedicines() != null && !dto.getMedicines().isEmpty()) {
                for (MedicineDTO medDto : dto.getMedicines()) {
                    if (medDto.getId() != null) {
                        // Try to find existing medicine
                        Optional<Medicine> existingMedOpt = existingPrescription.getMedicines().stream()
                                .filter(m -> m.getId().equals(medDto.getId()))
                                .findFirst();

                        if (existingMedOpt.isPresent()) {
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
                            existingMed.setNameAndAddressOfTheManufacturer(medDto.getNameAndAddressOfTheManufacturer());
                            existingMed.setBatchNumber(medDto.getBatchNumber());
                            existingMed.setDateOfManufacturing(medDto.getDateOfManufacturing());
                            existingMed.setDateOfExpriy(medDto.getDateOfExpriy());
                            existingMed.setManufacturingLicenseNumber(medDto.getManufacturingLicenseNumber());
                            existingMed.setStock(medDto.getStock());

                            updatedMedicines.add(existingMed);
                        } else {
                            // If medicineId not found → treat as new medicine
                            updatedMedicines.add(new Medicine(
                                    medDto.getId() != null ? medDto.getId() : UUID.randomUUID().toString(),
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
                                    medDto.getStock() 
                            ));
                        }
                    } else {
                        // New medicine (no id provided)
                        updatedMedicines.add(new Medicine(
                                UUID.randomUUID().toString(),
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
                                medDto.getStock()
                        ));
                    }
                }
            }

            existingPrescription.setMedicines(updatedMedicines);
            DoctorPrescription saved = repository.save(existingPrescription);

            // Convert back to DTO
            DoctorPrescriptionDTO responseDTO = new DoctorPrescriptionDTO();
            responseDTO.setId(saved.getId());
            responseDTO.setClinicId(saved.getClinicId());
            responseDTO.setMedicines(saved.getMedicines().stream()
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
                            m.getStock()
                    ))
                    .collect(Collectors.toList())
            );

            return new Response(true, responseDTO,
                    "Prescription updated successfully",
                    HttpStatus.OK.value());

        } catch (Exception e) {
            return new Response(false, null,
                    "Failed to update prescription: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    @Override
    public Response updateMedicineById(String medicineId, MedicineDTO dto) {
        try {
            // Find the prescription that contains this medicine
            Optional<DoctorPrescription> prescriptionOpt = repository.findAll().stream()
                    .filter(p -> p.getMedicines() != null &&
                            p.getMedicines().stream().anyMatch(m -> m.getId().equals(medicineId)))
                    .findFirst();

            if (prescriptionOpt.isEmpty()) {
                return new Response(false, null,
                        "Medicine not found with id: " + medicineId,
                        HttpStatus.NOT_FOUND.value());
            }

            DoctorPrescription prescription = prescriptionOpt.get();

            // find the medicine
            Medicine medicine = prescription.getMedicines().stream()
                    .filter(m -> m.getId().equals(medicineId))
                    .findFirst().get();

            // update fields if provided
            if (dto.getName() != null) medicine.setName(dto.getName());
            if (dto.getDose() != null) medicine.setDose(dto.getDose());
            if (dto.getDuration() != null) medicine.setDuration(dto.getDuration());
            if (dto.getDurationUnit() != null) medicine.setDurationUnit(dto.getDurationUnit());
            if (dto.getNote() != null) medicine.setNote(dto.getNote());
            if (dto.getFood() != null) medicine.setFood(dto.getFood());
            if (dto.getMedicineType() != null) medicine.setMedicineType(dto.getMedicineType());
            if (dto.getRemindWhen() != null) medicine.setRemindWhen(dto.getRemindWhen());
            if (dto.getTimes() != null) medicine.setTimes(dto.getTimes());
            if (dto.getOthers() != null) medicine.setOthers(dto.getOthers());
            if (dto.getSerialNumber() != null) medicine.setSerialNumber(dto.getSerialNumber());
            if (dto.getGenericName() != null) medicine.setGenericName(dto.getGenericName());
            if (dto.getBrandName() != null) medicine.setBrandName(dto.getBrandName());
            if (dto.getNameAndAddressOfTheManufacturer() != null)
                medicine.setNameAndAddressOfTheManufacturer(dto.getNameAndAddressOfTheManufacturer());
            if (dto.getBatchNumber() != null) medicine.setBatchNumber(dto.getBatchNumber());
            if (dto.getDateOfManufacturing() != null) medicine.setDateOfManufacturing(dto.getDateOfManufacturing());
            if (dto.getDateOfExpriy() != null) medicine.setDateOfExpriy(dto.getDateOfExpriy());
            if (dto.getManufacturingLicenseNumber() != null)
                medicine.setManufacturingLicenseNumber(dto.getManufacturingLicenseNumber());
            if (dto.getStock() != null)
                medicine.setStock(dto.getStock());

            // save prescription
            repository.save(prescription);

            // convert to DTO for response
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
                    medicine.getStock()
            );

            return new Response(true, updatedDto, "Medicine updated successfully", HttpStatus.OK.value());

        } catch (Exception e) {
            return new Response(false, null, "Failed to update medicine: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }


}
