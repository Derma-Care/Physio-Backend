package physiotherapydoctor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import physiotherapydoctor.dto.Response;
import physiotherapydoctor.service.RecoverySupportService;

@RestController
@RequestMapping("/physiotherapy-doctor")
public class RecoverySupportController {

    @Autowired
    private RecoverySupportService recoverySupportService;

    @GetMapping("/getAllRecoverySupportsByClinicId/{clinicId}")
    public Response getRecoverySupports(@PathVariable String clinicId) {
        return recoverySupportService.getRecoverySupports(clinicId);
    }
}
