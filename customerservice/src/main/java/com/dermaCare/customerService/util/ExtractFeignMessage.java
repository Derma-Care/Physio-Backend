package com.dermaCare.customerService.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;

public final class ExtractFeignMessage {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ExtractFeignMessage() {
    }

    public static String clearMessage(FeignException e) {

        if (e == null) {
            return "Unknown error";
        }

        String responseBody = e.contentUTF8();

        if (responseBody == null || responseBody.isBlank()) {
            return e.getMessage();
        }

        try {

            JsonNode root = OBJECT_MAPPER.readTree(responseBody);

            if (root.hasNonNull("message")) {
                return root.get("message").asText();
            }

            if (root.hasNonNull("error")) {
                return root.get("error").asText();
            }

            if (root.hasNonNull("details")) {
                return root.get("details").asText();
            }

            return responseBody;

        } catch (Exception ex) {
            return responseBody;
        }
    }
}