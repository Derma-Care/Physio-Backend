package com.dermacare.doctorservice.dermacaredoctorutils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.dermacare.doctorservice.model.MedicineType;
import java.io.IOException;
import java.util.List;

public class MedicineTypeDeserializer extends JsonDeserializer<MedicineType> {

    @Override
    public MedicineType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String typeName = p.getText();
        MedicineType mt = new MedicineType();
        mt.setMedicineTypes(List.of(typeName)); 
        return mt;
    }
}

