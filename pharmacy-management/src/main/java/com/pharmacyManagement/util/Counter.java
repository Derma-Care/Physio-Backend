package com.pharmacyManagement.util;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "counters")
public class Counter {

    @Id
    private String id;   // 👉 used as key (ex: date like 20260317)

    private long seq;    // 👉 sequence number (1,2,3...)
}