package com.chiselon.adminservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionAnswer {
    private String question;
    private boolean answer; // Only "Yes" or "No"
}