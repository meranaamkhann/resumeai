package com.resumeai.controller;

import com.resumeai.dto.PlanStatusResponse;
import com.resumeai.repository.UserRepository;
import com.resumeai.security.CurrentUser;
import com.resumeai.service.billing.BillingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;
    private final UserRepository userRepository;

    public BillingController(BillingService billingService, UserRepository userRepository) {
        this.billingService = billingService;
        this.userRepository = userRepository;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(HttpServletRequest request,
                                         @RequestHeader(value = "Stripe-Signature", required = false) String signature) throws IOException {
        String payload = readBody(request);
        billingService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/plan")
    public ResponseEntity<PlanStatusResponse> plan() {
        var user = CurrentUser.get();
        String plan = userRepository.findById(user.userId()).map(u -> u.getPlan().name()).orElse("FREE");
        return ResponseEntity.ok(new PlanStatusResponse(plan));
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}

