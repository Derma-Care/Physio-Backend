package com.chiselon.adminservice.service;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chiselon.adminservice.dto.ClinicTimingDTO;
import com.chiselon.adminservice.entity.ClinicTiming;
import com.chiselon.adminservice.repository.ClinicTimingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j

@RequiredArgsConstructor
public class ClinicTimingServiceImpl implements ClinicTimingService {
    private final ClinicTimingRepository repo;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private static final int OPEN_START = 7;   // 07:00 AM
    private static final int OPEN_END   = 22;  // 10:00 PM

    @Override
    @Transactional
    public List<ClinicTimingDTO> createTimings(ClinicTimingDTO dto) {

        log.info("Creating clinic timings. OpeningTime: {}, ClosingTime: {}",
                dto.getOpeningTime(), dto.getClosingTime());

        LocalTime start = LocalTime.parse(dto.getOpeningTime(), FMT);
        log.debug("Parsed opening time: {}", start);

        validateHour(start.getHour());
        log.debug("Validated opening hour: {}", start.getHour());

        List<ClinicTimingDTO> generated = new ArrayList<>();

        if (dto.getClosingTime() == null || dto.getClosingTime().isBlank()) {

            log.info("Closing time not provided. Creating a single 1-hour slot.");

            LocalTime end = start.plusHours(1);
            generated.add(saveSlot(start, end));

            log.info("Successfully created slot: {} - {}", start, end);

        } else {

            LocalTime endRange = LocalTime.parse(dto.getClosingTime(), FMT);
            log.debug("Parsed closing time: {}", endRange);

            if (!endRange.isAfter(start)) {
                log.error("Invalid timing. Closing time {} is not after opening time {}",
                        endRange, start);
                throw new IllegalArgumentException("closingTime must be after openingTime");
            }

            log.info("Generating hourly slots between {} and {}", start, endRange);

            for (LocalTime s = start; s.isBefore(endRange); s = s.plusHours(1)) {

                validateHour(s.getHour());

                LocalTime e = s.plusHours(1);

                log.debug("Creating slot: {} - {}", s, e);

                generated.add(saveSlot(s, e));
            }
        }

        log.info("Clinic timings created successfully. Total slots generated: {}",
                generated.size());

        return generated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicTimingDTO> getAllTimings() {

        log.info("Fetching all clinic timings.");

        seedIfEmpty();

        List<ClinicTimingDTO> timings = repo.findAllByOrderByStartHourAsc()
                .stream()
                .map(t -> new ClinicTimingDTO(t.getOpeningTime(), t.getClosingTime()))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} clinic timing slots.", timings.size());

        return timings;
    }

    private ClinicTimingDTO saveSlot(LocalTime s, LocalTime e) {

        log.debug("Saving clinic timing slot: {} - {}", s, e);

        ClinicTiming saved = repo.save(
                new ClinicTiming(null, s.getHour(), s.format(FMT), e.format(FMT)));

        log.info("Clinic timing slot saved successfully. Id: {}, Opening: {}, Closing: {}",
                saved.getId(), saved.getOpeningTime(), saved.getClosingTime());

        return new ClinicTimingDTO(saved.getOpeningTime(), saved.getClosingTime());
    }

    private void validateHour(int hour) {

        log.debug("Validating hour: {}", hour);

        if (hour < OPEN_START || hour >= OPEN_END) {

            log.error("Invalid hour received: {}. Allowed range is {}:00 AM to {}:00 PM.",
                    hour, OPEN_START, OPEN_END);

            throw new IllegalArgumentException(
                    "Hour must be between 07:00 AM and 09:00 PM");
        }

        log.debug("Hour {} validated successfully.", hour);
    }

    @Transactional
    protected void seedIfEmpty() {

        log.info("Checking whether clinic timing data already exists.");

        long count = repo.count();

        if (count > 0) {
            log.info("Clinic timing data already exists. Total records: {}. Skipping seed process.", count);
            return;
        }

        log.info("No clinic timings found. Seeding default timing slots.");

        IntStream.rangeClosed(OPEN_START, OPEN_END - 1)
                .forEach(h -> {
                    log.debug("Creating default slot: {}:00 - {}:00", h, h + 1);
                    saveSlot(LocalTime.of(h, 0), LocalTime.of(h + 1, 0));
                });

        log.info("Default clinic timing slots seeded successfully.");
    }
}