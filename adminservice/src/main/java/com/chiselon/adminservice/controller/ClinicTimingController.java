package com.chiselon.adminservice.controller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.chiselon.adminservice.dto.ClinicTimingDTO;
import com.chiselon.adminservice.dto.ResponseDTO;
import com.chiselon.adminservice.service.ClinicTimingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RequiredArgsConstructor
public class ClinicTimingController {

    private final ClinicTimingService service;

    @PostMapping("/saveclinictimings")
    public ResponseEntity<ResponseDTO<List<ClinicTimingDTO>>> create(
            @RequestBody @Valid ClinicTimingDTO dto) {

        List<ClinicTimingDTO> created = service.createTimings(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(true, created,
                        "Clinic timing(s) saved successfully", 201));
    }

    @GetMapping("/getAllClinicTimings")
    public ResponseEntity<ResponseDTO<List<ClinicTimingDTO>>> readAll() {
        List<ClinicTimingDTO> slots = service.getAllTimings();
        return ResponseEntity.ok(
                new ResponseDTO<>(true,
                                  slots,
                                  "Clinic timings fetched successfully",
                                  HttpStatus.OK.value()));
    }
}
