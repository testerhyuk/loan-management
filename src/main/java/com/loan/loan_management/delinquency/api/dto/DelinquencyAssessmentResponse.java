package com.loan.loan_management.delinquency.api.dto;

import com.loan.loan_management.delinquency.domain.AssessmentResult;
import com.loan.loan_management.delinquency.domain.DelinquencyStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DelinquencyAssessmentResponse(
        String loanId,
        int installmentNumber,
        LocalDate assessmentDate,
        AssessmentResult result,
        BigDecimal overdueAmount,
        DelinquencyStatus loanDelinquencyStatus
) {
    public static DelinquencyAssessmentResponse toResponse(
            DelinquencyAssessmentRequest request,
            LocalDate assessmentDate,
            AssessmentResult result,
            BigDecimal overdueAmount,
            DelinquencyStatus loanDelinquencyStatus
    ) {
        return new DelinquencyAssessmentResponse(
                request.loanId(),
                request.installmentNumber(),
                assessmentDate,
                result,
                overdueAmount,
                loanDelinquencyStatus
        );
    }
}
