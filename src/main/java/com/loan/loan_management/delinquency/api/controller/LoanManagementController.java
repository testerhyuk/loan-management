package com.loan.loan_management.delinquency.api.controller;

import com.loan.loan_management.delinquency.api.dto.DelinquencyAssessmentRequest;
import com.loan.loan_management.delinquency.api.dto.DelinquencyAssessmentResponse;
import com.loan.loan_management.delinquency.application.DelinquencyAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delinquency-assessments")
@RequiredArgsConstructor
public class LoanManagementController {
    private final DelinquencyAssessmentService delinquencyAssessmentService;

    @PostMapping
    public ResponseEntity<DelinquencyAssessmentResponse> assessDelinquency(
            @Valid @RequestBody DelinquencyAssessmentRequest request
    ) {
        DelinquencyAssessmentResponse response = delinquencyAssessmentService.assessment(request);

        return ResponseEntity.ok().body(response);
    }
}
