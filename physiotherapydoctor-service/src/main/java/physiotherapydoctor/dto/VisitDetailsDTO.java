package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class VisitDetailsDTO {

    private String visitNumber;
    private String visitDate;
    private String visitTime;
    private PhysiotherapyDoctorData physiotherapyDoctorData;

    @Data
    public static class PhysiotherapyDoctorData {

        private String therapistRecordId;
        private String bookingId;
        private String clinicId;
        private String branchId;
        private String createdAt;
        private String updatedAt;
        private PatientInfo patientInfo;
        private String prescriptionPdf;
        private String createdTime;
    }

    @Data
    public static class PatientInfo {

        private String patientId;
        private String patientName;
        private String mobileNumber;
        private Integer age;
        private String sex;
    }
}
