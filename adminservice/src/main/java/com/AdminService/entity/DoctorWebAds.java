package com.AdminService.entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "doctorWebAdd")  // <-- specify collection name here
public class DoctorWebAds {
    @Id
    private ObjectId adId;
    private String mediaUrlOrImage;  // base64 image or video URL
}
